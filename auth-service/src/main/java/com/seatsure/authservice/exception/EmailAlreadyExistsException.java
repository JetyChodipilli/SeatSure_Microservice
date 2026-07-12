package com.seatsure.authservice.exception;

public class EmailAlreadyExistsException  extends RuntimeException{
    public EmailAlreadyExistsException(String message){
        super(message);
    }
}
