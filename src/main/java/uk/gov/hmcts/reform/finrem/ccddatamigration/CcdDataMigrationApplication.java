package uk.gov.hmcts.reform.finrem.ccddatamigration;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import uk.gov.hmcts.reform.authorisation.healthcheck.ServiceAuthHealthIndicator;

@EnableFeignClients(basePackages = {"uk.gov.hmcts.reform.finrem"})
@SpringBootApplication(exclude = {ServiceAuthHealthIndicator.class})
public class CcdDataMigrationApplication {

    public static void main(String[] args) {

        System.setProperty("http.proxyHost", "proxyout.reform.hmcts.net");
        System.setProperty("http.proxyPort", "8080");
        System.setProperty("https.proxyHost", "proxyout.reform.hmcts.net");
        System.setProperty("https.proxyPort", "8080");

        SpringApplication.run(CcdDataMigrationApplication.class, args).close();
    }
}
