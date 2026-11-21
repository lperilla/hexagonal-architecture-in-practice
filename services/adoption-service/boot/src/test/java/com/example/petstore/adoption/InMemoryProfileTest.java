package com.example.petstore.adoption;

import com.example.petstore.adoption.domain.port.LoadPetPort;
import com.example.petstore.adoption.infrastructure.inmemory.InMemoryLoadPetAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sustituir la infraestructura: con el perfil in-memory la aplicación arranca sin MongoDB y el mismo
 * caso de uso funciona con otro adaptador de persistencia.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("in-memory")
class InMemoryProfileTest {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private LoadPetPort loadPetPort;

    @Test
    void runsWithoutMongoDb() {
        assertThat(loadPetPort).isInstanceOf(InMemoryLoadPetAdapter.class);
        assertThat(context.getBeanProvider(MongoTemplate.class).getIfAvailable()).isNull();
    }

    @Test
    void adoptsThroughTheSameUseCase() throws Exception {
        assertThat(adopt("3", "Ana").statusCode()).isEqualTo(200);
        assertThat(adopt("3", "Bea").statusCode()).isEqualTo(409);
        assertThat(adopt("99", "Ana").statusCode()).isEqualTo(404);
    }

    private HttpResponse<String> adopt(String petId, String ownerName) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/pets/" + petId + "/adoption"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"ownerName\":\"" + ownerName + "\"}"))
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
