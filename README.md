Stomp busmod for Vert.x
=============================

This module allows communcation between Message brokers such as ActiveMQ, RabbitMQ Apache Apollo.

Support the development of this module
--------------------------------------

[![Click here to lend your support to: Support com.jetdrone Vert.x modules and make a donation at www.pledgie.com !](http://www.pledgie.com/campaigns/19785.png?skin_name=chrome)](http://www.pledgie.com/campaigns/19785)


Quick and Dirty Guide:
--------------------------------------

First deploy the module:

    eb = vertx.eventBus()
    container.deployModule('com.jetdrone~mod-stomp-io~1.0.0', [address: 'my-address'], 1)

Second send/receive messages:

    eb.send(address, [command: "send", destination: "/queue/FOO.BAR", body: "Hello, queue FOO.BAR"]) { reply ->
        if (reply.body.status == 'OK') {
            // ...
        }
    }

    // sync messages (wait for ACK)
    eb.send(address, [command: "send", destination: "/queue/FOO.BAR", body: "Hello, queue FOO.BAR", sync: true]) { reply ->
        if (reply.body.status == 'OK') {
            // ...
        }
    }
    
Work pub/sub mode:

        // register a handler for the incoming message
        eb.registerHandler("${address}/queue/unsub", new Handler<Message<JsonObject>>() {
            @Override
            void handle(Message<JsonObject> received) {
                def value = received.body.getField('value')
                assertNotNull(value)
                testComplete()
            }
        });

        eb.send(address, [command: "send", destination: "/queue/unsub", body: "Hello, queue unsub"]) { reply0 ->
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
