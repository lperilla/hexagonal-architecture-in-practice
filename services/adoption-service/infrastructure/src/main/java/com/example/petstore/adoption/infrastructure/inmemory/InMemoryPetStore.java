package com.example.petstore.adoption.infrastructure.inmemory;

import com.example.petstore.adoption.domain.model.Pet;
import com.example.petstore.adoption.domain.model.PetId;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Almacén en memoria que comparten los dos adaptadores del perfil in-memory: es el equivalente a la
 * base de datos, no un puerto. Como Pet es inmutable, se guarda tal cual, sin copias defensivas:
 * nadie puede cambiar una mascota guardada desde fuera.
 */
@Component
@Profile("in-memory")
public class InMemoryPetStore {

    private final Map<PetId, Pet> pets = new ConcurrentHashMap<>();

    Optional<Pet> find(PetId petId) {
        return Optional.ofNullable(pets.get(petId));
    }

    void put(Pet pet) {
        pets.put(pet.id(), pet);
    }
}
