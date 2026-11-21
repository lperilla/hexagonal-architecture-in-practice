package com.example.petstore.adoption.domain.model;

/**
 * Identificador de una mascota.
 */
public record PetId(String value) {

    public PetId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El identificador de la mascota no puede estar vacío");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
