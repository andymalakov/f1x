package org.f1x.api.session;

public class FailedLockException extends Exception {

    public static final FailedLockException UNREGISTERED_SESSION_ID = new FailedLockException("Unregistered session id");
    public static final FailedLockException SESSION_ID_IS_ALREADY_USED = new FailedLockException("Session id is already used");

    public FailedLockException(String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

}
