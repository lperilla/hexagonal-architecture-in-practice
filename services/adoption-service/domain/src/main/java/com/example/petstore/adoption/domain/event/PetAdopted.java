package com.example.petstore.adoption.domain.event;

import com.example.petstore.adoption.domain.model.Owner;
import com.example.petstore.adoption.domain.model.Pet;
import com.example.petstore.adoption.domain.model.PetId;

import java.time.Instant;
import java.util.Objects;

/**
 * Hecho de dominio: una mascota ha sido adoptada. Inmutable, como todo hecho ya ocurrido.
 */
public record PetAdopted(PetId petId, String petName, Owner owner, Instant adoptedAt) {

    public PetAdopted {
        Objects.requireNonNull(petId, "petId");
        Objects.requireNonNull(petName, "petName");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(adoptedAt, "adoptedAt");
    }

    /**
     * El evento de una mascota que ya ha sido adoptada.
     *
     * @throws IllegalArgumentException si la mascota no está adoptada
     */
    public static PetAdopted of(Pet adoptedPet, Instant adoptedAt) {
        if (adoptedPet.isAvailable()) {
            throw new IllegalArgumentException("La mascota " + adoptedPet.id() + " no está adoptada");
        }
        return new PetAdopted(adoptedPet.id(), adoptedPet.name(), adoptedPet.owner(), adoptedAt);
    }
}
