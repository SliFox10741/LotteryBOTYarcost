package org.example.service.impl;

import org.example.dao.AppUserDAO;
import org.example.entity.AppUser;
import org.example.service.additionalCalsses.MessageSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserActivityServiceImpl {
    private static final long INACTIVITY_THRESHOLD_MINUTES = 60;
    private static final Map<Long, LocalDateTime> lastMessageTimes = new HashMap<>();
    private final AppUserDAO appUserDAO;
    private final MessageSender messageSender;

    public UserActivityServiceImpl (AppUserDAO appUserDAO, MessageSender messageSender) {
        this.appUserDAO = appUserDAO;
        this.messageSender = messageSender;
    }

       @Scheduled(fixedRate = 3600000) // Каждый час
    private void checkUserActivity() {

        List<AppUser> allUsers = appUserDAO.findAll();
        LocalDateTime currentTime = LocalDateTime.now();
        for (AppUser appUser : allUsers) {
            Long telegramUserId = appUser.getTelegramUserId();
            LocalDateTime lastMessageTime = appUser.getLocalDateTime();
            long minutesSinceLastMessage = ChronoUnit.MINUTES.between(lastMessageTime, currentTime);

            if (minutesSinceLastMessage > INACTIVITY_THRESHOLD_MINUTES) {
                if (!appUser.getIsActive()) {
                    if (!appUser.getTicketNumber().isEmpty()) {
                        messageSender.sendMessage(new MessageSender.MessageParams(generateReminderMessage(appUser.getName(), appUser.getTicketNumber()), telegramUserId, true, false));
                        appUser.setIsActive(true);
                        appUserDAO.save(appUser);
                    }

                }
            }
        }
    }

    public void sendReminders(String text, boolean forAll) {
        List<AppUser> users = appUserDAO.findAll();
        String reminderMessage = "";
	if (text.equals("/cancel")) {
		return;
	}
        for (AppUser user : users) {
            ArrayList<String> userTickets = user.getTicketNumber();
            reminderMessage = text
                    .replaceAll("\\bname\\b", user.getName() != null ? user.getName() : "Дорогая")
                    .replaceAll("\\btime\\b", (String.valueOf(printDaysUntilDate(LocalDate.of(2024, 10, 6))))+" дня(-ей)")
                    .replaceAll("\\btickets\\b", userTickets.isEmpty() ? "\nУ тебя пока нет билетов\n" : showTicketsString(userTickets));
            if (userTickets.isEmpty() && !forAll) {
                messageSender.sendMessage(new MessageSender.MessageParams(reminderMessage, user.getTelegramUserId(), true, false));
//                messageSender.sendMessage(new MessageSender.MessageParams(reminderMessage, 1076383248L, true, false));
            } else if (forAll) {
                messageSender.sendMessage(new MessageSender.MessageParams(reminderMessage, user.getTelegramUserId(), true, false));
                //messageSender.sendMessage(new MessageSender.MessageParams(reminderMessage, 1076383248L, true, false));
            }

        }
    }
    public void sendRemindersToAllUsers() {
        List<AppUser> users = appUserDAO.findAll();

        for (AppUser user : users) {
            ArrayList<String> userTickets = user.getTicketNumber();

            String reminderMessage = generateReminderMessage(user.getName(), userTickets);

            messageSender.sendMessage(new MessageSender.MessageParams(reminderMessage, user.getTelegramUserId(), true, false));
        }
    }

    public String generateReminderMessage(String username, ArrayList<String> ticketNumbers) {
        // Ваш код для формирования сообщения с напоминанием
        StringBuilder text= new StringBuilder(", это ваш личный кабинет! \n" +
                "\n" +
                "Номера ваших счастливых билетиков:\n" +
                "\n");

        text.insert(0, username);

        for (String ticket : ticketNumbers) {
            text.append(ticket + "\n");
        }

        text.append("\n" +
                "Розыгрыш состоиться через "+ (printDaysUntilDate(LocalDate.of(2024, 8, 12)))+ " дня(-ей)\n" +
                "Следите за результатами в группе @yarkostorganic");

        return text.toString();
    }

    public long printDaysUntilDate(LocalDate targetDate) {
        LocalDate currentDate = LocalDate.now();
        long daysUntil = ChronoUnit.DAYS.between(currentDate, targetDate);
        return daysUntil;
    }

    private String showTicketsString(ArrayList<String> ticketNumbers) {
        StringBuilder text= new StringBuilder("\n");
        for (String ticket : ticketNumbers) {
            text.append(ticket + "\n");
        }
        return text.toString();
    }
}
