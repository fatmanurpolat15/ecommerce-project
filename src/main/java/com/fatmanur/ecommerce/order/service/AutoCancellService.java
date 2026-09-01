package com.fatmanur.ecommerce.order.service;

import com.fatmanur.ecommerce.order.entity.Order;
import com.fatmanur.ecommerce.order.entity.OrderItem;
import com.fatmanur.ecommerce.order.entity.OrderStatusHistory;
import com.fatmanur.ecommerce.order.enums.OrderStatus;
import com.fatmanur.ecommerce.order.repository.OrderRepository;
import com.fatmanur.ecommerce.stock.entity.Inventory;
import com.fatmanur.ecommerce.stock.repository.InventoryRepository;
import com.fatmanur.ecommerce.stock.service.StockService;
import com.fatmanur.ecommerce.transaction.service.TransactionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoCancellService {

    private final OrderRepository orderRepository;
    private final StockService stockService;
    private final InventoryRepository inventoryRepository;
    private final TransactionService transactionService;

    @Value("${app.timezone}")
    private String timezone;

    @Value("${app.payment-window-minutes}")
    private long paymentWindowMinutes;

    @Value("${app.payment-retry-window-minutes}")
    private long retryWindowMinutes;


    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void cancelStuckOrders() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of(timezone));

        List<Order> createdOrders = orderRepository.findByStatus(OrderStatus.CREATED);
        for (Order order : createdOrders) {
            transactionService.requestPayment(order);
            orderRepository.save(order);
            log.info("Order {} transitioning from CREATED to PAYMENT_PENDING.", order.getOrderNumber());
        }

        List<Order> stuckOrders = orderRepository.findByStatusAndPaymentDeadlineBefore(OrderStatus.PAYMENT_PENDING, now);

        for (Order order : stuckOrders) {
            LocalDateTime retryDeadline = order.getCreatedAt().plusMinutes(paymentWindowMinutes + retryWindowMinutes);

            if (now.isBefore(retryDeadline)) {
                order.setPaymentDeadline(LocalDateTime.now().plusMinutes(retryWindowMinutes));
                transactionService.requestPayment(order);

                OrderStatusHistory history = OrderStatusHistory.builder()
                        .order(order)
                        .previousStatus(OrderStatus.PAYMENT_PENDING)
                        .newStatus(OrderStatus.PAYMENT_PENDING)
                        .changedAt(LocalDateTime.now())
                        .build();
                order.getStatusHistory().add(history);

                log.info("Order {} auto-retrying payment.", order.getOrderNumber());
            } else {
                order.setStatus(OrderStatus.NOT_COMPLETED);

                OrderStatusHistory history = OrderStatusHistory.builder()
                        .order(order)
                        .previousStatus(OrderStatus.PAYMENT_PENDING)
                        .newStatus(OrderStatus.NOT_COMPLETED)
                        .changedAt(LocalDateTime.now())
                        .build();
                order.getStatusHistory().add(history);

                log.info("Order {} marked NOT_COMPLETED after max retries.", order.getOrderNumber());
            }
            orderRepository.save(order);
        }

        LocalDateTime cleanupThreshold = now.minusMinutes(30);
        List<Order> oldNotCompleted = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.NOT_COMPLETED, cleanupThreshold);

        for (Order order : oldNotCompleted) {
            for (OrderItem orderItem : order.getOrderItems()) {
                Inventory inventory = inventoryRepository.findByProductId(orderItem.getProduct().getId()).orElse(null);
                if (inventory != null && inventory.getReservedQuantity() >= orderItem.getQuantity()) {
                    stockService.releaseStock(orderItem.getProduct().getId(), orderItem.getQuantity());
                }
            }

            orderRepository.save(order);
            log.info("Order {} stock released after 30 min grace period. Status remains NOT_COMPLETED.", order.getOrderNumber());
        }
    }

}
