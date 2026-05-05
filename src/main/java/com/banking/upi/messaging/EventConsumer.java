package com.banking.upi.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventConsumer {

    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    public void handleEvent(String message) {
        log.info("Received event from upi.notifications: {}", message);
        // In production: parse and process notification (push notification, SMS, email)
    }
}
