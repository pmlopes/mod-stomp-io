package com.jetdrone.vertx.mods.stomp

import org.junit.Test
import org.vertx.groovy.core.eventbus.EventBus
import org.vertx.java.core.AsyncResult
import org.vertx.java.core.AsyncResultHandler
import org.vertx.java.core.json.JsonObject
import org.vertx.testtools.TestVerticle

import static org.vertx.testtools.VertxAssert.*

class CrashTest extends TestVerticle {

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

    @Test
    void testCrash() {
        testComplete()
//        eb.registerHandler("$address/queue/tims_test_queue") { message ->
//            println "Message Received ${message.body.value.body}"
//        }
//
//        eb.send(address, [command: 'subscribe', destination: "/queue/tims_test_queue"]) { reply0 ->
//            println "OK"
//        }
    }
}
