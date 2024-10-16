package com.example;

public class Util {
    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // clear the thread.
            Thread.currentThread().interrupt();
            System.out.println("sleep interrupted");
        }
    }
}
