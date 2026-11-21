package com.example.petstore.adoption.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.config.annotation.WsConfigurer;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.server.endpoint.interceptor.PayloadValidatingInterceptor;

import java.util.List;

@Configuration
public class WebServiceConfig implements WsConfigurer {

    @Bean
    public Jaxb2Marshaller jaxb2Marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("com.example.petstore.adoption.soap.contract");
        return marshaller;
    }

    @Bean
    public PayloadValidatingInterceptor payloadValidatingInterceptor() {
        // Valida las peticiones contra el XSD: un nombre vacío se rechaza con un fault de cliente
        PayloadValidatingInterceptor interceptor = new PayloadValidatingInterceptor();
        interceptor.setSchema(new ClassPathResource("wsdl/adoption-types.xsd"));
        interceptor.setValidateRequest(true);
        interceptor.setValidateResponse(false);
        return interceptor;
    }

    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptors) {
        interceptors.add(payloadValidatingInterceptor());
    }
}
