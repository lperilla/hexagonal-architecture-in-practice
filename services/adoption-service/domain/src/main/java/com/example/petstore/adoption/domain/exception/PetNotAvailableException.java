package com.example.petstore.adoption.domain.exception;

import com.example.petstore.adoption.domain.model.PetId;

/**
 * La mascota existe, pero no está disponible para adopción.
 */
public class PetNotAvailableException extends RuntimeException {

    private final PetId petId;

    public PetNotAvailableException(PetId petId) {
        super("La mascota " + petId + " no está disponible para adopción");
        this.petId = petId;
    }

    public PetId getPetId() {
        return petId;
    }
}
