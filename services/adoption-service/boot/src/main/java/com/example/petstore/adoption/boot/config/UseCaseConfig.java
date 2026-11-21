package com.example.petstore.adoption.boot.config;

import com.example.petstore.adoption.application.port.in.AdoptPetUseCase;
import com.example.petstore.adoption.application.service.AdoptPetService;
import com.example.petstore.adoption.domain.port.LoadPetPort;
import com.example.petstore.adoption.domain.port.PublishPetAdoptedPort;
import com.example.petstore.adoption.domain.port.SavePetPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Composición: aquí, y solo aquí, se conecta el caso de uso con los adaptadores que implementan sus
 * puertos. El servicio de aplicación no lleva anotaciones de Spring.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public AdoptPetUseCase adoptPetUseCase(LoadPetPort loadPetPort, SavePetPort savePetPort,
                                           PublishPetAdoptedPort publishPetAdoptedPort, Clock clock) {
        return new AdoptPetService(loadPetPort, savePetPort, publishPetAdoptedPort, clock);
    }
}
