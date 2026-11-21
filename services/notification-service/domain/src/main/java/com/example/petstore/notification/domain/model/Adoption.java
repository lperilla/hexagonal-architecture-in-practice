package com.example.petstore.notification.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Una adopción de la que hay que avisar: qué mascota, quién la adoptó y cuándo.
 */
public record Adoption(String petId, String petName, String ownerName, Instant adoptedAt) {

    public Adoption {
        requireText(petId, "petId");
        requireText(petName, "petName");
        requireText(ownerName, "ownerName");
        Objects.requireNonNull(adoptedAt, "adoptedAt");
    }

    /**
     * Identifica la adopción: una mascota se adopta en un instante concreto.
     */
    public AdoptionKey key() {
        return new AdoptionKey(petId, adoptedAt);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("La adopción no tiene " + field);
        }
    }
}
