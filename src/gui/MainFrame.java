package gui;

/**
 * MainFrame class - class which is implemented after running GUIApp.
 * Has 4 tabs - conference, hotels, participants, companies 
 * which are representing conference, hotels, participants, companies accordingly.
 */

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import main.RabbitMQSender;
import main.SkypeConnector;

@SuppressWarnings("serial")
public class MainFrame extends JFrame {
	private JButton btnStart;
	private JButton btnStop;
	private JTextField txfPeriod;
	private JTextField txfSkypeLocation;
	private JTextField txfHost;
	private JTextField txfPort;

	private ScheduledExecutorService ses;
	private ScheduledFuture<?> runningTask;
	private SkypeConnector skypeConnector;
	private RabbitMQSender rabbitMqSender;

	private int paddingTop = 10;
	private int paddingLeft = 10;

	public MainFrame() {
		this.setTitle("Skype message listener");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setLocation(100, 100);
		this.setSize(510, 140);
		this.setResizable(false);
		this.setFocusable(true);
		this.setLayout(null);

		ses = Executors.newScheduledThreadPool(1);

		JLabel label = new JLabel("Run every:");
		label.setSize(90, 25);
		label.setLocation(paddingLeft, paddingTop);
		this.add(label);

		txfPeriod = new JTextField();
		txfPeriod.setLocation(label.getX() + label.getWidth() + 10, paddingTop);
		txfPeriod.setSize(50, 25);
		txfPeriod.setText("5");
		this.add(txfPeriod);

		label = new JLabel("seconds");
		label.setSize(70, 25);
		label.setLocation(txfPeriod.getX() + txfPeriod.getWidth() + 10, paddingTop);
		this.add(label);

		btnStart = new JButton("Start");
		btnStart.setSize(90, 25);
		btnStart.setLocation(label.getX() + label.getWidth() + 40, paddingTop);
		this.add(btnStart);
		
		btnStop = new JButton("Stop");
		btnStop.setSize(90, 25);
		btnStop.setLocation(btnStart.getX() + btnStart.getWidth() + 30, paddingTop);
		btnStop.setEnabled(false);
		this.add(btnStop);

		label = new JLabel("Path to skype:");
		label.setSize(90, 25);
		label.setLocation(paddingLeft, btnStart.getY() + btnStart.getHeight() + 10);
		this.add(label);

		txfSkypeLocation = new JTextField();
		txfSkypeLocation.setSize(380, 25);
		txfSkypeLocation.setLocation(label.getX() + label.getWidth() + 10, btnStart.getY() + btnStart.getHeight() + 10);
		txfSkypeLocation.setText("/Users/stan/Library/Application Support/Skype/turtletrail");
		this.add(txfSkypeLocation);
		
		label = new JLabel("Host:");
		label.setSize(90, 25);
		label.setLocation(paddingLeft, txfSkypeLocation.getY() + txfSkypeLocation.getHeight() + 10);
		this.add(label);
		
		txfHost = new JTextField();
		txfHost.setSize(290, 25);
		txfHost.setLocation(label.getX() + label.getWidth() + 10, txfSkypeLocation.getY() + txfSkypeLocation.getHeight() + 10);
		txfHost.setText("localhost");
		this.add(txfHost);
		
		label = new JLabel("Port:");
		label.setSize(30, 25);
		label.setLocation(txfHost.getX()+txfHost.getWidth(), txfSkypeLocation.getY() + txfSkypeLocation.getHeight() + 10);
		this.add(label);
		
		txfPort = new JTextField();
		txfPort.setSize(50, 25);
		txfPort.setLocation(label.getX() + label.getWidth() + 10, txfSkypeLocation.getY() + txfSkypeLocation.getHeight() + 10);
		txfPort.setText("5672");
		this.add(txfPort);

		btnStart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					int period = Integer.parseInt(txfPeriod.getText());
					String location = checkLocation(txfSkypeLocation.getText());
					
					String host = txfHost.getText();
					int port = Integer.parseInt(txfPort.getText());
					
					skypeConnector = new SkypeConnector(location);
					rabbitMqSender = new RabbitMQSender(host,port);
					
					//update with current timestamp
					skypeConnector.open();
					skypeConnector.updateTimestamp((int)System.currentTimeMillis()/1000);
					skypeConnector.close();
					
					runningTask = ses.scheduleAtFixedRate(new Runnable() {
						public void run() {
							System.out.println("Start Processing!");
							try {
								skypeConnector.open();
								skypeConnector.processMessages(rabbitMqSender);
								skypeConnector.close();
							} catch (ClassNotFoundException | IOException | SQLException ex) {
								JOptionPane.showMessageDialog(null, ex.toString(), "Error", JOptionPane.ERROR_MESSAGE);
							}
							System.out.println("End Processing!");
						}
					}, 0, period, TimeUnit.SECONDS);
				} catch (NumberFormatException |  IOException | SQLException | ClassNotFoundException ex) {
					JOptionPane.showMessageDialog(null, ex.toString(), "Error", JOptionPane.ERROR_MESSAGE);
				}
				btnStop.setEnabled(true);
				btnStart.setEnabled(false);
			}
		});
		
		btnStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				runningTask.cancel(false);
				btnStop.setEnabled(false);
				btnStart.setEnabled(true);
			}
		});
	}

	public String checkLocation(String text) throws FileNotFoundException {
		File f = new File(text);
		if (!f.isDirectory()){
			throw new FileNotFoundException("Not a correct Skype dicretory");
		}
		return text;
	}
}
