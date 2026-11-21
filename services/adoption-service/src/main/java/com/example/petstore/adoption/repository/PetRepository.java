package com.example.petstore.adoption.repository;

import com.example.petstore.adoption.model.PetDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PetRepository extends MongoRepository<PetDocument, String> {
}
