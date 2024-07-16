package org.example.service.impl;

import org.example.entity.AppUser;
import org.example.service.ProducerService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;

import static org.example.RabbitQueue.*;

@Service
public class ProducerServiceImpl implements ProducerService {
    private final RabbitTemplate rabbitTemplate;

    public ProducerServiceImpl(RabbitTemplate rabbitTemplate) {
	this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void producerAnswer(SendMessage sendMessage) {
        rabbitTemplate.convertAndSend(ANSWER_MESSAGE, sendMessage);
    }
    @Override
    public void producerAnswer(SendPhoto sendPhoto) {
        rabbitTemplate.convertAndSend(ANSWER_PHOTO, sendPhoto);
    }
    @Override
    public void producerAnswer(SendDocument sendDocument) {
        rabbitTemplate.convertAndSend(ANSWER_DOC, sendDocument);
    }
    @Override
    public void produceNewTickerRequest(AppUser appUser) {
        rabbitTemplate.convertAndSend(NEW_TICKET_REQUEST, appUser);
    }
}
