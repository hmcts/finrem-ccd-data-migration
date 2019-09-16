package uk.gov.hmcts.reform.finrem.ccddatamigration.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.PaginatedSearchMetadata;
import uk.gov.hmcts.reform.finrem.ccddatamigration.ccd.CcdUpdateService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Objects.nonNull;
import static org.springframework.util.StringUtils.isEmpty;


@Slf4j
@Component("generalMigrationService")
public class GeneralMigrationService implements MigrationService {
    private static final String EVENT_ID = "FR_migrateCase";
    private static final String EVENT_SUMMARY = "Migrate Case";
    private static final String EVENT_DESCRIPTION = "Migrate Case";
    private static final String JUDGE_ALLOCATED = "judgeAllocated";

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

    @Getter
    private String migratedCases;

    @Value("${log.debug}")
    private boolean debugEnabled;

    @Value("${ccd.dryrun}")
    private boolean dryRun;

    private static Predicate<CaseDetails> accepts() {
        return caseDetails -> caseDetails != null && caseDetails.getData() != null
                && !isEmpty(caseDetails.getData().get(JUDGE_ALLOCATED));
    }

    private static boolean isCandidateForMigration(CaseDetails aCase) {
        Object judgeAllocated = aCase.getData().get(JUDGE_ALLOCATED);
        if (nonNull(judgeAllocated) && !ObjectUtils.isEmpty(judgeAllocated)) {
            if (judgeAllocated instanceof String) {
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public void processSingleCase(String userToken, String s2sToken, String ccdCaseId) {
        CaseDetails aCase;
        try {
            aCase = ccdApi.getCase(userToken, s2sToken, ccdCaseId);
            log.info("case data {} ", aCase);
            if (isCandidateForMigration(aCase)) {
                String caseTypeId = aCase.getCaseTypeId();
                updateOneCase(userToken, aCase, caseTypeId);
            } else {
                log.info("Case {} doesn't has {} field.", aCase.getId(), JUDGE_ALLOCATED);
            }
        } catch (Exception ex) {
            log.error("case Id {} not found, {}", ccdCaseId, ex.getMessage());
        }
    }


    @Override
    public void processAllTheCases(String userToken, String s2sToken, String userId,
                                   String jurisdictionId, String caseType) {
        Map<String, String> searchCriteria = new HashMap<>();
        int numberOfPages = requestNumberOfPage(userToken, s2sToken, userId, jurisdictionId, caseType, searchCriteria);
        log.info("Number of pages : {}", numberOfPages);

        if (dryRun) {
            log.info("caseType {}  and dryRun for one case ...", caseType);
            dryRunWithOneCase(userToken, s2sToken, userId, jurisdictionId, caseType, numberOfPages);

        } else {
            log.info("migrating all the cases ...");
            IntStream.rangeClosed(1, numberOfPages)
                    .forEach(page -> migrateCasesForPage(userToken, s2sToken, userId,
                            jurisdictionId, caseType, page));
        }
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
        if (debugEnabled) {
            log.debug("Pagination>>" + paginationInfoForSearchForCaseworkers.toString());
        }
        return paginationInfoForSearchForCaseworkers.getTotalPagesCount();
    }

    private void dryRunWithOneCase(String userToken, String s2sToken, String userId,
                                   String jurisdictionId, String caseType, int numberOfPages) {
        boolean found = false;
        for (int i = 1; i <= numberOfPages && !found; i++) {
            List<CaseDetails> casesForPage = getCasesForPage(userToken, s2sToken, userId,
                    jurisdictionId, caseType, i);
            if (casesForPage.size() > 0) {
                found = true;
                log.info("Migrating Case Id {} for the dryRun", casesForPage.get(0).getId());
                updateOneCase(userToken, casesForPage.get(0), caseType);
            }
        }
    }

    private List<CaseDetails> getCasesForPage(String userToken,
                                              String s2sToken,
                                              String userId,
                                              String jurisdictionId,
                                              String caseType,
                                              int pageNumber) {
        Map<String, String> searchCriteria = new HashMap<>();
        searchCriteria.put("page", String.valueOf(pageNumber));
        return ccdApi.searchForCaseworker(userToken, s2sToken, userId, jurisdictionId, caseType, searchCriteria)
                .stream()
                .filter(accepts())
                .collect(Collectors.toList());

    }

    private void migrateCasesForPage(String userToken,
                                     String s2sToken,
                                     String userId,
                                     String jurisdictionId,
                                     String caseType,
                                     int pageNumber) {
        getCasesForPage(userToken, s2sToken, userId, jurisdictionId, caseType, pageNumber)
                .stream()
                .filter(accepts())
                .forEach(cd -> updateOneCase(userToken, cd, caseType));
    }


    private void updateOneCase(String authorisation, CaseDetails cd, String caseType) {
        totalNumberOfCases++;
        String caseId = cd.getId().toString();
        if (debugEnabled) {
            log.info("updating case with id :" + caseId);
        }
        try {
            updateCase(authorisation, cd, caseType);
            if (debugEnabled) {
                log.info(caseId + " updated!");
            }
            updateMigratedCases(cd.getId());
        } catch (Exception e) {
            log.error("update failed for case with id [{}] with error [{}] ", cd.getId().toString(),
                    e.getMessage());

            updateFailedCases(cd.getId());
        }
    }

    private void updateCase(String authorisation, CaseDetails cd, String caseType) {
        String caseId = cd.getId().toString();
        Object data = cd.getData();
        if (debugEnabled) {
            log.info("data {}", data.toString());
        }
        CaseDetails update = ccdUpdateService.update(caseId,
                data,
                EVENT_ID,
                authorisation,
                EVENT_SUMMARY,
                EVENT_DESCRIPTION,
                caseType);
        totalMigrationsPerformed++;
    }

    private void updateFailedCases(Long id) {
        failedCases = nonNull(this.failedCases) ? (this.failedCases + "," + id) : id.toString();
    }

    private void updateMigratedCases(Long id) {
        migratedCases = nonNull(this.migratedCases) ? (this.migratedCases + "," + id) : id.toString();
    }

}
