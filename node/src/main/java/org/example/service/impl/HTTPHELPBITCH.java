package org.example.service.impl;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Service
public class HTTPHELPBITCH {

//    @Value("${token}")
//    private static String botToken = "6096399998:AAHJB_lZZ920KrtvJnabVA-A2pn8F3-XHzc";
private static String botToken = "6777152058:AAEhAyYWeGeY1ZBllxh6bCNExpgCAadTVwc";
    public static void sendDocXML(String filePath, Long chatId) throws IOException, InterruptedException {

        String requestUrl = "https://api.telegram.org/bot" + botToken + "/sendDocument";

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost uploadFile = new HttpPost(requestUrl);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();

            // Добавляем параметры
            builder.addTextBody("chat_id", chatId.toString(), ContentType.TEXT_PLAIN);

            // Прикрепляем файл
            File file = new File(filePath);
            builder.addBinaryBody("document", file, ContentType.APPLICATION_OCTET_STREAM, file.getName());

            HttpEntity multipart = builder.build();
            uploadFile.setEntity(multipart);

            CloseableHttpResponse response = httpClient.execute(uploadFile);
            HttpEntity responseEntity = response.getEntity();

            response.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
