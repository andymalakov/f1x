package org.f1x.v1;

import org.f1x.api.FixParserException;
import org.f1x.api.session.FailedLockException;
import org.f1x.api.session.SessionManager;
import org.f1x.io.parsers.SimpleMessageScanner;
import org.gflogger.GFLog;
import org.gflogger.GFLogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

public abstract class SessionAcceptorWrapper implements Runnable {

    protected static final GFLog LOGGER = GFLogFactory.getLog(SessionAcceptorWrapper.class);

    protected final SessionManager manager;
    protected final SessionIDByteReferences sessionID;
    protected final byte[] logonBuffer;
    protected final int logonTimeout;

    protected Socket socket;

    protected SessionAcceptorWrapper(int logonBufferSize, int logonTimeout, SessionManager manager) {
        this.manager = manager;
        this.logonBuffer = new byte[logonBufferSize];
        this.sessionID = new SessionIDByteReferences();
        this.logonTimeout = logonTimeout;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            int logonLength = waitLogon();
            runAcceptor(logonLength);
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

    protected abstract void onStop();

    protected void runAcceptor(int logonLength) throws IOException, FailedLockException {
        FixSessionAcceptor acceptor = manager.lockSession(sessionID);
        try {
            acceptor.connect(socket);
            acceptor.run(logonBuffer, logonLength);
        } finally {
            manager.unlockSession(sessionID);
        }
    }

    protected int waitLogon() throws IOException, ConnectionProblemException, FixParserException {
        int oldSoTimeout = socket.getSoTimeout();
        socket.setSoTimeout(logonTimeout);

        int logonLength = extractSessionID();
        socket.setSoTimeout(oldSoTimeout);
        return logonLength;
    }

    protected int extractSessionID() throws IOException, ConnectionProblemException {
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
                int parsingResult = SessionIDParser.parseOpposite(logonBuffer, 0, logonLength, sessionID);
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

    protected static void close(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            LOGGER.info().append("Error closing socket: ").append(e).commit();
        }
    }

    protected static class LogonMessageIsTooLongException extends FixParserException {

        protected static final LogonMessageIsTooLongException INSTANCE = new LogonMessageIsTooLongException("Logon message length is more than logon buffer length");

        protected LogonMessageIsTooLongException(String message) {
            super(message);
        }

        @Override
        public Throwable fillInStackTrace() {
            return this;
        }

    }

}
