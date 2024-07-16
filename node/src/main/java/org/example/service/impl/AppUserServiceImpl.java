package org.example.service.impl;

import lombok.extern.log4j.Log4j;
import org.example.dao.AppUserDAO;
import org.example.entity.AppUser;
import org.example.service.AppUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import static org.example.entity.enums.UserState.*;

@Log4j
@Service
public class AppUserServiceImpl implements AppUserService {
    private final AppUserDAO appUserDAO;

    public AppUserServiceImpl(AppUserDAO appUserDAO) {
	this.appUserDAO = appUserDAO;
    }

    @Override
    public String setName (AppUser appUser, String name) {
        appUser.setName(name);
        appUser.setState(WAIT_FOR_NUMBER_STATE);
        appUserDAO.save(appUser);
        return "Куда звонить, когда вы окажетесь победителем главного приза?\n" +
                "Нажмите на кнопку под клавиатурой\n";
    }

}
