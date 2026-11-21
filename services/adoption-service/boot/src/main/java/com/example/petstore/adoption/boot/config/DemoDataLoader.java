package com.example.petstore.adoption.boot.config;

import com.example.petstore.adoption.domain.model.Pet;
import com.example.petstore.adoption.domain.model.PetId;
import com.example.petstore.adoption.domain.port.LoadPetPort;
import com.example.petstore.adoption.domain.port.SavePetPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Carga las mascotas de la demo si todavía no existen. Usa los puertos: funciona con cualquier
 * adaptador de persistencia.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataLoader implements ApplicationRunner {

    private static final List<Pet> DEMO_PETS = List.of(
            Pet.available(new PetId("1"), "Luna"),
            Pet.available(new PetId("2"), "Thor"),
            Pet.available(new PetId("3"), "Kira"));

    private final LoadPetPort loadPetPort;
    private final SavePetPort savePetPort;

    @Override
    public void run(ApplicationArguments args) {
        DEMO_PETS.stream()
                .filter(pet -> loadPetPort.loadPet(pet.id()).isEmpty())
                .forEach(pet -> {
                    savePetPort.savePet(pet);
                    log.info("Mascota de demo cargada: {} ({})", pet.id(), pet.name());
                });
    }
}
