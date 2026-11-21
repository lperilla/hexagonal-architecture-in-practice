package com.example.petstore.adoption;

import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comportamiento externo del servicio de adopción, visto desde fuera: REST, SOAP, MongoDB y RabbitMQ.
 * <p>
 * Solo usa HTTP, la colección "pets", el exchange "petstore.events" y los contratos. No depende de
 * ninguna clase del servicio, de modo que debe pasar sin cambios antes y después de la migración
 * a arquitectura hexagonal.
 * <p>
 * Necesita la infraestructura de compose.yaml en marcha (podman compose up -d). Usa su propia base
 * de datos, petstore-adoption-test, para no tocar los datos de la demo.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.mongodb.database=petstore-adoption-test")
class AdoptionEndToEndTest {

    private static final String EXCHANGE = "petstore.events";
    private static final String ROUTING_KEY = "pet.adopted";
    private static final String TEST_QUEUE = "adoption-e2e.pet-adopted";
    private static final String SOAP_NS = "http://example.com/petstore/adoption";

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort
    private int port;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void resetState() {
        var pets = mongoTemplate.getCollection("pets");
        pets.deleteMany(new Document());
        pets.insertMany(List.of(availablePet("1", "Luna"), availablePet("2", "Thor"), availablePet("3", "Kira")));

        TopicExchange exchange = new TopicExchange(EXCHANGE, true, false);
        Queue queue = new Queue(TEST_QUEUE, true, false, false);
        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY));
        amqpAdmin.purgeQueue(TEST_QUEUE, false);
    }

    @AfterEach
    void deleteTestQueue() {
        amqpAdmin.deleteQueue(TEST_QUEUE);
    }

    @Nested
    class Rest {

        @Test
        void adoptsAnAvailablePet() throws Exception {
            HttpResponse<String> response = adoptByRest("1", "{\"ownerName\":\"Ana\"}");

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(contentType(response)).startsWith("application/json");
            JsonNode body = JSON.readTree(response.body());
            assertThat(body.get("petId").asString()).isEqualTo("1");
            assertThat(body.get("petName").asString()).isEqualTo("Luna");
            assertThat(body.get("status").asString()).isEqualTo("ADOPTED");
            assertThat(body.get("ownerName").asString()).isEqualTo("Ana");
            assertStoredPet("1", "ADOPTED", "Ana");
        }

        @Test
        void rejectsASecondAdoptionWithConflict() throws Exception {
            adoptByRest("1", "{\"ownerName\":\"Ana\"}");

            HttpResponse<String> response = adoptByRest("1", "{\"ownerName\":\"Bea\"}");

            assertProblem(response, 409);
            assertStoredPet("1", "ADOPTED", "Ana");
        }

        @Test
        void rejectsAnUnknownPetWithNotFound() throws Exception {
            HttpResponse<String> response = adoptByRest("99", "{\"ownerName\":\"Ana\"}");

            assertProblem(response, 404);
        }

        @Test
        void rejectsABlankOwnerNameWithBadRequest() throws Exception {
            HttpResponse<String> response = adoptByRest("1", "{\"ownerName\":\"   \"}");

            assertProblem(response, 400);
            assertStoredPet("1", "AVAILABLE", null);
        }

        @Test
        void rejectsAMissingOwnerNameWithBadRequest() throws Exception {
            HttpResponse<String> response = adoptByRest("1", "{}");

            assertProblem(response, 400);
            assertStoredPet("1", "AVAILABLE", null);
        }
    }

    @Nested
    class Soap {

        @Test
        void adoptsAnAvailablePet() throws Exception {
            HttpResponse<String> response = adoptBySoap("1", "Ana");

            assertThat(response.statusCode()).isEqualTo(200);
            org.w3c.dom.Document xml = parseXml(response.body());
            Element adoptPetResponse = element(xml, SOAP_NS, "AdoptPetResponse");
            assertThat(text(adoptPetResponse, "petId")).isEqualTo("1");
            assertThat(text(adoptPetResponse, "petName")).isEqualTo("Luna");
            assertThat(text(adoptPetResponse, "status")).isEqualTo("ADOPTED");
            assertThat(text(adoptPetResponse, "ownerName")).isEqualTo("Ana");
            assertStoredPet("1", "ADOPTED", "Ana");
        }

        @Test
        void rejectsASecondAdoptionWithPetNotAvailableFault() throws Exception {
            adoptBySoap("1", "Ana");

            HttpResponse<String> response = adoptBySoap("1", "Bea");

            Element detail = assertClientFault(response, "PetNotAvailableFault");
            assertThat(text(detail, "petId")).isEqualTo("1");
            assertStoredPet("1", "ADOPTED", "Ana");
        }

        @Test
        void rejectsAnUnknownPetWithPetNotFoundFault() throws Exception {
            HttpResponse<String> response = adoptBySoap("99", "Ana");

            Element detail = assertClientFault(response, "PetNotFoundFault");
            assertThat(text(detail, "petId")).isEqualTo("99");
        }

        @Test
        void rejectsABlankOwnerNameWithClientFault() throws Exception {
            HttpResponse<String> response = adoptBySoap("1", "   ");

            assertThat(response.statusCode()).isEqualTo(500);
            assertThat(faultCode(parseXml(response.body()))).endsWith(":Client");
            assertStoredPet("1", "AVAILABLE", null);
        }

        @Test
        void publishesTheWsdlAndTheSchema() throws Exception {
            assertThat(get("/ws/adoption.wsdl").statusCode()).isEqualTo(200);
            assertThat(get("/ws/adoption-types.xsd").statusCode()).isEqualTo(200);
        }
    }

    @Nested
    class Events {

        @Test
        void publishesPetAdoptedWhenAdoptingByRest() throws Exception {
            Instant before = Instant.now();

            adoptByRest("1", "{\"ownerName\":\"Ana\"}");

            assertPetAdoptedEvent("1", "Luna", "Ana", before);
        }

        @Test
        void publishesPetAdoptedWhenAdoptingBySoap() throws Exception {
            Instant before = Instant.now();

            adoptBySoap("2", "Bea");

            assertPetAdoptedEvent("2", "Thor", "Bea", before);
        }

        @Test
        void publishesNothingWhenTheAdoptionFails() throws Exception {
            adoptByRest("99", "{\"ownerName\":\"Ana\"}");
            adoptBySoap("99", "Ana");

            assertThat(rabbitTemplate.receive(TEST_QUEUE, 1_000)).isNull();
        }

        private void assertPetAdoptedEvent(String petId, String petName, String ownerName, Instant notBefore)
                throws Exception {
            Message message = rabbitTemplate.receive(TEST_QUEUE, 5_000);

            assertThat(message).as("evento pet.adopted").isNotNull();
            assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
            assertThat(message.getMessageProperties().getReceivedRoutingKey()).isEqualTo(ROUTING_KEY);
            assertThat(message.getMessageProperties().getHeaders()).doesNotContainKey("__TypeId__");

            JsonNode event = JSON.readTree(new String(message.getBody(), StandardCharsets.UTF_8));
            assertThat(event.propertyNames()).containsExactlyInAnyOrder("petId", "petName", "ownerName", "adoptedAt");
            assertThat(event.get("petId").asString()).isEqualTo(petId);
            assertThat(event.get("petName").asString()).isEqualTo(petName);
            assertThat(event.get("ownerName").asString()).isEqualTo(ownerName);
            assertThat(Instant.parse(event.get("adoptedAt").asString())).isAfterOrEqualTo(notBefore.minusSeconds(1));

            assertThat(rabbitTemplate.receive(TEST_QUEUE, 500)).as("solo un evento por adopción").isNull();
        }
    }

    private HttpResponse<String> adoptByRest(String petId, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/pets/" + petId + "/adoption"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> adoptBySoap(String petId, String ownerName) throws Exception {
        String envelope = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:ad="http://example.com/petstore/adoption">
                   <soapenv:Body>
                      <ad:AdoptPetRequest>
                         <ad:petId>%s</ad:petId>
                         <ad:ownerName>%s</ad:ownerName>
                      </ad:AdoptPetRequest>
                   </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(petId, ownerName);
        HttpRequest request = HttpRequest.newBuilder(uri("/ws"))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "\"\"")
                .POST(HttpRequest.BodyPublishers.ofString(envelope))
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private static String contentType(HttpResponse<?> response) {
        return response.headers().firstValue("Content-Type").orElse("");
    }

    private static void assertProblem(HttpResponse<String> response, int status) throws Exception {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(contentType(response)).startsWith("application/problem+json");
        JsonNode problem = JSON.readTree(response.body());
        assertThat(problem.get("status").asInt()).isEqualTo(status);
        assertThat(problem.hasNonNull("title")).isTrue();
    }

    private static Element assertClientFault(HttpResponse<String> response, String detailElement) throws Exception {
        assertThat(response.statusCode()).isEqualTo(500);
        org.w3c.dom.Document xml = parseXml(response.body());
        assertThat(faultCode(xml)).endsWith(":Client");
        Element detail = element(xml, SOAP_NS, detailElement);
        assertThat(detail).as("detalle " + detailElement).isNotNull();
        assertThat(text(detail, "message")).isNotBlank();
        return detail;
    }

    private void assertStoredPet(String id, String status, String ownerName) {
        Document pet = mongoTemplate.getCollection("pets").find(new Document("_id", id)).first();
        assertThat(pet).isNotNull();
        assertThat(pet.getString("status")).isEqualTo(status);
        assertThat(pet.getString("ownerName")).isEqualTo(ownerName);
    }

    private static Document availablePet(String id, String name) {
        return new Document("_id", id).append("name", name).append("status", "AVAILABLE");
    }

    private static org.w3c.dom.Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private static String faultCode(org.w3c.dom.Document xml) {
        return xml.getElementsByTagNameNS("*", "faultcode").item(0).getTextContent();
    }

    private static Element element(org.w3c.dom.Document xml, String namespace, String localName) {
        return (Element) xml.getElementsByTagNameNS(namespace, localName).item(0);
    }

    private static String text(Element parent, String localName) {
        return parent.getElementsByTagNameNS(SOAP_NS, localName).item(0).getTextContent();
    }
}
