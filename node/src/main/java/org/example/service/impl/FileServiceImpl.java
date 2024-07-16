package org.example.service.impl;

import lombok.extern.log4j.Log4j;
import org.example.dao.AppDocumentDAO;
import org.example.dao.AppPhotoDAO;
import org.example.dao.AppUserDAO;
import org.example.dao.BinaryContentDAO;
import org.example.entity.*;
import org.example.exceptions.UploadFileException;
import org.example.service.ExcelHelper;
import org.example.service.FileService;
import org.example.service.ProducerService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.springframework.web.client.RestTemplate;

import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;


import static org.example.entity.enums.UserState.GOT_ANY_TICKET_STATE;
import static org.example.entity.enums.UserState.GOT_FIRST_TICKET_STATE;

@Log4j
@Service
public class FileServiceImpl implements FileService {
    @Value("${token}")
    private String token;
    @Value("${service.file_info.uri}")
    private String fileInfoUri;
    @Value("${service.file_storage.uri}")
    private String fileStorageUri;
    private final AppDocumentDAO appDocumentDAO;
    private final AppPhotoDAO appPhotoDAO;
    private final AppUserDAO appUserDAO;
    private final BinaryContentDAO binaryContentDAO;
    private final ProducerService producerService;
    private final ExcelHelper excelHelper;
    String filename;

    public FileServiceImpl(AppDocumentDAO appDocumentDAO, AppPhotoDAO appPhotoDAO, AppUserDAO appUserDAO, BinaryContentDAO binaryContentDAO, ProducerService producerService, ExcelHelper excelHelper) {
        this.appDocumentDAO = appDocumentDAO;
        this.appPhotoDAO = appPhotoDAO;
        this.appUserDAO = appUserDAO;
        this.binaryContentDAO = binaryContentDAO;
        this.producerService = producerService;
        this.excelHelper = excelHelper;
    }

    @Override
    public void processDoc(Message telegramMessage) {
        Document telegramDoc = telegramMessage.getDocument();
        String fileId = telegramDoc.getFileId();
        ResponseEntity<String> response = getFilePath(fileId);
        if (response.getStatusCode() == HttpStatus.OK) {
            BinaryContent persistentBinaryContent = getPersistentBinaryContent(response);
            AppDocument transientAppDoc = buildTransientAppDoc(telegramDoc, persistentBinaryContent);
            appDocumentDAO.save(transientAppDoc);
        } else {
            throw new UploadFileException("Bad response from telegram service: " + response);
        }
    }

    @Override
    public void processPhoto(PhotoSize telegramPhoto) {
        String fileId = telegramPhoto.getFileId();
        ResponseEntity<String> response = getFilePath(fileId);
        if (response.getStatusCode() == HttpStatus.OK) {
            BinaryContent persistentBinaryContent = getPersistentBinaryContent(response);
            AppPhoto transientAppPhoto = buildTransientAppPhoto(telegramPhoto, persistentBinaryContent);
            appPhotoDAO.save(transientAppPhoto);
        } else {
            throw new UploadFileException("Bad response from telegram service: " + response);
        }
    }

    private BinaryContent getPersistentBinaryContent(ResponseEntity<String> response) {
        String filePath = getFilePath(response);
        byte[] fileInByte = downloadFile(filePath);
        BinaryContent transientBinaryContent = BinaryContent.builder()
                .fileAsArrayOfBytes(fileInByte)
                .build();
        return binaryContentDAO.save(transientBinaryContent);
    }

    private String getFilePath(ResponseEntity<String> response) {
        JSONObject jsonObject = new JSONObject(response.getBody());
        return String.valueOf(jsonObject
                .getJSONObject("result")
                .getString("file_path"));
    }

    private AppDocument buildTransientAppDoc(Document telegramDoc, BinaryContent persistentBinaryContent) {
        return AppDocument.builder()
                .telegramFileId(telegramDoc.getFileId())
                .docName(telegramDoc.getFileName())
                .binaryContent(persistentBinaryContent)
                .mimeType(telegramDoc.getMimeType())
                .fileSize(telegramDoc.getFileSize())
                .build();
    }

    private AppPhoto buildTransientAppPhoto(PhotoSize telegramPhoto, BinaryContent persistentBinaryContent) {
        return AppPhoto.builder()
                .telegramFileId(telegramPhoto.getFileId())
                .binaryContent(persistentBinaryContent)
                .fileSize(telegramPhoto.getFileSize())
                .build();
    }

    private ResponseEntity<String> getFilePath(String fileId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> request = new HttpEntity<>(headers);
        log.info(request);
        return restTemplate.exchange(
                fileInfoUri,
                HttpMethod.GET,
                request,
                String.class,
                token, fileId
        );

    }

    private byte[] downloadFile(String filePath) {
        String fullUri = fileStorageUri.replace("{token}", token)
                .replace("{filePath}", filePath);
        URL urlObj = null;
        try {
            urlObj = new URL(fullUri);
        } catch (MalformedURLException e) {
            throw new UploadFileException(e);
        }

        //TODO подумать над оптимизацией
        try (InputStream is = urlObj.openStream()) {
            return is.readAllBytes();
        } catch (IOException e) {
            throw new UploadFileException(urlObj.toExternalForm(), e);
        }
    }

    public void generateUniqueTicket(AppUser appUser) {
        System.out.println("Начал генерироват");
//        filename = "D:\\Prog\\Progect\\LotteryBot2\\tickets\\tickets.txt";
        filename = "/root/lotteryBot/LotteryBot2/tickets/tickets.txt";
        HashSet<String> existingNumbers = readNumbersFromFile(filename);
        String newNumber = generateUniqueNumber(existingNumbers);
        writeNumberToFile(filename, newNumber);
        ArrayList<String> ticketList = new ArrayList<>(appUser.getTicketNumber());
        ticketList.add(newNumber);
        appUser.setTicketNumber(ticketList);

        if (ticketList.size() > 1) {
            appUser.setState(GOT_ANY_TICKET_STATE);
        } else {
            appUser.setState(GOT_FIRST_TICKET_STATE);
        }
        appUserDAO.save(appUser);
    }

    private static HashSet<String> readNumbersFromFile(String filename) {
        HashSet<String> numbers = new HashSet<>();
        try (InputStream inputStream = new FileInputStream(filename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                numbers.add(line);
            }
        } catch (IOException e) {
            log.debug("Error reading file: " + e.getMessage());
        }
        return numbers;
    }

    private static void writeNumberToFile(String filename, String number) {
        try (OutputStream outputStream = new FileOutputStream(filename, true);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            writer.newLine();
            writer.write(number);
            writer.flush();
        } catch (IOException e) {
            log.debug("Error writing to file: " + e.getMessage());
        }
    }

    private static String generateUniqueNumber(HashSet<String> existingNumbers) {
        Random random = new Random();
        String newNumber;
        do {
            newNumber = String.valueOf(random.nextInt(89999999) + 10000000);
        } while (!existingNumbers.add(newNumber));
        return newNumber;
    }

    @Override
    public boolean saveToEXEL(Long chatId) {
        List<AppUser> allUsers = appUserDAO.findAll();

        if (allUsers.isEmpty()) {
            return false;
        }

//        String filePath = "D:\\Prog\\Progect\\LotteryBot2\\exel\\";
        String filePath ="/root/lotteryBot/LotteryBot2/exel/";
        try {
            excelHelper.saveUsersToPojo(allUsers, filePath);

            // Получаем список всех файлов в каталоге
            Path directory = Paths.get(filePath);
            List<Path> files = Files.list(directory).toList();
            File delFile;
            if (!files.isEmpty()) {
                for (Path file : files) {
//                     Вызываем метод sendDocXML для каждого файла
                    HTTPHELPBITCH.sendDocXML(String.valueOf(file), chatId);
                    delFile = new File(String.valueOf(file));
                    delFile.delete();
                }
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


}
