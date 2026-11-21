package com.example.petstore.adoption.domain.exception;

import com.example.petstore.adoption.domain.model.PetId;

/**
 * No existe ninguna mascota con ese identificador.
 */
public class PetNotFoundException extends RuntimeException {

    private final PetId petId;

    public PetNotFoundException(PetId petId) {
        super("La mascota " + petId + " no existe");
        this.petId = petId;
    }

    public PetId getPetId() {
        return petId;
    }
}
