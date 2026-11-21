package com.example.petstore.adoption.infrastructure.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

interface SpringDataPetRepository extends MongoRepository<PetDocument, String> {
}
