package com.example.petstore.notification.boot.config;

import com.example.petstore.notification.application.port.in.NotifyAdoptionUseCase;
import com.example.petstore.notification.application.service.NotifyAdoptionService;
import com.example.petstore.notification.domain.model.EmailAddress;
import com.example.petstore.notification.domain.port.NotifiedAdoptionsPort;
import com.example.petstore.notification.domain.port.SendNotificationPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composición: conecta el caso de uso con los adaptadores activos y con la dirección del refugio, que
 * llega de la configuración. El servicio de aplicación no lleva anotaciones de Spring.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public NotifyAdoptionUseCase notifyAdoptionUseCase(SendNotificationPort sendNotificationPort,
                                                       NotifiedAdoptionsPort notifiedAdoptionsPort,
                                                       NotificationProperties properties) {
        return new NotifyAdoptionService(sendNotificationPort, notifiedAdoptionsPort,
                new EmailAddress(properties.shelterEmail()));
    }
}
