package com.example.petstore.adoption.soap;

import com.example.petstore.adoption.soap.contract.AdoptionFault;
import com.example.petstore.adoption.soap.contract.ObjectFactory;
import jakarta.xml.bind.JAXBElement;
import lombok.Getter;

/**
 * Fault SOAP del contrato de adopción. Lleva el elemento de detalle que define el XSD.
 */
@Getter
public class AdoptionFaultException extends RuntimeException {

    private static final ObjectFactory FACTORY = new ObjectFactory();

    private final transient JAXBElement<AdoptionFault> detail;

    private AdoptionFaultException(String message, JAXBElement<AdoptionFault> detail) {
        super(message);
        this.detail = detail;
    }

    public static AdoptionFaultException petNotFound(String petId, String message) {
        return new AdoptionFaultException(message, FACTORY.createPetNotFoundFault(fault(petId, message)));
    }

    public static AdoptionFaultException petNotAvailable(String petId, String message) {
        return new AdoptionFaultException(message, FACTORY.createPetNotAvailableFault(fault(petId, message)));
    }

    private static AdoptionFault fault(String petId, String message) {
        AdoptionFault fault = FACTORY.createAdoptionFault();
        fault.setPetId(petId);
        fault.setMessage(message);
        return fault;
    }
}
