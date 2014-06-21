package org.f1x.v1.state;

import org.junit.Test;
import sun.misc.Cleaner;
import sun.nio.ch.DirectBuffer;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class Test_FileSessionState extends SessionStateTest {

    private Path tempFile;

    @Override
    protected FileSessionState createSessionState() throws IOException {
        Files.createTempDirectory(null);
        tempFile = Files.createTempFile(null, null);
        Files.delete(tempFile);
        return new FileSessionState(tempFile);
    }

    @Override
    public void destroy() throws IOException {
        FileSessionState sessionState = (FileSessionState) this.sessionState;
        unmap(sessionState.buffer);
        Files.deleteIfExists(tempFile);
        super.destroy();
    }

    @Test
    public void testCreatingParentDirectories() throws IOException {
        Path tempDirectory = Files.createTempDirectory(null);
        try {
            Path tempFile = Paths.get(tempDirectory.toString(), "1", "2", "3", "4", "5", "6", "7");
            try {
                FileSessionState sessionState = new FileSessionState(tempFile);
                unmap(sessionState.buffer);
            } finally {
                Files.walkFileTree(tempDirectory, new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.deleteIfExists(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.deleteIfExists(dir);
                        return FileVisitResult.CONTINUE;
                    }

                });
            }
        } finally {
           // Files.deleteIfExists(tempDirectory);
        }
    }

    private static void unmap(MappedByteBuffer buffer) {
        Cleaner cleaner = ((DirectBuffer) buffer).cleaner();
        cleaner.clean(); // If file was memory mapped then windows does not allow to delete it
    }

}
