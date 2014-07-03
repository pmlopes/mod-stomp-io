package com.jetdrone.vertx.mods.stomp;

import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class Frame {

    final String command;
    final Map<String, String> headers = new HashMap<>();
    String body;

    Frame(String command) {
        this.command = command.toUpperCase();
    }

    JsonObject toJSON() {
        JsonObject json = new JsonObject();

        if (headers.keySet().size() > 0) {
            JsonObject jHeaders = new JsonObject();
            for (Map.Entry<String, String> kv : this.headers.entrySet()) {
                jHeaders.putString(kv.getKey(), kv.getValue());
            }
            json.putObject("headers", jHeaders);
        }
        if (body != null) {
            String mapping = headers.get("transformation");
            if ("jms-map-json".equals(mapping)) {
                json.putObject("body", new JsonObject(body));
            } else {
                json.putString("body", body);
            }
        }

        return json;
    }

    void parseHeader(String key, String value) {
        headers.put(key, unescape(value));
    }

    void putHeader(String key, String value) {
        headers.put(key, value);
    }

    static String escape(String value) {
        return value.replaceAll("\\\\", "\\\\").replaceAll(":", "\\c").replaceAll("\n", "\\n").replaceAll("\r", "\\r");
    }

    static String unescape(String value) {
        return value.replaceAll("\\\\r", "\r").replaceAll("\\\\n", "\n").replaceAll("\\\\c", ":").replaceAll("\\\\\\\\", "\\\\");
    }
}
