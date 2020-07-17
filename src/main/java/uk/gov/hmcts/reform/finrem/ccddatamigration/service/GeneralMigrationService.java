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
import static uk.gov.hmcts.reform.finrem.ccddatamigration.MigrationConstants.EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.finrem.ccddatamigration.MigrationConstants.EVENT_ID;
import static uk.gov.hmcts.reform.finrem.ccddatamigration.MigrationConstants.EVENT_SUMMARY;
import static uk.gov.hmcts.reform.finrem.ccddatamigration.service.CommonFunction.isContestedCase;

@Component("generalMigrationService")
@RequiredArgsConstructor
@Slf4j
public class GeneralMigrationService implements MigrationService {

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
            log.error("Case {} not found, {}", ccdCaseId, ex.getMessage());
        }
    }

    @Override
    public void processAllTheCases(final String userToken, final String s2sToken, final String userId,
                                   final String jurisdictionId, final String caseType) {
        final Map<String, String> searchCriteria = new HashMap<>();
        final int numberOfPages = requestNumberOfPage(userToken, s2sToken, userId, jurisdictionId, caseType, searchCriteria);
        log.info("Number of pages : {}", numberOfPages);

        if (dryRun) {
            log.info("caseType {} and dryRun for one case ...", caseType);
            dryRunWithOneCase(userToken, s2sToken, userId, jurisdictionId, caseType, numberOfPages);
        } else {
            log.info("Migrating all the cases ...");
            IntStream.rangeClosed(1, numberOfPages)
                    .forEach(pageNumber -> migrateCasesForPage(userToken, s2sToken, userId, jurisdictionId, caseType, pageNumber));
        }
    }

    private static boolean isCandidateForMigration(final CaseDetails caseDetails) {
        if (caseDetails != null && caseDetails.getData() != null) {
            Map<String, Object> caseData = caseDetails.getData();
            return isContestedCase(caseDetails) && !hasRegionList(caseData) && hasCourtDetails(caseData);
        }
        return false;
    }

    private static boolean hasRegionList(Map<String, Object> caseData) {
        return caseData.containsKey("regionList");
    }

    private static boolean hasCourtDetails(Map<String, Object> caseData) {
        return caseData.containsKey("regionListSL") || hasAllocatedCourtDetails(caseData) || hasAllocatedCourtDetailsGA(caseData);
    }

    private static boolean hasAllocatedCourtDetails(Map<String, Object> caseData) {
        if (caseData.containsKey("allocatedCourtList")) {
            try {
                Map<String, Object> allocatedCourtList = (Map<String, Object>) caseData.get("allocatedCourtList");
                return allocatedCourtList.containsKey("region");
            } catch (ClassCastException e) {
                return false;
            }
        }

        return false;
    }

    private static boolean hasAllocatedCourtDetailsGA(Map<String, Object> caseData) {
        if (caseData.containsKey("allocatedCourtListGA")) {
            try {
                Map<String, Object> allocatedCourtList = (Map<String, Object>) caseData.getOrDefault("allocatedCourtListGA", new HashMap<>());
                return allocatedCourtList.containsKey("region");
            } catch (ClassCastException e) {
                return false;
            }
        }

        return false;
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
            log.debug("Pagination>> " + paginationInfoForSearchForCaseworkers.toString());
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
            log.info("Updating case with Case ID:" + caseId);
        }
        try {
            updateCase(authorisation, caseDetails, caseType);
            if (debugEnabled) {
                log.info(caseId + " updated!");
            }
            updateMigratedCases(caseDetails.getId());
        } catch (final Exception e) {
            log.error("Update failed for Case ID [{}] with error [{}] ", caseDetails.getId().toString(), e.getMessage());
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
