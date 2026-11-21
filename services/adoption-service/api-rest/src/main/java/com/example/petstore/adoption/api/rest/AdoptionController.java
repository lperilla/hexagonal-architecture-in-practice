package com.example.petstore.adoption.api.rest;

import com.example.petstore.adoption.api.contract.AdoptionApi;
import com.example.petstore.adoption.api.contract.model.AdoptionRequest;
import com.example.petstore.adoption.api.contract.model.AdoptionResponse;
import com.example.petstore.adoption.application.port.in.AdoptPetUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST: traduce HTTP al caso de uso y el resultado a HTTP. Sin reglas de negocio.
 */
@RestController
@RequiredArgsConstructor
public class AdoptionController implements AdoptionApi {

    private final AdoptPetUseCase adoptPetUseCase;
    private final AdoptionRestMapper mapper;

    @Override
    public ResponseEntity<AdoptionResponse> adoptPet(String petId, AdoptionRequest adoptionRequest) {
        var result = adoptPetUseCase.adopt(mapper.toCommand(petId, adoptionRequest));
        return ResponseEntity.ok(mapper.toResponse(result));
    }
}
