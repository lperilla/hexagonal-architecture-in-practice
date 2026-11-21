package com.example.petstore.notification.domain.port;

import com.example.petstore.notification.domain.model.Notification;

/**
 * Puerto de salida: hacer llegar un aviso a su destinatario. Lo implementan el envío por SMTP y el log.
 */
public interface SendNotificationPort {

    void send(Notification notification);
}
