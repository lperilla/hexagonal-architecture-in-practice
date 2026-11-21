package com.example.petstore.adoption.infrastructure.rabbitmq;

import com.example.petstore.adoption.domain.event.PetAdopted;
import com.example.petstore.adoption.domain.model.Owner;
import com.example.petstore.adoption.domain.model.PetId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba el adaptador contra el RabbitMQ de compose.yaml: el mensaje cumple el contrato del evento.
 */
@SpringBootTest(properties = {
        "spring.mongodb.database=petstore-adoption-infra-test",
        "spring.rabbitmq.username=petstore",
        "spring.rabbitmq.password=petstore"})
class RabbitPetAdoptedPublisherTest {

    private static final String TEST_QUEUE = "adoption-infra-test.pet-adopted";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private RabbitPetAdoptedPublisher publisher;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void bindTestQueue() {
        TopicExchange exchange = new TopicExchange(RabbitConfig.EXCHANGE, true, false);
        Queue queue = new Queue(TEST_QUEUE, true, false, false);
        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(RabbitConfig.PET_ADOPTED_ROUTING_KEY));
        amqpAdmin.purgeQueue(TEST_QUEUE, false);
    }

    @AfterEach
    void deleteTestQueue() {
        amqpAdmin.deleteQueue(TEST_QUEUE);
    }

    @Test
    void publishesThePetAdoptedEventAsDefinedByTheContract() throws Exception {
        Instant adoptedAt = Instant.parse("2026-12-04T10:15:30Z");

        publisher.publish(new PetAdopted(new PetId("1"), "Luna", new Owner("Ana"), adoptedAt));

        Message message = rabbitTemplate.receive(TEST_QUEUE, 5_000);
        assertThat(message).isNotNull();
        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(message.getMessageProperties().getReceivedRoutingKey()).isEqualTo("pet.adopted");
        assertThat(message.getMessageProperties().getHeaders()).doesNotContainKey("__TypeId__");

        JsonNode event = JSON.readTree(new String(message.getBody(), StandardCharsets.UTF_8));
        assertThat(event.propertyNames()).containsExactlyInAnyOrder("petId", "petName", "ownerName", "adoptedAt");
        assertThat(event.get("petId").asString()).isEqualTo("1");
        assertThat(event.get("petName").asString()).isEqualTo("Luna");
        assertThat(event.get("ownerName").asString()).isEqualTo("Ana");
        assertThat(event.get("adoptedAt").asString()).isEqualTo("2026-12-04T10:15:30Z");
    }
}
