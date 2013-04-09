package com.jetdrone.vertx.mods.stomp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetClient;

import java.io.IOException;
import java.util.*;

/**
 * STOMP StompClient Class
 *
 *All STOMP protocol is exposed as methods of this class (`connect()`, `send()`, etc.)
 */
public class StompClient {

    private static class Heartbeat {
        int sx;
        int sy;

        static Heartbeat parse(String header) {
            String[] token = header.split(",");
            Heartbeat beat = new Heartbeat();
            beat.sx = Integer.parseInt(token[0]);
            beat.sy = Integer.parseInt(token[1]);

            return beat;
        }

        @Override
        public String toString() {
            return sx + "," + sy;
        }
    }

    private final Heartbeat heartbeat = new Heartbeat();
    private final Queue<Handler<Frame>> replies = new LinkedList<>();
    private final StompSubscriptions subscriptions;

    private long pinger;
    private long ponger;
    private long serverActivity;

    private final Vertx vertx;
    private final Logger logger;
    private final String host;
    private final int port;
    private final String login;
    private final String passcode;

    private NetSocket netSocket;

    private static enum State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    private State state = State.DISCONNECTED;

    private static String getSupportedVersions() {
        return Protocol.V1_2.version + "," + Protocol.V1_1.version + "," + Protocol.V1_0.version;
    }

    public StompClient(Vertx vertx, Logger logger, String host, int port, String login, String passcode, StompSubscriptions subscriptions) {
        this.vertx = vertx;
        this.logger = logger;
        this.host = host;
        this.port = port;
        this.login = login;
        this.passcode = passcode;
        this.subscriptions = subscriptions;
    }


    public void connect(final AsyncResultHandler<Void> resultHandler) {
        if (state == State.DISCONNECTED) {
            state = State.CONNECTING;
            NetClient client = vertx.createNetClient();
            client.exceptionHandler(new Handler<Exception>() {
                public void handle(Exception e) {
                    logger.error("Net client error", e);
                    if (resultHandler != null) {
                        AsyncResult<Void> asyncResult = new AsyncResult<>();
                        asyncResult.setFailure(e);
                        resultHandler.handle(asyncResult);
                    }
                    disconnect();
                }
            });
            client.connect(port, host, new Handler<org.vertx.java.core.net.NetSocket>() {
                @Override
                public void handle(org.vertx.java.core.net.NetSocket socket) {
                    state = State.CONNECTED;
                    netSocket = socket;
                    init(netSocket);
                    socket.exceptionHandler(new Handler<Exception>() {
                        public void handle(Exception e) {
                            logger.error("Socket client error", e);
                            disconnect();
                        }
                    });
                    socket.closedHandler(new Handler<Void>() {
                        public void handle(Void arg0) {
                            logger.info("Socket closed");
                            disconnect();
                        }
                    });
                    if (resultHandler != null) {
                        AsyncResult<Void> asyncResult = new AsyncResult<>();
                        asyncResult.setResult(null);
                        resultHandler.handle(asyncResult);
                    }
                }
            });
        }
    }

    /**
     * Clean up client resources when it is disconnected or the server did not
     * send heart beats in a timely fashion
     */
    private void disconnect() {
        state = State.DISCONNECTED;
        if (pinger != 0) {
            vertx.cancelTimer(pinger);
        }
        if (ponger != 0) {
            vertx.cancelTimer(ponger);
        }
        // make sure the socket is closed
        if (netSocket != null) {
            netSocket.close();
        }
    }

    void send(final Frame frame, final Handler<Frame> replyHandler) {
        switch (state) {
            case CONNECTED:
                netSocket.write(frame.command);
                netSocket.write("\n");

                for (Map.Entry<String, String> entry : frame.headers.entrySet()) {
                    String value = entry.getValue();
                    if (value != null) {
                        netSocket.write(entry.getKey());
                        netSocket.write(":");
                        netSocket.write(entry.getValue());
                        netSocket.write("\n");
                    }
                }

                if (frame.body != null) {
                    netSocket.write("content-length:");
                    netSocket.write(Integer.toString(frame.body.length()));
                    netSocket.write("\n");
                }

                netSocket.write("\n");
                if (frame.body != null) {
                    netSocket.write(frame.body);
                }

                netSocket.write("\0");
                if (replyHandler != null) {
                    replies.offer(replyHandler);
                }
                break;
            case DISCONNECTED:
                logger.info("Got request when disconnected. Trying to connect.");
                connect(new AsyncResultHandler<Void>() {
                    public void handle(AsyncResult<Void> connection) {
                        if (connection.succeeded()) {
                            send(frame, replyHandler);
                        } else {
                            replyHandler.handle(new Frame("ERROR", null, "Unable to connect"));
                        }
                    }
                });
                break;
            case CONNECTING:
                logger.debug("Got send request while connecting. Will try again in a while.");
                vertx.setTimer(100, new Handler<Long>() {
                    public void handle(Long event) {
                        send(frame, replyHandler);
                    }
                });
        }
    }


    private void init(NetSocket netSocket) {
        this.netSocket = netSocket;
        final StompDecoder stompDecoder = new StompDecoder();
        // send heartbeat every 10s by default (value is in ms)
        heartbeat.sx = 10000;
        // expect to receive server heartbeat at least every 10s by default (value in ms)
        heartbeat.sy = 10000;
        // setup the handlers
        netSocket.dataHandler(new Handler<Buffer>() {
            private ByteBuf read = null;

            @Override
            public void handle(Buffer buffer) {
                System.out.println("<<<" + buffer.toString("UTF-8").replaceAll("\0", "^@"));
                serverActivity = System.currentTimeMillis();
                // Should only get one callback at a time, no sychronization necessary
                ByteBuf byteBuf = buffer.getByteBuf();

                if (read == null) {
                    // PONG
                    while (byteBuf.isReadable() && byteBuf.getByte(byteBuf.readerIndex()) == '\n') {
                        byteBuf.skipBytes(1);
                    }

                    if (!byteBuf.isReadable()) {
                        return;
                    }
                } else {
                    // Merge the new buffer with the previous buffer
                    byteBuf = Unpooled.copiedBuffer(read, byteBuf);
                    read = null;
                }

                try {
                    // Attempt to decode a full reply from the channelbuffer
                    Frame receive = stompDecoder.receive(byteBuf);
                    // If successful, grab the matching handler
                    handleReply(receive);
                    // May be more to read
                    if (byteBuf.isReadable()) {
                        // More than one message in the buffer, need to be careful
                        handle(new Buffer(Unpooled.copiedBuffer(byteBuf)));
                    }
                } catch (IOException e) {
                    logger.error("Error receiving data", e);
                    disconnect();
                } catch (IndexOutOfBoundsException th) {
                    th.printStackTrace();
                    // Got to catch decoding fails and try it again
                    byteBuf.resetReaderIndex();
                    read = Unpooled.copiedBuffer(byteBuf);
                }
            }
        });
        // perform the connect command
        logger.debug("Socket Opened...");
        Map<String, String> headers = new HashMap<>();
        headers.put("accept-version", getSupportedVersions());
        headers.put("heart-beat", heartbeat.toString());
        headers.put("vhost", host);
        if (login != null) {
            headers.put("login", login);
        }
        if (passcode != null) {
            headers.put("passcode", passcode);
        }

        send(new Frame("CONNECT", headers, null), new Handler<Frame>() {
            @Override
            public void handle(Frame frame) {
                logger.debug("connected to server " + frame.headers.get("server"));
                // connected = true
                setupHeartbeat(frame.headers);
            }
        });
    }

    private void setupHeartbeat(Map<String, String> headers) {
        if (headers.get("version").equals(Protocol.V1_0.version)) {
            return;
        }

        // heart-beat header received from the server looks like:
        //
        //     heart-beat: sx, sy
        Heartbeat heartbeat = Heartbeat.parse(headers.get("heart-beat"));

        if (this.heartbeat.sx != 0 && heartbeat.sy != 0) {
            final int ttl = Math.max(this.heartbeat.sx, heartbeat.sy);
            logger.debug("send PING every " + ttl + "ms");
            this.pinger = vertx.setPeriodic(ttl, new Handler<Long>() {
                @Override
                public void handle(Long event) {
                    if (state == State.CONNECTED) {
                        logger.debug("PING");
                        netSocket.write("\n");
                    }
                }
            });
        }

        if (this.heartbeat.sy != 0 && heartbeat.sx != 0) {
            final int ttl = Math.max(this.heartbeat.sy, heartbeat.sx);
            logger.debug("check PONG every " + ttl + "ms");
            ponger = vertx.setPeriodic(ttl, new Handler<Long>() {
                @Override
                public void handle(Long event) {
                    long delta = System.currentTimeMillis() - serverActivity;
                    // We wait twice the TTL to be flexible on window's setInterval calls
                    if (delta > ttl * 2) {
                        logger.debug("did not receive server activity for the last " + delta + "ms");
                        disconnect();
                    }
                }
            });
        }
    }

    void handleReply(Frame reply) throws IOException {
        if ("ERROR".equals(reply.command)) {
            logger.error(reply.body);
            disconnect();
            return;
        }

        Handler<Frame> handler = replies.poll();

        if (handler != null) {
            // handler waits for this response
            handler.handle(reply);
            return;
        }

        // this is a subscribe message
        if ("MESSAGE".equals(reply.command)) {
            handler = subscriptions.getHandler(reply.headers.get("subscription"));
            if (handler != null) {
                // pub sub handler exists
                handler.handle(reply);
                return;
            }

        }

        throw new IOException("Received a non MESSAGE while in SUBSCRIBE mode");
    }
}
