package com.banking.application.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    DirectExchange paymentsExchange(@Value("${app.rabbit.exchange}") String exchange) {
        return new DirectExchange(exchange);
    }

    @Bean
    Queue notificationsQueue(@Value("${app.rabbit.queue}") String queue) {
        return new Queue(queue, true);
    }

    @Bean
    Binding notificationsBinding(Queue notificationsQueue,
                                 DirectExchange paymentsExchange,
                                 @Value("${app.rabbit.routing-key}") String routingKey) {
        return BindingBuilder.bind(notificationsQueue).to(paymentsExchange).with(routingKey);
    }
}
