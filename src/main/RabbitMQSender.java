package main;

import java.io.IOException;

import org.json.simple.JSONObject;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQSender {

	//final String HOST = "localhost";
	//final int PORT = 5672;
	final String EXCHANGE_NAME = "msg_exchange";
	final String ROUTING_KEY = "im.skype";

	private Connection connection;
	private Channel channel;

	public RabbitMQSender(String host, int port) throws IOException {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(host);
		factory.setPort(port);
		connection = factory.newConnection();
		channel = connection.createChannel();

		channel.exchangeDeclare(EXCHANGE_NAME, "topic");
	}

	public void sendMessage(JSONObject message) throws IOException {
		channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, null, message.toJSONString().getBytes());
	}

	public void close() throws IOException {
		connection.close();
	}
}
