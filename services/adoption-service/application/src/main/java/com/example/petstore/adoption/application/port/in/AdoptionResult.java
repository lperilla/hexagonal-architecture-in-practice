package com.example.petstore.adoption.application.port.in;

import com.example.petstore.adoption.domain.model.Owner;
import com.example.petstore.adoption.domain.model.Pet;
import com.example.petstore.adoption.domain.model.PetId;
import com.example.petstore.adoption.domain.model.PetStatus;

/**
 * Resultado de una adopción: la mascota con su nuevo dueño.
 */
public record AdoptionResult(PetId petId, String petName, PetStatus status, Owner owner) {

    public static AdoptionResult from(Pet pet) {
        return new AdoptionResult(pet.id(), pet.name(), pet.status(), pet.owner());
    }
}
