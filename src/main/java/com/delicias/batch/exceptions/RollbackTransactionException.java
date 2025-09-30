package com.delicias.batch.exceptions;

public class RollbackTransactionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RollbackTransactionException(String message) {
        super(message);
    }

    public RollbackTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
