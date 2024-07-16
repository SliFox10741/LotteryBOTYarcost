package org.example.service.impl;

import com.vdurmont.emoji.EmojiParser;
import lombok.extern.log4j.Log4j;
import org.example.dao.AppUserDAO;
import org.example.dao.RawDataDAO;
import org.example.entity.AppUser;
import org.example.entity.RawData;
import org.example.exceptions.UploadFileException;
import org.example.service.AppUserService;
import org.example.service.FileService;
import org.example.service.MainService;
import org.example.service.ProducerService;
import org.example.service.additionalCalsses.MessageSender;
import org.example.service.enums.ServiceCommand;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

import static org.example.entity.enums.UserState.*;
import static org.example.service.enums.ServiceCommand.*;

@Log4j
@Service
public class MainServiceImpl implements MainService {
    private final RawDataDAO rawDataDAO;
    private final ProducerService producerService;
    private final AppUserDAO appUserDAO;
    private final FileService fileService;
    private final AppUserService appUserService;
    private final MessageSender messageSender;
    private final UserActivityServiceImpl userActivityService;
    private final ArrayList<String> productList = new ArrayList<>(List.of("сыворотка" ,"шампунь", "спрей-гидролат", "мыло", "масло-суфле"));

    public MainServiceImpl(RawDataDAO rawDataDAO, ProducerService producerService, AppUserDAO appUserDAO,
                           FileService fileService, AppUserService appUserService, MessageSender messageSender, UserActivityServiceImpl userActivityService) {
        this.rawDataDAO = rawDataDAO;
        this.producerService = producerService;
        this.appUserDAO = appUserDAO;
        this.fileService = fileService;
        this.appUserService = appUserService;
        this.messageSender = messageSender;
        this.userActivityService = userActivityService;
    }

    @Override
    public void processTextMessage(Update update) {
        saveRawData(update); //todo а нужно ли?
        var appUser = findOrSaveAppUser(update); //todo доработать

        var userState = appUser.getState();
        var text = update.getMessage().getText();
        var output = "";
        var chatId = update.getMessage().getChatId();

        var serviceCommand = fromValue(text);

        appUser.setLocalDateTime(LocalDateTime.now());
        appUser.setIsActive(false);
        appUserDAO.save(appUser);

        if (START.equals(serviceCommand)) {
            if (START_STATE.equals(userState)) {
                output = processServiceCommand(appUser, text);
                messageSender.sendMessage(new MessageSender.MessageParams(output, chatId, "Register.jpg", new ArrayList<>(List.of("УЧАСТВУЮ")),  false,true, false, false));
            } else if (!appUser.getTicketNumber().isEmpty()) {
                output = userActivityService.generateReminderMessage(appUser.getName(), appUser.getTicketNumber());
                messageSender.sendMessage(new MessageSender.MessageParams(output, chatId, false, false));
                seePhoto(update);
            }
        } else if (WAIT_FOR_NAME_STATE.equals(userState)) {
            output = appUserService.setName(appUser, text);
            messageSender.sendMessage(new MessageSender.MessageParams(output, chatId, true, false, false));
        }  else if (ADMIN_STATE.equals(userState)) {
            if (GETUSER.equals(serviceCommand)) {
                output = processServiceCommand(appUser, text);
                sendAnswer(output, chatId);
                if (fileService.saveToEXEL(chatId)) {
                    output = "Все записи отправлены";
                    sendAnswer(output, chatId);
                } else {
                    output = "Произошла ошибка при считывании базы. Попробуйте ещё раз или обратитесь к администратору";
                    sendAnswer(output, chatId);
                }

            } else if (HELP.equals(serviceCommand)) {
                output = processServiceCommand(appUser, text);
                sendAnswer(output, chatId);
            } else if (CANCEL.equals(serviceCommand)) {
                output = cancelProcess(appUser);
                sendAnswer(output, chatId);
            } else if (sendReminders.equals(serviceCommand)) {
                userActivityService.sendRemindersToAllUsers();
            } else if (EXIT_FROM_ADMIN.equals(serviceCommand)) {
                cancelProcess(appUser);
            } else if (sendRemindersWithText.equals(serviceCommand)) {
                output = processServiceCommand(appUser, serviceCommand.toString());
                messageSender.sendMessage(new MessageSender.MessageParams(output, chatId, new ArrayList<>(List.of("Всем", "Безбилетникам")), true, false));
            } else {
                messageSender.sendMessage(new MessageSender.MessageParams("Ой.. такую команду я не знаю", chatId, false, false));
            }
        } else if (REMINDERS_SELECTION_OPTIONS_ALL.equals(userState) || REMINDERS_SELECTION_OPTIONS_NOTTICKET.equals(userState)) {
            userActivityService.sendReminders(text, REMINDERS_SELECTION_OPTIONS_ALL.equals(userState));
        	appUser.setState(ADMIN_STATE);
		appUserDAO.save(appUser);
	} else if (CANCEL.equals(serviceCommand)) {
            output = cancelProcess(appUser);
            sendAnswer(output, chatId);
        }else if (serviceCommand!=null) {
            output = processServiceCommand(appUser, text);
            messageSender.sendMessage(new MessageSender.MessageParams(output, chatId, false, false));
        } else if (WAIT_FOR_NUMBER_STATE.equals(userState)) {
            messageSender.sendMessage(new MessageSender.MessageParams("Пожалуйста, нажмите кнопку \"Поделиться контактом\" под клавиаурой.", chatId, false, false));
        } else {
            messageSender.sendMessage(new MessageSender.MessageParams("Неизвестная команда или действие. Пожалуйста, проверьте, что вы отправляете что нужно или отправьте /cancel и пройдите регистрацию", chatId, false, false));
        }
    }
    private String processServiceCommand(AppUser appUser, String cmd) {
        var userState = appUser.getState();
        var serviceCommand = fromValue(cmd);
        if (START.equals(serviceCommand)) {
            return "\ud83e\ude77\ud83e\ude77 КАК ПОЛУЧИТЬ ПОДАРОК?\n" +
                    "Все очень просто:\n" +
                    "\n" +
                    "_ Оставить отзыв о продукте YARKOST на сайте маркетплейса.\n" +
                    "\n" +
                    "*каждому участнику гарантированный подарок!\n" +
                    "Победителей главных призов определим 12.08.2024 в @yarkostorganic в прямом эфире.\n" +
                    "\n" +
                    "<a href=\"https://docs.google.com/document/d/1kRxHGhj3EI-sHQg_qFpQyU11VJv-FMAcQlr6hMYr-Bs/edit\">Порядок, условия, сроки проведения рекламной акции</a> / <a href=\"https://t.me/yarkost_organic\">Задать вопрос</a>\n" +
                    "\n" +
                    "Жмите кнопку УЧАСТВУЮ" + EmojiParser.parseToUnicode(":arrow_down:");
        } else if (HELP.equals(serviceCommand)) {
            if (ADMIN_STATE.equals(userState)) {
                return "Список команд: " +
                        "/sendReminders - делает рассылку всем пользователям с напоминаним их номеров билетов " +
                        "/getcontact - собирает из базы данных инфрмацию по пользователям в xml файл и отправляет.\n" +
                        "/exit - вы выходите из режима администратора. После этого заного введите /start";
            } else {
                return "Если у вас возникли сложности или просто есть вопрос, " +
                        "напишите нашему заботливому менеджеру @yarkost_organic, он оперативно ответит вам.";
            }

        } else if (FEEDBACK.equals(serviceCommand)) {
            return EmojiParser.parseToUnicode(":paperclip:") + "Прикрепите 2 фотографии воспользовавшись скрепкой около клавиатуры\n" +
                    "Пожалуйста, убедитесь, что скриншоты соответствуют правилам акции, должно быть загружено 2 фото: \n" +
                    "- чек об оплате товара из раздела «покупки» и отзыв, в котором четко видно артикул товара \n" +
                    "— тип файла: JPEG, PNG, JPG, BMP, HEIC \n" +
                    "— размер фото не более 5 Мб — разрешение не менее 200 dpi \n" +
                    "— размер по высоте и ширине до 2048px \n" +
                    "Смело отправляйте фотографии \n";
        } else if (sendRemindersWithText.equals(serviceCommand)) {
            if (ADMIN_STATE.equals(userState)) {
                appUser.setState(REMINDERS_SELECTION_OPTIONS);
                appUserDAO.save(appUser);

                return "Вы хотите отправить сообщение всем или только тем, кто не получил ни одного билета? Выберите верный ответ";
            } else {
                return "Неизвестная команда";
            }
        } else if (ADMIN.equals(serviceCommand)) {
            appUser.setState(ADMIN_AUTHENTICATION_STATE);
            appUserDAO.save(appUser);
            return "Введите пароль:"; //todo admin
        } else if (ADMIN_AUTHENTICATION_STATE.equals(userState)) {
            if (ADMIN_PASS.equals(serviceCommand)) {
                appUser.setState(ADMIN_STATE);
                appUserDAO.save(appUser);
                return "Вы вошли в режим администратора!";
            } else {
                appUser.setState(START_STATE);
                appUserDAO.save(appUser);
                return "Неверный пароль";
            }
        } else if (GETUSER.equals(serviceCommand)) {
            return "Запрос получен. Ожидайте (весь процесс может занимать от одной до нескольких минут. Я сообщу, когда закончу отправку)";
        }  else {
            return "Неизвестная команда!";
        }
    }

    @Override
    public void processDocMessage(Update update) {
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var chatId = update.getMessage().getChatId();

        if (WAIT_FOR_PHOTO_1_STATE.equals(appUser.getState()) || WAIT_FOR_PHOTO_2_STATE.equals(appUser.getState())) {
            try {
                fileService.processDoc(update.getMessage());
            } catch (UploadFileException ex) {
                log.error(ex);
                String error = "К сожалению, загрузка файла не удалась. Повторите попытку позже.";
                sendAnswer(error, chatId);
            }
        }

    }

    @Override
    public void processPhotoMessage(Update update) {
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var chatId = update.getMessage().getChatId();
        String output = "";
        var userState = appUser.getState();

        appUser.setLocalDateTime(LocalDateTime.now());
        appUser.setIsActive(false);
            appUserDAO.save(appUser);

        if (WAIT_FOR_PHOTO_1_STATE.equals(userState) || WAIT_FOR_PHOTO_2_STATE.equals(userState)) {
            try {
                List<PhotoSize> photos = update.getMessage().getPhoto();

                Map<String, PhotoSize> uniquePhotos = new HashMap<>();

                for (PhotoSize photo : photos) {
                    String baseFileId = photo.getFileId().substring(0, 30); // Выбираем базовую часть fileId
                    if (!uniquePhotos.containsKey(baseFileId)) {
                        uniquePhotos.put(baseFileId, photo); // Добавляем фото, если оно уникально
                    }
                }

                for (PhotoSize photo : uniquePhotos.values()) {
                    fileService.processPhoto(photo);
                    if (WAIT_FOR_PHOTO_1_STATE.equals(appUser.getState())) {
                        appUser.setState(WAIT_FOR_PHOTO_2_STATE);
                        appUserDAO.save(appUser);
                        if (photos.size() == 1) {
                            sendAnswer("Ожидаю второй скриншот..", chatId);
                        }
                    } else {
                        if (photos.size() > 1) {
                            photos = photos.stream()
                                    .limit(1)
                                    .toList();
                        }
                        output = "Начинаю проверку, секундочку…";
                        sendAnswer(output, chatId);

                        producerService.produceNewTickerRequest(appUser);

                        try {
                            Thread.sleep(20000); // 20 seconds in milliseconds
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        appUser = findOrSaveAppUser(update); //todo оптимизировать обновление состтояния
                        output = "Поздравляю, ваш отзыв зарегистрирован! \nНомер вашего счастливого купона: " + appUser.getTicketNumber().get(appUser.getTicketNumber().size()-1).replace("[", "").replace("]", "");
                        messageSender.sendMessage(new MessageSender.MessageParams(output, chatId, false, false));

                        seePhoto(update);

                    }
                }
            } catch (UploadFileException ex) {
                log.error(ex);
                String error = "К сожалению, загрузка фото не удалась. Повторите попытку позже.";
                sendAnswer(error, chatId);
            }
        }
    }
    @Override
    public void processContactMessage(Update update) {
        var appUser = findOrSaveAppUser(update);
        var chatId = update.getMessage().getChatId();

        appUser.setLocalDateTime(LocalDateTime.now());
        appUser.setIsActive(false);
            appUserDAO.save(appUser);

        if (WAIT_FOR_NUMBER_STATE.equals(appUser.getState())) {

            messageSender.sendMessage(new MessageSender.MessageParams("Cупер!", chatId, false, true));
            appUser.setPhoneNumber(update.getMessage().getContact().getPhoneNumber());

            messageSender.sendMessage(new MessageSender.MessageParams(
                    "Теперь выберите купленный товар и нажмите на кнопку!" + EmojiParser.parseToUnicode(":arrow_down:"),
                    chatId,
                    new ArrayList<>(List.of("Сыворотка" ,"Шампунь", "Спрей-гидролат", "Мыло", "Масло-суфле")), false, false)
            );

            appUser.setState(PRODUCT_SELECTION);
            appUserDAO.save(appUser);
        }
    }
    @Override
    public void processCallBackMessage(Update update) {
        var text = update.getCallbackQuery().getData().toLowerCase();
        System.out.println("Получил callback в node");
        ServiceCommand serviceCommand = fromValue(text);;
        var appUser = findOrSaveAppUser(update);
        var chatId = update.getCallbackQuery().getMessage().getChatId();
        var output = "";
        var userState = appUser.getState();

        appUser.setLocalDateTime(LocalDateTime.now());
        appUser.setIsActive(false);
            appUserDAO.save(appUser);

        if (text.equals(HELP.toString())) {
            output = processServiceCommand(appUser, text);
            messageSender.sendMessage(new MessageSender.MessageParams(output, chatId, false, false));
        } else if (START_STATE.equals(userState)) {
            if (text.equals(SET_NAME.toString())) {
                output = "Как я могу к вам обращаться?\n\n" +"Напишите мне сюда сообщением" + EmojiParser.parseToUnicode(":arrow_down:");

                messageSender.sendMessage(new MessageSender.MessageParams(output, chatId, false, false));

                appUser.setState(WAIT_FOR_NAME_STATE);
                appUserDAO.save(appUser);
            }
        } else if (productList.contains(text)) {

            output = "\ud83e\ude77Оставьте честный отзыв о " + modifyWord(text) + " от YARKOST\n" +
                    EmojiParser.parseToUnicode(":paperclip:") + "Прикрепите здесь 2 скрина: " +
                    "чек об оплате с маркетплейса и " +
                    "отзыв с артикулом товара, " +
                    "воспользовавшись скрепкой около клавиатуры.";


            messageSender.sendMessage(new MessageSender.MessageParams(output,
                    chatId, "MarketPlace.jpg", false, true,false, false
            ));
            appUser.setState(WAIT_FOR_PHOTO_1_STATE);
            appUserDAO.save(appUser);
        } else if (userState.equals(REMINDERS_SELECTION_OPTIONS)) {
            if ("всем".equals(text)) {
                output = "Вы выбрали отправить сообщение всем. " +
                        "Отправьте в чат сообщение, которое хотите разослать (вместо имени напишите \"name\", вместо времени сколько осталось до розыгрыша, напишите \"time\", вместо списка билетов напишите \"tickets\") \n\nИли введите /cancel для отмены.";
                appUser.setState(REMINDERS_SELECTION_OPTIONS_ALL);
                appUserDAO.save(appUser);
            } else if ("безбилетникам".equals(text)) {
                output = "Вы выбрали отправить сообщение только безбилетникам. " +
                        "Отправьте в чат сообщение, которое хотите разослать (вместо имени напишите \"name\", вместо времени сколько осталось до розыгрыша, напишите \"time\", вместо списка билетов напишите \"tickets\") \n\nИли введите /cancel для отмены.";
                appUser.setState(REMINDERS_SELECTION_OPTIONS_NOTTICKET);
                appUserDAO.save(appUser);
            } else {
                output = "Я не знаю такой команды. попробуйте ещё раз или введите /cancel.";
            }
            messageSender.sendMessage(new MessageSender.MessageParams(output, chatId, false, false));
        } else if (serviceCommand != null) {
            if (userState.equals(ADMIN_STATE)) {
                output = processServiceCommand(appUser, serviceCommand.toString());
                if (serviceCommand.equals(sendRemindersWithText)) {
                    messageSender.sendMessage(new MessageSender.MessageParams(output, chatId, new ArrayList<>(List.of("Всем", "Безбилетникам")), true, false));
                } else {
                    messageSender.sendMessage(new MessageSender.MessageParams(output, chatId, false, false));
                }
            }
        }

    }

    private void sendAnswer(String output, Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(output);
        producerService.producerAnswer(sendMessage);
    }

    private void sendAnswerWithButton(String output, Long chatId, ArrayList<String> textButtons, boolean needContact) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(output);
        if (needContact) {
            createContactButton(sendMessage);
        } else {
            createButton(sendMessage, textButtons);
        }

        producerService.producerAnswer(sendMessage);
    }

    private void sendAnswerWithUrlButton(String output, Long chatId, ArrayList<String> urls, ArrayList<String> textButtons) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton buttonInline;
        SendMessage sendMessage = new SendMessage();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        sendMessage.setChatId(chatId);
        sendMessage.setText(output);
        int i = 0;
        String text;
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        for (String url : urls){
            text = textButtons.get(i);
            buttonInline = new InlineKeyboardButton();
            buttonInline.setCallbackData(url);
            buttonInline.setUrl(url);
            buttonInline.setText(text);
            rowInline.add(buttonInline);
            i++;
        }
        rowsInline.add(rowInline);
        inlineKeyboard.setKeyboard(rowsInline);
        sendMessage.setReplyMarkup(inlineKeyboard);
        producerService.producerAnswer(sendMessage);
    }
    private void sendAnswerWithPhoto(String output, Long chatId, String fileName) {
        SendPhoto sendPhoto = new SendPhoto();
        File file = null;
        try {
            file = new File("D:/Prog/Progect/LotteryBot2/images/" + fileName);
        } catch (Exception e) {
            System.out.println(e);
        }

        assert file != null;
        sendPhoto.setPhoto(new InputFile(file));
        sendPhoto.setCaption(output);
        sendPhoto.setChatId(chatId);
        producerService.producerAnswer(sendPhoto);
    }

    //todo (связь с оператором)
    private String help() {
        return "Список доступных команд:\n"
                + "/cancel - отмена выполнения текущей команды;\n"
                + "/registration - регистрация пользователя.";
    }

    //TODO (переделать отмену)
    private String cancelProcess(AppUser appUser) {
        appUser.setState(START_STATE);
        appUserDAO.save(appUser);
        return "Вы вернулись назад. Пропишите /start для начал регистрации";
    }

    private AppUser findOrSaveAppUser(Update update) {
        User telegramUser;
        if (update.hasCallbackQuery()) {
            telegramUser = update.getCallbackQuery().getFrom();
        } else {
            telegramUser = update.getMessage().getFrom();
        }

        var optional = appUserDAO.findByTelegramUserId(telegramUser.getId());
        if (optional.isEmpty()) {
            AppUser transientAppUser = AppUser.builder()
                    .telegramUserId(telegramUser.getId())
                    .username(telegramUser.getUserName())
                    .isActive(false)
                    .state(START_STATE)
                    .ticketNumber(new ArrayList<>())
                    .build();
            return appUserDAO.save(transientAppUser);
        }
        return optional.get();
    }

    private void saveRawData(Update update) {
        RawData rawData = RawData.builder()
                .event(update)
                .build();
        rawDataDAO.save(rawData);
    }

    private SendMessage createContactButton(SendMessage sendMessage) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();
        KeyboardButton keyboardButton = new KeyboardButton();
        keyboardButton.setText("Поделиться контактом");
        keyboardButton.setRequestContact(true);
        keyboardRow.add(keyboardButton);

        keyboard.add(keyboardRow);

        replyKeyboardMarkup.setKeyboard(keyboard);

        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        return sendMessage;
    }

    private SendMessage createButton(SendMessage sendMessage, ArrayList<String> textButtons) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (String textButton : textButtons) {

            InlineKeyboardButton buttonInline = new InlineKeyboardButton();
            buttonInline.setText(textButton);
            buttonInline.setCallbackData(textButton.toLowerCase());

            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            rowInline.add(buttonInline);

            rowsInline.add(rowInline);
        }

        inlineKeyboard.setKeyboard(rowsInline);
        sendMessage.setReplyMarkup(inlineKeyboard);

        return sendMessage;
    }

    public void seePhoto (Update update) {
        String output;
        Long chatId = update.getMessage().getChatId();
        AppUser appUser = findOrSaveAppUser(update);
        if (GOT_FIRST_TICKET_STATE.equals(appUser.getState())) {
            output = "А чтобы получить еще один купон, ответьте на пару вопросов.";
            messageSender.sendMessage(new MessageSender.MessageParams(output,
                    chatId,
                    new ArrayList<>(List.of("ОТВЕТИТЬ")),
                    new ArrayList<>(List.of("https://docs.google.com/forms/d/1hBB46t2ejwORoIrIy67z9P6VwY3H38SJouNPCG6q1uo/edit")),
                    false,
                    false, false));

            producerService.produceNewTickerRequest(appUser);
            try {
                Thread.sleep(120000); // 120 seconds in milliseconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            output = "Спасибо, передаю ответы команде YARKOST, чтобы продукты бренда стали еще лучше, секундочку…";
            messageSender.sendMessage(new MessageSender.MessageParams(output, chatId, false, false));

            appUser = findOrSaveAppUser(update);

            try {
                Thread.sleep(20000); // 60 seconds in milliseconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            output = "Поздравляю, ваш отзыв зарегистрирован! Номер вашего счастливого купона: " +
                    appUser.getTicketNumber().get(appUser.getTicketNumber().size()-1).replace("[", "").replace("]", "");
            messageSender.sendMessage(new MessageSender.MessageParams(output, chatId, false, false));
            appUser.setState(GOT_ANY_TICKET_STATE);
            appUserDAO.save(appUser);


            try {
                Thread.sleep(5000); // 60 seconds in milliseconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            seePhoto(update);
        } else  {
            output = "Больше купонов-больше шансов выиграть главный приз:\n" +
                    "1. Покупайте средства YARKOST\n" +
                    "2. Регистрируйте покупку и получайте за каждую счастливый купон\n";
            messageSender.sendMessage(new MessageSender.MessageParams(output,
                    chatId, "MarketPlace.jpg",
                    new ArrayList<>(List.of("ЗОЛОТОЕ ЯБЛОКО",
                            "WB",
                            "OZON")),
                    new ArrayList<>(List.of("https://goldapple.ru/brands/yarkost?p=1",
                            "https://www.wildberries.ru/brands/yarkost-1032169",
                            "https://www.ozon.ru/seller/yarkost-634316/products/?miniapp=seller_634316")
                    ),
                    false, false,
                    false, false)
            );
            appUserDAO.save(appUser);
            try {
                Thread.sleep(20000); // 20 seconds in milliseconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            output = "\ud83e\ude77Оставьте честный отзыв о купленном товаре\n" +
                    EmojiParser.parseToUnicode(":paperclip:") + "Прикрепите здесь 2 скрина: " +
                    "чек об оплате с маркетплейса и " +
                    "отзыв с артикулом товара, " +
                    "воспользовавшись скрепкой около клавиатуры.";


            messageSender.sendMessage(new MessageSender.MessageParams(output,
                    chatId, "MarketPlace.jpg", false, true,false, false
            ));
            appUser.setState(WAIT_FOR_PHOTO_1_STATE);
            appUserDAO.save(appUser);
        }

    }

    public static String modifyWord(String word) {
        // Проверяем, является ли слово "спрей-гидролат"
        if (word.equalsIgnoreCase("спрей-гидролат")) {
            // Если да, то добавляем букву "е" в конец слова
            return word + "е";
        } else {
            // Если нет, то удаляем последнюю букву и добавляем "е" в конец
            return word.substring(0, word.length() - 1) + "е";
        }
    }
}
