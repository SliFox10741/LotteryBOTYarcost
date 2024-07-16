package org.example.service;

import org.example.entity.AppDocument;
import org.example.entity.AppPhoto;
import org.example.entity.AppUser;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;

import java.io.IOException;

public interface FileService {
    void processDoc(Message telegramMessage);
    void processPhoto(PhotoSize telegramPhoto);

    void generateUniqueTicket(AppUser appUser);
    boolean saveToEXEL (Long chadId);

//    boolean saveToXmlDB(Long chatId);
}
