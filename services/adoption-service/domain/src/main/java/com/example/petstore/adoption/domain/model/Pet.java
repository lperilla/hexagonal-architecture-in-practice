package com.example.petstore.adoption.domain.model;

import com.example.petstore.adoption.domain.exception.PetNotAvailableException;

import java.util.Objects;

/**
 * Mascota de la tienda. Regla de negocio: solo puede adoptarse si está disponible y, al
 * adoptarla, queda asociada a su nuevo dueño.
 * <p>
 * Es inmutable: adoptar no cambia esta mascota, devuelve otra con el estado adoptado. Así nadie
 * puede saltarse la regla con un setter, ninguna mascota queda a medio cambiar si algo falla, y se
 * puede compartir entre hilos, tests y adaptadores sin copias defensivas.
 *
 * @param owner el dueño; solo existe si la mascota está adoptada
 */
public record Pet(PetId id, String name, PetStatus status, Owner owner) {

    public Pet {
        Objects.requireNonNull(id, "id");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre de la mascota no puede estar vacío");
        }
        Objects.requireNonNull(status, "status");
        if ((status == PetStatus.ADOPTED) != (owner != null)) {
            throw new IllegalArgumentException("Una mascota tiene dueño si y solo si está adoptada");
        }
    }

    /**
     * Crea una mascota nueva, disponible para adopción.
     */
    public static Pet available(PetId id, String name) {
        return new Pet(id, name, PetStatus.AVAILABLE, null);
    }

    public boolean isAvailable() {
        return status == PetStatus.AVAILABLE;
    }

    /**
     * Adopta la mascota.
     *
     * @return la mascota adoptada por su nuevo dueño; esta mascota no cambia
     * @throws PetNotAvailableException si la mascota no está disponible
     */
    public Pet adopt(Owner newOwner) {
        Objects.requireNonNull(newOwner, "newOwner");
        if (!isAvailable()) {
            throw new PetNotAvailableException(id);
        }
        return new Pet(id, name, PetStatus.ADOPTED, newOwner);
    }
}
