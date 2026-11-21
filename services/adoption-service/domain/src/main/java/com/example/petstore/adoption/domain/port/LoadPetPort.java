package com.example.petstore.adoption.domain.port;

import com.example.petstore.adoption.domain.model.Pet;
import com.example.petstore.adoption.domain.model.PetId;

import java.util.Optional;

/**
 * Puerto de salida: obtener una mascota. El núcleo lo define; la tecnología lo implementa.
 */
public interface LoadPetPort {

    Optional<Pet> loadPet(PetId petId);
}
