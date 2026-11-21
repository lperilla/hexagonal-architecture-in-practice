package com.example.petstore.adoption.service;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final RabbitTemplate rabbitTemplate;

    public PetDocument adopt(String petId, String ownerName) {
        PetDocument pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "La mascota " + petId + " no existe"));

        if (!PetDocument.AVAILABLE.equals(pet.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "La mascota " + petId + " no está disponible para adopción");
        }

        pet.setStatus(PetDocument.ADOPTED);
        pet.setOwnerName(ownerName);
        PetDocument adopted = petRepository.save(pet);

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.PET_ADOPTED_ROUTING_KEY,
                new PetAdoptedMessage(adopted.getId(), adopted.getName(), adopted.getOwnerName(), Instant.now()));

        log.info("Mascota {} ({}) adoptada por {}", adopted.getId(), adopted.getName(), adopted.getOwnerName());
        return adopted;
    }
}
