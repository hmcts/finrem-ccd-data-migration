package uk.gov.hmcts.reform.finrem.ccddatamigration.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
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
@PropertySource("classpath:application.properties")
public class DataMigrationProcessor implements CommandLineRunner {

    @Value("${idam.username}")
    private String idamUserName;

    @Value("${idam.userpassword}")
    private String idamUserPassword;

    @Value("${ccd.jurisdictionid}")
    private String jurisdictionId;

    @Value("#{'${ccd.casetypes}'.split(',')}")
    private List<String> caseTypes;

    @Value("${ccd.caseId}")
    private String ccdCaseId;

    @Value("${idam.s2s-auth.replaceBearer:false}")
    private boolean replaceBearer;

    @Value("${log.debug}")
    private boolean debugEnabled;

    @Value("${ccd.file}")
    private String file;

    @Value("${migration.specificEvent}")
    private String specificMigrationEvent;

    @Autowired
    private IdamUserClient idamClient;

    @Autowired
    private IdamUserService idamUserService;

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Autowired
    private MigrationService migrationService;

    public static void main(final String[] args) {
        SpringApplication.run(DataMigrationProcessor.class, args);
    }

    @Override
    public void run(final String... args) {
        try {
            log.info("Start processing cases. specificMigrationEvent:{}", specificMigrationEvent);
            final String userToken = idamClient.generateUserTokenWithNoRoles(idamUserName, idamUserPassword);
            final String s2sToken = replaceBearer ? authTokenGenerator.generate().replace("Bearer ", "") : authTokenGenerator.generate();
            final String userId = idamUserService.retrieveUserDetails(userToken).getId();
            if (debugEnabled) {
                log.info("  userToken  : {}", userToken);
                log.info("  s2sToken : {}", s2sToken);
                log.info("  userId  : {}", userId);
            }
            log.info("Case Id {}", ccdCaseId);

            if (isNotBlank(ccdCaseId)) {
                log.info("Migrate single case with Case ID: {}", ccdCaseId);
                migrationService.processSingleCase(userToken, s2sToken, ccdCaseId);
            } else if (isNotBlank(file)) {
                log.info("Migrate cases in file");
                migrationService.processCasesInFile(userToken, s2sToken, file);
            } else {
                log.info("Migrate multiple cases .....");
                caseTypes.forEach(caseType -> {
                    log.info("migrate caseType ....." + caseType);
                    migrationService.processAllTheCases(userToken, s2sToken, userId, jurisdictionId, caseType);
                });
            }
            log.info("Migrated Cases {} ", isNotBlank(migrationService.getMigratedCases()) ? migrationService.getMigratedCases() : "NONE");
            log.info("-----------------------------");
            log.info("Data migration completed");
            log.info("-----------------------------");
            log.info("Total number of cases: " + migrationService.getTotalNumberOfCases());
            log.info("Total migrations performed: " + migrationService.getTotalMigrationsPerformed());
            log.info("Total cases skipped: " + migrationService.getTotalNumberOfSkips());
            log.info("Total cases failed: " + migrationService.getTotalNumberOfFails());
            log.info("-----------------------------");
            log.info("Failed Cases: {}", isNotBlank(migrationService.getFailedCases()) ? migrationService.getFailedCases() : "NONE");
        } catch (final Throwable e) {
            log.error("Migration failed with the following reason :" + e.getMessage());
            e.printStackTrace();
        }
    }
}
