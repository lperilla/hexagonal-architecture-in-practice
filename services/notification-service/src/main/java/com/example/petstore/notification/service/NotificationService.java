package com.example.petstore.notification.service;

import com.example.petstore.notification.config.NotificationProperties;
import com.example.petstore.notification.messaging.PetAdoptedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;
    private final Environment environment;

    // Un mismo mensaje puede llegar dos veces: se recuerdan las adopciones ya notificadas
    private final Set<String> notifiedAdoptions = ConcurrentHashMap.newKeySet();

    public void notifyAdoption(PetAdoptedMessage message) {
        if (message.petId() == null || message.petName() == null
                || message.ownerName() == null || message.adoptedAt() == null) {
            throw new IllegalArgumentException("Mensaje pet.adopted incompleto: " + message);
        }

        String adoptionKey = message.petId() + "@" + message.adoptedAt();
        if (notifiedAdoptions.contains(adoptionKey)) {
            log.info("La adopción {} ya se notificó: se ignora el mensaje repetido", adoptionKey);
            return;
        }

        String subject = message.ownerName() + " ha adoptado a " + message.petName();
        String text = """
                Hola:

                %s ha adoptado a %s (mascota %s).
                Fecha de la adopción: %s

                Tienda de mascotas
                """.formatted(message.ownerName(), message.petName(), message.petId(), message.adoptedAt());

        if (environment.matchesProfiles("log-notifications")) {
            log.info("Notificación para {} (solo log): {}", properties.shelterEmail(), subject);
        } else {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(properties.fromEmail());
            mail.setTo(properties.shelterEmail());
            mail.setSubject(subject);
            mail.setText(text);
            mailSender.send(mail);
            log.info("Email enviado a {}: {}", properties.shelterEmail(), subject);
        }

        notifiedAdoptions.add(adoptionKey);
    }
}
