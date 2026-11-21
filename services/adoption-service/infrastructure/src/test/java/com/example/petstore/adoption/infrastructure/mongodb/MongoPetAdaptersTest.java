package com.example.petstore.adoption.infrastructure.mongodb;

import com.example.petstore.adoption.domain.model.Owner;
import com.example.petstore.adoption.domain.model.Pet;
import com.example.petstore.adoption.domain.model.PetId;
import com.example.petstore.adoption.domain.model.PetStatus;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba los adaptadores de MongoDB contra el MongoDB de compose.yaml, en su propia base de datos.
 */
@DataMongoTest(properties = "spring.mongodb.database=petstore-adoption-infra-test")
@Import({MongoLoadPetAdapter.class, MongoSavePetAdapter.class, PetMongoMapper.class})
class MongoPetAdaptersTest {

    @Autowired
    private MongoLoadPetAdapter loadAdapter;

    @Autowired
    private MongoSavePetAdapter saveAdapter;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanCollection() {
        mongoTemplate.getCollection("pets").deleteMany(new Document());
    }

    @Test
    void savesAndLoadsAnAvailablePet() {
        saveAdapter.savePet(Pet.available(new PetId("1"), "Luna"));

        assertThat(loadAdapter.loadPet(new PetId("1")))
                .hasValueSatisfying(pet -> {
                    assertThat(pet.name()).isEqualTo("Luna");
                    assertThat(pet.status()).isEqualTo(PetStatus.AVAILABLE);
                    assertThat(pet.owner()).isNull();
                });
    }

    @Test
    void savesAndLoadsAnAdoptedPet() {
        saveAdapter.savePet(new Pet(new PetId("1"), "Luna", PetStatus.ADOPTED, new Owner("Ana")));

        assertThat(loadAdapter.loadPet(new PetId("1")))
                .hasValueSatisfying(pet -> {
                    assertThat(pet.status()).isEqualTo(PetStatus.ADOPTED);
                    assertThat(pet.owner()).isEqualTo(new Owner("Ana"));
                });
    }

    @Test
    void returnsNothingForAnUnknownPet() {
        assertThat(loadAdapter.loadPet(new PetId("99"))).isEmpty();
    }

    @Test
    void keepsTheDocumentShapeOfTheExistingData() {
        saveAdapter.savePet(new Pet(new PetId("1"), "Luna", PetStatus.ADOPTED, new Owner("Ana")));

        Document stored = mongoTemplate.getCollection("pets").find(new Document("_id", "1")).first();
        assertThat(stored).isNotNull();
        assertThat(stored.getString("name")).isEqualTo("Luna");
        assertThat(stored.getString("status")).isEqualTo("ADOPTED");
        assertThat(stored.getString("ownerName")).isEqualTo("Ana");
    }
}
