package com.example.petstore.adoption.config;

import com.example.petstore.adoption.model.PetDocument;
import com.example.petstore.adoption.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Carga las mascotas de la demo si todavía no existen.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataLoader implements ApplicationRunner {

    private static final List<PetDocument> DEMO_PETS = List.of(
            new PetDocument("1", "Luna", PetDocument.AVAILABLE, null),
            new PetDocument("2", "Thor", PetDocument.AVAILABLE, null),
            new PetDocument("3", "Kira", PetDocument.AVAILABLE, null));

    private final PetRepository petRepository;

    @Override
    public void run(ApplicationArguments args) {
        DEMO_PETS.stream()
                .filter(pet -> !petRepository.existsById(pet.getId()))
                .forEach(pet -> {
                    petRepository.save(pet);
                    log.info("Mascota de demo cargada: {} ({})", pet.getId(), pet.getName());
                });
    }
}
