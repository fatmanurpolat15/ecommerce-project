package com.fatmanur.ecommerce.transaction.service;

import com.fatmanur.ecommerce.order.entity.Order;
import com.fatmanur.ecommerce.order.entity.OrderItem;
import com.fatmanur.ecommerce.order.entity.OrderStatusHistory;
import com.fatmanur.ecommerce.order.enums.OrderStatus;
import com.fatmanur.ecommerce.order.exception.OrderNotFoundException;
import com.fatmanur.ecommerce.order.repository.OrderRepository;
import com.fatmanur.ecommerce.stock.service.StockService;
import com.fatmanur.ecommerce.transaction.entity.OutboxMessage;
import com.fatmanur.ecommerce.transaction.enums.OutboxStatus;
import com.fatmanur.ecommerce.transaction.repository.OutboxRepository;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;
import com.fatmanur.ecommerce.transaction.dto.PaymentRequest;
import com.fatmanur.ecommerce.transaction.dto.PaymentResult;
import com.fatmanur.ecommerce.transaction.entity.Transaction;
import com.fatmanur.ecommerce.transaction.enums.TransactionStatus;
import com.fatmanur.ecommerce.transaction.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final OrderRepository orderRepository;
    private final StockService stockService;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public void requestPayment(Order order) {
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
        } catch (Exception e) {
            log.error("Failed to serialize payment request for order: {}", order.getOrderNumber(), e);
        }
    }

    @Transactional
    public void handlePaymentResult(PaymentResult result) {
        Order order = orderRepository.findById(result.orderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        Transaction transaction = transactionRepository.findByOrderId(order.getId())
                .orElseGet(() -> Transaction.builder()
                        .order(order)
                        .amount(order.getTotalPrice())
                        .status(TransactionStatus.CREATED)
                        .build());

        if (transaction.getStatus() != TransactionStatus.CREATED) {
            log.info("Transaction already applied for order {}, status: {}", order.getOrderNumber(), transaction.getStatus());
            return;
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.info("Order {} is already CANCELLED, ignoring payment result: {}", order.getOrderNumber(), result.status());
            return;
        }

        OrderStatus previousStatus = order.getStatus();

        if ("SUCCESS".equals(result.status())) {
            transaction.setStatus(TransactionStatus.SUCCEEDED);
            transaction.setPaymentReference(result.paymentReference());
            order.setStatus(OrderStatus.PAID);
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setPaymentReference(result.paymentReference());
            order.setStatus(OrderStatus.PAYMENT_FAILED);

            for (OrderItem item : order.getOrderItems()) {
                stockService.releaseStock(item.getProduct().getId(), item.getQuantity());
            }

            order.setStatus(OrderStatus.CANCELLED);
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
}
