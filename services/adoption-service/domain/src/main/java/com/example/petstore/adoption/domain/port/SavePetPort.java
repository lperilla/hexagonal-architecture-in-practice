package com.example.petstore.adoption.domain.port;

import com.example.petstore.adoption.domain.model.Pet;

/**
 * Puerto de salida: guardar el estado de una mascota.
 */
public interface SavePetPort {

    void savePet(Pet pet);
}
