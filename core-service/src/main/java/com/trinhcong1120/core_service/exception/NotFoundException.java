package com.trinhcong1120.core_service.exception;

public class NotFoundException
        extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}