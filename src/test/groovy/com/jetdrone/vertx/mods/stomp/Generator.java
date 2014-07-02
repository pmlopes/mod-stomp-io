package com.jetdrone.vertx.mods.stomp;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.io.IOException;

public class Generator {
    private static final String QUEUE_NAME = "tims_test_queue";

    public static void main(String[] args) throws IOException, JMSException {

        int nMessages = args.length > 0 ? Integer.parseInt(args[0]) : 1;

        // Getting JMS connection from the server and starting it
        ConnectionFactory factory = new ActiveMQConnectionFactory(ActiveMQConnection.DEFAULT_BROKER_URL);
        Connection connection = factory.createConnection();
        connection.start();

        // JMS messages are sent and received using a Session. We will
        // create here a non-transactional session object. If you want
        // to use transactions you should set the first parameter to 'true'
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        // Destination represents here our queue 'TESTQUEUE' on the
        // JMS server. You don't have to do anything special on the
        // server to create it, it will be created automatically.
        Destination destination = session.createQueue(QUEUE_NAME);

        // MessageProducer is used for sending messages (as opposed
        // to MessageConsumer which is used for receiving them)
        MessageProducer producer = session.createProducer(destination);

        // Send nMessages messages
        for (int msg = 1; msg <= nMessages; msg++) {
            TextMessage message = session.createTextMessage("Hello World! " + msg);
            // Here we are sending the message!
            producer.send(message);
            System.out.println(" [x] Sent '" + message + "'");
        }

        connection.close();
    }
}
