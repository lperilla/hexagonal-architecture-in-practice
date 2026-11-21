package com.example.petstore.notification;

import com.example.petstore.notification.domain.port.SendNotificationPort;
import com.example.petstore.notification.infrastructure.logging.LogNotificationSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sustituir la infraestructura: con el perfil log-notifications el aviso se escribe en el log, sin
 * tocar el caso de uso. El listener no arranca para no competir por la cola con otros tests.
 */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@ActiveProfiles("log-notifications")
class LogNotificationsProfileTest {

    @Autowired
    private SendNotificationPort sendNotificationPort;

    @Test
    void usesTheLogAdapterInsteadOfSmtp() {
        assertThat(sendNotificationPort).isInstanceOf(LogNotificationSender.class);
    }
}
