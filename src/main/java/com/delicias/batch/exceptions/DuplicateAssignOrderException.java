package com.delicias.batch.exceptions;


public class DuplicateAssignOrderException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DuplicateAssignOrderException(String message) {
        super(message);
    }

    public DuplicateAssignOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
