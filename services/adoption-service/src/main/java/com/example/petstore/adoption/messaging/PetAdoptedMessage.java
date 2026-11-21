package com.example.petstore.adoption.messaging;

import java.time.Instant;

/**
 * Mensaje del evento pet.adopted, tal como lo define contracts/events/pet-adopted.json.
 */
public record PetAdoptedMessage(String petId, String petName, String ownerName, Instant adoptedAt) {
}
