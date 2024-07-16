package org.example.service;

import org.example.entity.AppUser;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;

import static org.example.RabbitQueue.ANSWER_MESSAGE;

public interface ProducerService {
    void producerAnswer(SendMessage sendMessage);
    void producerAnswer(SendPhoto sendPhoto);
    void producerAnswer(SendDocument sendDocument);
    void produceNewTickerRequest(AppUser appUser);
}
