package uk.gov.hmcts.reform.finrem.ccddatamigration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = {"uk.gov.hmcts.reform.finrem"})
@SpringBootApplication()
public class CcdDataMigrationApplication {

    public static void main(String[] args) {

        String proxyDisabled = System.getProperty("proxy_disabled");
        if (proxyDisabled == null || !proxyDisabled.equals("true")) {
            System.setProperty("http.proxyHost", "proxyout.reform.hmcts.net");
            System.setProperty("http.proxyPort", "8080");
            System.setProperty("https.proxyHost", "proxyout.reform.hmcts.net");
            System.setProperty("https.proxyPort", "8080");
        }

        SpringApplication.run(CcdDataMigrationApplication.class, args).close();
    }
}
