package com.example.petstore.adoption.controller;

import com.example.petstore.adoption.api.contract.AdoptionApi;
import com.example.petstore.adoption.api.contract.model.AdoptionRequest;
import com.example.petstore.adoption.api.contract.model.AdoptionResponse;
import com.example.petstore.adoption.api.contract.model.PetStatus;
import com.example.petstore.adoption.model.PetDocument;
import com.example.petstore.adoption.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdoptionController implements AdoptionApi {

    private final PetService petService;

    @Override
    public ResponseEntity<AdoptionResponse> adoptPet(String petId, AdoptionRequest adoptionRequest) {
        PetDocument pet = petService.adopt(petId, adoptionRequest.getOwnerName());
        return ResponseEntity.ok(new AdoptionResponse(
                pet.getId(), pet.getName(), PetStatus.fromValue(pet.getStatus()), pet.getOwnerName()));
    }
}
