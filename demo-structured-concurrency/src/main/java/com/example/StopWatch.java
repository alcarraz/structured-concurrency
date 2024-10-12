package com.example;

import java.time.Duration;
import java.time.Instant;

public class StopWatch {
    Instant start = Instant.now();
    Duration elapsed() {
        return Duration.between(start, Instant.now());
    }
    void reset() {
        start = Instant.now();
    }
    
    @Override
    public String toString() {
        return "Elapsed time: %s ms".formatted(elapsed().toMillis());
    }
    
    public static StopWatch start() {
        return new StopWatch();
    }
    
    public static void measureTime(Runnable runnable) {
        StopWatch stopWatch = start();
        try {
            runnable.run();
        } finally {
            System.out.println(stopWatch);
        }
    }

}
