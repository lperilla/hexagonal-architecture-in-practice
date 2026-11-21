package com.example.petstore.notification.application.service;

import com.example.petstore.notification.application.port.in.NotificationResult;
import com.example.petstore.notification.application.port.in.NotifyAdoptionCommand;
import com.example.petstore.notification.application.port.in.NotifyAdoptionUseCase;
import com.example.petstore.notification.domain.model.Adoption;
import com.example.petstore.notification.domain.model.EmailAddress;
import com.example.petstore.notification.domain.model.Notification;
import com.example.petstore.notification.domain.port.NotifiedAdoptionsPort;
import com.example.petstore.notification.domain.port.SendNotificationPort;
import lombok.RequiredArgsConstructor;

/**
 * Avisa al refugio de cada adopción una sola vez, aunque el evento llegue repetido.
 * <p>
 * Sin anotaciones de Spring: se registra como bean en el módulo de arranque.
 */
@RequiredArgsConstructor
public class NotifyAdoptionService implements NotifyAdoptionUseCase {

    private final SendNotificationPort sendNotificationPort;
    private final NotifiedAdoptionsPort notifiedAdoptionsPort;
    private final EmailAddress shelter;

    @Override
    public NotificationResult notifyAdoption(NotifyAdoptionCommand command) {
        Adoption adoption = command.toAdoption();
        if (notifiedAdoptionsPort.isNotified(adoption.key())) {
            return NotificationResult.ALREADY_NOTIFIED;
        }

        sendNotificationPort.send(Notification.adoptionNotice(adoption, shelter));

        // Se marca después de enviar: si el envío falla, el reintento volverá a intentarlo
        notifiedAdoptionsPort.markNotified(adoption.key());
        return NotificationResult.SENT;
    }
}
