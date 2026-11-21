package com.example.petstore.adoption.application.port.in;

import com.example.petstore.adoption.domain.exception.PetNotAvailableException;
import com.example.petstore.adoption.domain.exception.PetNotFoundException;

/**
 * Puerto de entrada: adoptar una mascota. Es lo único que ven los canales de entrada (REST, SOAP...).
 */
public interface AdoptPetUseCase {

    /**
     * @throws PetNotFoundException     si la mascota no existe
     * @throws PetNotAvailableException si la mascota no está disponible
     */
    AdoptionResult adopt(AdoptPetCommand command);
}
