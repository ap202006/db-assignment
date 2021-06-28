package com.db.assignment.tradestore.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ValidationFailureException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ValidationFailureException() {
        super();
    }

    public ValidationFailureException(String message) {
        super(message);
    }

    public ValidationFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
