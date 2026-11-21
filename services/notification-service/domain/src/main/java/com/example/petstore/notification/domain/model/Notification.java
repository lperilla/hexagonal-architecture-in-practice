package com.example.petstore.notification.domain.model;

import java.util.Objects;

/**
 * Aviso para una persona: a quién, con qué asunto y con qué texto. No sabe si viaja por email, por
 * log o por otro canal; eso lo decide el adaptador.
 */
public record Notification(EmailAddress recipient, String subject, String text) {

    public Notification {
        Objects.requireNonNull(recipient, "recipient");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(text, "text");
    }

    /**
     * El aviso al refugio de que una de sus mascotas ha sido adoptada.
     */
    public static Notification adoptionNotice(Adoption adoption, EmailAddress shelter) {
        String subject = adoption.ownerName() + " ha adoptado a " + adoption.petName();
        String text = """
                Hola:

                %s ha adoptado a %s (mascota %s).
                Fecha de la adopción: %s

                Tienda de mascotas
                """.formatted(adoption.ownerName(), adoption.petName(), adoption.petId(), adoption.adoptedAt());
        return new Notification(shelter, subject, text);
    }
}
