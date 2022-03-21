package ru.usb.restclientconf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.usb.restclientconf.service.ServiceTemplate;

@SpringBootApplication
public class RestclientconfApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(RestclientconfApplication.class, args);
    }

    @Autowired
    ServiceTemplate serviceTemplate;

    @Override
    public void run(String... args) throws Exception {
        serviceTemplate.getPetrSSL();
    }
}
