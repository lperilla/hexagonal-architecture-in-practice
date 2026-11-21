package com.example.petstore.adoption.domain.model;

/**
 * Dueño de una mascota adoptada.
 */
public record Owner(String name) {

    public Owner {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del dueño no puede estar vacío");
        }
    }
}
