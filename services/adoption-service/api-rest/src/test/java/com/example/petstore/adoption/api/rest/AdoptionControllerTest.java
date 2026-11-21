package com.example.petstore.adoption.api.rest;

import com.example.petstore.adoption.application.port.in.AdoptPetCommand;
import com.example.petstore.adoption.application.port.in.AdoptPetUseCase;
import com.example.petstore.adoption.application.port.in.AdoptionResult;
import com.example.petstore.adoption.domain.exception.PetNotAvailableException;
import com.example.petstore.adoption.domain.exception.PetNotFoundException;
import com.example.petstore.adoption.domain.model.Owner;
import com.example.petstore.adoption.domain.model.PetId;
import com.example.petstore.adoption.domain.model.PetStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El adaptador REST, probado solo: el caso de uso es un doble. Sin MongoDB ni RabbitMQ.
 */
@WebMvcTest(AdoptionController.class)
@Import(AdoptionRestMapper.class)
class AdoptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdoptPetUseCase adoptPetUseCase;

    @Test
    void translatesTheRequestIntoACommandAndTheResultIntoTheResponse() throws Exception {
        when(adoptPetUseCase.adopt(new AdoptPetCommand(new PetId("1"), new Owner("Ana"))))
                .thenReturn(new AdoptionResult(new PetId("1"), "Luna", PetStatus.ADOPTED, new Owner("Ana")));

        mockMvc.perform(adopt("1", "{\"ownerName\":\"Ana\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.petId").value("1"))
                .andExpect(jsonPath("$.petName").value("Luna"))
                .andExpect(jsonPath("$.status").value("ADOPTED"))
                .andExpect(jsonPath("$.ownerName").value("Ana"));
    }

    @Test
    void translatesPetNotFoundInto404() throws Exception {
        when(adoptPetUseCase.adopt(any())).thenThrow(new PetNotFoundException(new PetId("99")));

        mockMvc.perform(adopt("99", "{\"ownerName\":\"Ana\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("La mascota 99 no existe"));
    }

    @Test
    void translatesPetNotAvailableInto409() throws Exception {
        when(adoptPetUseCase.adopt(any())).thenThrow(new PetNotAvailableException(new PetId("1")));

        mockMvc.perform(adopt("1", "{\"ownerName\":\"Ana\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("La mascota 1 no está disponible para adopción"));
    }

    @Test
    void rejectsABlankOwnerNameWithoutCallingTheUseCase() throws Exception {
        mockMvc.perform(adopt("1", "{\"ownerName\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(adoptPetUseCase);
    }

    private static org.springframework.test.web.servlet.RequestBuilder adopt(String petId, String body) {
        return post("/pets/{petId}/adoption", petId).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
