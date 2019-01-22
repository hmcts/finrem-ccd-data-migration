package uk.gov.hmcts.reform.finrem.ccddatamigration.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.PaginatedSearchMetadata;
import uk.gov.hmcts.reform.finrem.ccddatamigration.ccd.CcdUpdateService;
import uk.gov.hmcts.reform.finrem.ccddatamigration.idam.IdamUserClient;
import uk.gov.hmcts.reform.finrem.ccddatamigration.idam.IdamUserService;
import uk.gov.hmcts.reform.finrem.ccddatamigration.service.MigrationService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DataMigrationProcessor implements CommandLineRunner {
    @Value("${ccd.update}")
    private boolean ccdUpdate;

    @Value("${idam.username}")
    private String idamUserName;

    @Value("${idam.userpassword}")
    private String idamUserPassword;

    @Value("${ccd.jurisdictionid}")
    private String jurisdictionId;

    @Value("${ccd.casetype}")
    private String caseType;

    @Autowired
    private IdamUserClient idamClient;

    @Autowired
    private IdamUserService idamUserService;

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Autowired
    private CoreCaseDataApi ccdApi;

    @Autowired
    private CcdUpdateService ccdUpdateService;

    @Autowired
    private MigrationService migrationService;

    @Value("${ccd.casesList}")
    private String[] ccdCaseIds;

    public static void main(String[] args) {
        SpringApplication.run(DataMigrationProcessor.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("Start processing cases");
        log.info("CCD Cases >>" + ccdCaseIds);

        String userToken = idamClient.generateUserTokenWithNoRoles(idamUserName, idamUserPassword);
        log.info("  userToken  ", userToken);
        String s2sToken = authTokenGenerator.generate();
        log.info("  s2sToken  ", s2sToken);
        String userId = idamUserService.retrieveUserDetails(userToken).getId();
        log.info("  userId   ", userId);

        if (ccdCaseIds.length > 0) {
            CaseDetails aCase = ccdApi.getCase(userToken, s2sToken, ccdCaseIds[0]);
            migrationService.processData(Arrays.asList(aCase))
                    .forEach(cd -> updateOneCase(userToken, cd));
        } else {
            Map<String, String> searchCriteria = new HashMap<>();
            int numberOfPages = requestNumberOfPage(userToken, s2sToken, userId, searchCriteria);
            //Process all the pages
            for (int i = numberOfPages; i > 0; i--) {
                log.debug("Process page:" + i);
                processOnePage(userToken, s2sToken, userId, i, searchCriteria);
            }
        }
        log.debug("Data migration completed");
        log.debug("Total number of cases: " + migrationService.getTotalNumberOfCases());
        log.debug("Total migrations performed: " + migrationService.getTotalMigrationsPerformed());

    }

    private void processOnePage(String authorisation,
                                String serviceAuthorisation,
                                String userId,
                                int pageNumber,
                                Map<String, String> searchCriteria) {
        searchCriteria.put("page", String.valueOf(pageNumber));
        List<CaseDetails> caseDetails = ccdApi.searchForCaseworker(
                authorisation,
                serviceAuthorisation,
                userId,
                jurisdictionId,
                caseType,
                searchCriteria
        );

        //Process all the cases
        migrationService.processData(
                caseDetails.stream()
                        .filter(migrationService::accepts)
                        .collect(Collectors.toList()))
                .forEach(cd -> updateOneCase(authorisation, cd));
    }

    private int requestNumberOfPage(String authorisation,
                                    String serviceAuthorisation,
                                    String userId,
                                    Map<String, String> searchCriteria) {
        PaginatedSearchMetadata paginationInfoForSearchForCaseworkers = ccdApi.getPaginationInfoForSearchForCaseworkers(
                authorisation,
                serviceAuthorisation,
                userId,
                jurisdictionId,
                caseType,
                searchCriteria);
        log.debug("Pagination>>" + paginationInfoForSearchForCaseworkers.toString());
        return paginationInfoForSearchForCaseworkers.getTotalPagesCount();
    }

    private void updateOneCase(String authorisation, CaseDetails cd) {
        String caseId = cd.getId().toString();
        log.info("updating case with id :" + caseId);
        if (ccdUpdate) {
            try {
                migrationService.updateCase(authorisation, cd);
                log.info(caseId + " updated!");
            } catch (Exception e) {
                log.warn("update failed for case with id [{}] with error [{}] ", cd.getId().toString(), e.getMessage());
            }
        } else {
            log.info("Updating of ccd skipped for " + caseId);
        }

    }
}
