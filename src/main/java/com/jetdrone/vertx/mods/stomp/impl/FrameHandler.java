package com.jetdrone.vertx.mods.stomp.impl;

import com.jetdrone.vertx.mods.stomp.Frame;

public interface FrameHandler {

    public void handleFrame(Frame frame);
}
