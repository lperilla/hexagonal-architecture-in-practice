package com.example.petstore.adoption.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("pets")
public class PetDocument {

    public static final String AVAILABLE = "AVAILABLE";
    public static final String ADOPTED = "ADOPTED";

    @Id
    private String id;
    private String name;
    private String status;
    private String ownerName;
}
