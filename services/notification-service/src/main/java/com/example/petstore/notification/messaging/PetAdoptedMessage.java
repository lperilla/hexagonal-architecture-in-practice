package com.example.petstore.notification.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * Evento pet.adopted tal como lo define contracts/events/pet-adopted.json. Es una clase propia de
 * este servicio: no depende de las clases del productor e ignora los campos que no conoce.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PetAdoptedMessage(String petId, String petName, String ownerName, Instant adoptedAt) {
}
