package com.jetdrone.vertx.mods.stomp;

import com.jetdrone.vertx.mods.stomp.impl.FrameHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;

public class FrameParser implements Handler<Buffer> {

    private Buffer _buffer;
    private int _offset;
    private static final String _encoding = "utf-8";

    enum State {
        HEADERS,
        BODY,
        EOF
    }

    private final FrameHandler client;

    public FrameParser(FrameHandler client) {
        this.client = client;
    }

    private Frame parseResult() throws IndexOutOfBoundsException {
        int start, end;

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
                            //System.out.println("Switching to BODY parsing");
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

                        if (end >= _buffer.length()) {
                            throw new ArrayIndexOutOfBoundsException("needs more data");
                        }
                    }

                    // include the delimiter
                    _offset = end + 1;

                    body = _buffer.getString(start, end, _encoding);
                    //System.out.println("Received body: " + body);

                    frame.body = body;

                    //System.out.println("Switching to EOF parsing");
                    state = State.EOF;
                    break;
                case EOF:
                    if ((_buffer.length() - _offset) > 0) {
                        if (_buffer.getByte(_offset + 1) == '\n') {
                            //System.out.println("Skiping PONG");
                            _offset++;
                            break;
                        }
                    }

                    // There is more than 1 frame in this buffer
                    if (frame != null) {
                        //System.out.println("Received a FRAME");
                        return frame;
                    }

                    //System.out.println("Switching to HEADERS parsing");
                    state = State.HEADERS;
                    break;
            }
        }

        // if the loop ended but the state was not EOF the we did not complete parsing the frame
        if (state != State.EOF) {
            throw new ArrayIndexOutOfBoundsException("need more data");
        }

        return frame;
    }

    public void handle(Buffer buffer) {
        append(buffer);

        Frame ret;
        int offset;

        while (true) {
            // set a rewind point. if a failure occurs,
            // wait for the next handle()/append() and try again
            offset = _offset;
            try {
                // at least 1 byte
                if (bytesRemaining() == 0) {
                    break;
                }

                ret = parseResult();

                if (ret == null) {
                    break;
                }

                client.handleFrame(ret);
            } catch (IndexOutOfBoundsException err) {
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

    private int packetEndOffset(byte delim) throws ArrayIndexOutOfBoundsException {
        int offset = _offset;

        while (_buffer.getByte(offset) != delim) {
            offset++;

            if (offset >= _buffer.length()) {
                throw new ArrayIndexOutOfBoundsException("didn't see delimiter");
            }
        }

        return offset;
    }

    private int bytesRemaining() {
        return (_buffer.length() - _offset) < 0 ? 0 : (_buffer.length() - _offset);
    }
}