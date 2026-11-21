package com.example.petstore.notification.infrastructure.logging;

import com.example.petstore.notification.domain.model.Notification;
import com.example.petstore.notification.domain.port.SendNotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida para el perfil log-notifications: escribe el aviso en el log en lugar de enviarlo.
 * Otra implementación del mismo puerto; el caso de uso no se entera.
 */
@Slf4j
@Component
@Profile("log-notifications")
public class LogNotificationSender implements SendNotificationPort {

    @Override
    public void send(Notification notification) {
        log.info("Notificación para {} (solo log): {}", notification.recipient(), notification.subject());
    }
}
