package com.example.petstore.notification.messaging;

import com.example.petstore.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PetAdoptedListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void onPetAdopted(PetAdoptedMessage message) {
        notificationService.notifyAdoption(message);
    }
}
