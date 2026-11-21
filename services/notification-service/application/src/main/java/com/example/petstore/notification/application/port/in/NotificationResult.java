package com.example.petstore.notification.application.port.in;

public enum NotificationResult {
    /** Se ha enviado el aviso. */
    SENT,
    /** La adopción ya se había notificado: el evento llegó repetido y no se envía nada. */
    ALREADY_NOTIFIED
}
