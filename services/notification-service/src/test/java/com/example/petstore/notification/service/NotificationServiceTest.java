package com.example.petstore.notification.service;

import com.example.petstore.notification.config.NotificationProperties;
import com.example.petstore.notification.messaging.PetAdoptedMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final PetAdoptedMessage LUNA_ADOPTED =
            new PetAdoptedMessage("1", "Luna", "Ana", Instant.parse("2026-12-04T10:15:30Z"));

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private Environment environment;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(mailSender,
                new NotificationProperties("refugio@petstore.example", "notificaciones@petstore.example"), environment);
    }

    @Test
    void emailsTheShelterWhenAPetIsAdopted() {
        notificationService.notifyAdoption(LUNA_ADOPTED);

        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mail.capture());
        assertThat(mail.getValue().getTo()).containsExactly("refugio@petstore.example");
        assertThat(mail.getValue().getFrom()).isEqualTo("notificaciones@petstore.example");
        assertThat(mail.getValue().getSubject()).isEqualTo("Ana ha adoptado a Luna");
        assertThat(mail.getValue().getText()).contains("Ana ha adoptado a Luna (mascota 1)");
    }

    @Test
    void sendsOnlyOneEmailForTheSameAdoption() {
        notificationService.notifyAdoption(LUNA_ADOPTED);
        notificationService.notifyAdoption(LUNA_ADOPTED);

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void onlyLogsWithTheLogNotificationsProfile() {
        when(environment.matchesProfiles("log-notifications")).thenReturn(true);

        notificationService.notifyAdoption(LUNA_ADOPTED);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void rejectsAnIncompleteMessage() {
        assertThatThrownBy(() -> notificationService.notifyAdoption(new PetAdoptedMessage("1", "Luna", null, null)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
