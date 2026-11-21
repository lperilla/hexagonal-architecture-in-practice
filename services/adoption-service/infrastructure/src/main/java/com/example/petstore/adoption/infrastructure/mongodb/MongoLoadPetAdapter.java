package com.example.petstore.adoption.infrastructure.mongodb;

import com.example.petstore.adoption.domain.model.Pet;
import com.example.petstore.adoption.domain.model.PetId;
import com.example.petstore.adoption.domain.port.LoadPetPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adaptador de salida: implementa un único puerto, cargar una mascota, con MongoDB. Solo traduce.
 */
@Component
@Profile("!in-memory")
@RequiredArgsConstructor
public class MongoLoadPetAdapter implements LoadPetPort {

    private final SpringDataPetRepository repository;
    private final PetMongoMapper mapper;

    @Override
    public Optional<Pet> loadPet(PetId petId) {
        return repository.findById(petId.value()).map(mapper::toDomain);
    }
}
