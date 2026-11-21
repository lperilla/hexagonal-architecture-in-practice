package com.example.petstore.adoption.infrastructure.inmemory;

import com.example.petstore.adoption.domain.model.Pet;
import com.example.petstore.adoption.domain.port.SavePetPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida del perfil in-memory: implementa un único puerto, guardar una mascota.
 */
@Component
@Profile("in-memory")
@RequiredArgsConstructor
public class InMemorySavePetAdapter implements SavePetPort {

    private final InMemoryPetStore store;

    @Override
    public void savePet(Pet pet) {
        store.put(pet);
    }
}
