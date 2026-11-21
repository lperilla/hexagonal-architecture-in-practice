package com.example.petstore.adoption.soap;

import com.example.petstore.adoption.application.port.in.AdoptPetCommand;
import com.example.petstore.adoption.application.port.in.AdoptPetUseCase;
import com.example.petstore.adoption.application.port.in.AdoptionResult;
import com.example.petstore.adoption.domain.model.Owner;
import com.example.petstore.adoption.domain.model.PetId;
import com.example.petstore.adoption.soap.contract.AdoptPetRequest;
import com.example.petstore.adoption.soap.contract.AdoptPetResponse;
import com.example.petstore.adoption.soap.contract.PetStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
@RequiredArgsConstructor
public class AdoptionEndpoint {

    public static final String NAMESPACE = "http://example.com/petstore/adoption";

    private final AdoptPetUseCase adoptPetUseCase;

    @PayloadRoot(namespace = NAMESPACE, localPart = "AdoptPetRequest")
    @ResponsePayload
    public AdoptPetResponse adoptPet(@RequestPayload AdoptPetRequest request) {
        try {
            AdoptionResult result = adoptPetUseCase.adopt(
                    new AdoptPetCommand(new PetId(request.getPetId()), new Owner(request.getOwnerName())));

            AdoptPetResponse response = new AdoptPetResponse();
            response.setPetId(result.petId().value());
            response.setPetName(result.petName());
            response.setStatus(PetStatus.fromValue(result.status().name()));
            response.setOwnerName(result.owner().name());
            return response;
        } catch (ResponseStatusException e) {
            // El servicio solo sabe responder con códigos HTTP: hay que traducirlos a faults SOAP
            if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                throw AdoptionFaultException.petNotFound(request.getPetId(), e.getReason());
            }
            if (e.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT)) {
                throw AdoptionFaultException.petNotAvailable(request.getPetId(), e.getReason());
            }
            throw e;
        }
    }
}
