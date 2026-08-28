package com.fatmanur.ecommerce.order.service;

import com.fatmanur.ecommerce.order.entity.Order;
import com.fatmanur.ecommerce.order.entity.OrderItem;
import com.fatmanur.ecommerce.order.entity.OrderStatusHistory;
import com.fatmanur.ecommerce.order.enums.OrderStatus;
import com.fatmanur.ecommerce.order.repository.OrderRepository;
import com.fatmanur.ecommerce.stock.service.StockService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Scheduled(fixedDelay = 600000)
    @Transactional
    public void cancelStuckOrders() {
        LocalDateTime cutoff = LocalDateTime.now(ZoneId.of("Europe/Istanbul")).minusMinutes(20);
        List<Order> stuckOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.CREATED, cutoff);

        for (Order order : stuckOrders) {
            for (OrderItem orderItem : order.getOrderItems()) {
                stockService.releaseStock(orderItem.getProduct().getId(), orderItem.getQuantity());
            }

            order.setStatus(OrderStatus.CANCELLED);
            OrderStatusHistory history = OrderStatusHistory.builder()
                    .order(order)
                    .previousStatus(OrderStatus.CREATED)
                    .newStatus(OrderStatus.CANCELLED)
                    .changedAt(LocalDateTime.now())
                    .build();
            order.getStatusHistory().add(history);

            orderRepository.save(order);
            log.info("Order {} has been automatically cancelled due to timeout.", order.getOrderNumber());


        }

    }


}
