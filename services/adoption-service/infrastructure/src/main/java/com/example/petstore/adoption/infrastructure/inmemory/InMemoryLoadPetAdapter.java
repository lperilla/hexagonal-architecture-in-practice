package com.example.petstore.adoption.infrastructure.inmemory;

import com.example.petstore.adoption.domain.model.Pet;
import com.example.petstore.adoption.domain.model.PetId;
import com.example.petstore.adoption.domain.port.LoadPetPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adaptador de salida del perfil in-memory: implementa un único puerto, cargar una mascota.
 */
@Component
@Profile("in-memory")
@RequiredArgsConstructor
public class InMemoryLoadPetAdapter implements LoadPetPort {

    private final InMemoryPetStore store;

    @Override
    public Optional<Pet> loadPet(PetId petId) {
        return store.find(petId);
    }
}
