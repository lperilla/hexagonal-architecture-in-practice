package com.example.petstore.adoption.infrastructure.mongodb;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Modelo técnico de MongoDB. Solo existe dentro del adaptador: el dominio no lo conoce.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("pets")
public class PetDocument {

    @Id
    private String id;
    private String name;
    private String status;
    private String ownerName;
}
