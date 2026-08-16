package com.fatmanur.ecommerce.cart.service;

import com.fatmanur.ecommerce.cart.dto.CartItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CART_PREFIX = "cart:";

    public Map<Long, CartItem> getCart(Long userId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(CART_PREFIX + userId);
        Map<Long, CartItem> cart = new LinkedHashMap<>();
        entries.forEach((key, value) -> {
            if (value instanceof CartItem item) {
                cart.put(Long.parseLong(key.toString()), item);
            }
        });
        return cart;
    }

    public void addItem(Long userId, CartItem item) {
        redisTemplate.opsForHash().put(CART_PREFIX + userId, item.productId().toString(), item);
    }
}
