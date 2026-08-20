package com.fatmanur.ecommerce.order.exception;

import com.fatmanur.ecommerce.order.entity.Order;

public class InvalidOrderStatusTransitionException extends RuntimeException {
    public InvalidOrderStatusTransitionException(String from , String to) {
        super("Invalid transition from " + from + " to " + to);
    }
}
