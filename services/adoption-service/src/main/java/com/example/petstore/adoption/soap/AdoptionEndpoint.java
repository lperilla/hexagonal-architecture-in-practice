package com.example.petstore.adoption.soap;

import com.example.petstore.adoption.model.PetDocument;
import com.example.petstore.adoption.service.PetService;
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

    private final PetService petService;

    @PayloadRoot(namespace = NAMESPACE, localPart = "AdoptPetRequest")
    @ResponsePayload
    public AdoptPetResponse adoptPet(@RequestPayload AdoptPetRequest request) {
        try {
            PetDocument pet = petService.adopt(request.getPetId(), request.getOwnerName());

            AdoptPetResponse response = new AdoptPetResponse();
            response.setPetId(pet.getId());
            response.setPetName(pet.getName());
            response.setStatus(PetStatus.fromValue(pet.getStatus()));
            response.setOwnerName(pet.getOwnerName());
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
