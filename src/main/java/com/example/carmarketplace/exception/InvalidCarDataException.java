package com.example.carmarketplace.exception;

public class InvalidCarDataException extends RuntimeException {

    public InvalidCarDataException(String message) {
        super(message);
    }
}
