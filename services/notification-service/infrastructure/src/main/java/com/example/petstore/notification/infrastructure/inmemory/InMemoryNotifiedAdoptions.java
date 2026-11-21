package com.example.petstore.notification.infrastructure.inmemory;

import com.example.petstore.notification.domain.model.AdoptionKey;
import com.example.petstore.notification.domain.port.NotifiedAdoptionsPort;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro en memoria de las adopciones ya notificadas. Suficiente para la demo: se pierde al reiniciar.
 * Para producción bastaría otro adaptador del mismo puerto (por ejemplo, una colección de MongoDB).
 */
@Component
public class InMemoryNotifiedAdoptions implements NotifiedAdoptionsPort {

    private final Set<AdoptionKey> notified = ConcurrentHashMap.newKeySet();

    @Override
    public boolean isNotified(AdoptionKey adoption) {
        return notified.contains(adoption);
    }

    @Override
    public void markNotified(AdoptionKey adoption) {
        notified.add(adoption);
    }
}
