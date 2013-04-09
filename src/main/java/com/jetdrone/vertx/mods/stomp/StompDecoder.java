package com.jetdrone.vertx.mods.stomp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufIndexFinder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.io.IOException;
import java.nio.charset.Charset;

public class StompDecoder extends ReplayingDecoder<Void> {

    private static final ByteBufIndexFinder NULEOL = new ByteBufIndexFinder() {
        @Override
        public boolean find(ByteBuf buffer, int guessedIndex) {
            return guessedIndex != 0 && buffer.getByte(guessedIndex - 1) == 0 && buffer.getByte(guessedIndex) == '\n';
        }
    };

    @Override
    public void checkpoint() {
        if (internalBuffer() != null) {
            super.checkpoint();
        }
    }

    public Frame receive(ByteBuf in) throws IOException {
        String frame = in.readBytes(in.bytesBefore(NULEOL)).toString(Charset.forName("UTF-8"));
        in.skipBytes(1);
        return Frame.unmarshall(frame);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        return receive(in);
    }
}
