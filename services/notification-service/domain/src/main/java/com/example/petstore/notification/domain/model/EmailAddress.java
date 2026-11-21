package com.example.petstore.notification.domain.model;

public record EmailAddress(String value) {

    public EmailAddress {
        if (value == null || !value.matches("[^@\\s]+@[^@\\s]+")) {
            throw new IllegalArgumentException("Dirección de email no válida: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
