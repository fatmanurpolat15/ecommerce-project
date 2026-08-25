package com.fatmanur.ecommerce.transaction.consumer;

import tools.jackson.databind.ObjectMapper;
import com.fatmanur.ecommerce.transaction.dto.PaymentResult;
import com.fatmanur.ecommerce.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentConsumer {

    private final TransactionService transactionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment.results", groupId = "shop-payment-results")
    public void handlePaymentResult(String message) {
        try {
            PaymentResult result = objectMapper.readValue(message, PaymentResult.class);
            log.info("Payment result received for order {}: {}", result.orderId(), result.status());
            transactionService.handlePaymentResult(result);
        } catch (Exception e) {
            log.error("Failed to deserialize payment result: {}", message, e);
        }
    }
}
