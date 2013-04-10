package com.jetdrone.vertx.mods.stomp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufIndexFinder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.nio.charset.Charset;

public class StompDecoder extends ReplayingDecoder<Void> {

    static final Charset UTF8 = Charset.forName("UTF-8");

    enum State {
        HEADERS,
        BODY,
        EOF
    }

    State state = State.EOF;

    @Override
    public void checkpoint() {
        if (internalBuffer() != null) {
            super.checkpoint();
        }
    }

    public Frame receive(ByteBuf in) {

        Frame frame = null;

        while(in.isReadable()) {
            switch (state) {
                case HEADERS:
                    ByteBuf linebuf = in.readBytes(in.bytesBefore(ByteBufIndexFinder.LF));
                    // skip LF
                    in.skipBytes(1);

                    if (linebuf.readableBytes() != 0) {
                        String line = linebuf.toString(UTF8);

                        if (frame == null) {
                            //System.out.println("Received command: " + line);
                            frame = new Frame(line);
                        } else {
                            //System.out.println("Received header: " + line);
                            // add header
                            int idx = line.indexOf(":");

                            String key = line.substring(0, idx);
                            String value = line.substring(idx + 1);
                            // utility function to trim any whitespace before and after a string
                            key = key.replaceAll("^\\s+|\\s+$", "");
                            value = value.replaceAll("^\\s+|\\s+$", "");
                            frame.parseHeader(key, value);
                        }
                        break;
                    }
                    state = State.BODY;
                    break;
                case BODY:
                    String contentLength = frame.headers.get("content-length");
                    int read = -1;

                    if (contentLength != null) {
                        read = Integer.parseInt(contentLength);
                    }

                    ByteBuf body;

                    //System.out.println("Content-Length: " + read);

                    if (read == -1) {
                        body = in.readBytes(in.bytesBefore(ByteBufIndexFinder.NUL));
                    } else {
                        body = in.readBytes(read);
                    }
                    in.skipBytes(1);
                    frame.body = body.toString(UTF8);
                    //System.out.println("Read body: " + frame.body);
                    state = State.EOF;
                    break;
                case EOF:
                    if (in.getByte(in.readerIndex()) == '\n') {
                        in.skipBytes(1);
                        //System.out.println("Skiping PONG");
                        break;
                    }

                    // There is more than 1 frame in this buffer
                    if (frame != null) {
                        //System.out.println("Parse complete!");
                        return frame;
                    }

                    state = State.HEADERS;
                    break;
            }
        }

        //System.out.println("Parse complete!");
        return frame;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        return receive(in);
    }
}
