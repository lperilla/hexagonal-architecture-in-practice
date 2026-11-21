package com.example.petstore.adoption.soap;

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
 * Convierte AdoptionFaultException en un fault SOAP de cliente con el detalle del contrato.
 */
@Component
public class AdoptionFaultResolver extends AbstractSoapFaultDefinitionExceptionResolver {

    private static final Locale SPANISH = Locale.forLanguageTag("es");

    private final Marshaller marshaller;

    public AdoptionFaultResolver(Marshaller marshaller) {
        this.marshaller = marshaller;
        setOrder(Ordered.HIGHEST_PRECEDENCE);
    }

    @Override
    protected SoapFaultDefinition getFaultDefinition(Object endpoint, Exception ex) {
        if (!(ex instanceof AdoptionFaultException)) {
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
        AdoptionFaultException adoptionFault = (AdoptionFaultException) ex;
        try {
            marshaller.marshal(adoptionFault.getDetail(), fault.addFaultDetail().getResult());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
