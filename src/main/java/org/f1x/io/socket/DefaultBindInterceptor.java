package org.f1x.io.socket;

import org.f1x.v1.SocketOptions;

import java.io.IOException;
import java.net.ServerSocket;

public class DefaultBindInterceptor implements BindInterceptor {
    private final SocketOptions socketOptions;

    public DefaultBindInterceptor() {
        this (new SocketOptions()); // load defaults
    }

    public DefaultBindInterceptor(SocketOptions socketOptions) {
        this.socketOptions = socketOptions;
    }

    @Override
    public void onBind(ServerSocket serverSocket) throws IOException {
        serverSocket.setSoTimeout(socketOptions.getSocketTimeout());
    }

}
