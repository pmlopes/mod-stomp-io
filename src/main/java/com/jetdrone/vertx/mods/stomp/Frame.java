package com.jetdrone.vertx.mods.stomp;

import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

class Frame {

    final String command;
    final Map<String, String> headers;
    String body;

    Frame(String command, Map<String, String> headers, String body) {
        this.command = command;
        this.headers = headers;
        this.body = body;
    }

    Frame(String command) {
        this.command = command.toUpperCase();
        this.headers = new HashMap<>();
    }

    JsonObject toJSON() {
        JsonObject json = new JsonObject();

        if (headers != null && headers.keySet().size() > 0) {
            JsonObject jHeaders = new JsonObject();
            for (Map.Entry<String, String> kv : this.headers.entrySet()) {
                jHeaders.putString(kv.getKey(), kv.getValue());
            }
            json.putObject("headers", jHeaders);
        }
        if (body != null && body.length() > 2 && body.charAt(0) == '\0' && body.charAt(1) == '\f') {
            json.putBinary("body", body.getBytes());
        } else {
            json.putString("body", body);
        }

        return json;
    }

    /**
     * Unmarshall a single STOMP frame from a `data` string
     * TODO: unescape
     */
    static Frame unmarshall(String data) {
        // search for 2 consecutives LF byte to split the command
        // and headers from the body
        int divider = data.indexOf("\n\n");
        String[] headerLines = data.substring(0, divider).split("\\n");
        String command = headerLines[0];
        Map<String, String> headers = new HashMap<>();
        // Parse headers
        for (int i = 1; i < headerLines.length; i++) {
            String line = headerLines[i];
            int idx = line.indexOf(":");

            String key = line.substring(0, idx);
            String value = line.substring(idx + 1);
            // utility function to trim any whitespace before and after a string
            key = key.replaceAll("^\\s+|\\s+$", "");
            value = value.replaceAll("^\\s+|\\s+$", "");
            headers.put(key, value);
        }

        // Parse body
        // check for content-length or  topping at the first NULL byte found.
        String body = "";
        // skip the 2 LF bytes that divides the headers from the body
        int start = divider + 2;

        if (headers.get("content-length") != null) {
            int len = Integer.parseInt(headers.get("content-length"));
            body = data.substring(start, start + len);
        } else {
            int end = data.length();

            for (int i = start; i < end; i++) {
                if (data.charAt(i) == '\0') {
                    end = i;
                    break;
                }
            }

            body = data.substring(start, end);
        }
        return new Frame(command, headers, body);
    }

    /**
     * Marshall a Stomp frame
     */
    static String marshall(String command, Map<String, String> headers, String body) {
        Frame frame = new Frame(command, headers, body);
        return frame.toString() + "\0";
    }
}
