package uk.gov.hmcts.reform.finrem.ccddatamigration.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.PaginatedSearchMetadata;
import uk.gov.hmcts.reform.finrem.ccddatamigration.ccd.CcdUpdateService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Objects.nonNull;

@Component("generalMigrationService")
@RequiredArgsConstructor
@Slf4j
public class GeneralMigrationService implements MigrationService {

    private static final String EVENT_ID = "FR_migrateCase";
    private static final String EVENT_SUMMARY = "Migrate Case";
    private static final String EVENT_DESCRIPTION = "Migrate Case";

    private final CcdUpdateService ccdUpdateService;
    private final CoreCaseDataApi ccdApi;

    @Getter private int totalMigrationsPerformed;
    @Getter private int totalNumberOfCases;
    @Getter private String failedCases;
    @Getter private String migratedCases;

    @Value("${log.debug}")
    private boolean debugEnabled;

    @Value("${ccd.dryrun}")
    private boolean dryRun;

    @Override
    public void processSingleCase(final String userToken, final String s2sToken, final String ccdCaseId) {
        final CaseDetails aCase;
        try {
            aCase = ccdApi.getCase(userToken, s2sToken, ccdCaseId);
            log.info("case data {} ", aCase);
            if (isCandidateForMigration(aCase)) {
                final String caseTypeId = aCase.getCaseTypeId();
                updateOneCase(userToken, aCase, caseTypeId);
            } else {
                log.info("Case {} doesn't meet migration criteria", aCase.getId());
            }
        } catch (final Exception ex) {
            log.error("case {} not found, {}", ccdCaseId, ex.getMessage());
        }
    }

    @Override
    public void processAllTheCases(final String userToken, final String s2sToken, final String userId,
                                   final String jurisdictionId, final String caseType) {
        final Map<String, String> searchCriteria = new HashMap<>();
        final int numberOfPages = requestNumberOfPage(userToken, s2sToken, userId, jurisdictionId, caseType, searchCriteria);
        log.info("Number of pages : {}", numberOfPages);

        if (dryRun) {
            log.info("caseType {}  and dryRun for one case ...", caseType);
            dryRunWithOneCase(userToken, s2sToken, userId, jurisdictionId, caseType, numberOfPages);
        } else {
            log.info("migrating all the cases ...");
            IntStream.rangeClosed(1, numberOfPages)
                    .forEach(pageNumber -> migrateCasesForPage(userToken, s2sToken, userId, jurisdictionId, caseType, pageNumber));
        }
    }

    private static boolean isCandidateForMigration(final CaseDetails caseDetails) {
        if (caseDetails != null && caseDetails.getData() != null) {
            Map<String, Object> caseData = caseDetails.getData();
            return isContestedCase(caseData) && !hasRegionList(caseData);
        }
        return false;
    }

    private static boolean isContestedCase(Map<String, Object> caseData) {
        return caseData.get("case_type_id") == "FinancialRemedyContested";
    }

    private static boolean hasRegionList(Map<String, Object> caseData) {
        return caseData.containsKey("regionList");
    }

    private int requestNumberOfPage(final String authorisation,
                                    final String serviceAuthorisation,
                                    final String userId,
                                    final String jurisdictionId,
                                    final String caseType,
                                    final Map<String, String> searchCriteria) {
        final PaginatedSearchMetadata paginationInfoForSearchForCaseworkers = ccdApi.getPaginationInfoForSearchForCaseworkers(
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

    private void dryRunWithOneCase(final String userToken, final String s2sToken, final String userId,
                                   final String jurisdictionId, final String caseType, final int numberOfPages) {
        boolean found = false;
        for (int i = 1; i <= numberOfPages && !found; i++) {
            final List<CaseDetails> casesForPage = getCasesForPage(userToken, s2sToken, userId,
                    jurisdictionId, caseType, i);
            if (casesForPage.size() > 0) {
                found = true;
                log.info("Migrating Case Id {} for the dryRun", casesForPage.get(0).getId());
                updateOneCase(userToken, casesForPage.get(0), caseType);
            }
        }
    }

    private List<CaseDetails> getCasesForPage(final String userToken,
                                              final String s2sToken,
                                              final String userId,
                                              final String jurisdictionId,
                                              final String caseType,
                                              final int pageNumber) {
        final Map<String, String> searchCriteria = new HashMap<>();
        searchCriteria.put("page", String.valueOf(pageNumber));
        return ccdApi.searchForCaseworker(userToken, s2sToken, userId, jurisdictionId, caseType, searchCriteria)
                       .stream()
                       .filter(GeneralMigrationService::isCandidateForMigration)
                       .collect(Collectors.toList());
    }

    private void migrateCasesForPage(final String userToken,
                                     final String s2sToken,
                                     final String userId,
                                     final String jurisdictionId,
                                     final String caseType,
                                     final int pageNumber) {
        getCasesForPage(userToken, s2sToken, userId, jurisdictionId, caseType, pageNumber)
                .stream()
                .filter(GeneralMigrationService::isCandidateForMigration)
                .forEach(caseDetails -> updateOneCase(userToken, caseDetails, caseType));
    }

    private void updateOneCase(final String authorisation, final CaseDetails caseDetails, final String caseType) {
        totalNumberOfCases++;
        final String caseId = caseDetails.getId().toString();
        if (debugEnabled) {
            log.info("updating case with id :" + caseId);
        }
        try {
            updateCase(authorisation, caseDetails, caseType);
            if (debugEnabled) {
                log.info(caseId + " updated!");
            }
            updateMigratedCases(caseDetails.getId());
        } catch (final Exception e) {
            log.error("update failed for case with id [{}] with error [{}] ", caseDetails.getId().toString(), e.getMessage());
            updateFailedCases(caseDetails.getId());
        }
    }

    private void updateCase(final String authorisation, final CaseDetails caseDetails, final String caseType) {
        final String caseId = caseDetails.getId().toString();
        final Object data = caseDetails.getData();
        if (debugEnabled) {
            log.info("data {}", data.toString());
        }
        final CaseDetails update = ccdUpdateService.update(caseId,
                data,
                EVENT_ID,
                authorisation,
                EVENT_SUMMARY,
                EVENT_DESCRIPTION,
                caseType);
        totalMigrationsPerformed++;
    }

    private void updateFailedCases(final Long id) {
        failedCases = nonNull(failedCases) ? (failedCases + "," + id) : id.toString();
    }

    private void updateMigratedCases(final Long id) {
        migratedCases = nonNull(migratedCases) ? (migratedCases + "," + id) : id.toString();
    }
}
