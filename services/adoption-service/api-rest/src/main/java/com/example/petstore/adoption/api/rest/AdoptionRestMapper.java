package com.example.petstore.adoption.api.rest;

import com.example.petstore.adoption.api.contract.model.AdoptionRequest;
import com.example.petstore.adoption.api.contract.model.AdoptionResponse;
import com.example.petstore.adoption.api.contract.model.PetStatus;
import com.example.petstore.adoption.application.port.in.AdoptPetCommand;
import com.example.petstore.adoption.application.port.in.AdoptionResult;
import com.example.petstore.adoption.domain.model.Owner;
import com.example.petstore.adoption.domain.model.PetId;
import org.springframework.stereotype.Component;

/**
 * Traduce entre los DTOs generados del contrato REST y el caso de uso. Los DTOs no salen de este módulo.
 */
@Component
class AdoptionRestMapper {

    AdoptPetCommand toCommand(String petId, AdoptionRequest request) {
        return new AdoptPetCommand(new PetId(petId), new Owner(request.getOwnerName()));
    }

    AdoptionResponse toResponse(AdoptionResult result) {
        return new AdoptionResponse(result.petId().value(), result.petName(),
                PetStatus.fromValue(result.status().name()), result.owner().name());
    }
}
