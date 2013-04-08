package com.jetdrone.vertx.mods.stomp;

public enum Protocol {

    V1_0("1.0"),
    V1_1("1.1"),
    V1_2("1.2");

    public final String version;

    Protocol(String version) {
        this.version = version;
    }

}
