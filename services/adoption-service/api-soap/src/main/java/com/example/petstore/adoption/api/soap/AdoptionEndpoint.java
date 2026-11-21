package com.example.petstore.adoption.api.soap;

import com.example.petstore.adoption.application.port.in.AdoptPetUseCase;
import com.example.petstore.adoption.soap.contract.AdoptPetRequest;
import com.example.petstore.adoption.soap.contract.AdoptPetResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

/**
 * Adaptador de entrada SOAP: el mismo caso de uso que REST, por un segundo canal. Sin reglas de
 * negocio y sin capturar excepciones: los faults los resuelve {@link AdoptionFaultResolver}.
 */
@Endpoint
@RequiredArgsConstructor
public class AdoptionEndpoint {

    public static final String NAMESPACE = "http://example.com/petstore/adoption";

    private final AdoptPetUseCase adoptPetUseCase;
    private final AdoptionSoapMapper mapper;

    @PayloadRoot(namespace = NAMESPACE, localPart = "AdoptPetRequest")
    @ResponsePayload
    public AdoptPetResponse adoptPet(@RequestPayload AdoptPetRequest request) {
        var result = adoptPetUseCase.adopt(mapper.toCommand(request));
        return mapper.toResponse(result);
    }
}
