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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Objects.nonNull;
import static org.springframework.util.ObjectUtils.isEmpty;
import static uk.gov.hmcts.reform.finrem.ccddatamigration.MigrationConstants.*;
import static uk.gov.hmcts.reform.finrem.ccddatamigration.service.CommonFunction.isCaseInCorrectState;
import static uk.gov.hmcts.reform.finrem.ccddatamigration.service.CommonFunction.isConsentedCase;

@Component("generalMigrationService")
@RequiredArgsConstructor
@Slf4j
public class GeneralMigrationService implements MigrationService {

    private final CcdUpdateService ccdUpdateService;
    private final CoreCaseDataApi ccdApi;

    private boolean specificMigrationEvent = true;

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
        final CaseDetails singleCase;
        try {
            singleCase = ccdApi.getCase(userToken, s2sToken, ccdCaseId);
            if (isCandidateForMigration(singleCase)) {
                final String caseTypeId = singleCase.getCaseTypeId();
                updateOneCase(userToken, singleCase, caseTypeId);
            } else {
                log.info("Case {} doesn't meet migration criteria", singleCase.getId());
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

    @Override
    public void processCasesInFile(final String userToken, final String s2sToken, final String file) throws IOException {
        try {
            // remove or change this to true if you want to run a certain event otherwise it defaults to FR_migrateCase
            specificMigrationEvent = false;
            List<String> extractedCaseIds = extractCaseIdsFromCSV(file);
            CaseDetails singleCase;
            for (String ccdCaseId : extractedCaseIds) {
                try {
                    singleCase = ccdApi.getCase(userToken, s2sToken, ccdCaseId);
                    updateOneCase(userToken, singleCase, singleCase.getCaseTypeId());
                } catch (final Exception e) {
                    log.error("Case {} not found, {}", ccdCaseId, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
            throw e;
        }
    }

    private List<String> extractCaseIdsFromCSV(String file) throws IOException {
        int caseCounter = 0;
        int duplicationCounter = 0;
        List<String> extractedCaseIds = new ArrayList<>();
        String csvLine = "";

        try(BufferedReader fileReader = new BufferedReader(new FileReader(file))) {
            while((csvLine = fileReader.readLine()) != null) {
                if (extractedCaseIds.contains(csvLine)) {
                    log.info("Duplicated Case ID found: {}", csvLine);
                    duplicationCounter++;
                } else {
                    extractedCaseIds.add(csvLine);
                    caseCounter++;
                }
            }
            log.info("Extracted {} case ID's From file: {} with {} duplications", caseCounter, file, duplicationCounter);
        } catch (FileNotFoundException e) {
            log.error(e.getMessage());
            throw e;
        }
        return extractedCaseIds;
    }

    private static boolean isCandidateForMigration(final CaseDetails caseDetails) {
        if (caseDetails != null && caseDetails.getData() != null) {
            return isConsentedCase(caseDetails)
                && isCaseInCorrectState(caseDetails, "consentOrderMade", "awaitingResponse")
                && isLatestConsentOrderFieldPopulated(caseDetails);
        }
        return false;
    }

    private static boolean isLatestConsentOrderFieldPopulated(CaseDetails caseDetails) {

        Map<String, Object> caseData = caseDetails.getData();
        Object latestConsentOrder = caseData.get("latestConsentOrder");

        return !isEmpty(latestConsentOrder);
    }

    private int requestNumberOfPage(final String authorisation, final String serviceAuthorisation, final String userId,
                                    final String jurisdictionId, final String caseType, final Map<String, String> searchCriteria) {
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

    private List<CaseDetails> getCasesForPage(final String userToken, final String s2sToken, final String userId,
                                              final String jurisdictionId, final String caseType, final int pageNumber) {
        final Map<String, String> searchCriteria = new HashMap<>();
        searchCriteria.put("page", String.valueOf(pageNumber));
        return ccdApi.searchForCaseworker(userToken, s2sToken, userId, jurisdictionId, caseType, searchCriteria)
                       .stream()
                       .filter(GeneralMigrationService::isCandidateForMigration)
                       .collect(Collectors.toList());
    }

    private void migrateCasesForPage(final String userToken, final String s2sToken, final String userId,
                                     final String jurisdictionId, final String caseType, final int pageNumber) {
        getCasesForPage(userToken, s2sToken, userId, jurisdictionId, caseType, pageNumber)
                .stream()
                .filter(GeneralMigrationService::isCandidateForMigration)
                .forEach(caseDetails -> updateOneCase(userToken, caseDetails, caseType));
    }

    private void updateOneCase(final String authorisation, final CaseDetails caseDetails, final String caseType) {
        totalNumberOfCases++;
        final String caseId = caseDetails.getId().toString();
        if (debugEnabled) {
            log.info("Updating case with Case ID: " + caseId);
        }
        try {
            if (!specificMigrationEvent) {
                updateCase(authorisation, caseDetails, caseType);
            } else {
                callSpecificEventForCase(authorisation, caseDetails, caseType);
            }
            if (debugEnabled) {
                log.info(caseId + " updated!");
            }
            updateMigratedCases(caseDetails.getId());
        } catch (final Exception e) {
            log.error("Update failed for Case ID [{}] with error [{}] ", caseDetails.getId().toString(), e.getMessage());
            updateFailedCases(caseDetails.getId());
        }
    }

    // Default updateCase
    private void updateCase(final String authorisation, final CaseDetails caseDetails, final String caseType) {
        final String caseId = caseDetails.getId().toString();
        final Object data = caseDetails.getData();
        if (debugEnabled) {
            log.info("data {}", data.toString());
        }
        ccdUpdateService.update(
            caseId,
            data,
            EVENT_ID,
            authorisation,
            EVENT_SUMMARY,
            EVENT_DESCRIPTION,
            caseType);
        totalMigrationsPerformed++;
    }

    // Use this if you want to just run a specific event on all eligible cases - e.g. Send Order
    private void callSpecificEventForCase(final String authorisation, final CaseDetails caseDetails, final String caseType) {
        final String caseId = caseDetails.getId().toString();
        final Object data = caseDetails.getData();
        if (debugEnabled) {
            log.info("data {}", data.toString());
        }
        ccdUpdateService.update(
            caseId,
            data,
            // change below to required Event ID
            "FR_sendOrderForApproved",
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
