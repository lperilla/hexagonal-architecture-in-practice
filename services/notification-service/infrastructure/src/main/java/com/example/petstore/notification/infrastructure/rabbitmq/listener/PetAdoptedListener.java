package com.example.petstore.notification.infrastructure.rabbitmq.listener;

import com.example.petstore.notification.application.port.in.NotificationResult;
import com.example.petstore.notification.application.port.in.NotifyAdoptionCommand;
import com.example.petstore.notification.application.port.in.NotifyAdoptionUseCase;
import com.example.petstore.notification.infrastructure.rabbitmq.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada por mensajería: traduce el mensaje de RabbitMQ al comando del caso de uso.
 * Es el equivalente de un controlador REST, pero el canal es una cola. Sin reglas de negocio.
 * <p>
 * Si el caso de uso falla, la excepción vuelve a RabbitMQ: reintentos y, al final, cola de mensajes
 * fallidos. Esa política es de la infraestructura y se configura en application.yml.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PetAdoptedListener {

    private final NotifyAdoptionUseCase notifyAdoptionUseCase;

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void onPetAdopted(PetAdoptedMessage message) {
        NotificationResult result = notifyAdoptionUseCase.notifyAdoption(new NotifyAdoptionCommand(
                message.petId(), message.petName(), message.ownerName(), message.adoptedAt()));
        if (result == NotificationResult.ALREADY_NOTIFIED) {
            log.info("La adopción de la mascota {} ya se notificó: se ignora el mensaje repetido", message.petId());
        }
    }
}
