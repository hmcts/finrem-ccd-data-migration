package uk.gov.hmcts.reform.finrem.ccddatamigration.service;

import feign.FeignException;
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
import static uk.gov.hmcts.reform.finrem.ccddatamigration.MigrationConstants.EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.finrem.ccddatamigration.MigrationConstants.EVENT_ID;
import static uk.gov.hmcts.reform.finrem.ccddatamigration.MigrationConstants.EVENT_SUMMARY;
import static uk.gov.hmcts.reform.finrem.ccddatamigration.service.CommonFunction.isConsentedCase;

@Component("generalMigrationService")
@RequiredArgsConstructor
@Slf4j
public class GeneralMigrationService implements MigrationService {

    private final CcdUpdateService ccdUpdateService;
    private final CoreCaseDataApi ccdApi;

    // Defaults to FR_migrateCase event, change this to true and modify "callSpecificEventForCase" for a specific event
    @Value("${migration.specificEvent}")
    private String specificMigrationEvent;

    @Getter
    private int totalMigrationsPerformed;
    @Getter
    private int totalNumberOfCases;
    @Getter
    private int totalNumberOfSkips;
    @Getter
    private int totalNumberOfFails;
    @Getter
    private String failedCases;
    @Getter
    private String migratedCases;
    @Getter
    private String skippedCases;

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
            List<String> extractedCaseIds = extractCaseIdsFromCsv(file);
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

    private List<String> extractCaseIdsFromCsv(String file) throws IOException {
        int caseCounter = 0;
        int duplicationCounter = 0;
        List<String> extractedCaseIds = new ArrayList<>();
        String csvLine;

        try (BufferedReader fileReader = new BufferedReader(new FileReader(file))) {
            while ((csvLine = fileReader.readLine()) != null) {
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
            return isConsentedCase(caseDetails);
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
        log.info("Processing case with Case ID: " + caseId + " specificMigrationEvent:" + specificMigrationEvent);
        try {
            if (specificMigrationEvent != null && !specificMigrationEvent.isBlank()) {
                if (specificMigrationEvent.equalsIgnoreCase("FR_migrateFrcCase")) { //RPET-164 specific section - might be removed later
                    String northWestFRC = (String) caseDetails.getData().getOrDefault("northWestFRCList", "");
                    String southWestFRC = (String) caseDetails.getData().getOrDefault("southWestFRCList", "");
                    String southEastFRC = (String) caseDetails.getData().getOrDefault("southEastFRCList", "");
                    String walesFRC = (String) caseDetails.getData().getOrDefault("walesFRCList", "");
                    if ((northWestFRC != null && northWestFRC.equalsIgnoreCase("other"))
                            || (southWestFRC != null && southWestFRC.equalsIgnoreCase("other"))
                            || (southEastFRC != null && southEastFRC.equalsIgnoreCase("other"))
                            || (walesFRC != null && walesFRC.equalsIgnoreCase("other"))) {
                        callSpecificMigrateEventForCase(authorisation, caseDetails, caseType);
                    } else {
                        updateSkippedCases(caseDetails.getId());
                        log.info("Case id {} is not migration candidate. Skipping...", caseId);
                    }
                } else {
                    callSpecificMigrateEventForCase(authorisation, caseDetails, caseType);
                }
            } else {
                callDefaultMigrateEventForCase(authorisation, caseDetails, caseType);
            }
            if (debugEnabled) {
                log.info(caseId + " updated!");
            }
            updateMigratedCases(caseDetails.getId());
        } catch (final Exception e) {
            if (e instanceof FeignException) {
                log.error(((FeignException) e).contentUTF8());
            } else {
                log.error("Update failed for Case ID [{}] with error [{}] ", caseDetails.getId().toString(), e.getMessage());
            }
            updateFailedCases(caseDetails.getId());
        }
    }

    private void callDefaultMigrateEventForCase(final String authorisation, final CaseDetails caseDetails, final String caseType) {
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

    private void callSpecificMigrateEventForCase(final String authorisation, final CaseDetails caseDetails, final String caseType) {
        final String caseId = caseDetails.getId().toString();
        final Object data = caseDetails.getData();
        if (debugEnabled) {
            log.info("data {}", data.toString());
        }
        ccdUpdateService.update(
                caseId,
                data,
                // change below to required Event ID
                specificMigrationEvent,
                authorisation,
                EVENT_SUMMARY,
                EVENT_DESCRIPTION,
                caseType);
        totalMigrationsPerformed++;
    }

    private void updateFailedCases(final Long id) {
        totalNumberOfFails++;
        failedCases = nonNull(failedCases) ? (failedCases + "," + id) : id.toString();
    }

    private void updateMigratedCases(final Long id) {
        migratedCases = nonNull(migratedCases) ? (migratedCases + "," + id) : id.toString();
    }

    private void updateSkippedCases(final Long id) {
        totalNumberOfSkips++;
        skippedCases = nonNull(skippedCases) ? (skippedCases + "," + id) : id.toString();
    }
}
