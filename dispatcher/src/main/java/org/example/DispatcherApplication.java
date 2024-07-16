package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

//@PropertySource(value = {"file:///D:/Prog/Progect/LotteryBot2/dispatcher/src/main/resources/application.properties"})
@SpringBootApplication
public class DispatcherApplication {
    public static void main(String[] args) {
	SpringApplication.run(DispatcherApplication.class);
    }
}
