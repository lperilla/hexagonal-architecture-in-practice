package com.example.petstore.adoption.infrastructure.rabbitmq;

import com.example.petstore.adoption.domain.event.PetAdopted;
import com.example.petstore.adoption.domain.port.PublishPetAdoptedPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida: publica el evento de dominio PetAdopted como mensaje pet.adopted en RabbitMQ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitPetAdoptedPublisher implements PublishPetAdoptedPort {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(PetAdopted event) {
        PetAdoptedMessage message = new PetAdoptedMessage(
                event.petId().value(), event.petName(), event.owner().name(), event.adoptedAt());
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.PET_ADOPTED_ROUTING_KEY, message);
        log.info("Publicado pet.adopted: mascota {} adoptada por {}", message.petId(), message.ownerName());
    }
}
