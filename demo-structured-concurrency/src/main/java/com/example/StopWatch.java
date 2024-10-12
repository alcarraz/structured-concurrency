package com.example;

import java.time.Duration;
import java.time.Instant;

public class StopWatch {
    Instant start = Instant.now();
    String additionalDescription = "";
    
    public StopWatch() {
    }
    
    public StopWatch(String name) {
        if (name != null) this.additionalDescription = " for %s".formatted(name);
    }
    
    public Duration elapsed() {
        return Duration.between(start, Instant.now());
    }
   public void reset() {
        start = Instant.now();
    }
    
    @Override
    public String toString() {
        return "Elapsed time%s: %s ms".formatted(additionalDescription, elapsed().toMillis());
    }
    
    public static StopWatch start(String name) {
        return new StopWatch(name);
    }
    
    public static StopWatch start() {
        return new StopWatch();
    }
    
    public static void measureTime(Runnable runnable) {
        measureTime(runnable, null);
    }
    
    public static void measureTime(Runnable runnable, String name) {
        StopWatch stopWatch = start(name);
        try {
            runnable.run();
        } finally {
            stopWatch.completed();
        }
    }

    public void completed() {
        System.out.println(this);
    }
}
