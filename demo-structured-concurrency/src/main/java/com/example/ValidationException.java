package com.example;

public class ValidationException extends Exception {
    String what;
    String reason;
    
    public ValidationException(String what) {
        super(message(what));
        this.what = what;
    }
    
    public ValidationException(String what, String reason) {
        super(message(what, reason));
        this.what = what;
        this.reason = reason;
    }

    public ValidationException(String what, Throwable cause) {
        super(message(what), cause);
    }

    public ValidationException(Throwable cause) {
        super(cause);
    }

    public String getWhat() {
        return what;
    }

    public String getReason() {
        return reason;
    }

    protected static String message(String what, String reason) {
        return "Validation of %s failed: %s".formatted(what, reason);
    }
    
    protected static String message(String what) {
        return "Validation of %s failed".formatted(what);
    }
}
