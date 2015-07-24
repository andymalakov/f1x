package org.f1x.api.session;

@SuppressWarnings("ThrowableInstanceNeverThrown")
public class FailedLockException extends Exception {

    public static final FailedLockException UNREGISTERED_SESSION_ID = new FailedLockException("Unregistered session id");
    public static final FailedLockException SESSION_ID_IS_ALREADY_USED = new FailedLockException("Session id is already used");
    public static final FailedLockException CLOSED_SESSION_MANAGER = new FailedLockException("Session Manager is closed");

    public FailedLockException(String message) {
        super(message);
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

}
