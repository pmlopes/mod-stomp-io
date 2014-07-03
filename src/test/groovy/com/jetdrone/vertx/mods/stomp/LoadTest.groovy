package com.jetdrone.vertx.mods.stomp

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import org.junit.Test
import org.vertx.groovy.core.eventbus.EventBus
import org.vertx.java.core.AsyncResult
import org.vertx.java.core.AsyncResultHandler
import org.vertx.java.core.json.JsonObject
import org.vertx.testtools.TestVerticle

import static org.vertx.testtools.VertxAssert.*

class LoadTest extends TestVerticle {

    private static final String QUEUE_NAME = "tims_test_queue"

    private final String address = "test.stomp"
    private EventBus eb

    private void appReady() {
        super.start()
    }

    void start() {
        eb = new EventBus(vertx.eventBus())
        JsonObject config = new JsonObject()

        config.putString("address", address)

        container.deployModule(System.getProperty("vertx.modulename"), config, 1, new AsyncResultHandler<String>() {
            public void handle(AsyncResult<String> res) {
                appReady()
            }
        })
    }

    private static void generate(int nMessages) throws IOException {

        ConnectionFactory factory = new ConnectionFactory()
        factory.setHost("localhost")
        Connection connection = factory.newConnection()
        Channel channel = connection.createChannel()

        // Create a new durable channel (if one doesn't already exist)
        channel.queueDeclare(QUEUE_NAME, true, false, false, null)

        // Send nMessages messages
        for (int msg = 1; msg <= nMessages; msg++) {
            String message = "Hello World! " + msg
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes())
        }

        channel.close();
        connection.close();
    }


    @Test
    void testIssue6() {
        final int total_messages = 50000;
        int counter = 0;

        eb.registerHandler("$address/queue/$QUEUE_NAME") { message ->
            if (++counter == total_messages) {
                testComplete()
            }
        }

        eb.send(address, [command: 'subscribe', destination: "/queue/tims_test_queue"]) { reply0 ->
            generate(total_messages)
        }
    }
}
