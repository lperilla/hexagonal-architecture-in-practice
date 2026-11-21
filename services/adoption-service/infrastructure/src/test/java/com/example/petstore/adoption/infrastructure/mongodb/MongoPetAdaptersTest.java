package com.example.petstore.adoption.infrastructure.mongodb;

import com.example.petstore.adoption.domain.model.Owner;
import com.example.petstore.adoption.domain.model.Pet;
import com.example.petstore.adoption.domain.model.PetId;
import com.example.petstore.adoption.domain.model.PetStatus;
import com.example.petstore.adoption.domain.port.LoadPetPort;
import com.example.petstore.adoption.domain.port.SavePetPort;
import com.example.petstore.adoption.infrastructure.PetPersistenceContract;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los adaptadores de MongoDB cumplen el contrato común de persistencia. Usan el MongoDB de
 * compose.yaml, en su propia base de datos.
 */
@DataMongoTest(properties = "spring.mongodb.database=petstore-adoption-infra-test")
@Import({MongoLoadPetAdapter.class, MongoSavePetAdapter.class, PetMongoMapper.class})
class MongoPetAdaptersTest extends PetPersistenceContract {

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

    @Override
    protected LoadPetPort loadPetPort() {
        return loadAdapter;
    }

    @Override
    protected SavePetPort savePetPort() {
        return saveAdapter;
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
