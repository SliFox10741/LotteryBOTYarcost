package org.example.service;

import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;

public interface AnswerConsumer {
    void consume(SendMessage sendMessage);
    void consume(SendPhoto sendPhoto);
    void consume(SendDocument sendDocument);

}
