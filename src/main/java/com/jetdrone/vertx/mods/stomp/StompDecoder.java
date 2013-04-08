package com.jetdrone.vertx.mods.stomp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufIndexFinder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.io.IOException;
import java.nio.charset.Charset;

public class StompDecoder extends ReplayingDecoder<Void> {

    @Override
    public void checkpoint() {
        if (internalBuffer() != null) {
            super.checkpoint();
        }
    }

    public Frame receive(ByteBuf in) throws IOException {
        String frame = in.readBytes(in.bytesBefore(ByteBufIndexFinder.NUL)).toString(Charset.forName("UTF-8"));
        in.skipBytes(1);
        return Frame.unmarshall(frame);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        return receive(in);
    }
}
