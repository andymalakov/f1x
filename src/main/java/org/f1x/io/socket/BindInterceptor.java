package org.f1x.io.socket;

import java.io.IOException;
import java.net.ServerSocket;

public interface BindInterceptor {
    void onBind(ServerSocket socket) throws IOException;
}
