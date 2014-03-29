package org.f1x.v1;

import org.f1x.api.session.SessionID;
import org.f1x.util.ByteArrayReference;

public class SessionIDByteReferences extends SessionID {

    private final ByteArrayReference senderCompID = new ByteArrayReference();
    private final ByteArrayReference senderSubID = new ByteArrayReference();
    private final ByteArrayReference targetCompID = new ByteArrayReference();
    private final ByteArrayReference targetSubID = new ByteArrayReference();

    public void setSenderCompId(byte [] buffer, int offset, int length) {
        senderCompID.set(buffer, offset, length);
    }

    @Override
    public CharSequence getSenderCompId() {
        return senderCompID;
    }

    public void setSenderSubId(byte [] buffer, int offset, int length) {
        senderSubID.set(buffer, offset, length);
    }

    @Override
    public CharSequence getSenderSubId() {
        return senderSubID;
    }

    public void setTargetCompId(byte [] buffer, int offset, int length) {
        targetCompID.set(buffer, offset, length);
    }

    @Override
    public CharSequence getTargetCompId() {
        return targetCompID;
    }

    public void setTargetSubId(byte [] buffer, int offset, int length) {
        targetSubID.set(buffer, offset, length);
    }

    @Override
    public CharSequence getTargetSubId() {
        return targetSubID;
    }

    public void clear() {
        senderCompID.clear();
        senderSubID.clear();
        targetCompID.clear();
        targetSubID.clear();
    }

}
