package com.example.petstore.adoption.infrastructure.rabbitmq;

import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.ClassMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "petstore.events";
    public static final String PET_ADOPTED_ROUTING_KEY = "pet.adopted";

    @Bean
    public TopicExchange petstoreEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(jsonMapper);
        // El contrato del evento no incluye la cabecera __TypeId__: los consumidores
        // no deben depender de los nombres de clase de este servicio.
        converter.setClassMapper(new ClassMapper() {
            @Override
            public void fromClass(Class<?> clazz, MessageProperties properties) {
            }

            @Override
            public Class<?> toClass(MessageProperties properties) {
                throw new UnsupportedOperationException("Este servicio solo publica eventos");
            }
        });
        return converter;
    }
}
