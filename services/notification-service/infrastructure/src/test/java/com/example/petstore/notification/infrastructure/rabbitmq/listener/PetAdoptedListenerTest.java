package com.example.petstore.notification.infrastructure.rabbitmq.listener;

import com.example.petstore.notification.application.port.in.NotificationResult;
import com.example.petstore.notification.application.port.in.NotifyAdoptionCommand;
import com.example.petstore.notification.application.port.in.NotifyAdoptionUseCase;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El adaptador de entrada, probado solo: traduce el mensaje al comando y deja pasar los errores para
 * que RabbitMQ aplique reintentos y cola de mensajes fallidos.
 */
class PetAdoptedListenerTest {

    private final List<NotifyAdoptionCommand> commands = new ArrayList<>();

    @Test
    void translatesTheMessageIntoACommand() {
        Instant adoptedAt = Instant.parse("2026-12-04T10:15:30Z");
        PetAdoptedListener listener = new PetAdoptedListener(recording(NotificationResult.SENT));

        listener.onPetAdopted(new PetAdoptedMessage("1", "Luna", "Ana", adoptedAt));

        assertThat(commands).containsExactly(new NotifyAdoptionCommand("1", "Luna", "Ana", adoptedAt));
    }

    @Test
    void letsTheErrorReachRabbitMq() {
        NotifyAdoptionUseCase failing = command -> {
            throw new IllegalArgumentException("La adopción no tiene ownerName");
        };
        PetAdoptedListener listener = new PetAdoptedListener(failing);

        assertThatThrownBy(() -> listener.onPetAdopted(new PetAdoptedMessage("1", "Luna", null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private NotifyAdoptionUseCase recording(NotificationResult result) {
        return command -> {
            commands.add(command);
            return result;
        };
    }
}
