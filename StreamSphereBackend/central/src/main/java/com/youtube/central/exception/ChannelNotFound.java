package com.youtube.central.exception;

public class ChannelNotFound extends RuntimeException{
    
    public ChannelNotFound(String message){
        super(message);
    }
}
