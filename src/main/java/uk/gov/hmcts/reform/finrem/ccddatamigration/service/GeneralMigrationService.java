package uk.gov.hmcts.reform.finrem.ccddatamigration.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.PaginatedSearchMetadata;
import uk.gov.hmcts.reform.finrem.ccddatamigration.ccd.CcdUpdateService;

import java.util.Collections;
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

    @Getter
    private int totalMigrationsPerformed;

    @Getter
    private int totalNumberOfCases;

    @Autowired
    private CcdUpdateService ccdUpdateService;

    @Autowired
    private CoreCaseDataApi ccdApi;

    @Value("${ccd.jurisdictionid}")
    private String jurisdictionId;

    @Value("${ccd.casetype}")
    private String caseType;

    @Value("${ccd.dryrun}")
    private boolean dryRun;

    @Getter
    private String failedCases;

    @Override
    public List<CaseDetails> processData(List<CaseDetails> caseDetails) {
        for (CaseDetails caseDetail : caseDetails) {
            log.debug("Case Before Migration " + caseDetail.toString()
                    .replace(System.getProperty("line.separator"), " "));
        }
        return caseDetails;
    }

    @Override
    public boolean accepts(CaseDetails caseDetails) {
        if (caseDetails == null || caseDetails.getData() == null) {
            return false;
        }
        Map<String, Object> data = caseDetails.getData();
        return !isEmpty(data.get("solicitorAddress1"));
    }

    @Override
    public void updateCase(String authorisation, CaseDetails cd) {
        String caseId = cd.getId().toString();
        Object data = cd.getData();
        log.info("data {}", data.toString());

        CaseDetails update = ccdUpdateService.update(
                caseId,
                data,
                EVENT_ID,
                authorisation,
                EVENT_SUMMARY,
                EVENT_DESCRIPTION
        );
        totalMigrationsPerformed++;
    }

    public void processSingleCase(String userToken, String s2sToken, String ccdCaseId) {
        CaseDetails aCase = ccdApi.getCase(userToken, s2sToken, ccdCaseId);
        processData(Collections.singletonList(aCase))
                .forEach(cd -> updateOneCase(userToken, cd));
    }

    public void updateOneCase(String authorisation, CaseDetails cd) {
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

    private void updateFailedCases(Long id) {
        failedCases = nonNull(failedCases) ? (failedCases + "," + id) : id.toString();
    }

    @Override
    public void processAllTheCases(String userToken, String s2sToken, String userId) {
        Map<String, String> searchCriteria = new HashMap<>();
        int numberOfPages = requestNumberOfPage(userToken, s2sToken, userId, searchCriteria);
        //Process all the pages
        List<CaseDetails> list;
        boolean found = false;
        for (int i = numberOfPages; i > 0 && !found; i--) {
            log.debug("Process page:" + i);
            searchCriteria.put("page", String.valueOf(i));
            //1. get the cases in a page
            List<CaseDetails> caseDetails = ccdApi.searchForCaseworker(
                    userToken,
                    s2sToken,
                    userId,
                    jurisdictionId,
                    caseType,
                    searchCriteria);
            // 2. find the candidate cases
            list = caseDetails.stream()
                    .filter(this::accepts)
                    .collect(Collectors.toList());
            // 3.if dryRun -> then apply migration for th first case, else migrate all the cases.
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


}
