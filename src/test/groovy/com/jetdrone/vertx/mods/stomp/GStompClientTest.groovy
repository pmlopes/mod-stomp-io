package com.jetdrone.vertx.mods.stomp

import org.junit.Test
import org.vertx.java.core.*
import org.vertx.java.core.eventbus.EventBus
import org.vertx.java.core.eventbus.Message
import org.vertx.java.core.json.JsonObject
import org.vertx.testtools.TestVerticle

import static org.vertx.testtools.VertxAssert.*

class GStompClientTest extends TestVerticle {

    private final String address = "test.stomp"
    private EventBus eb

    private void appReady() {
        super.start()
    }

    void start() {
        eb = vertx.eventBus()
        JsonObject config = new JsonObject()

        config.putString("address", address)

        container.deployModule(System.getProperty("vertx.modulename"), config, 1, new AsyncResultHandler<String>() {
            public void handle(AsyncResult<String> res) {
                appReady()
            }
        })
    }

    /**
     * Helper method to allow simple Groovy closure calls and simplified maps as json messages
     * @param json message
     * @param closure response handler
     */
    void stomp(Map json, boolean fail = false, Closure<Void> closure) {
        eb.send(address, new JsonObject(json), new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> reply) {
                if (fail) {
                    assertEquals("error", reply.body.getString("status"))
                } else {
                    assertEquals("ok", reply.body.getString("status"))
                }

                closure.call(reply)
            }
        })
    }

    @Test
    void testSend() {
        stomp([command: "send", destination: "/queue/FOO.BAR", body: "Hello, queue FOO.BAR"]) { reply0 ->
            testComplete()
        }
    }

    @Test
    void testSendSync() {
        stomp([command: "send", destination: "/queue/FOO.BAR", body: "Hello, queue FOO.BAR", sync: true]) { reply0 ->
            assertNotNull(reply0.body.getString("receipt"))
            testComplete()
        }
    }

    @Test
    void testSubscribe() {
        // register a handler for the incoming message
        eb.registerHandler("${address}/queue/pubsub", new Handler<Message<JsonObject>>() {
            @Override
            void handle(Message<JsonObject> received) {
                def value = received.body.getField('value')
                assertNotNull(value)
                testComplete()
            }
        });

        stomp([command: "send", destination: "/queue/pubsub", body: "Hello, queue pubsub"]) { reply0 ->
            stomp([command: "subscribe", destination: "/queue/pubsub"]) { reply1 ->
            }
        }
    }

    @Test
    void testUnSubscribe() {
        // register a handler for the incoming message
        eb.registerHandler("${address}/queue/unsub", new Handler<Message<JsonObject>>() {
            @Override
            void handle(Message<JsonObject> received) {
                def value = received.body.getField('value')
                assertNotNull(value)
                testComplete()
            }
        });

        stomp([command: "send", destination: "/queue/unsub", body: "Hello, queue unsub"]) { reply0 ->
            stomp([command: "subscribe", destination: "/queue/unsub"]) { reply1 ->
                def id = reply1.body.getString("id")

                // sleep 1 second to avoid receiving any old intransit messages
                vertx.setTimer(1000, new Handler<Long>() {
                    @Override
                    public void handle(Long event) {
                        stomp([command: "unsubscribe", id: id]) { reply2 ->
                            testComplete()
                        }
                    }
                });
            }
        }
    }
}
