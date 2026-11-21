package com.example.petstore.notification.boot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Direcciones de correo de las notificaciones, configuradas en application.yml.
 */
@ConfigurationProperties("notification")
public record NotificationProperties(String shelterEmail, String fromEmail) {
}
