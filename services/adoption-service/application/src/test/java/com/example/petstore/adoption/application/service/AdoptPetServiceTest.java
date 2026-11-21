package com.example.petstore.adoption.application.service;

import com.example.petstore.adoption.application.port.in.AdoptPetCommand;
import com.example.petstore.adoption.application.port.in.AdoptionResult;
import com.example.petstore.adoption.domain.event.PetAdopted;
import com.example.petstore.adoption.domain.exception.PetNotAvailableException;
import com.example.petstore.adoption.domain.exception.PetNotFoundException;
import com.example.petstore.adoption.domain.model.Owner;
import com.example.petstore.adoption.domain.model.Pet;
import com.example.petstore.adoption.domain.model.PetId;
import com.example.petstore.adoption.domain.model.PetStatus;
import com.example.petstore.adoption.domain.port.LoadPetPort;
import com.example.petstore.adoption.domain.port.PublishPetAdoptedPort;
import com.example.petstore.adoption.domain.port.SavePetPort;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El caso de uso, probado sin Spring, sin MongoDB y sin RabbitMQ: los puertos se implementan
 * con dobles escritos a mano.
 */
class AdoptPetServiceTest {

    private static final Instant NOW = Instant.parse("2026-12-04T10:15:30Z");

    private final FakePets pets = new FakePets();
    private final RecordingPublisher publisher = new RecordingPublisher();
    private final AdoptPetService adoptPetService =
            new AdoptPetService(pets, pets, publisher, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void adoptsAnAvailablePet() {
        pets.add(Pet.available(new PetId("1"), "Luna"));

        AdoptionResult result = adoptPetService.adopt(command("1", "Ana"));

        assertThat(result).isEqualTo(new AdoptionResult(new PetId("1"), "Luna", PetStatus.ADOPTED, new Owner("Ana")));
        assertThat(pets.saved(new PetId("1")).status()).isEqualTo(PetStatus.ADOPTED);
        assertThat(publisher.events).containsExactly(new PetAdopted(new PetId("1"), "Luna", new Owner("Ana"), NOW));
    }

    @Test
    void rejectsAPetThatIsNotAvailable() {
        pets.add(new Pet(new PetId("1"), "Luna", PetStatus.ADOPTED, new Owner("Ana")));

        assertThatThrownBy(() -> adoptPetService.adopt(command("1", "Bea")))
                .isInstanceOf(PetNotAvailableException.class);

        assertThat(pets.saveCount).isZero();
        assertThat(publisher.events).isEmpty();
    }

    @Test
    void rejectsAPetThatDoesNotExist() {
        assertThatThrownBy(() -> adoptPetService.adopt(command("99", "Ana")))
                .isInstanceOf(PetNotFoundException.class)
                .hasMessage("La mascota 99 no existe");

        assertThat(pets.saveCount).isZero();
        assertThat(publisher.events).isEmpty();
    }

    private static AdoptPetCommand command(String petId, String ownerName) {
        return new AdoptPetCommand(new PetId(petId), new Owner(ownerName));
    }

    private static final class FakePets implements LoadPetPort, SavePetPort {

        private final Map<PetId, Pet> pets = new HashMap<>();
        private int saveCount;

        void add(Pet pet) {
            pets.put(pet.id(), pet);
        }

        Pet saved(PetId petId) {
            return pets.get(petId);
        }

        @Override
        public Optional<Pet> loadPet(PetId petId) {
            return Optional.ofNullable(pets.get(petId));
        }

        @Override
        public void savePet(Pet pet) {
            saveCount++;
            pets.put(pet.id(), pet);
        }
    }

    private static final class RecordingPublisher implements PublishPetAdoptedPort {

        private final List<PetAdopted> events = new ArrayList<>();

        @Override
        public void publish(PetAdopted event) {
            events.add(event);
        }
    }
}
