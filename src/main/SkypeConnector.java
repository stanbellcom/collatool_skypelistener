package main;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;

public class SkypeConnector {

	private Connection conSkype;
	private Connection conCollatool;
	private Statement stmtSkype;
	private Statement stmtCollatool;

	private String dbPathSkype;
	private String dbPathSkypeCopy;
	private String dbPathCollatool;

	public SkypeConnector(String skypePath) {
		this.dbPathSkype = skypePath + "/main.db";
		this.dbPathSkypeCopy = skypePath + "/main_copy.db";
		this.dbPathCollatool = skypePath + "/collatool.db";
	}

	public void open() throws IOException, ClassNotFoundException, SQLException {

		// init/opening
		Class.forName("org.sqlite.JDBC");
		conSkype = DriverManager.getConnection("jdbc:sqlite:" + dbPathSkype);
		stmtSkype = conSkype.createStatement();

		// add check for database is locked
		try {
			stmtSkype.executeQuery("select 1 from messages;");
		} catch (SQLException e) {
			if (e.getErrorCode() == 0) {// database busy
				FileUtils.copyFile(new File(dbPathSkype), new File(dbPathSkypeCopy));
				System.out.println("Skype copy file created");
				conSkype = DriverManager.getConnection("jdbc:sqlite:" + dbPathSkypeCopy);
				stmtSkype = conSkype.createStatement();
			}
		}

		conCollatool = DriverManager.getConnection("jdbc:sqlite:" + dbPathCollatool);
		stmtCollatool = conCollatool.createStatement();

		stmtCollatool.executeUpdate("CREATE TABLE IF NOT EXISTS last_message_timestamp(timestamp INT NOT NULL);");
	}

	@SuppressWarnings("unchecked")
	public void processMessages(RabbitMQSender rabbitSender) throws SQLException, IOException {
		int lastTimestamp = stmtCollatool.executeQuery("SELECT MAX(timestamp) FROM last_message_timestamp;").getInt(1);

		String query = "SELECT * FROM Messages";
		if (lastTimestamp != 0)
			query += " WHERE timestamp > " + lastTimestamp;
		query += " GROUP BY timestamp limit 500";
		ResultSet rs = stmtSkype.executeQuery(query);
		int timestamp = 0;
		while (rs.next()) {
			int id = rs.getInt("id");
			String author = rs.getString("author");
			String receiver = rs.getString("dialog_partner");
			String body = rs.getString("body_xml");
			timestamp = rs.getInt("timestamp");

			JSONObject obj = new JSONObject();
			obj.put("id", id);
			obj.put("sender", author);
			obj.put("receiver", receiver);
			obj.put("date", timestamp);
			obj.put("body", body);

			rabbitSender.sendMessage(obj);
		}
		if (timestamp != 0)
			updateTimestamp(timestamp);

		rs.close();
	}

	public void close() throws SQLException, IOException {
		stmtSkype.close();
		stmtCollatool.close();
		conSkype.close();
		conCollatool.close();

		File file = new File(dbPathSkypeCopy);

		if (file.delete()) {
			System.out.println("Skype copy file deleted");
		}
	}
	
	public void updateTimestamp(int timestamp) throws SQLException{
		stmtCollatool.executeUpdate("INSERT INTO last_message_timestamp(timestamp) values(" + timestamp + ");");
	}
}