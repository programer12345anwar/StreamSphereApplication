package com.youtube.video_service.exception;

public class InvalidFileType extends RuntimeException {
    /*
    RuntimeException is an unchecked exception in Java that occurs during program execution and doesn’t need to be declared in throws or handled with try-catch.
     */
    public InvalidFileType(String message) {
        super(message);//super(message) → calls the parent constructor of RuntimeException and stores the error message.
    }
    
}
