package org.example.service.additionalCalsses;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.example.service.ProducerService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Log4j
@Service
public class MessageSender {
    @Value("${token}")
    String botToken;
    private ProducerService producerService;
//    private static final String IMAGE_PATH = "D:\\Prog\\Progect\\LotteryBot2\\images\\";
    private static final String IMAGE_PATH = "/root/lotteryBot/LotteryBot2/images/";


    public MessageSender (ProducerService producerService) {
        this.producerService = producerService;
    }
    public void sendMessage(MessageParams params) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(params.getText());
        sendMessage.setChatId(params.getChatId());
        if (params.deleteButton) {
            ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove();
            keyboardRemove.setRemoveKeyboard(true);

            // Устанавливаем replyMarkup в null для удаления кнопки
            sendMessage.setReplyMarkup(keyboardRemove);
        }
        if (params.isPhoto()) {
            sendPhotoMessage(params.getPhotoName(), params.getChatId(), params.getText(), params.buttonTexts, params.buttonUrls, params.needContact);
            return;
//            sendPhoto.setChatId(params.getChatId());
//            producerService.producerAnswer(sendPhoto);
        }
        if (params.isButtons()) {
            if (params.needContact) {
                sendMessage.setReplyMarkup(sendMessageWithButtonsContact());
            } else {
                assert params.buttonTexts != null;
                if (params.buttonUrls != null) {
                    sendMessage.setReplyMarkup(sendMessageWithButtonsUrl(params.buttonTexts, params.buttonUrls));

                } else {
                    sendMessage.setReplyMarkup(sendMessageWithButtons(params.buttonTexts));
                }
            }
        }
        if (params.needHTML) {
            sendMessage.setParseMode(ParseMode.HTML);
            sendMessage.setDisableWebPagePreview(true);
        }


        if (params.deleteButton) {
            ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove();
            keyboardRemove.setRemoveKeyboard(true);

                // Устанавливаем replyMarkup в null для удаления кнопки
            sendMessage.setReplyMarkup(keyboardRemove);
        }

        producerService.producerAnswer(sendMessage);
    }


    private ReplyKeyboardMarkup sendMessageWithButtonsContact() {
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

        return replyKeyboardMarkup;
    }

    private InlineKeyboardMarkup sendMessageWithButtonsUrl(List<String> buttonTexts, List<String> buttonUrls) {

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (int i = 0; i < buttonTexts.size(); i++) {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            InlineKeyboardButton buttonInline = new InlineKeyboardButton();
            buttonInline.setText(buttonTexts.get(i));
            buttonInline.setUrl(buttonUrls.get(i));
            rowInline.add(buttonInline);
            rowsInline.add(rowInline);
        }

        inlineKeyboard.setKeyboard(rowsInline);
        return inlineKeyboard;
    }

    private InlineKeyboardMarkup sendMessageWithButtons(List<String> buttonTexts) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (String buttonText : buttonTexts) {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            InlineKeyboardButton buttonInline = new InlineKeyboardButton();
            buttonInline.setText(buttonText);
            buttonInline.setCallbackData(buttonText);
            rowInline.add(buttonInline);
            rowsInline.add(rowInline);
        }

        inlineKeyboard.setKeyboard(rowsInline);
        return inlineKeyboard;
    }

    private void sendPhotoMessage(String photoName, Long chatId, String text, List<String> buttonTexts, List<String> buttonUrls, boolean needContact) {
        try {

            String photoPath = IMAGE_PATH + photoName;

            URI uri = new URIBuilder()
                    .setScheme("https")
                    .setHost("api.telegram.org")
                    .setPath("/bot" + botToken + "/sendPhoto")
                    .setParameter("chat_id", String.valueOf(chatId))
                    .build();

            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(uri);

            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
            entityBuilder.addBinaryBody("photo", new File(photoPath), ContentType.DEFAULT_BINARY, "Register.jpg");

            entityBuilder.addTextBody("caption", text, ContentType.create("text/plain", Consts.UTF_8));
            entityBuilder.addTextBody("parse_mode", "HTML");

            if (buttonTexts != null && buttonUrls != null && buttonTexts.size() == buttonUrls.size()) {
                JSONObject replyMarkup = new JSONObject();
                JSONArray keyboard = new JSONArray();
                for (int i = 0; i < buttonTexts.size(); i++) {
                    JSONObject button = new JSONObject();
                    button.put("text", buttonTexts.get(i));
                    button.put("url", buttonUrls.get(i));
                    JSONArray row = new JSONArray();
                    row.put(button);
                    keyboard.put(row);
                }
                replyMarkup.put("inline_keyboard", keyboard);
                entityBuilder.addTextBody("reply_markup", replyMarkup.toString(), ContentType.APPLICATION_JSON);
            } else if (buttonTexts != null) {
                JSONObject replyMarkup = new JSONObject();
                JSONArray keyboard = new JSONArray();
                for (int i = 0; i < buttonTexts.size(); i++) {
                    JSONObject button = new JSONObject();
                    button.put("text", buttonTexts.get(i));
                    button.put("callback_data", buttonTexts.get(i));
                    JSONArray row = new JSONArray();
                    row.put(button);
                    keyboard.put(row);
                }
                replyMarkup.put("inline_keyboard", keyboard);
                entityBuilder.addTextBody("reply_markup", replyMarkup.toString(), ContentType.APPLICATION_JSON);
            } else if (needContact) {
                JSONObject replyMarkup = new JSONObject();
                JSONArray keyboard = new JSONArray();
                JSONObject contactButton = new JSONObject();
                contactButton.put("text", "Поделиться контактом");
                contactButton.put("request_contact", true);
                JSONArray contactRow = new JSONArray();
                contactRow.put(contactButton);
                keyboard.put(contactRow);

                replyMarkup.put("inline_keyboard", keyboard);
                entityBuilder.addTextBody("reply_markup", replyMarkup.toString(), ContentType.APPLICATION_JSON);
            }

            HttpEntity entity = entityBuilder.build();

            httpPost.setEntity(entity);

            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();

            //if (responseEntity != null) {
            //    System.out.println("Response: " + EntityUtils.toString(responseEntity));
            //}

            httpClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Getter
    @Setter
    public static class MessageParams {
        private final String text;
        private final Long chatId;
        private final List<String> buttonTexts;
        private final List<String> buttonUrls;
        private final String photoName;
        private final boolean needContact;
        private final boolean needHTML;
        private final boolean needButton;
        private final boolean deleteButton;


        //стандартное сообщение
        public MessageParams(String text, Long chatId, boolean needHTML, boolean deleteButton) {
            this.text = text;
            this.chatId = chatId;
            this.deleteButton = deleteButton;
            this.needButton = false;
            this.needContact = false;
            this.needHTML = false;
            this.buttonTexts = null;
            this.buttonUrls = null;
            this.photoName = null;
        }

        //кнопка и ссылка
        public MessageParams(String text, Long chatId, List<String> buttonTexts, List<String> buttonUrls, boolean needContact, boolean needHTML, boolean deleteButton) {
            this.text = text;
            this.chatId = chatId;
            this.buttonTexts = buttonTexts;
            this.buttonUrls = buttonUrls;
            this.needContact = needContact;
            this.needHTML = needHTML;
            this.deleteButton = deleteButton;
            this.needButton = true;
            this.photoName = null;
        }

        //кнопка
        public MessageParams(String text, Long chatId, List<String> buttonTexts, boolean needHTML, boolean deleteButton) {
            this.text = text;
            this.chatId = chatId;
            this.buttonTexts = buttonTexts;
            this.deleteButton = deleteButton;
            this.needButton = true;
            this.buttonUrls = null;
            this.needContact = false;
            this.needHTML = needHTML;
            this.photoName = null;
        }


        //контакт
        public MessageParams(String text, Long chatId, boolean needContact, boolean needHTML, boolean deleteButton) {
            this.text = text;
            this.chatId = chatId;
            this.needContact = needContact;
            this.needHTML = needHTML;
            this.deleteButton = deleteButton;
            this.needButton = false;
            this.buttonTexts = null;
            this.buttonUrls = null;
            this.photoName = null;
        }


        //html
        public MessageParams(String text, Long chatId, boolean needContact, boolean needHTML, boolean needButton, boolean deleteButton) {
            this.text = text;
            this.chatId = chatId;
            this.needButton = needButton;
            this.deleteButton = deleteButton;
            this.needContact = false;
            this.needHTML = needHTML;
            this.buttonTexts = null;
            this.buttonUrls = null;
            this.photoName = null;
        }

        //фото
        public MessageParams(String text, Long chatId, String photoName, boolean needContact, boolean needHTML, boolean needButton, boolean deleteButton) {
            this.text = text;
            this.chatId = chatId;
            this.needContact = needContact;
            this.needHTML = needHTML;
            this.needButton = needButton;
            this.deleteButton = deleteButton;
            this.buttonTexts = null;
            this.buttonUrls = null;
            this.photoName = photoName;
        }

        public MessageParams(String text, Long chatId, String photoName, List<String> buttonTexts, boolean needContact, boolean needHTML, boolean needButton, boolean deleteButton) {
            this.text = text;
            this.chatId = chatId;
            this.needContact = needContact;
            this.needHTML = needHTML;
            this.needButton = needButton;
            this.deleteButton = deleteButton;
            this.buttonTexts = buttonTexts;
            this.buttonUrls = null;
            this.photoName = photoName;
        }
        public MessageParams(String text, Long chatId, String photoName, List<String> buttonTexts, List<String> buttonUrls, boolean needContact, boolean needHTML, boolean needButton, boolean deleteButton) {
            this.text = text;
            this.chatId = chatId;
            this.needContact = needContact;
            this.needHTML = needHTML;
            this.needButton = needButton;
            this.deleteButton = deleteButton;
            this.buttonTexts = buttonTexts;
            this.buttonUrls = buttonUrls;
            this.photoName = photoName;
        }

        public boolean isButtons() {
            return (needButton || needContact);
        }

        public boolean isPhoto() {
            return photoName != null;
        }
    }
}