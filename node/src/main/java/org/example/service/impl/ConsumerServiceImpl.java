package org.example.service.impl;

import lombok.extern.log4j.Log4j;
import org.example.dao.AppUserDAO;
import org.example.entity.AppUser;
import org.example.entity.enums.UserState;
import org.example.service.ConsumerService;
import org.example.service.FileService;
import org.example.service.MainService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;


import java.io.IOException;


import static org.example.RabbitQueue.*;

@Service
@Log4j
public class ConsumerServiceImpl implements ConsumerService {
    private final MainService mainService;
    private final AppUserDAO appUserDAO;
    private final FileService fileService;


    public ConsumerServiceImpl(MainService mainService, AppUserDAO appUserDAO, FileService fileService) {
        this.mainService = mainService;
        this.appUserDAO = appUserDAO;
        this.fileService = fileService;
    }

    @Override
    @RabbitListener(queues = TEXT_MESSAGE_UPDATE)
    public void consumeTextMessageUpdates(Update update) {
        log.debug("NODE: Text message is received");
        mainService.processTextMessage(update);
    }


    @Override
    @RabbitListener(queues = PHOTO_MESSAGE_UPDATE)
    public void consumePhotoMessageUpdates(Update update) {
        log.debug("NODE: Photo message is received");
        mainService.processPhotoMessage(update);
    }

    @Override
    @RabbitListener(queues = CONTACT_UPDATE)
    public void consumeContactMessageUpdates(Update update) {
        log.debug("NODE: Contact message is received");
        mainService.processContactMessage(update);
    }

    @Override
    @RabbitListener(queues = CALLBACK_UPDATE)
    public void consumeCallBackUpdates(Update update) {
        log.debug("NODE: CallBack message is received");
        mainService.processCallBackMessage(update);
    }

    @Override
    @RabbitListener(queues = NEW_TICKET_REQUEST)
    public void consumeNewTicketRequest(AppUser appUser) {
        log.debug("NODE: need new ticket");
        fileService.generateUniqueTicket(appUser);
    }

}
