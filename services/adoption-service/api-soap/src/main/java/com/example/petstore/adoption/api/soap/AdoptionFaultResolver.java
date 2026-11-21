package com.example.petstore.adoption.api.soap;

import com.example.petstore.adoption.domain.exception.PetNotAvailableException;
import com.example.petstore.adoption.domain.exception.PetNotFoundException;
import com.example.petstore.adoption.domain.model.PetId;
import com.example.petstore.adoption.soap.contract.AdoptionFault;
import com.example.petstore.adoption.soap.contract.ObjectFactory;
import jakarta.xml.bind.JAXBElement;
import org.springframework.core.Ordered;
import org.springframework.oxm.Marshaller;
import org.springframework.stereotype.Component;
import org.springframework.ws.soap.SoapFault;
import org.springframework.ws.soap.server.endpoint.AbstractSoapFaultDefinitionExceptionResolver;
import org.springframework.ws.soap.server.endpoint.SoapFaultDefinition;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;

/**
 * El único sitio donde las excepciones de dominio se convierten en faults SOAP, con el detalle que
 * define el contrato: PetNotFound y PetNotAvailable.
 */
@Component
public class AdoptionFaultResolver extends AbstractSoapFaultDefinitionExceptionResolver {

    private static final Locale SPANISH = Locale.forLanguageTag("es");
    private static final ObjectFactory FACTORY = new ObjectFactory();

    private final Marshaller marshaller;

    public AdoptionFaultResolver(Marshaller marshaller) {
        this.marshaller = marshaller;
        setOrder(Ordered.HIGHEST_PRECEDENCE);
    }

    @Override
    protected SoapFaultDefinition getFaultDefinition(Object endpoint, Exception ex) {
        if (!(ex instanceof PetNotFoundException) && !(ex instanceof PetNotAvailableException)) {
            return null;
        }
        SoapFaultDefinition definition = new SoapFaultDefinition();
        definition.setFaultCode(SoapFaultDefinition.CLIENT);
        definition.setFaultStringOrReason(ex.getMessage());
        definition.setLocale(SPANISH);
        return definition;
    }

    @Override
    protected void customizeFault(Object endpoint, Exception ex, SoapFault fault) {
        JAXBElement<AdoptionFault> detail = switch (ex) {
            case PetNotFoundException e -> FACTORY.createPetNotFoundFault(fault(e.getPetId(), e.getMessage()));
            case PetNotAvailableException e -> FACTORY.createPetNotAvailableFault(fault(e.getPetId(), e.getMessage()));
            default -> throw new IllegalStateException("Excepción sin fault SOAP: " + ex);
        };
        try {
            marshaller.marshal(detail, fault.addFaultDetail().getResult());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static AdoptionFault fault(PetId petId, String message) {
        AdoptionFault fault = FACTORY.createAdoptionFault();
        fault.setPetId(petId.value());
        fault.setMessage(message);
        return fault;
    }
}
