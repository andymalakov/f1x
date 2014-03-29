package org.f1x.v1;

import org.f1x.api.session.FailedLockException;
import org.f1x.api.session.SessionManager;
import org.f1x.api.session.SessionState;
import org.f1x.io.parsers.SimpleMessageScanner;
import org.gflogger.GFLog;
import org.gflogger.GFLogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

abstract class SessionAcceptorWrapper implements Runnable {

    private static final GFLog LOGGER = GFLogFactory.getLog(SessionAcceptorWrapper.class);

    private final SessionManager manager;
    private final FixSessionAcceptor acceptor;
    final SessionIDByteReferences sessionID;
    private final byte[] logonBuffer;
    private final int logonTimeout;

    Socket socket;

    SessionAcceptorWrapper(FixSessionAcceptor acceptor, SessionManager manager, int logonTimeout) {
        int maxMessageSize = acceptor.getSettings().getMaxInboundMessageSize(); // TODO: move validation to acceptor settings
        if (maxMessageSize < SimpleMessageScanner.MIN_MESSAGE_LENGTH)
            throw new IllegalArgumentException("max message size: " + maxMessageSize + " is less than min required length: " + SimpleMessageScanner.MIN_MESSAGE_LENGTH);

        this.manager = manager;
        this.acceptor = acceptor;
        this.logonBuffer = new byte[maxMessageSize];  // TODO: combine buffers
        this.sessionID = new SessionIDByteReferences();
        this.logonTimeout = logonTimeout;
    }

    @Override
    public void run() {
        try {
            int logonLength = waitLogon();
            if (logonLength > 0)
                startAcceptor(logonLength);
            else
                close(socket);
        } finally {
            sessionID.clear();
            socket = null;
            onStop();
        }
    }

    abstract void onStop();

    private void startAcceptor(int logonLength) {
        try {
            SessionState state = manager.lock(sessionID, acceptor); // TODO: use session state
            try {
                acceptor.connect(socket, sessionID);
                acceptor.run(logonBuffer, logonLength);
            } catch (IOException e) {
                LOGGER.warn().append("Closing socket. Error connecting: ").append(e).commit();
                close(socket);
            } finally {
                manager.unlock(sessionID);
            }
        } catch (FailedLockException e) {
            LOGGER.info().append("Someone wants to initiate session. Failed lock session id: ").append(e.getMessage()).append(". Closing socket").commit(); // TODO: log session id
            close(socket);
        }
    }

    /**
     * @return the logon length if session id was extracted from first message otherwise -1
     */
    private int waitLogon() {
        try {
            int oldSoTimeout = socket.getSoTimeout();
            socket.setSoTimeout(logonTimeout);

            int logonLength = extractSessionID();
            socket.setSoTimeout(oldSoTimeout);
            return logonLength;
        } catch (IOException e) {
            LOGGER.debug().append("During receiving logon error occurred: ").append(e).commit();
            return -1;
        }
    }

    private int extractSessionID() throws IOException {
        InputStream in = socket.getInputStream();

        int result = -1;
        int logonLength = 0;
        int requiredLogonLength = SimpleMessageScanner.MIN_MESSAGE_LENGTH;
        boolean continueExtraction = true;
        while (continueExtraction) {
            int bytesRead = in.read(logonBuffer, logonLength, logonBuffer.length - logonLength);
            if (bytesRead == -1) {
                LOGGER.debug().append("Disconnected (no more data).").commit();
                break;
            }

            logonLength += bytesRead;
            if (logonLength >= requiredLogonLength) {
                try {
                    int parsingResult = SessionIDParser.parse(logonBuffer, 0, logonLength, sessionID);
                    if (parsingResult > 0) {
                        result = logonLength;
                        continueExtraction = false;
                    } else {
                        requiredLogonLength = parsingResult + logonLength;
                        if (requiredLogonLength > logonBuffer.length) {
                            LOGGER.warn().append("Required logon length greater than logon buffer length").commit();
                            continueExtraction = false;
                        }
                    }
                } catch (SimpleMessageScanner.MessageFormatException e) {
                    LOGGER.warn().append("Invalid logon message: ").append(e.getMessage()).commit();
                    continueExtraction = false;
                }
            }
        }

        return result;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    private static void close(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            LOGGER.info().append("Error closing socket: ").append(e).commit();
        }
    }

    void logout(String cause) {
        acceptor.logout(cause);
    }

    void close() {
        acceptor.close();
    }

}
