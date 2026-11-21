package com.example.petstore.adoption.service;

import com.example.petstore.adoption.application.port.in.AdoptPetCommand;
import com.example.petstore.adoption.application.port.in.AdoptionResult;
import com.example.petstore.adoption.domain.model.Owner;
import com.example.petstore.adoption.domain.model.PetId;
import com.example.petstore.adoption.domain.model.PetStatus;
import com.example.petstore.adoption.messaging.PetAdoptedMessage;
import com.example.petstore.adoption.messaging.RabbitConfig;
import com.example.petstore.adoption.model.PetDocument;
import com.example.petstore.adoption.repository.PetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @Mock
    private PetRepository petRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PetService petService;

    @Test
    void adoptsAnAvailablePetAndPublishesTheEvent() {
        when(petRepository.findById("1"))
                .thenReturn(Optional.of(new PetDocument("1", "Luna", PetDocument.AVAILABLE, null)));
        when(petRepository.save(any(PetDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdoptionResult result = petService.adopt(command("1", "Ana"));

        assertThat(result).isEqualTo(new AdoptionResult(new PetId("1"), "Luna", PetStatus.ADOPTED, new Owner("Ana")));
        verify(petRepository).save(new PetDocument("1", "Luna", PetDocument.ADOPTED, "Ana"));

        ArgumentCaptor<Object> message = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.PET_ADOPTED_ROUTING_KEY), message.capture());
        assertThat(message.getValue()).isInstanceOfSatisfying(PetAdoptedMessage.class, event -> {
            assertThat(event.petId()).isEqualTo("1");
            assertThat(event.petName()).isEqualTo("Luna");
            assertThat(event.ownerName()).isEqualTo("Ana");
            assertThat(event.adoptedAt()).isNotNull();
        });
    }

    @Test
    void rejectsAPetThatIsNotAvailable() {
        when(petRepository.findById("1"))
                .thenReturn(Optional.of(new PetDocument("1", "Luna", PetDocument.ADOPTED, "Ana")));

        assertThatThrownBy(() -> petService.adopt(command("1", "Bea")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(petRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void rejectsAPetThatDoesNotExist() {
        when(petRepository.findById("99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> petService.adopt(command("99", "Ana")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(petRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    private static AdoptPetCommand command(String petId, String ownerName) {
        return new AdoptPetCommand(new PetId(petId), new Owner(ownerName));
    }
}
