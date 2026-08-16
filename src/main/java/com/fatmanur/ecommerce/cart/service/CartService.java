package com.fatmanur.ecommerce.cart.service;

import com.fatmanur.ecommerce.cart.dto.CartItem;
import com.fatmanur.ecommerce.cart.dto.CartResponse;
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

    public CartResponse getCart(Long userId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(CART_PREFIX + userId);
        Map<Long, CartItem> items = new LinkedHashMap<>();
        double totalPrice = 0;

        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            if (entry.getValue() instanceof CartItem item) {
                items.put(Long.parseLong(entry.getKey().toString()), item);
                totalPrice += item.price() * item.quantity();
            }
        }

        return new CartResponse(items, totalPrice);
    }

    public void addItem(Long userId, CartItem item) {
        redisTemplate.opsForHash().put(CART_PREFIX + userId, item.productId().toString(), item);
    }

    public void updateQuantity(Long userId, Long productId, int quantity) {
        String key = CART_PREFIX + userId;
        CartItem item = (CartItem) redisTemplate.opsForHash().get(key, productId.toString());
        if (item != null) {
            CartItem updated = new CartItem(item.productId(), item.name(), item.price(), quantity);
            redisTemplate.opsForHash().put(key, productId.toString(), updated);
        }
    }

    public void removeItem(Long userId, Long productId) {
        redisTemplate.opsForHash().delete(CART_PREFIX + userId, productId.toString());
    }
}
