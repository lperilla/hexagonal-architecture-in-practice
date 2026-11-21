package com.example.petstore.adoption.domain.model;

import com.example.petstore.adoption.domain.event.PetAdopted;
import com.example.petstore.adoption.domain.exception.PetNotAvailableException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PetTest {

    private static final PetId LUNA_ID = new PetId("1");
    private static final Instant NOW = Instant.parse("2026-12-04T10:15:30Z");

    @Test
    void anAvailablePetCanBeAdopted() {
        Pet luna = Pet.available(LUNA_ID, "Luna");

        Pet adopted = luna.adopt(new Owner("Ana"));

        assertThat(adopted.status()).isEqualTo(PetStatus.ADOPTED);
        assertThat(adopted.owner()).isEqualTo(new Owner("Ana"));
    }

    @Test
    void adoptingDoesNotChangeTheOriginalPet() {
        Pet luna = Pet.available(LUNA_ID, "Luna");

        luna.adopt(new Owner("Ana"));

        assertThat(luna.isAvailable()).isTrue();
        assertThat(luna.owner()).isNull();
    }

    @Test
    void anAdoptedPetCannotBeAdoptedAgain() {
        Pet luna = new Pet(LUNA_ID, "Luna", PetStatus.ADOPTED, new Owner("Ana"));

        assertThatThrownBy(() -> luna.adopt(new Owner("Bea")))
                .isInstanceOf(PetNotAvailableException.class)
                .hasMessage("La mascota 1 no está disponible para adopción");
    }

    @Test
    void aPetHasAnOwnerIfAndOnlyIfItIsAdopted() {
        assertThatThrownBy(() -> new Pet(LUNA_ID, "Luna", PetStatus.ADOPTED, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Pet(LUNA_ID, "Luna", PetStatus.AVAILABLE, new Owner("Ana")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anOwnerNeedsAName() {
        assertThatThrownBy(() -> new Owner("   ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Owner(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theEventDescribesAnAdoptedPet() {
        Pet adopted = Pet.available(LUNA_ID, "Luna").adopt(new Owner("Ana"));

        assertThat(PetAdopted.of(adopted, NOW)).isEqualTo(new PetAdopted(LUNA_ID, "Luna", new Owner("Ana"), NOW));
        assertThatThrownBy(() -> PetAdopted.of(Pet.available(LUNA_ID, "Luna"), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
