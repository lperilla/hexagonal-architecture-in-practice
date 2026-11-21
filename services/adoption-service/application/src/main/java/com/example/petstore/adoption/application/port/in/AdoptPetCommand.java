package com.example.petstore.adoption.application.port.in;

import com.example.petstore.adoption.domain.model.Owner;
import com.example.petstore.adoption.domain.model.PetId;

import java.util.Objects;

/**
 * Petición de adopción: qué mascota y quién la adopta.
 */
public record AdoptPetCommand(PetId petId, Owner owner) {

    public AdoptPetCommand {
        Objects.requireNonNull(petId, "petId");
        Objects.requireNonNull(owner, "owner");
    }
}
