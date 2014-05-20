package org.f1x.util.format;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 4, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class Benchmark_DoubleFormatters {

    private final DoubleFormatter formatter1 = new DoubleFormatter(10);
    private final byte[] buffer = new byte[256];

    @Param({"12345.67890","1234567890","0.1234567890"})
    private double number = 123456.78901234;
    @Param({"0", "2", "4", "6", "8"})
    private int precision;

    @GenerateMicroBenchmark
    public void measureFormat1() {
        formatter1.format(number, precision, buffer, 0);
    }

    @GenerateMicroBenchmark
    public void measureFormat2() {
        DoubleFormatter2.format(number, precision, buffer, 0);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(".*" + Benchmark_DoubleFormatters.class.getSimpleName() + ".*")
                .build();

        new Runner(opt).run();
    }

}
