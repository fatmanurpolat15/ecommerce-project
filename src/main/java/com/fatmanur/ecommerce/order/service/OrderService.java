package com.fatmanur.ecommerce.order.service;

import com.fatmanur.ecommerce.auth.exception.UserNotFoundException;
import com.fatmanur.ecommerce.cart.dto.CartItem;
import com.fatmanur.ecommerce.cart.dto.CartResponse;
import com.fatmanur.ecommerce.cart.service.CartService;
import com.fatmanur.ecommerce.order.dto.OrderResponse;
import com.fatmanur.ecommerce.order.entity.Order;
import com.fatmanur.ecommerce.order.entity.OrderItem;
import com.fatmanur.ecommerce.order.entity.OrderStatusHistory;
import com.fatmanur.ecommerce.order.enums.OrderStatus;
import com.fatmanur.ecommerce.order.exception.InvalidOrderStatusTransitionException;
import com.fatmanur.ecommerce.order.exception.OrderNotFoundException;
import com.fatmanur.ecommerce.order.repository.OrderRepository;
import com.fatmanur.ecommerce.product.entity.Product;
import com.fatmanur.ecommerce.product.exception.ProductNotFoundException;
import com.fatmanur.ecommerce.product.repository.ProductRepository;
import com.fatmanur.ecommerce.stock.service.StockService;
import com.fatmanur.ecommerce.user.entity.User;
import com.fatmanur.ecommerce.user.entity.UserAddress;
import com.fatmanur.ecommerce.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final ProductRepository productRepository;
    private final StockService stockService;
    private final UserRepository userRepository;

    private static final Map<OrderStatus , Set<OrderStatus>> ALLOWED_STATUS_TRANSITIONS = Map.of(
            OrderStatus.CREATED, Set.of(OrderStatus.PAID, OrderStatus.CANCELLED),
            OrderStatus.PAID, Set.of(OrderStatus.FULFILLED),
            OrderStatus.FULFILLED, Set.of(),
            OrderStatus.PAYMENT_FAILED, Set.of(OrderStatus.CANCELLED),
            OrderStatus.CANCELLED, Set.of()
    );

    @Transactional
    public OrderResponse checkout(Long userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        UserAddress address = user.getAddresses().stream()
                .filter(a -> !a.isDeleted())
                .filter(UserAddress::isDefault)
                .findFirst()
                .orElseThrow(() -> new UserNotFoundException("Address not found"));

        CartResponse cart = cartService.getCart(userId);

        if (cart.items().isEmpty()) {
            throw new ProductNotFoundException("Cart is empty");
        }

        for (var entry : cart.items().entrySet()) {
            CartItem cartItem = entry.getValue();
            stockService.reserveStock(cartItem.productId(), cartItem.quantity());
        }

        Order order = Order.builder()
                .user(user)
                .orderNumber(generateOrderNumber())
                .userAddress(address)
                .totalPrice(BigDecimal.valueOf(cart.totalPrice()))
                .build();

        for (var entry : cart.items().entrySet()) {
            CartItem cartItem = entry.getValue();
            Product product = productRepository.findByIdAndDeletedFalse(cartItem.productId())
                    .orElseThrow(() -> new ProductNotFoundException("Product not found"));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(cartItem.name())
                    .price(BigDecimal.valueOf(cartItem.price()))
                    .quantity(cartItem.quantity())
                    .build();

            order.getOrderItems().add(orderItem);
        }

        Order savedOrder = orderRepository.save(order);
        cartService.clearCart(userId);

        return toResponse(savedOrder);
    }

    public List<OrderResponse> getOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public OrderResponse getOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        return toResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        OrderStatus currentStatus = order.getStatus();
        if (!ALLOWED_STATUS_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(newStatus)) {
            throw new InvalidOrderStatusTransitionException(currentStatus.name(), newStatus.name());
        }

        OrderStatusHistory statusHistory = OrderStatusHistory.builder()
                .order(order)
                .previousStatus(currentStatus)
                .newStatus(newStatus)
                .changedAt(LocalDateTime.now())
                .build();
        order.getStatusHistory().add(statusHistory);
        order.setStatus(newStatus);

        return toResponse(order);
    }

    private OrderResponse toResponse(Order order) {
        String fullAddress = order.getUserAddress().getAddress() + ", " +
                order.getUserAddress().getStreet() + ", " +
                order.getUserAddress().getDistrict() + ", " +
                order.getUserAddress().getCity() + ", " +
                order.getUserAddress().getCountry();

        List<OrderResponse.OrderItemResponse> items = order.getOrderItems().stream()
                .map(item -> new OrderResponse.OrderItemResponse(
                        item.getProductName(),
                        item.getPrice(),
                        item.getQuantity(),
                        item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                ))
                .toList();
        List<OrderResponse.StatusTimeLineItem> statusTimeline = order.getStatusHistory().stream()
                .map(history -> new OrderResponse.StatusTimeLineItem(
                        history.getPreviousStatus(),
                        history.getNewStatus(),
                        history.getChangedAt()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus().name(),
                fullAddress,
                items,
                order.getTotalPrice(),
                order.getCreatedAt(),
                statusTimeline

        );
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
