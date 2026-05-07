package com.banking.application.payments.messaging;

import com.banking.application.payments.entity.OutboxStatus;
import com.banking.application.payments.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           RabbitTemplate rabbitTemplate,
                           @Value("${app.rabbit.exchange}") String exchange,
                           @Value("${app.rabbit.routing-key}") String routingKey) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publisher-delay-ms:3000}")
    @Transactional
    public void publishPending() {
        outboxEventRepository.findTop100ByStatusOrderByIdAsc(OutboxStatus.PENDING).forEach(event -> {
            try {
                rabbitTemplate.convertAndSend(exchange, routingKey, event.getPayloadJson());
                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
                outboxEventRepository.save(event);
            } catch (Exception ex) {
                log.error("Failed to publish outbox event id={}", event.getId(), ex);
            }
        });
    }
}
