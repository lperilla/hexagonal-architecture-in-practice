package com.example.petstore.adoption.infrastructure.mongodb;

import com.example.petstore.adoption.domain.model.Pet;
import com.example.petstore.adoption.domain.port.SavePetPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida: implementa un único puerto, guardar una mascota, con MongoDB. Solo traduce.
 */
@Component
@Profile("!in-memory")
@RequiredArgsConstructor
public class MongoSavePetAdapter implements SavePetPort {

    private final SpringDataPetRepository repository;
    private final PetMongoMapper mapper;

    @Override
    public void savePet(Pet pet) {
        repository.save(mapper.toDocument(pet));
    }
}
