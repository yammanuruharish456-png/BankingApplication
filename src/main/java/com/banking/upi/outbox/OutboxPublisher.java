package com.banking.upi.outbox;

import com.banking.upi.domain.OutboxEvent;
import com.banking.upi.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE = "upi.events";

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> unpublished = outboxEventRepository.findByPublishedFalse();

        if (unpublished.isEmpty()) {
            return;
        }

        log.debug("Publishing {} outbox events", unpublished.size());

        for (OutboxEvent event : unpublished) {
            try {
                String routingKey = "upi." + event.getEventType().toLowerCase();
                rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event.getPayload());
                event.setPublished(true);
                outboxEventRepository.save(event);
                log.info("Published outbox event: id={}, type={}", event.getId(), event.getEventType());
            } catch (Exception e) {
                log.error("Failed to publish outbox event id={}: {}", event.getId(), e.getMessage());
            }
        }
    }
}
