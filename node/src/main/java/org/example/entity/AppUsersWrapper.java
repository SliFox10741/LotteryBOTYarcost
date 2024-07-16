package org.example.entity;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;

@XmlRootElement(name = "appUsers")
public class AppUsersWrapper {
    private List<AppUser> appUsers;

    public AppUsersWrapper() {} // JAXB требует наличия конструктора без аргументов

    public AppUsersWrapper(List<AppUser> appUsers) {
        this.appUsers = appUsers;
    }

    @XmlElement(name = "appUser")
    public List<AppUser> getAppUsers() {
        return appUsers;
    }

    public void setAppUsers(List<AppUser> appUsers) {
        this.appUsers = appUsers;
    }
}