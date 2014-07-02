package com.jetdrone.vertx.mods.stomp;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;

public class Generator {
    private static final String QUEUE_NAME = "tims_test_queue";

    public static void main(String[] args) throws IOException {

        int nMessages = args.length > 0 ? Integer.parseInt(args[0]) : 1;

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // Create a new durable channel (if one doesn't already exist)
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);

        // Send nMessages messages
        for (int msg = 1; msg <= nMessages; msg++) {
            String message = "Hello World! " + msg;
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
            System.out.println(" [x] Sent '" + message + "'");
        }

        channel.close();
        connection.close();
    }
}