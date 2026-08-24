package com.fatmanur.ecommerce.order.exception;

public class OrderNotCancellableException extends RuntimeException {
    public OrderNotCancellableException(String message) {
        super(message);
    }
}
