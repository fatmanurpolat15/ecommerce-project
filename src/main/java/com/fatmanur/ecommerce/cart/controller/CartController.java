package com.fatmanur.ecommerce.cart.controller;

import com.fatmanur.ecommerce.cart.dto.CartItem;
import com.fatmanur.ecommerce.cart.dto.CartResponse;
import com.fatmanur.ecommerce.cart.service.CartService;
import com.fatmanur.ecommerce.auth.exception.UserNotFoundException;
import com.fatmanur.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.base-uri}/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal UserDetails user) {
        Long userId = getUserId(user);
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<Void> addItem(@AuthenticationPrincipal UserDetails user,
                                        @RequestBody CartItem item) {
        Long userId = getUserId(user);
        cartService.addItem(userId, item);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<Void> updateQuantity(@AuthenticationPrincipal UserDetails user,
                                               @PathVariable Long productId,
                                               @RequestParam int quantity) {
        Long userId = getUserId(user);
        cartService.updateQuantity(userId, productId, quantity);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> removeItem(@AuthenticationPrincipal UserDetails user,
                                           @PathVariable Long productId) {
        Long userId = getUserId(user);
        cartService.removeItem(userId, productId);
        return ResponseEntity.ok().build();
    }

    private Long getUserId(UserDetails user) {
        String email = user.getUsername();
        return userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + email))
                .getId();
    }
}
