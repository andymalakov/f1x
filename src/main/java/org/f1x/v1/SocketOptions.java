package org.f1x.v1;

public class SocketOptions {
    private boolean isSocketKeepAlive = true;
    private boolean isSocketTcpNoDelay = true;
    private int socketTimeout = 0;
    private int socketRecvBufferSize = 64*1024;
    private int socketSendBufferSize = 64*1024;

    public boolean isSocketKeepAlive() {
        return isSocketKeepAlive;
    }

    public boolean isSocketTcpNoDelay() {
        return isSocketTcpNoDelay;
    }

    public int getSocketRecvBufferSize() {
        return socketRecvBufferSize;
    }

    public int getSocketSendBufferSize() {
        return socketSendBufferSize;
    }

    /** @return Specifies SO_TIMEOUT in milliseconds, zero means infinity */
    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

}
