package ru.usb.restclientconf.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServiceTemplate {

    @Autowired
    private RestTemplate restTemplateW;

    String fooResourceUrl = "https://127.0.0.1:8443";

    public void getPetrSSL() {
        ResponseEntity<String> response
                = restTemplateW.getForEntity(fooResourceUrl + "/hello", String.class);

        System.out.println("Request SSL Config>>>> ");
        System.out.println("<<<<<Response SSL Config");
        System.out.println("getPetrSSL (SSL Config):response :: " + response);
        System.out.println("body :: " + response.getBody());
        System.out.println("----");
    }
}
