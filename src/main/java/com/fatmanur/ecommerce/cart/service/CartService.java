package com.fatmanur.ecommerce.cart.service;

import com.fatmanur.ecommerce.cart.dto.CartItem;
import com.fatmanur.ecommerce.cart.dto.CartResponse;
import com.fatmanur.ecommerce.product.entity.Product;
import com.fatmanur.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductRepository productRepository;
    private static final String CART_PREFIX = "cart:";
    private static final java.time.Duration CART_TTL = java.time.Duration.ofDays(7);

    public CartResponse getCart(Long userId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(CART_PREFIX + userId);
        Map<Long, CartItem> items = new LinkedHashMap<>();
        double totalPrice = 0;

        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            if (entry.getValue() instanceof CartItem cartItem) {
                Product product = productRepository.findByIdAndDeletedFalse(cartItem.productId())
                        .orElse(null);

                if (product != null && product.isActive()) {
                    CartItem displayItem = new CartItem(
                            cartItem.productId(),
                            cartItem.name(),
                            product.getPrice().doubleValue(),
                            cartItem.quantity()
                    );
                    items.put(cartItem.productId(), displayItem);
                    totalPrice += product.getPrice().doubleValue() * cartItem.quantity();
                } else {
                    redisTemplate.opsForHash().delete(CART_PREFIX + userId, cartItem.productId().toString());
                }
            }
        }

        redisTemplate.expire(CART_PREFIX + userId, CART_TTL);
        return new CartResponse(items, totalPrice);
    }

    public void addItem(Long userId, CartItem item) {
        boolean isProductValid = productRepository
                .findByIdAndDeletedFalse(item.productId())
                .map(Product::isActive)
                .orElse(false);

        if (!isProductValid) {
            return;
        }

        redisTemplate.opsForHash().put(CART_PREFIX + userId, item.productId().toString(), item);
        redisTemplate.expire(CART_PREFIX + userId, CART_TTL);
    }

    public void updateQuantity(Long userId, Long productId, int quantity) {
        String key = CART_PREFIX + userId;
        CartItem item = (CartItem) redisTemplate.opsForHash().get(key, productId.toString());
        if (item != null) {
            CartItem updated = new CartItem(item.productId(), item.name(), item.price(), quantity);
            redisTemplate.opsForHash().put(key, productId.toString(), updated);
            redisTemplate.expire(key, CART_TTL);
        }
    }

    public void removeItem(Long userId, Long productId) {
        String key = CART_PREFIX + userId;
        redisTemplate.opsForHash().delete(key, productId.toString());
        redisTemplate.expire(key, CART_TTL);
    }
}
