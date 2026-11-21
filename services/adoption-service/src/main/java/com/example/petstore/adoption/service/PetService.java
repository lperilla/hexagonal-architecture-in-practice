package com.example.petstore.adoption.service;

import com.example.petstore.adoption.application.port.in.AdoptPetCommand;
import com.example.petstore.adoption.application.port.in.AdoptPetUseCase;
import com.example.petstore.adoption.application.port.in.AdoptionResult;
import com.example.petstore.adoption.domain.model.Owner;
import com.example.petstore.adoption.domain.model.PetId;
import com.example.petstore.adoption.domain.model.PetStatus;
import com.example.petstore.adoption.messaging.PetAdoptedMessage;
import com.example.petstore.adoption.messaging.RabbitConfig;
import com.example.petstore.adoption.model.PetDocument;
import com.example.petstore.adoption.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/**
 * Paso intermedio de la migración: el servicio antiguo implementa el puerto de entrada, de modo
 * que los canales ya dependen del caso de uso. Por dentro todavía es el código acoplado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PetService implements AdoptPetUseCase {

    private final PetRepository petRepository;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public AdoptionResult adopt(AdoptPetCommand command) {
        String petId = command.petId().value();
        PetDocument pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "La mascota " + petId + " no existe"));

        if (!PetDocument.AVAILABLE.equals(pet.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "La mascota " + petId + " no está disponible para adopción");
        }

        pet.setStatus(PetDocument.ADOPTED);
        pet.setOwnerName(command.owner().name());
        PetDocument adopted = petRepository.save(pet);

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.PET_ADOPTED_ROUTING_KEY,
                new PetAdoptedMessage(adopted.getId(), adopted.getName(), adopted.getOwnerName(), Instant.now()));

        log.info("Mascota {} ({}) adoptada por {}", adopted.getId(), adopted.getName(), adopted.getOwnerName());
        return new AdoptionResult(new PetId(adopted.getId()), adopted.getName(),
                PetStatus.valueOf(adopted.getStatus()), new Owner(adopted.getOwnerName()));
    }
}
