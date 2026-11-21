package com.example.petstore.notification.infrastructure.mail;

import com.example.petstore.notification.domain.model.Notification;
import com.example.petstore.notification.domain.port.SendNotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida: envía el aviso como email por SMTP (Mailpit en la demo).
 */
@Slf4j
@Component
@Profile("!log-notifications")
public class SmtpNotificationSender implements SendNotificationPort {

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public SmtpNotificationSender(JavaMailSender mailSender, @Value("${notification.from-email}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    @Override
    public void send(Notification notification) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(fromEmail);
        mail.setTo(notification.recipient().value());
        mail.setSubject(notification.subject());
        mail.setText(notification.text());
        mailSender.send(mail);
        log.info("Email enviado a {}: {}", notification.recipient(), notification.subject());
    }
}
