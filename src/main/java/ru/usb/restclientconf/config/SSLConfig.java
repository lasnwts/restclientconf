package ru.usb.restclientconf.config;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.Properties;



/**
 * @author Petr Vershinin
 * create on 07.10.2021
 * Команда для генерации файла с доверенными URL: keytool -importcert -file nameCertificate.cer -keystore mytruststore -storetype PKCS12
 * После генерации добавить полученный файл в: resources/ssl
 * Ручная регистрация SSL сертификатов в системе  в системе.
 * Для работы используется определения getProtocol(), для запуска в IDE нужен тип - file, тогда ClassLoader использует локальный файл,
 * полсе сборки в jar, необходимо использовать InputStream.
 * В случае если ssl установлен в машине убрать аннотацию @Configuration
 */
@Configuration
public class SSLConfig<trustStorePassword> {

    private boolean cert;
    String trustStoreFile;
    private String trustStorePassword; //пароль
    private static final String TRUST_STORE_PASSWORD = "changeit";
    private static final Logger logger = LoggerFactory.getLogger(SSLConfig.class);

    File file = null;

    public SSLConfig(){
        //считываем app.worker.cert если true то используется dev сертификат к sugar иначе prod
        //внимание аннотация @Value не работает в данном контексте потому, что на момент создания конструктора, значение еще не доступно
        //поэтому используем "старый-добрый" способ получения из ресурсов
        var resource = new ClassPathResource("/application.properties");
        try {
            Properties properties = PropertiesLoaderUtils.loadProperties(resource);
            String prop = properties.getProperty("app.worker.cert");
            cert= Boolean.parseBoolean(prop);
            logger.info("из файла application.properties считано значение app.worker.cert {}",prop);

           trustStorePassword = properties.getProperty("server.ssl.trust-store-password");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
        }

        trustStoreFile = "jksstore";

        if (Objects.requireNonNull(getClass().getClassLoader().getResource(trustStoreFile)).getProtocol().equals("jar")) {
            try {
                InputStream input = getClass().getClassLoader().getResourceAsStream(trustStoreFile);
                file = File.createTempFile("tempfile", ".tmp");
                try (OutputStream out = new FileOutputStream(file)) {
                    int read;
                    byte[] bytes = new byte[1024];

                    while ((read = Objects.requireNonNull(input).read(bytes)) != -1) {
                        out.write(bytes, 0, read);
                    }
                    extracted();
                }
                file.deleteOnExit();
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        } else {
            //this will probably work in your IDE, but not from a JAR
            URL res1 = getClass().getClassLoader().getResource(trustStoreFile);
            try {
                file = new File(Objects.requireNonNull(res1).toURI());
                extracted();
            } catch (URISyntaxException e) {
                logger.error(e.getMessage(), e);
            }
        }

//        //this will probably work in your IDE, but not from a JAR
//        URL res1 = getClass().getClassLoader().getResource(trustStoreFile);
//        try {
//            file = new File(Objects.requireNonNull(res1).toURI());
//            extracted();
//        } catch (URISyntaxException e) {
//            logger.error(e.getMessage(), e);
//        }


        if (file != null && !file.exists()) {
            throw new RuntimeException("Error: File " + file + " not found!");
        }



    }

    private void extracted() {
        System.setProperty("javax.net.ssl.trustStore", file.getAbsolutePath());
        System.setProperty("javax.net.ssl.trustStorePassword", TRUST_STORE_PASSWORD);
        logger.info("выполнена регистрация SSL сертификата для SugarCRM в системе");
    }

    @Bean
    public RestTemplate restTemplateW(RestTemplateBuilder builder) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {
        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
        SSLContext sslContext = new SSLContextBuilder()
                .loadTrustMaterial(file, trustStorePassword.toCharArray(),acceptingTrustStrategy)
                .build();
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

        HttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(socketFactory)
                .build();

        return builder
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(httpClient))
                .build();
    }

}
