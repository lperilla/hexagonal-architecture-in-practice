package com.example.petstore.adoption.infrastructure.mongodb;

import com.example.petstore.adoption.domain.model.Owner;
import com.example.petstore.adoption.domain.model.Pet;
import com.example.petstore.adoption.domain.model.PetId;
import com.example.petstore.adoption.domain.model.PetStatus;
import org.springframework.stereotype.Component;

/**
 * Traduce entre el modelo de dominio y el documento de MongoDB.
 */
@Component
class PetMongoMapper {

    Pet toDomain(PetDocument document) {
        Owner owner = document.getOwnerName() == null ? null : new Owner(document.getOwnerName());
        return new Pet(new PetId(document.getId()), document.getName(), PetStatus.valueOf(document.getStatus()), owner);
    }

    PetDocument toDocument(Pet pet) {
        String ownerName = pet.owner() == null ? null : pet.owner().name();
        return new PetDocument(pet.id().value(), pet.name(), pet.status().name(), ownerName);
    }
}
