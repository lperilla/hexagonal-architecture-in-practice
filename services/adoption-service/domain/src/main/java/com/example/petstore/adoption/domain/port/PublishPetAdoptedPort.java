package com.example.petstore.adoption.domain.port;

import com.example.petstore.adoption.domain.event.PetAdopted;

/**
 * Puerto de salida: avisar al resto del sistema de que una mascota ha sido adoptada.
 */
public interface PublishPetAdoptedPort {

    void publish(PetAdopted event);
}
