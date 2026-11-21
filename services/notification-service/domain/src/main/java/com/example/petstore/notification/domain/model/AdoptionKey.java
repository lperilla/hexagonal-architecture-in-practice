package com.example.petstore.notification.domain.model;

import java.time.Instant;

/**
 * Clave de una adopción, para no notificarla dos veces aunque el evento llegue repetido.
 */
public record AdoptionKey(String petId, Instant adoptedAt) {
}
