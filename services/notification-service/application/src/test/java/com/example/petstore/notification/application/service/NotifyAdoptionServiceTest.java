package com.example.petstore.notification.application.service;

import com.example.petstore.notification.application.port.in.NotificationResult;
import com.example.petstore.notification.application.port.in.NotifyAdoptionCommand;
import com.example.petstore.notification.domain.model.AdoptionKey;
import com.example.petstore.notification.domain.model.EmailAddress;
import com.example.petstore.notification.domain.model.Notification;
import com.example.petstore.notification.domain.port.NotifiedAdoptionsPort;
import com.example.petstore.notification.domain.port.SendNotificationPort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El caso de uso, probado sin Spring, sin RabbitMQ y sin servidor de correo.
 */
class NotifyAdoptionServiceTest {

    private static final EmailAddress SHELTER = new EmailAddress("refugio@petstore.example");
    private static final NotifyAdoptionCommand LUNA_ADOPTED =
            new NotifyAdoptionCommand("1", "Luna", "Ana", Instant.parse("2026-12-04T10:15:30Z"));

    private final RecordingSender sender = new RecordingSender();
    private final FakeNotifiedAdoptions notifiedAdoptions = new FakeNotifiedAdoptions();
    private final NotifyAdoptionService service = new NotifyAdoptionService(sender, notifiedAdoptions, SHELTER);

    @Test
    void notifiesTheShelterOfAnAdoption() {
        NotificationResult result = service.notifyAdoption(LUNA_ADOPTED);

        assertThat(result).isEqualTo(NotificationResult.SENT);
        assertThat(sender.sent).singleElement().satisfies(notification -> {
            assertThat(notification.recipient()).isEqualTo(SHELTER);
            assertThat(notification.subject()).isEqualTo("Ana ha adoptado a Luna");
        });
    }

    @Test
    void notifiesEachAdoptionOnlyOnce() {
        service.notifyAdoption(LUNA_ADOPTED);

        NotificationResult result = service.notifyAdoption(LUNA_ADOPTED);

        assertThat(result).isEqualTo(NotificationResult.ALREADY_NOTIFIED);
        assertThat(sender.sent).hasSize(1);
    }

    @Test
    void triesAgainIfSendingFailed() {
        sender.failNext = true;
        assertThatThrownBy(() -> service.notifyAdoption(LUNA_ADOPTED)).isInstanceOf(IllegalStateException.class);

        NotificationResult retry = service.notifyAdoption(LUNA_ADOPTED);

        assertThat(retry).isEqualTo(NotificationResult.SENT);
        assertThat(sender.sent).hasSize(1);
    }

    @Test
    void rejectsAnIncompleteAdoption() {
        assertThatThrownBy(() -> service.notifyAdoption(new NotifyAdoptionCommand("1", "Luna", null, null)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(sender.sent).isEmpty();
    }

    private static final class RecordingSender implements SendNotificationPort {

        private final List<Notification> sent = new ArrayList<>();
        private boolean failNext;

        @Override
        public void send(Notification notification) {
            if (failNext) {
                failNext = false;
                throw new IllegalStateException("El servidor de correo no responde");
            }
            sent.add(notification);
        }
    }

    private static final class FakeNotifiedAdoptions implements NotifiedAdoptionsPort {

        private final Set<AdoptionKey> notified = new HashSet<>();

        @Override
        public boolean isNotified(AdoptionKey adoption) {
            return notified.contains(adoption);
        }

        @Override
        public void markNotified(AdoptionKey adoption) {
            notified.add(adoption);
        }
    }
}
