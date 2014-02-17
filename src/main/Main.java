package main;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
	public static void main(String[] args) throws InterruptedException {

		final SkypeConnector skypeConnector = new SkypeConnector();
		
		ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
		Runnable task = new Runnable() {
			public void run() {
				try {
					System.out.println("Start Processing!");
					skypeConnector.open();
					skypeConnector.processMessages();
					skypeConnector.close();
					System.out.println("End Processing!");
				} catch (ClassNotFoundException | IOException | SQLException e) {
					e.printStackTrace();
				}
				
			}
		};
		ses.scheduleAtFixedRate(task, 0, 5, TimeUnit.SECONDS);
	}
}
