package org.example.service;

import org.example.entity.AppUser;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;

import static org.example.RabbitQueue.NEW_TICKET_REQUEST;

public interface ConsumerService {
    void consumeTextMessageUpdates(Update update);
    void consumePhotoMessageUpdates(Update update);
    void consumeContactMessageUpdates(Update update);
    void consumeCallBackUpdates(Update update);
    void consumeNewTicketRequest(AppUser appUser) throws IOException;
}
