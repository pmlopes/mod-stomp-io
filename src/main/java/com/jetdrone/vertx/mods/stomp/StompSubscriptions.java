package com.jetdrone.vertx.mods.stomp;

import org.vertx.java.core.Handler;

import java.util.HashMap;
import java.util.Map;

public class StompSubscriptions {

    private final Map<String, Handler<Frame>> subscribers = new HashMap<>();

    public void registerSubscribeHandler(String id, Handler<Frame> replyHandler) {
        subscribers.put(id, replyHandler);
    }

    public void unregisterSubscribeHandler(String id) {
        if (id == null) {
            subscribers.clear();
        } else {
            subscribers.remove(id);
        }
    }

    public Handler<Frame> getHandler(String id) {
        return subscribers.get(id);
    }
}
