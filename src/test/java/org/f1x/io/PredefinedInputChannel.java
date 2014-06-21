package org.f1x.io;

import org.f1x.util.AsciiUtils;

import java.io.IOException;


public class PredefinedInputChannel implements InputChannel {
    private final String [] chunks;
    private int index;

    public PredefinedInputChannel(String... chunks) {
        this.chunks = chunks;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (index >= chunks.length)
            return -1;

        String chunk = chunks[index++].replace('|', '\u0001');
        byte [] bytes = AsciiUtils.getBytes(chunk);

        if (bytes.length > length)
            throw new IllegalStateException("FIX Communicator buffer is too small to fit message of size " + bytes.length);
        System.arraycopy(bytes, 0, buffer, offset, bytes.length);
        return bytes.length;
    }

    @Override
    public void close() throws IOException {
        index = chunks.length;
    }
}
