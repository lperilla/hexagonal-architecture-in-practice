package com.example.petstore.adoption.application.service;

import com.example.petstore.adoption.application.port.in.AdoptPetCommand;
import com.example.petstore.adoption.application.port.in.AdoptPetUseCase;
import com.example.petstore.adoption.application.port.in.AdoptionResult;
import com.example.petstore.adoption.domain.event.PetAdopted;
import com.example.petstore.adoption.domain.exception.PetNotFoundException;
import com.example.petstore.adoption.domain.model.Pet;
import com.example.petstore.adoption.domain.port.LoadPetPort;
import com.example.petstore.adoption.domain.port.PublishPetAdoptedPort;
import com.example.petstore.adoption.domain.port.SavePetPort;
import lombok.RequiredArgsConstructor;

import java.time.Clock;

/**
 * Orquesta la adopción: carga la mascota, aplica la regla del dominio, guarda y publica el evento.
 * <p>
 * Sin anotaciones de Spring: se registra como bean en el módulo de arranque.
 */
@RequiredArgsConstructor
public class AdoptPetService implements AdoptPetUseCase {

    private final LoadPetPort loadPetPort;
    private final SavePetPort savePetPort;
    private final PublishPetAdoptedPort publishPetAdoptedPort;
    private final Clock clock;

    @Override
    public AdoptionResult adopt(AdoptPetCommand command) {
        Pet pet = loadPetPort.loadPet(command.petId())
                .orElseThrow(() -> new PetNotFoundException(command.petId()));

        Pet adopted = pet.adopt(command.owner());

        savePetPort.savePet(adopted);
        publishPetAdoptedPort.publish(PetAdopted.of(adopted, clock.instant()));
        return AdoptionResult.from(adopted);
    }
}
