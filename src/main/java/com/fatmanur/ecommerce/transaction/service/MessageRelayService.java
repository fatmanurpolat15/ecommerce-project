package com.fatmanur.ecommerce.transaction.service;

import com.fatmanur.ecommerce.transaction.entity.OutboxMessage;
import com.fatmanur.ecommerce.transaction.enums.OutboxStatus;
import com.fatmanur.ecommerce.transaction.repository.OutboxRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageRelayService {
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 600000)
    @Transactional
    public void sendPendingMessages() {
        List<OutboxMessage> pendingMessages = outboxRepository.findByStatus(OutboxStatus.PENDING);

        for (OutboxMessage message : pendingMessages) {
            try {
                SendResult<String, String> result = kafkaTemplate.send(
                        message.getTopic(),
                        message.getMessageKey(),
                        message.getPayload()
                ).get(10, TimeUnit.SECONDS);

                message.setStatus(OutboxStatus.SENT);
                message.setSentAt(LocalDateTime.now());
                outboxRepository.save(message);
                log.info("Outbox message sent: id={}, topic={}, partition={}, offset={}",
                        message.getId(), message.getTopic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } catch (Exception e) {
                message.setStatus(OutboxStatus.FAILED);
                outboxRepository.save(message);
                log.error("Failed to send outbox message: id={}", message.getId(), e);
            }
        }
    }
}
