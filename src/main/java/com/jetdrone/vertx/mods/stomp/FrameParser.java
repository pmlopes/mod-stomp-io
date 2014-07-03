package com.jetdrone.vertx.mods.stomp;

import io.netty.buffer.ByteBuf;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;

public class FrameParser implements Handler<Buffer> {

    private Buffer _buffer;
    private int _offset;
    private final String _encoding = "utf-8";

    enum State {
        HEADERS,
        BODY,
        EOF
    }

//    private final ReplyHandler client;
//
//    public FrameParser(ReplyHandler client) {
//        this.client = client;
//    }


    private Frame parseResult() throws ArrayIndexOutOfBoundsException {
        int start, end, offset;
        int packetSize;

        State state = State.EOF;
        Frame frame = null;

        while (bytesRemaining() > 0) {
            switch (state) {
                case HEADERS:
                    end = packetEndOffset((byte) '\n');
                    start = _offset;

                    // include the delimiter
                    _offset = end + 1;

                    String line = _buffer.getString(start, end, _encoding);

                    if (line.length() > 0) {
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
                    } else {
                        if (frame != null) {
                            state = State.BODY;
                        }
                    }

                    break;
                case BODY:
                    String contentLength = frame.headers.get("content-length");
                    int read = -1;

                    if (contentLength != null) {
                        read = Integer.parseInt(contentLength);
                    }

                    String body;

                    //System.out.println("Content-Length: " + read);

                    if (read == -1) {
                        end = packetEndOffset((byte) '\0');
                        start = _offset;
                    } else {
                        start = _offset;
                        end = start + read;
                    }

                    body = _buffer.getString(start, end, _encoding);

                    // include the delimiter
                    _offset = end + 1;

                    frame.body = body;

                    //System.out.println("Read body: " + frame.body);
                    state = State.EOF;
                    break;
                case EOF:
                    if (read() == '\n') {
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

//        if (type == '+' || type == '-') {
//            // up to the delimiter
//            end = packetEndOffset() - 1;
//            start = _offset;
//
//            // include the delimiter
//            _offset = end + 2;
//
//            if (end > _buffer.length()) {
//                _offset = start;
//                throw new ArrayIndexOutOfBoundsException("Wait for more data.");
//            }
//
//            if (type == '+') {
//                return new Reply(type, _buffer.getString(start, end, _encoding));
//            } else {
//                return new Reply(type, _buffer.getString(start, end, _encoding));
//            }
//        } else if (type == ':') {
//            // up to the delimiter
//            end = packetEndOffset() - 1;
//            start = _offset;
//
//            // include the delimiter
//            _offset = end + 2;
//
//            if (end > _buffer.length()) {
//                _offset = start;
//                throw new ArrayIndexOutOfBoundsException("Wait for more data.");
//            }
//
//            // return the coerced numeric value
//            return new Reply(type, Long.parseLong(_buffer.getString(start, end)));
//        } else if (type == '$') {
//            // set a rewind point, as the packet could be larger than the
//            // buffer in memory
//            offset = _offset - 1;
//
//            packetSize = parsePacketSize();
//
//            // packets with a size of -1 are considered null
//            if (packetSize == -1) {
//                return new Reply(type, null);
//            }
//
//            end = _offset + packetSize;
//            start = _offset;
//
//            // set the offset to after the delimiter
//            _offset = end + 2;
//
//            if (end > _buffer.length()) {
//                _offset = offset;
//                throw new ArrayIndexOutOfBoundsException("Wait for more data.");
//            }
//
//            return new Reply(type, _buffer.getBuffer(start, end));
//        } else if (type == '*') {
//            offset = _offset;
//            packetSize = parsePacketSize();
//
//            if (packetSize < 0) {
//                return null;
//            }
//
//            if (packetSize > bytesRemaining()) {
//                _offset = offset - 1;
//                throw new ArrayIndexOutOfBoundsException("Wait for more data.");
//            }
//
//            Reply reply = new Reply(type, packetSize);
//
//            byte ntype;
//            Reply res;
//
//            for (int i = 0; i < packetSize; i++) {
//                ntype = _buffer.getByte(_offset++);
//
//                if (_offset > _buffer.length()) {
//                    throw new ArrayIndexOutOfBoundsException("Wait for more data.");
//                }
//                res = parseResult(ntype);
//                reply.set(i, res);
//            }
//
//            return reply;
//        }

        throw new RuntimeException("Unsupported message type");
    }

    public void handle(Buffer buffer) {
        append(buffer);

        Frame ret;
        int offset;

        while (true) {
            offset = _offset;
            try {
                // at least 1 byte
                if (bytesRemaining() < 1) {
                    break;
                }

                // set a rewind point. if a failure occurs,
                // wait for the next handle()/append() and try again
                offset = _offset - 1;

                ret = parseResult();

                if (ret == null) {
                    break;
                }

//                client.handleReply(ret);
            } catch (ArrayIndexOutOfBoundsException err) {
                // catch the error (not enough data), rewind, and wait
                // for the next packet to appear
                _offset = offset;
                break;
            }
        }
    }

    private void append(Buffer newBuffer) {
        if (newBuffer == null) {
            return;
        }

        // first run
        if (_buffer == null) {
            _buffer = newBuffer;

            return;
        }

        // out of data
        if (_offset >= _buffer.length()) {
            _buffer = newBuffer;
            _offset = 0;

            return;
        }

        // very large packet
        if (_offset > 0) {
            _buffer = _buffer.getBuffer(_offset, _buffer.length());
        }
        _buffer.appendBuffer(newBuffer);

        _offset = 0;
    }

    private int parsePacketSize() throws ArrayIndexOutOfBoundsException {
        int end = packetEndOffset((byte) '\n');
        String value = _buffer.getString(_offset, end - 1, _encoding);

        _offset = end + 1;

        long size = Long.parseLong(value);

        if (size > Integer.MAX_VALUE) {
            throw new RuntimeException("Cannot allocate more than " + Integer.MAX_VALUE + " bytes");
        }

        if (size < Integer.MIN_VALUE) {
            throw new RuntimeException("Cannot allocate less than " + Integer.MIN_VALUE + " bytes");
        }
        return (int) size;
    }

    private int packetEndOffset(byte delim) throws ArrayIndexOutOfBoundsException {
        int offset = _offset;

        while (_buffer.getByte(offset) != delim) {
            offset++;

            if (offset >= _buffer.length()) {
                throw new ArrayIndexOutOfBoundsException("didn't see delimiter");
            }
        }

        offset++;
        return offset;
    }

    private int bytesRemaining() {
        return (_buffer.length() - _offset) < 0 ? 0 : (_buffer.length() - _offset);
    }

    private byte read() {
        if ((_buffer.length() - _offset) < 0) {
            return -1;
        }

        return _buffer.getByte(_offset++);
    }
}