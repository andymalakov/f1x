package org.f1x.v1;

import org.f1x.api.FixParserException;
import org.f1x.api.session.FailedLockException;
import org.f1x.api.session.SessionManager;
import org.f1x.api.session.SessionState;
import org.f1x.io.parsers.SimpleMessageScanner;
import org.gflogger.GFLog;
import org.gflogger.GFLogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

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
            startAcceptor(logonLength);
        } catch (SocketTimeoutException e) {
            LOGGER.warn().append("Error occurred during starting acceptor. Logon timeout expired").commit();
            close(socket);
        } catch (ConnectionProblemException e) {
            LOGGER.warn().append("Error occurred during starting acceptor: ").append(e.getMessage()).commit();
            close(socket);
        } catch (FixParserException e) {
            LOGGER.warn().append("Error occurred during starting acceptor. Invalid logon message: ").append(e.getMessage()).commit();
            close(socket);
        } catch (FailedLockException e) {
            LOGGER.info()
                    .append("Error occurred during starting acceptor. Failed to lock session id: ").append(e.getMessage())
                    .append(" (Sender Comp ID: ").append(sessionID.getSenderCompId())
                    .append(", Sender Sub ID: ").append(sessionID.getSenderSubId())
                    .append(", Target Comp ID: ").append(sessionID.getTargetCompId())
                    .append(", Target Sub ID: ").append(sessionID.getTargetSubId())
                    .append(")").commit();
            close(socket);
        } catch (IOException e) {
            LOGGER.warn().append("Error occurred during starting acceptor: ").append(e).commit();
            close(socket);
        } finally {
            sessionID.clear();
            socket = null;
            onStop();
        }
    }

    abstract void onStop();

    private void startAcceptor(int logonLength) throws IOException, FailedLockException {
        SessionState state = manager.lock(sessionID, acceptor);
        try {
            acceptor.setSessionState(state);
            acceptor.connect(socket, sessionID);
            acceptor.run(logonBuffer, logonLength);
        } finally {
            manager.unlock(sessionID);
        }
    }

    private int waitLogon() throws IOException, ConnectionProblemException, FixParserException {
        int oldSoTimeout = socket.getSoTimeout();
        socket.setSoTimeout(logonTimeout);

        int logonLength = extractSessionID();
        socket.setSoTimeout(oldSoTimeout);
        return logonLength;
    }

    private int extractSessionID() throws IOException, ConnectionProblemException {
        InputStream in = socket.getInputStream();

        int logonLength = 0;
        int requiredLogonLength = SimpleMessageScanner.MIN_MESSAGE_LENGTH;
        boolean continueExtraction = true;
        while (continueExtraction) {
            int bytesRead = in.read(logonBuffer, logonLength, logonBuffer.length - logonLength);
            if (bytesRead <= 0)
                throw ConnectionProblemException.NO_SOCKET_DATA;

            logonLength += bytesRead;
            if (logonLength >= requiredLogonLength) {
                int parsingResult = SessionIDParser.parse(logonBuffer, 0, logonLength, sessionID);
                if (parsingResult > 0) {
                    continueExtraction = false;
                } else {
                    requiredLogonLength = logonLength - parsingResult;
                    if (requiredLogonLength > logonBuffer.length)
                        throw LogonMessageIsTooLongException.INSTANCE;
                }
            }
        }

        return logonLength;
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

    private static class LogonMessageIsTooLongException extends FixParserException {

        private static final LogonMessageIsTooLongException INSTANCE = new LogonMessageIsTooLongException("Logon message length is more than logon buffer length");

        private LogonMessageIsTooLongException(String message) {
            super(message);
        }

        @Override
        public Throwable fillInStackTrace() {
            return null;
        }

    }

}
