package com.example.petstore.adoption.infrastructure.inmemory;

import com.example.petstore.adoption.domain.port.LoadPetPort;
import com.example.petstore.adoption.domain.port.SavePetPort;
import com.example.petstore.adoption.infrastructure.PetPersistenceContract;

/**
 * Los adaptadores en memoria cumplen el mismo contrato que los de MongoDB. Sin Spring y sin contenedores.
 */
class InMemoryPetAdaptersTest extends PetPersistenceContract {

    private final InMemoryPetStore store = new InMemoryPetStore();
    private final InMemoryLoadPetAdapter loadAdapter = new InMemoryLoadPetAdapter(store);
    private final InMemorySavePetAdapter saveAdapter = new InMemorySavePetAdapter(store);

    @Override
    protected LoadPetPort loadPetPort() {
        return loadAdapter;
    }

    @Override
    protected SavePetPort savePetPort() {
        return saveAdapter;
    }
}
