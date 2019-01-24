package uk.gov.hmcts.reform.finrem.ccddatamigration.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.finrem.ccddatamigration.idam.IdamUserClient;
import uk.gov.hmcts.reform.finrem.ccddatamigration.idam.IdamUserService;
import uk.gov.hmcts.reform.finrem.ccddatamigration.service.MigrationService;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Component
@Configuration
public class DataMigrationProcessor implements CommandLineRunner {

    @Value("${idam.username}")
    private String idamUserName;

    @Value("${idam.userpassword}")
    private String idamUserPassword;

    @Value("${ccd.jurisdictionid}")
    private String jurisdictionId;

    @Value("${ccd.casetype}")
    private String caseType;

    @Value("${ccd.caseId}")
    private String ccdCaseId;

    @Autowired
    private IdamUserClient idamClient;


    @Autowired
    private IdamUserService idamUserService;

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Autowired
    private MigrationService migrationService;


    public static void main(String[] args) {
        SpringApplication.run(DataMigrationProcessor.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("Start processing cases");
        String userToken = idamClient.generateUserTokenWithNoRoles(idamUserName, idamUserPassword);
        log.info("  userToken  ", userToken);
        String s2sToken = authTokenGenerator.generate();
        log.info("  s2sToken  ", s2sToken);
        String userId = idamUserService.retrieveUserDetails(userToken).getId();
        log.info("  userId   ", userId);
        if (isNotBlank(ccdCaseId)) {
            log.info("Given caseId  {}", ccdCaseId);
            migrationService.processSingleCase(userToken, s2sToken, ccdCaseId);
        } else {
            migrationService.processAllTheCases(userToken, s2sToken, userId);
        }
        log.info("-----------------------------");
        log.info("Data migration completed");
        log.info("-----------------------------");
        log.info("Total number of cases: " + migrationService.getTotalNumberOfCases());
        log.info("Total migrations performed: " + migrationService.getTotalMigrationsPerformed());
        log.info("-----------------------------");
        log.info("Failed Cases {}", migrationService.getFailedCases());

    }
}
