package com.example.petstore.adoption.api.soap;

import com.example.petstore.adoption.application.port.in.AdoptPetCommand;
import com.example.petstore.adoption.application.port.in.AdoptionResult;
import com.example.petstore.adoption.domain.model.Owner;
import com.example.petstore.adoption.domain.model.PetId;
import com.example.petstore.adoption.soap.contract.AdoptPetRequest;
import com.example.petstore.adoption.soap.contract.AdoptPetResponse;
import com.example.petstore.adoption.soap.contract.PetStatus;
import org.springframework.stereotype.Component;

/**
 * Traduce entre las clases JAXB del contrato SOAP y el caso de uso. Las clases JAXB no salen de este módulo.
 */
@Component
class AdoptionSoapMapper {

    AdoptPetCommand toCommand(AdoptPetRequest request) {
        return new AdoptPetCommand(new PetId(request.getPetId()), new Owner(request.getOwnerName()));
    }

    AdoptPetResponse toResponse(AdoptionResult result) {
        AdoptPetResponse response = new AdoptPetResponse();
        response.setPetId(result.petId().value());
        response.setPetName(result.petName());
        response.setStatus(PetStatus.fromValue(result.status().name()));
        response.setOwnerName(result.owner().name());
        return response;
    }
}
