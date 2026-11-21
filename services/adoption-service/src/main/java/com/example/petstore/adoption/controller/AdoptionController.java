package com.example.petstore.adoption.controller;

import com.example.petstore.adoption.api.contract.AdoptionApi;
import com.example.petstore.adoption.api.contract.model.AdoptionRequest;
import com.example.petstore.adoption.api.contract.model.AdoptionResponse;
import com.example.petstore.adoption.api.contract.model.PetStatus;
import com.example.petstore.adoption.application.port.in.AdoptPetCommand;
import com.example.petstore.adoption.application.port.in.AdoptPetUseCase;
import com.example.petstore.adoption.application.port.in.AdoptionResult;
import com.example.petstore.adoption.domain.model.Owner;
import com.example.petstore.adoption.domain.model.PetId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdoptionController implements AdoptionApi {

    private final AdoptPetUseCase adoptPetUseCase;

    @Override
    public ResponseEntity<AdoptionResponse> adoptPet(String petId, AdoptionRequest adoptionRequest) {
        AdoptionResult result = adoptPetUseCase.adopt(
                new AdoptPetCommand(new PetId(petId), new Owner(adoptionRequest.getOwnerName())));
        return ResponseEntity.ok(new AdoptionResponse(
                result.petId().value(), result.petName(), PetStatus.fromValue(result.status().name()),
                result.owner().name()));
    }
}
