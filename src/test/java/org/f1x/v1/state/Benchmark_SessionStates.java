package org.f1x.v1.state;

import org.f1x.api.session.SessionState;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import sun.misc.Cleaner;
import sun.nio.ch.DirectBuffer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 4, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class Benchmark_SessionStates {

    private Path tempFile;
    private FileSessionState fileSessionState;
    private MemorySessionState memorySessionState;
    private TestSessionState testSessionState;

    @Setup
    public void init() throws IOException {
        tempFile = Files.createTempFile(null,null);
        Files.delete(tempFile);
        fileSessionState = new FileSessionState(tempFile);
        memorySessionState = new MemorySessionState();
        testSessionState = new TestSessionState();
    }

    @GenerateMicroBenchmark
    public void measureFileSessionState() {
        measure(fileSessionState);
    }

    @GenerateMicroBenchmark
    public void measureMemorySessionState() {
        measure(memorySessionState);
    }

    @GenerateMicroBenchmark
    public void measureTestSessionState(){
        measure(testSessionState);
    }

    @TearDown
    public void destroy() throws IOException {
        Cleaner cleaner = ((DirectBuffer) fileSessionState.buffer).cleaner();
        cleaner.clean(); // If file was memory mapped then windows does not allow to delete it
        Files.deleteIfExists(tempFile);
    }

    private static void measure(SessionState sessionState) {
        sessionState.consumeNextSenderSeqNum();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(".*" + Benchmark_SessionStates.class.getSimpleName() + ".*")
                .build();

        new Runner(opt).run();
    }

}
