package com.jetdrone.vertx.mods.stomp;

import com.jetdrone.vertx.mods.stomp.impl.FrameHandler;
import org.junit.Test;
import org.vertx.java.core.buffer.Buffer;

import static org.junit.Assert.*;

public class FrameParserTest {

    @Test
    public void testHandle() throws Exception {
        FrameParser parser = new FrameParser(new FrameHandler() {
            @Override
            public void handleFrame(Frame frame) {
                System.out.println(frame.toJSON());
            }
        });

        parser.handle(new Buffer("CONNECTED\n" +
                "session:session-3xQIK-sMw--yOf39QSPQig\n" +
                "heart-beat:10000,10000\n" +
                "server:RabbitMQ/3.0.4\n" +
                "version:1.2\n" +
                "\n" +
                "\0"));
        parser.handle(new Buffer("MESSAGE\n" +
                "subscription:6778f46b-62c8-f50f-ded1-490f515e0467\n" +
                "destination:/queue/tims_test_queue\n" +
                "message-id:T_6778f46b-62c8-f50f-ded1-490f515e0467@@session-3xQIK-sMw--yOf39QSPQig@@1\n" +
                "content-length:14\n" +
                "\n" +
                "Hello World! 1\0MESSAGE\n" +
                "subscription:6778f46b-62c8-f50f-ded1-490f515e0467\n" +
                "destination:/queue/tims_test_queue\n" +
                "message-id:T_6778f46b-62c8-f50f-ded1-490f515e0467@@session-3xQIK-sMw--yOf39QSPQig@@2\n" +
                "content-length:14\n" +
                "\n" +
                "Hello World! 2\0MESSAGE\n" +
                "subscription:6778f46b-62c8-f50f-ded1-490f515e0467\n" +
                "destination:/queue/tims_test_queue\n" +
                "message-id:T_6778f46b-62c8-f50f-ded1-490f515e0467@@session-3xQIK-sMw--yOf39QSPQig@@3\n" +
                "content-length:14\n" +
                "\n" +
                "Hello World! 3\0MESSAGE\n" +
                "subscription:6778f46b-62c8-f50f-ded1-490f515e0467\n" +
                "destination:/queue/tims_test_queue\n" +
                "message-id:T_6778f46b-62c8-f50f-ded1-490f515e0467@@session-3xQIK-sMw--yOf39QSPQig@@4\n" +
                "content-length:14\n" +
                "\n" +
                "Hello World! 4\0MESSAGE\n" +
                "subscription:6778f46b-62c8-f50f-ded1-490f515e0467\n" +
                "destination:/queue/tims_test_queue\n" +
                "message-id:T_6778f46b-62c8-f50f-ded1-490f515e0467@@session-3xQIK-sMw--yOf39QSPQig@@"));
        parser.handle(new Buffer("5\n" +
                "content-length:14\n" +
                "\n" +
                "Hello World! 5\0MESSAGE\n" +
                "subscription:6778f46b-62c8-f50f-ded1-490f515e0467\n" +
                "destination:/queue/tims_test_queue\n" +
                "message-id:T_6778f46b-62c8-f50f-ded1-490f515e0467@@session-3xQIK-sMw--yOf39QSPQig@@6\n" +
                "content-length:14\n" +
                "\n" +
                "Hello World! 6\0MESSAGE\n" +
                "subscription:6778f46b-62c8-f50f-ded1-490f515e0467\n" +
                "destination:/queue/tims_test_queue\n" +
                "message-id:T_6778f46b-62c8-f50f-ded1-490f515e0467@@session-3xQIK-sMw--yOf39QSPQig@@7\n" +
                "content-length:14\n" +
                "\n" +
                "Hello World! 7\0MESSAGE\n" +
                "subscription:6778f46b-62c8-f50f-ded1-490f515e0467\n" +
                "destination:/queue/tims_test_queue\n" +
                "message-id:T_6778f46b-62c8-f50f-ded1-490f515e0467@@session-3xQIK-sMw--yOf39QSPQig@@8\n" +
                "content-length:14\n" +
                "\n" +
                "Hello World! 8\0MESSAGE\n" +
                "subscription:6778f46b-62c8-f50f-ded1-490f515e0467\n" +
                "destination:/queue/tims_test_queue\n" +
                "message-id:T_6778f46b-62c8-f50f-ded1-490f515e0467@@session-3xQIK-sMw--yOf39QSPQig@@9\n" +
                "content-length:14\n" +
                "\n" +
                "Hello World! 9\0MESSAGE\n" +
                "subscription:6778f46b-62c8-f50f-ded1-490f515e0467\n" +
                "destination:/queue/tims_test_queue\n" +
                "message-id:T_6778f46b-62c8-f50f-ded1-490f515e04"));
        parser.handle(new Buffer("67@@session-3xQIK-sMw--yOf39QSPQig@@10\n" +
                "content-length:15\n" +
                "\n" +
                "Hello World! 10\0"));
        parser.handle(new Buffer("\n"));
    }

    @Test
    public void testHandle2() throws Exception {
        FrameParser parser = new FrameParser(new FrameHandler() {
            @Override
            public void handleFrame(Frame frame) {
                System.out.println(frame.toJSON());
            }
        });

        parser.handle(new Buffer("MESSAGE\n" +
                "subscription:6778f46b-62c8-f50f-ded1-490f515e0467\n" +
                "destination:/queue/tims_test_queue\n" +
                "message-id:T_6778f46b-62c8-f50f-ded1-490f515e0467@@session-3xQIK-sMw--yOf39QSPQig@@1\n" +
                "content-length:14\n" +
                "\n" +
                "Hello World! 1\0MESSAGE\n" +
                "subscription:6778f46b-62c8-f50f-ded1-490f515e0467\n" +
                "destination:/queue/tims_test_queue\n" +
                "message-id:T_6778f46b-62c8-f50f-ded1-490f515e0467@@session-3xQIK-sMw--yOf39QSPQig@@2\n" +
                "content-length:14\n" +
                "\n" +
                "He"));
        parser.handle(new Buffer("llo World! 2\0MESSAGE\n" +
                "subscription:6778f46b-62c8-f50f-ded1-490f515e0467\n" +
                "destination:/queue/tims_test_queue\n" +
                "message-id:T_6778f46b-62c8-f50f-ded1-490f515e0467@@session-3xQIK-sMw--yOf39QSPQig@@3\n" +
                "content-length:14\n" +
                "\n" +
                "Hello World! 3\0MESSAGE\n" +
                "subscription:6778f46b-62c8-f50f-ded1-490f515e0467\n" +
                "destination:/queue/tims_test_queue\n" +
                "message-id:T_6778f46b-62c8-f50f-ded1-490f515e0467@@session-3xQIK-sMw--yOf39QSPQig@@4\n" +
                "content-length:14\n" +
                "\n" +
                "Hello World! 4\0"));
    }

    @Test
    public void testHandler3() {

        String s = "MESSAGE\n" +
                "subscription:27304a35-5e0d-bee9-bb18-4e39992c448c\n" +
                "destination:/queue/tims_test_queue\n" +
                "message-id:T_27304a35-5e0d-bee9-bb18-4e39992c448c@@session-DYAi1VbOHO3KhFe-vg8fPA@@11\n" +
                "content-length:15\n" +
                "\n" +
                "Hello World! 11\u0000MESSAGE\n" +
                "subscription:27304a35-5e0d-bee9-bb18-4e39992c448c\n" +
                "destination:/queue/tims_test_queue\n" +
                "message-id:T_27304a35-5e0d-bee9-bb18-4e39992c448c@@session-DYAi1VbOHO3KhFe-vg8fPA@@12\n" +
                "content-length:15\n" +
                "\n" +
                "Hello World! 12\u0000MESSAGE\n" +
                "subscription:27304a35-5e0d-bee9-bb18-4e39992c448c\n" +
                "destination:/queue/tims_test_queue\n" +
                "message-id:T_27304a35-5e0d-bee9-bb18-4e39992c448c@@session-DYAi1VbOHO3KhFe-vg8fPA@@13\n" +
                "content-length:15\n" +
                "\n" +
                "Hello World! 13\u0000MESSAGE\n";

        Buffer b = new Buffer(s.substring(642));

        FrameParser parser = new FrameParser(new FrameHandler() {
            @Override
            public void handleFrame(Frame frame) {
                System.out.println(frame.toJSON());
            }
        });

        parser.handle(b);
    }

    @Test
    public void testHandler4() {

        String s0 = "MESSAGE\n" +
                "subscription:27304a35-5e0d-bee9-bb18-4e39992c448c\n" +
                "destination:/queue/tims_test_queue\n" +
                "message-id:T_27304a35-5e0d-bee9-bb18-4e39992c448c@@session-DYAi1VbOHO3KhFe-vg8fPA@@11\n" +
                "content-length:15\n" +
                "\n";

        String s1 =
                "Hello World! 11\u0000MESSAGE\n" +
                "subscription:27304a35-5e0d-bee9-bb18-4e39992c448c\n" +
                "destination:/queue/tims_test_queue\n" +
                "message-id:T_27304a35-5e0d-bee9-bb18-4e39992c448c@@session-DYAi1VbOHO3KhFe-vg8fPA@@12\n" +
                "content-length:15\n" +
                "\n" +
                "Hello World! 12\u0000";

        FrameParser parser = new FrameParser(new FrameHandler() {
            @Override
            public void handleFrame(Frame frame) {
                System.out.println(frame.toJSON());
            }
        });

        parser.handle(new Buffer(s0));
        parser.handle(new Buffer(s1));
    }
}