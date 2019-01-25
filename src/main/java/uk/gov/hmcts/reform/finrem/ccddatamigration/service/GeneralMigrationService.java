package uk.gov.hmcts.reform.finrem.ccddatamigration.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.PaginatedSearchMetadata;
import uk.gov.hmcts.reform.finrem.ccddatamigration.ccd.CcdUpdateService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static org.springframework.util.StringUtils.isEmpty;


@Slf4j
@Component("generalMigrationService")
public class GeneralMigrationService implements MigrationService {
    private static final String EVENT_ID = "FR_migrateCase";
    private static final String EVENT_SUMMARY = "Migrate Case";
    private static final String EVENT_DESCRIPTION = "Migrate Case";
    private static final String SOLICITOR_ADDRESS_1 = "solicitorAddress1";

    @Getter
    private int totalMigrationsPerformed;

    @Getter
    private int totalNumberOfCases;

    @Autowired
    private CcdUpdateService ccdUpdateService;

    @Autowired
    private CoreCaseDataApi ccdApi;

    @Getter
    private String failedCases;

    @Override
    public boolean accepts(CaseDetails caseDetails) {
        if (caseDetails == null || caseDetails.getData() == null) {
            return false;
        }
        Map<String, Object> data = caseDetails.getData();
        return !isEmpty(data.get(SOLICITOR_ADDRESS_1));
    }

    @Override
    public void processSingleCase(String userToken, String s2sToken, String ccdCaseId) {
        CaseDetails aCase = ccdApi.getCase(userToken, s2sToken, ccdCaseId);
        updateOneCase(userToken, aCase);
    }

    @Override
    public void processAllTheCases(String userToken, String s2sToken, String userId,
                                   String jurisdictionId, String caseType, boolean dryRun) {
        Map<String, String> searchCriteria = new HashMap<>();
        int numberOfPages = requestNumberOfPage(userToken, s2sToken, userId, jurisdictionId, caseType, searchCriteria);
        List<CaseDetails> list;
        boolean found = false;
        for (int i = numberOfPages; i > 0 && !found; i--) {
            log.info("Process page:" + i);
            searchCriteria.put("page", String.valueOf(i));
            List<CaseDetails> caseDetails = getCases(
                    userToken,
                    s2sToken,
                    userId,
                    jurisdictionId,
                    caseType,
                    searchCriteria);

            printData(caseDetails);

            list = caseDetails.stream()
                    .filter(this::accepts)
                    .collect(Collectors.toList());
            if (!list.isEmpty()) {
                if (dryRun) {
                    found = true;
                    log.info("dryRun with case Id  {}", list.get(0).getId());
                    updateOneCase(userToken, list.get(0));
                } else {
                    log.info("migrating all the cases ..");
                    list.forEach(cd -> updateOneCase(userToken, cd));
                }
            }
        }
        if (!found) {
            log.info("No matching cases for data migration");
        }
    }

    private List<CaseDetails> getCases(String userToken,
                                       String s2sToken,
                                       String userId,
                                       String jurisdictionId,
                                       String caseType,
                                       Map<String, String> searchCriteria) {
        return ccdApi.searchForCaseworker(
                userToken,
                s2sToken,
                userId,
                jurisdictionId,
                caseType,
                searchCriteria);
    }

    private int requestNumberOfPage(String authorisation,
                                    String serviceAuthorisation,
                                    String userId,
                                    String jurisdictionId,
                                    String caseType,
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

    private void updateCase(String authorisation, CaseDetails cd) {
        String caseId = cd.getId().toString();
        Object data = cd.getData();
        log.info("data {}", data.toString());

        CaseDetails update = ccdUpdateService.update(caseId,
                data,
                EVENT_ID,
                authorisation,
                EVENT_SUMMARY,
                EVENT_DESCRIPTION);
        totalMigrationsPerformed++;
    }

    private void updateFailedCases(Long id) {
        failedCases = nonNull(failedCases) ? (failedCases + "," + id) : id.toString();
    }

    private void updateOneCase(String authorisation, CaseDetails cd) {
        totalNumberOfCases++;
        String caseId = cd.getId().toString();
        log.info("updating case with id :" + caseId);
        try {
            updateCase(authorisation, cd);
            log.info(caseId + " updated!");
        } catch (Exception e) {
            log.error("update failed for case with id [{}] with error [{}] ", cd.getId().toString(), e.getMessage());
            updateFailedCases(cd.getId());
        }
    }

    private List<CaseDetails> printData(List<CaseDetails> caseDetails) {
        for (CaseDetails caseDetail : caseDetails) {
            log.debug("Case Before Migration " + caseDetail.toString()
                    .replace(System.getProperty("line.separator"), " "));
        }
        return caseDetails;
    }
}
