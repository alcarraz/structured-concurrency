package com.example;

public class Util {
    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException _) {
            // clear the thread.
            Thread.currentThread().interrupt();
        }
    }
}
