package com.example.petstore.notification.domain.port;

import com.example.petstore.notification.domain.model.AdoptionKey;

/**
 * Puerto de salida: recordar qué adopciones ya se han notificado. Un mismo evento puede llegar dos veces.
 */
public interface NotifiedAdoptionsPort {

    boolean isNotified(AdoptionKey adoption);

    void markNotified(AdoptionKey adoption);
}
