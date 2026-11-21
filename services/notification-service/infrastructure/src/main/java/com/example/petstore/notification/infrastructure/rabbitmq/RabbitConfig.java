package com.example.petstore.notification.infrastructure.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "petstore.events";
    public static final String PET_ADOPTED_ROUTING_KEY = "pet.adopted";
    public static final String QUEUE = "notification.pet-adopted";
    public static final String DEAD_LETTER_QUEUE = QUEUE + ".dlq";

    @Bean
    public TopicExchange petstoreEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    /**
     * Cola propia del servicio. Los mensajes que no se pueden procesar van a la cola de mensajes fallidos.
     */
    @Bean
    public Queue petAdoptedQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    public Queue petAdoptedDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding petAdoptedBinding(Queue petAdoptedQueue, TopicExchange petstoreEventsExchange) {
        return BindingBuilder.bind(petAdoptedQueue).to(petstoreEventsExchange).with(PET_ADOPTED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(jsonMapper);
        // El tipo lo decide el parámetro del listener, nunca una cabecera __TypeId__ del productor
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}
