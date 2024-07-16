package org.example.service.impl;

import org.example.controller.UpdateProcessor;
import org.example.service.AnswerConsumer;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;

import static org.example.RabbitQueue.*;

@Service
public class AnswerConsumerImpl implements AnswerConsumer {
    private final UpdateProcessor updateProcessor;

    public AnswerConsumerImpl(UpdateProcessor updateProcessor) {
	this.updateProcessor = updateProcessor;
    }

    @Override
    @RabbitListener(queues = ANSWER_MESSAGE)
    public void consume(SendMessage sendMessage) {
        updateProcessor.setView(sendMessage);
    }
    @Override
    @RabbitListener(queues = ANSWER_PHOTO)
    public void consume(SendPhoto sendPhoto) {
        updateProcessor.setView(sendPhoto);
    }

    @Override
    @RabbitListener(queues = ANSWER_DOC)
    public void consume(SendDocument sendDocument) {
        System.out.println("Получил док");
        updateProcessor.setView(sendDocument);
    }
}
