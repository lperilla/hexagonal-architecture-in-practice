package com.example.petstore.notification.application.port.in;

/**
 * Puerto de entrada: avisar de una adopción. Lo invoca el listener de RabbitMQ, pero podría invocarlo
 * cualquier otro canal sin cambiar nada aquí.
 */
public interface NotifyAdoptionUseCase {

    /**
     * @throws IllegalArgumentException si el comando no describe una adopción completa
     */
    NotificationResult notifyAdoption(NotifyAdoptionCommand command);
}
