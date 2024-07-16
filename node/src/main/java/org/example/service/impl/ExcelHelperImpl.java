package org.example.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.extern.log4j.Log4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.entity.AppPhoto;
import org.example.entity.AppUser;
import org.example.entity.BinaryContent;
import org.example.service.ExcelHelper;
import org.hibernate.engine.spi.SessionImplementor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j
@Service
public class ExcelHelperImpl implements ExcelHelper {
    private static final int BATCH_SIZE = 500;

    @Override
    public void saveUsersToPojo(List<AppUser> allUsers, String filePath) throws IOException {
        Set<AppUser> uniqueUsers = new HashSet<>();
        int offset = 0;
        int fileCount = 1;

        while (offset < allUsers.size()) {
            List<AppUser> users = allUsers.subList(offset, Math.min(offset + BATCH_SIZE, allUsers.size()));

            uniqueUsers.addAll(users);
            offset += BATCH_SIZE;

            if (uniqueUsers.size() >= BATCH_SIZE || offset >= allUsers.size()) {
                saveUsersToFile(uniqueUsers, filePath + fileCount + ".xlsx");
                fileCount++;
                uniqueUsers.clear();
            }
        }
    }

    private static void saveUsersToFile(Set<AppUser> users, String filePath) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(filePath)) {
            XSSFSheet sheet = workbook.createSheet("Users");
            int rowNum = 0;
            Row row = sheet.createRow(rowNum++);

            int colNum = 0;

            writeCell(row, colNum++, "ID");
            writeCell(row, colNum++, "TelegramID");
            writeCell(row, colNum++, "Username");
            writeCell(row, colNum++, "Имя");
            writeCell(row, colNum++, "Номер телефона");
            writeCell(row, colNum++, "Номера купонов");

            for (AppUser user : users) {
                row = sheet.createRow(rowNum++);
                colNum = 0;
                writeCell(row, colNum++, user.getId());
                writeCell(row, colNum++, user.getTelegramUserId());
                writeCell(row, colNum++, user.getUsername());
                writeCell(row, colNum++, user.getName());
                writeCell(row, colNum++, user.getPhoneNumber());
                writeCell(row, colNum++, user.getTicketNumber());
            }

            workbook.write(fos);
        }
    }

    private static void writeCell(Row row, int cellNum, Object value) {
        Cell cell = row.createCell(cellNum);
        if (value != null) {
            cell.setCellValue(value.toString());
        }
    }
}