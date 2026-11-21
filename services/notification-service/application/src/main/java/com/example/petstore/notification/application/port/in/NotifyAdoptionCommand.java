package com.example.petstore.notification.application.port.in;

import com.example.petstore.notification.domain.model.Adoption;

import java.time.Instant;

/**
 * Petición de aviso: los datos de la adopción tal como llegan del exterior. Se validan al convertirlos
 * en una {@link Adoption} del dominio.
 */
public record NotifyAdoptionCommand(String petId, String petName, String ownerName, Instant adoptedAt) {

    public Adoption toAdoption() {
        return new Adoption(petId, petName, ownerName, adoptedAt);
    }
}
