package org.f1x.v1.state;

import java.io.File;
import java.io.IOException;

public class Test_FileSessionState extends SessionStateTest {

    private File tempFile;

    @Override
    protected SimpleFileSessionStore createSessionState() throws IOException {
        tempFile = File.createTempFile("SessionState", null);
        tempFile.delete();
        return new SimpleFileSessionStore(tempFile);
    }

    @Override
    public void destroy() throws IOException {
        tempFile.delete();
        super.destroy();
    }

}