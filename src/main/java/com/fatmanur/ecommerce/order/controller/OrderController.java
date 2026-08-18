package com.fatmanur.ecommerce.order.controller;

import com.fatmanur.ecommerce.auth.exception.UserNotFoundException;
import com.fatmanur.ecommerce.order.dto.OrderResponse;
import com.fatmanur.ecommerce.order.service.OrderService;
import com.fatmanur.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(@AuthenticationPrincipal UserDetails user) {
        Long userId = getUserId(user);
        return ResponseEntity.ok(orderService.checkout(userId));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(@AuthenticationPrincipal UserDetails user) {
        Long userId = getUserId(user);
        return ResponseEntity.ok(orderService.getOrders(userId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@AuthenticationPrincipal UserDetails user,
                                                   @PathVariable Long orderId) {
        Long userId = getUserId(user);
        return ResponseEntity.ok(orderService.getOrder(userId, orderId));
    }

    private Long getUserId(UserDetails user) {
        String email = user.getUsername();
        return userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"))
                .getId();
    }
}
