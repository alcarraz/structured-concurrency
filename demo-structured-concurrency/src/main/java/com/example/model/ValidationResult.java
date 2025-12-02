package com.example.model;

public sealed interface ValidationResult {
    record Success() implements ValidationResult {};
    Success SUCCESS = new Success();
    
    record Failure(String message) implements ValidationResult {};
    
    static ValidationResult success() {
        return SUCCESS;
    }
    
    static ValidationResult failure(String message) {
        return new Failure(message);
    }
    
    static boolean failure(ValidationResult result) {
        return result instanceof Failure; 
    }
    
    static boolean success(ValidationResult result) {
        return result instanceof Success;
    }
}
