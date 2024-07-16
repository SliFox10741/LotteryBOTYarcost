package org.example.service;

import jakarta.persistence.EntityManager;
import org.example.entity.AppUser;

import java.io.IOException;
import java.util.List;

public interface ExcelHelper {
    public void saveUsersToPojo(List<AppUser> allUsers, String filePath) throws IOException;
}
