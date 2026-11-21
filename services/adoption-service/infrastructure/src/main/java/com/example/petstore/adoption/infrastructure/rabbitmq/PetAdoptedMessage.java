package com.example.petstore.adoption.infrastructure.rabbitmq;

import java.time.Instant;

/**
 * Mensaje del evento pet.adopted, tal como lo define contracts/events/pet-adopted.json.
 * Es el modelo técnico del adaptador: el dominio publica {@code PetAdopted}.
 */
public record PetAdoptedMessage(String petId, String petName, String ownerName, Instant adoptedAt) {
}
