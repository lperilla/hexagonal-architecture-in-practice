package com.example.petstore.notification;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comportamiento externo del servicio de notificaciones, visto desde fuera: se publica pet.adopted en
 * RabbitMQ, como lo haría adoption-service, y se comprueba el email en la API de Mailpit.
 * <p>
 * No depende de ninguna clase del servicio: debe pasar sin cambios en las ramas mvc y hexagonal.
 * Necesita la infraestructura de compose.yaml en marcha (podman compose up -d). Cada test usa una
 * mascota con un nombre único, así que no borra los emails que haya en Mailpit.
 */
@SpringBootTest
class NotificationEndToEndTest {

    private static final String EXCHANGE = "petstore.events";
    private static final String ROUTING_KEY = "pet.adopted";
    private static final String DEAD_LETTER_QUEUE = "notification.pet-adopted.dlq";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${notification.shelter-email}")
    private String shelterEmail;

    @Value("${mailpit.url:http://localhost:8025}")
    private String mailpitUrl;

    @Test
    void emailsTheShelterWhenAPetIsAdopted() throws Exception {
        String petName = uniquePetName();

        publish(petAdopted("1", petName, "Ana", Instant.now()));

        JsonNode email = awaitEmail("Ana ha adoptado a " + petName);
        assertThat(email.get("To").get(0).get("Address").asString()).isEqualTo(shelterEmail);
        assertThat(email.get("From").get("Address").asString()).isEqualTo("notificaciones@petstore.example");
        assertThat(emailText(email)).contains("Ana ha adoptado a " + petName + " (mascota 1)");
    }

    @Test
    void sendsOnlyOneEmailWhenTheSameEventArrivesTwice() throws Exception {
        String petName = uniquePetName();
        String event = petAdopted("2", petName, "Bea", Instant.now());

        publish(event);
        publish(event);

        awaitEmail("Bea ha adoptado a " + petName);
        Thread.sleep(2_000);
        assertThat(emailsWithSubject("Bea ha adoptado a " + petName)).hasSize(1);
    }

    @Test
    void ignoresFieldsItDoesNotKnow() throws Exception {
        String petName = uniquePetName();
        String eventWithNewField = """
                {"petId":"3","petName":"%s","ownerName":"Carlos","adoptedAt":"%s","adopterId":"A-42"}
                """.formatted(petName, Instant.now());

        publish(eventWithNewField);

        awaitEmail("Carlos ha adoptado a " + petName);
    }

    @Test
    void sendsAnIncompleteEventToTheDeadLetterQueue() throws Exception {
        String petName = uniquePetName();

        publish("""
                {"petId":"4","petName":"%s"}
                """.formatted(petName));

        assertThat(awaitDeadLetter(petName)).as("mensaje en " + DEAD_LETTER_QUEUE).isNotNull();
    }

    @Test
    void sendsAMessageThatIsNotJsonToTheDeadLetterQueue() throws Exception {
        String marker = uniquePetName();

        publish("esto no es JSON " + marker);

        assertThat(awaitDeadLetter(marker)).as("mensaje en " + DEAD_LETTER_QUEUE).isNotNull();
    }

    private void publish(String body) {
        Message message = MessageBuilder.withBody(body.getBytes(StandardCharsets.UTF_8))
                .setContentType("application/json")
                .build();
        rabbitTemplate.send(EXCHANGE, ROUTING_KEY, message);
    }

    private static String petAdopted(String petId, String petName, String ownerName, Instant adoptedAt) {
        return """
                {"petId":"%s","petName":"%s","ownerName":"%s","adoptedAt":"%s"}
                """.formatted(petId, petName, ownerName, adoptedAt);
    }

    private static String uniquePetName() {
        return "Mascota-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private JsonNode awaitEmail(String subject) throws Exception {
        Instant deadline = Instant.now().plus(TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            List<JsonNode> emails = emailsWithSubject(subject);
            if (!emails.isEmpty()) {
                return emails.getFirst();
            }
            Thread.sleep(250);
        }
        throw new AssertionError("No ha llegado a Mailpit ningún email con el asunto: " + subject);
    }

    private List<JsonNode> emailsWithSubject(String subject) throws Exception {
        JsonNode response = get("/api/v1/messages?limit=500");
        List<JsonNode> emails = new ArrayList<>();
        for (JsonNode email : response.get("messages")) {
            if (subject.equals(email.get("Subject").asString())) {
                emails.add(email);
            }
        }
        return emails;
    }

    private String emailText(JsonNode email) throws Exception {
        return get("/api/v1/message/" + email.get("ID").asString()).get("Text").asString();
    }

    private JsonNode get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(mailpitUrl + path)).GET().build();
        return JSON.readTree(HTTP.send(request, HttpResponse.BodyHandlers.ofString()).body());
    }

    private Message awaitDeadLetter(String marker) {
        Instant deadline = Instant.now().plus(TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            Message message = rabbitTemplate.receive(DEAD_LETTER_QUEUE, 1_000);
            if (message != null && new String(message.getBody(), StandardCharsets.UTF_8).contains(marker)) {
                return message;
            }
        }
        return null;
    }
}
