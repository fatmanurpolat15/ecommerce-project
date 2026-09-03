package com.fatmanur.ecommerce.transaction.service;

import com.fatmanur.ecommerce.order.entity.Order;
import com.fatmanur.ecommerce.order.entity.OrderItem;
import com.fatmanur.ecommerce.order.entity.OrderStatusHistory;
import com.fatmanur.ecommerce.order.enums.OrderStatus;
import com.fatmanur.ecommerce.order.exception.OrderNotFoundException;
import com.fatmanur.ecommerce.order.repository.OrderRepository;
import com.fatmanur.ecommerce.stock.entity.Inventory;
import com.fatmanur.ecommerce.stock.repository.InventoryRepository;
import com.fatmanur.ecommerce.stock.service.StockService;
import com.fatmanur.ecommerce.transaction.entity.OutboxMessage;
import com.fatmanur.ecommerce.transaction.enums.OutboxStatus;
import com.fatmanur.ecommerce.transaction.repository.OutboxRepository;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;
import com.fatmanur.ecommerce.transaction.dto.PaymentRequest;
import com.fatmanur.ecommerce.transaction.dto.PaymentResult;
import com.fatmanur.ecommerce.transaction.dto.TransactionResponse;
import com.fatmanur.ecommerce.transaction.entity.Transaction;
import com.fatmanur.ecommerce.transaction.enums.TransactionStatus;
import com.fatmanur.ecommerce.transaction.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final OrderRepository orderRepository;
    private final StockService stockService;
    private final InventoryRepository inventoryRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public void requestPayment(Order order) {
        boolean hasPendingTransaction = transactionRepository
                .findAllByOrderIdOrderByCreatedAtDesc(order.getId())
                .stream()
                .anyMatch(t -> t.getStatus() == TransactionStatus.CREATED);
        if (hasPendingTransaction) {
            log.info("Order {} already has a pending transaction, skipping.", order.getOrderNumber());
            return;
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.PAYMENT_PENDING);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .previousStatus(previousStatus)
                .newStatus(OrderStatus.PAYMENT_PENDING)
                .changedAt(LocalDateTime.now())
                .build();
        order.getStatusHistory().add(history);

        Transaction transaction = Transaction.builder()
                .order(order)
                .amount(order.getTotalPrice())
                .status(TransactionStatus.CREATED)
                .build();
        transactionRepository.save(transaction);

        PaymentRequest request = new PaymentRequest(
                order.getId(),
                order.getOrderNumber(),
                order.getTotalPrice(),
                order.getUser().getEmail()
        );

        try {
            String json = objectMapper.writeValueAsString(request);

            OutboxMessage outboxMessage = OutboxMessage.builder()
                    .topic("payment.requests")
                    .messageKey(order.getId().toString())
                    .payload(json)
                    .status(OutboxStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            outboxRepository.save(outboxMessage);

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        kafkaTemplate.send("payment.requests", order.getId().toString(), json)
                                .get(10, TimeUnit.SECONDS);
                        outboxMessage.setStatus(OutboxStatus.SENT);
                        outboxMessage.setSentAt(LocalDateTime.now());
                        outboxRepository.save(outboxMessage);
                        log.info("Payment request sent to Kafka for order: {}", order.getOrderNumber());
                    } catch (Exception e) {
                        log.warn("Direct Kafka send failed for order: {}, poller will retry", order.getOrderNumber());
                    }
                }
            });
        } catch (Exception e) {
            log.error("Failed to serialize payment request for order: {}", order.getOrderNumber(), e);
        }
    }

    @Transactional
    public void handlePaymentResult(PaymentResult result) {
        Order order = orderRepository.findById(result.orderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        Transaction transaction = transactionRepository.findAllByOrderIdOrderByCreatedAtDesc(order.getId())
                .stream()
                .filter(t -> t.getStatus() == TransactionStatus.CREATED)
                .findFirst()
                .orElse(null);

        if (transaction == null) {
            log.info("No pending transaction found for order {}, ignoring result: {}", order.getOrderNumber(), result.status());
            return;
        }

        if (order.getStatus() == OrderStatus.NOT_COMPLETED && result.status() == TransactionStatus.FAILED) {
            log.info("Order {} is already NOT_COMPLETED, ignoring FAILED result: {}", order.getOrderNumber(), result.paymentReference());
            return;
        }

        OrderStatus previousStatus = order.getStatus();

        if (result.status() == TransactionStatus.SUCCEEDED) {
            boolean alreadySucceeded = transactionRepository.findAllByOrderIdOrderByCreatedAtDesc(order.getId())
                    .stream()
                    .anyMatch(t -> t.getStatus() == TransactionStatus.SUCCEEDED);
            if (alreadySucceeded) {
                return;
            }
            transaction.setStatus(TransactionStatus.SUCCEEDED);
            transaction.setPaymentReference(result.paymentReference());
            order.setStatus(OrderStatus.PAID);

            for (OrderItem item : order.getOrderItems()) {
                stockService.consumeStock(item.getProduct().getId(), item.getQuantity());
            }
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setPaymentReference(result.paymentReference());
            order.setStatus(OrderStatus.NOT_COMPLETED);

            for (OrderItem item : order.getOrderItems()) {
                Inventory inventory = inventoryRepository.findByProductId(item.getProduct().getId()).orElse(null);
                if (inventory != null && inventory.getReservedQuantity() >= item.getQuantity()) {
                    stockService.releaseStock(item.getProduct().getId(), item.getQuantity());
                }
            }
        }

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .previousStatus(previousStatus)
                .newStatus(order.getStatus())
                .changedAt(LocalDateTime.now())
                .build();
        order.getStatusHistory().add(history);

        transactionRepository.save(transaction);
        orderRepository.save(order);

        log.info("Payment result applied for order {}: {}", order.getOrderNumber(), order.getStatus());
    }

    public List<TransactionResponse> getTransactionsByOrderId(Long orderId) {
        return transactionRepository.findAllByOrderIdOrderByCreatedAtDesc(orderId)
                .stream()
                .map(t -> new TransactionResponse(
                        t.getId(),
                        t.getOrder().getId(),
                        t.getAmount(),
                        t.getStatus(),
                        t.getPaymentReference(),
                        t.getCreatedAt(),
                        t.getUpdatedAt()
                ))
                .toList();
    }

    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(t -> new TransactionResponse(
                        t.getId(),
                        t.getOrder().getId(),
                        t.getAmount(),
                        t.getStatus(),
                        t.getPaymentReference(),
                        t.getCreatedAt(),
                        t.getUpdatedAt()
                ))
                .toList();
    }
}
