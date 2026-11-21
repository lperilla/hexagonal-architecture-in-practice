package com.example.petstore.adoption.infrastructure;

import com.example.petstore.adoption.domain.model.Owner;
import com.example.petstore.adoption.domain.model.Pet;
import com.example.petstore.adoption.domain.model.PetId;
import com.example.petstore.adoption.domain.model.PetStatus;
import com.example.petstore.adoption.domain.port.LoadPetPort;
import com.example.petstore.adoption.domain.port.SavePetPort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contrato común de los puertos de persistencia: cualquier pareja de adaptadores que implemente
 * LoadPetPort y SavePetPort debe pasar estos tests, sea MongoDB, memoria o lo que venga.
 */
public abstract class PetPersistenceContract {

    protected abstract LoadPetPort loadPetPort();

    protected abstract SavePetPort savePetPort();

    @Test
    void returnsNothingForAnUnknownPet() {
        assertThat(loadPetPort().loadPet(new PetId("99"))).isEmpty();
    }

    @Test
    void savesAndLoadsAnAvailablePet() {
        savePetPort().savePet(Pet.available(new PetId("1"), "Luna"));

        assertThat(loadPetPort().loadPet(new PetId("1"))).contains(Pet.available(new PetId("1"), "Luna"));
    }

    @Test
    void savesAndLoadsAnAdoptedPet() {
        Pet adopted = new Pet(new PetId("1"), "Luna", PetStatus.ADOPTED, new Owner("Ana"));

        savePetPort().savePet(adopted);

        assertThat(loadPetPort().loadPet(new PetId("1"))).contains(adopted);
    }

    @Test
    void savingTheAdoptedPetReplacesTheStoredOne() {
        savePetPort().savePet(Pet.available(new PetId("1"), "Luna"));
        Pet luna = loadPetPort().loadPet(new PetId("1")).orElseThrow();

        savePetPort().savePet(luna.adopt(new Owner("Ana")));

        assertThat(loadPetPort().loadPet(new PetId("1")))
                .hasValueSatisfying(pet -> assertThat(pet.owner()).isEqualTo(new Owner("Ana")));
    }

    @Test
    void adoptingWithoutSavingChangesNothing() {
        savePetPort().savePet(Pet.available(new PetId("1"), "Luna"));

        loadPetPort().loadPet(new PetId("1")).orElseThrow().adopt(new Owner("Ana"));

        assertThat(loadPetPort().loadPet(new PetId("1")))
                .hasValueSatisfying(pet -> assertThat(pet.isAvailable()).isTrue());
    }
}
