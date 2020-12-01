package uk.gov.hmcts.reform.finrem.ccddatamigration.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.ReflectionUtils;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.PaginatedSearchMetadata;
import uk.gov.hmcts.reform.finrem.ccddatamigration.ccd.CcdUpdateService;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_LIST;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.finrem.ccddatamigration.MigrationConstants.CASE_TYPE_ID_CONSENTED;
import static uk.gov.hmcts.reform.finrem.ccddatamigration.MigrationConstants.CASE_TYPE_ID_CONTESTED;
import static uk.gov.hmcts.reform.finrem.ccddatamigration.MigrationConstants.EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.finrem.ccddatamigration.MigrationConstants.EVENT_SUMMARY;
import static uk.gov.hmcts.reform.finrem.ccddatamigration.MigrationConstants.JURISDICTION_ID;
import static uk.gov.hmcts.reform.finrem.ccddatamigration.TestConstants.TEST_CASE_ID;
import static uk.gov.hmcts.reform.finrem.ccddatamigration.TestConstants.TEST_S2S_TOKEN;
import static uk.gov.hmcts.reform.finrem.ccddatamigration.TestConstants.TEST_USER_ID;
import static uk.gov.hmcts.reform.finrem.ccddatamigration.TestConstants.TEST_USER_TOKEN;

@RunWith(MockitoJUnitRunner.class)
public class GeneralMigrationServiceTest {

    @InjectMocks
    private GeneralMigrationService migrationService;

    @Mock
    private CcdUpdateService ccdUpdateService;

    @Mock
    private CoreCaseDataApi ccdApi;

    private Map<String, String> searchCriteriaForPagination;
    private Map<String, String> searchCriteriaForCaseWorker;

    private CaseDetails caseDetails1;
    private CaseDetails caseDetails2;
    private CaseDetails caseDetails3;

    private static final String EVENT_ID = "FR_migrateCase";
    private static final String SPECIFIC_EVENT_ID = "FR_migrateSpecial";
    private static final String CASE_TYPE = CASE_TYPE_ID_CONSENTED;

    @Test
    public void shouldProcessASingleCaseAndMigrationIsSuccessful() {
        CaseDetails caseDetails = createCaseDetails(1111L, CASE_TYPE);
        when(ccdApi.getCase(TEST_USER_TOKEN, TEST_S2S_TOKEN, TEST_CASE_ID))
                .thenReturn(caseDetails);
        migrationService.processSingleCase(TEST_USER_TOKEN, TEST_S2S_TOKEN, TEST_CASE_ID);
        verify(ccdApi, times(1)).getCase(TEST_USER_TOKEN, TEST_S2S_TOKEN, TEST_CASE_ID);
        assertThat(migrationService.getTotalNumberOfCases(), is(1));
        assertThat(migrationService.getTotalMigrationsPerformed(), is(1));
        assertNull(migrationService.getFailedCases());
        assertThat(migrationService.getMigratedCases(), is("1111"));
    }

    @Test
    public void shouldNotProcessASingleCaseContested() {
        CaseDetails caseDetails = createCaseDetails(1111L, CASE_TYPE_ID_CONTESTED);

        when(ccdApi.getCase(TEST_USER_TOKEN, TEST_S2S_TOKEN, TEST_CASE_ID))
                .thenReturn(caseDetails);
        migrationService.processSingleCase(TEST_USER_TOKEN, TEST_S2S_TOKEN, TEST_CASE_ID);
        verify(ccdApi, times(1)).getCase(TEST_USER_TOKEN, TEST_S2S_TOKEN, TEST_CASE_ID);
        assertThat(migrationService.getTotalNumberOfCases(), is(0));
        assertThat(migrationService.getTotalMigrationsPerformed(), is(0));
        assertNull(migrationService.getFailedCases());
    }

    @Test
    public void shouldProcessASingleCaseAndMigrationIsFailed() {
        CaseDetails caseDetails = createCaseDetails(1111L, CASE_TYPE);
        when(ccdUpdateService.update(caseDetails.getId().toString(),
                caseDetails.getData(),
                EVENT_ID,
                TEST_USER_TOKEN,
                EVENT_SUMMARY,
                EVENT_DESCRIPTION,
                CASE_TYPE))
                .thenThrow(new RuntimeException("Internal server error"));
        when(ccdApi.getCase(TEST_USER_TOKEN, TEST_S2S_TOKEN, TEST_CASE_ID))
                .thenReturn(caseDetails);
        migrationService.processSingleCase(TEST_USER_TOKEN, TEST_S2S_TOKEN, TEST_CASE_ID);
        verify(ccdApi, times(1)).getCase(TEST_USER_TOKEN, TEST_S2S_TOKEN, TEST_CASE_ID);
        verify(ccdUpdateService, times(1)).update("1111", caseDetails.getData(),
                EVENT_ID,
                TEST_USER_TOKEN,
                EVENT_SUMMARY,
                EVENT_DESCRIPTION,
                CASE_TYPE);
        assertThat(migrationService.getTotalNumberOfCases(), is(1));
        assertThat(migrationService.getTotalMigrationsPerformed(), is(0));
        assertThat(migrationService.getFailedCases(), is("1111"));
        assertNull(migrationService.getMigratedCases());
    }

    @Test
    public void shouldProcessOnlyOneCandidateCase_whenDryRunIsTrue() {
        setupFields(true, true);
        setupMocks();
        migrationService.processAllTheCases(TEST_USER_TOKEN, TEST_S2S_TOKEN, TEST_USER_ID, JURISDICTION_ID, CASE_TYPE);
        assertThat(migrationService.getTotalNumberOfCases(), is(1));
        assertThat(migrationService.getTotalMigrationsPerformed(), is(1));
        assertNull(migrationService.getFailedCases());
        assertThat(migrationService.getMigratedCases(), is("1111"));
    }

    @Test
    public void shouldCallSpecificEvent_whenSpecificMigrationEventIsSet() {
        final Field field = ReflectionUtils.findField(GeneralMigrationService.class, "specificMigrationEvent");
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, migrationService, "FR_migrateSpecial");
        setupFields(true, true);
        setupMocks();
        when(ccdUpdateService.update(caseDetails1.getId().toString(),
                caseDetails1.getData(),
                SPECIFIC_EVENT_ID,
                TEST_USER_TOKEN,
                EVENT_SUMMARY,
                EVENT_DESCRIPTION,
                CASE_TYPE))
                .thenReturn(caseDetails1);
        migrationService.processAllTheCases(TEST_USER_TOKEN, TEST_S2S_TOKEN, TEST_USER_ID, JURISDICTION_ID, CASE_TYPE);
        verify(ccdUpdateService, times(1)).update(caseDetails1.getId().toString(), caseDetails1.getData(),
                SPECIFIC_EVENT_ID,
                TEST_USER_TOKEN,
                EVENT_SUMMARY,
                EVENT_DESCRIPTION,
                CASE_TYPE);
        verify(ccdUpdateService, times(0)).update(caseDetails1.getId().toString(), caseDetails1.getData(),
                EVENT_ID,
                TEST_USER_TOKEN,
                EVENT_SUMMARY,
                EVENT_DESCRIPTION,
                CASE_TYPE);
        assertThat(migrationService.getTotalNumberOfCases(), is(1));
        assertThat(migrationService.getTotalMigrationsPerformed(), is(1));
        assertNull(migrationService.getFailedCases());
        assertThat(migrationService.getMigratedCases(), is("1111"));
    }

    @Test
    public void shouldPassFilteringForFRCMigration() {
        final Field field = ReflectionUtils.findField(GeneralMigrationService.class, "specificMigrationEvent");
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, migrationService, "FR_migrateFrcCase");
        setupFields(true, true);
        setupMocks();
        when(ccdUpdateService.update(caseDetails1.getId().toString(),
                caseDetails1.getData(),
                "FR_migrateFrcCase",
                TEST_USER_TOKEN,
                EVENT_SUMMARY,
                EVENT_DESCRIPTION,
                CASE_TYPE))
                .thenReturn(caseDetails1);
        caseDetails1.getData().put("northWestFRCList","other");
        migrationService.processAllTheCases(TEST_USER_TOKEN, TEST_S2S_TOKEN, TEST_USER_ID, JURISDICTION_ID, CASE_TYPE);
        verify(ccdUpdateService, times(1)).update(caseDetails1.getId().toString(), caseDetails1.getData(),
                "FR_migrateFrcCase",
                TEST_USER_TOKEN,
                EVENT_SUMMARY,
                EVENT_DESCRIPTION,
                CASE_TYPE);
        verify(ccdUpdateService, times(0)).update(caseDetails1.getId().toString(), caseDetails1.getData(),
                EVENT_ID,
                TEST_USER_TOKEN,
                EVENT_SUMMARY,
                EVENT_DESCRIPTION,
                CASE_TYPE);
        assertThat(migrationService.getTotalNumberOfCases(), is(1));
        assertThat(migrationService.getTotalMigrationsPerformed(), is(1));
        assertNull(migrationService.getFailedCases());
        assertThat(migrationService.getMigratedCases(), is("1111"));
    }

    @Test
    public void shouldSkipCaseForFRCMigration() {
        final Field field = ReflectionUtils.findField(GeneralMigrationService.class, "specificMigrationEvent");
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, migrationService, "FR_migrateFrcCase");
        setupFields(true, true);
        setupMocks();
        migrationService.processAllTheCases(TEST_USER_TOKEN, TEST_S2S_TOKEN, TEST_USER_ID, JURISDICTION_ID, CASE_TYPE);
        verify(ccdUpdateService, times(0)).update(caseDetails1.getId().toString(), caseDetails1.getData(),
                "FR_migrateFrcCase",
                TEST_USER_TOKEN,
                EVENT_SUMMARY,
                EVENT_DESCRIPTION,
                CASE_TYPE);
        verify(ccdUpdateService, times(0)).update(caseDetails1.getId().toString(), caseDetails1.getData(),
                EVENT_ID,
                TEST_USER_TOKEN,
                EVENT_SUMMARY,
                EVENT_DESCRIPTION,
                CASE_TYPE);
        assertThat(migrationService.getTotalNumberOfCases(), is(1));
        assertThat(migrationService.getTotalMigrationsPerformed(), is(0));
        assertThat(migrationService.getTotalNumberOfSkips(), is(1));
        assertNull(migrationService.getFailedCases());
        assertThat(migrationService.getMigratedCases(), is("1111"));
    }

    @Test
    public void shouldProcessAllTheCandidateCases_whenDryRunIsFalseAndOneCaseFailed() {
        setupFields(false, true);
        setupMocks();
        setUpMockForUpdate(caseDetails1);
        setUpMockForUpdate(caseDetails2);
        when(ccdUpdateService.update(caseDetails3.getId().toString(),
            caseDetails3.getData(),
            EVENT_ID,
            TEST_USER_TOKEN,
            EVENT_SUMMARY,
            EVENT_DESCRIPTION,
            CASE_TYPE))
            .thenThrow(new RuntimeException("Internal server error"));
        migrationService.processAllTheCases(TEST_USER_TOKEN, TEST_S2S_TOKEN, TEST_USER_ID, JURISDICTION_ID, CASE_TYPE);
        assertThat(migrationService.getTotalNumberOfCases(), is(3));
        assertThat(migrationService.getTotalMigrationsPerformed(), is(2));
        assertThat(migrationService.getFailedCases(), is("1113"));
        assertThat(migrationService.getMigratedCases(), is("1111,1112"));
    }

    @Test
    public void shouldProcessAllTheCandidateCases_whenDryRunIsFalseAndTwoCasesFailed() {
        setupFields(false, false);
        setupMocks();
        setUpMockForUpdate(caseDetails1);
        when(ccdUpdateService.update(caseDetails2.getId().toString(),
            caseDetails2.getData(),
            EVENT_ID,
            TEST_USER_TOKEN,
            EVENT_SUMMARY,
            EVENT_DESCRIPTION,
            CASE_TYPE))
            .thenThrow(new RuntimeException("Internal server error"));
        when(ccdUpdateService.update(caseDetails3.getId().toString(),
            caseDetails3.getData(),
            EVENT_ID,
            TEST_USER_TOKEN,
            EVENT_SUMMARY,
            EVENT_DESCRIPTION, CASE_TYPE))
            .thenThrow(new RuntimeException("Internal server error"));
        migrationService.processAllTheCases(TEST_USER_TOKEN, TEST_S2S_TOKEN, TEST_USER_ID, JURISDICTION_ID, CASE_TYPE);
        assertThat(migrationService.getTotalNumberOfCases(), is(3));
        assertThat(migrationService.getTotalMigrationsPerformed(), is(1));
        assertThat(migrationService.getFailedCases(), is("1112,1113"));
        assertThat(migrationService.getMigratedCases(), is("1111"));
    }

    @Test
    public void shouldProcessNoCaseWhenNoCasesAvailableWithDryRun() {
        setupFields(true, false);
        final PaginatedSearchMetadata paginatedSearchMetadata = new PaginatedSearchMetadata();
        paginatedSearchMetadata.setTotalPagesCount(0);
        paginatedSearchMetadata.setTotalResultsCount(0);

        setupMocksForSearchCases(EMPTY_LIST, paginatedSearchMetadata);

        migrationService.processAllTheCases(TEST_USER_TOKEN, TEST_S2S_TOKEN, TEST_USER_ID, JURISDICTION_ID, CASE_TYPE);
        assertThat(migrationService.getTotalNumberOfCases(), is(0));
        assertThat(migrationService.getTotalMigrationsPerformed(), is(0));
        assertNull(migrationService.getFailedCases());
        assertNull(migrationService.getFailedCases());
    }

    @Test
    public void shouldProcessNoCaseWhenNoCasesAvailableWithDryRunAsFalse() {
        setupFields(false, false);
        final PaginatedSearchMetadata paginatedSearchMetadata = new PaginatedSearchMetadata();
        paginatedSearchMetadata.setTotalPagesCount(0);
        paginatedSearchMetadata.setTotalResultsCount(0);

        setupMocksForSearchCases(EMPTY_LIST, paginatedSearchMetadata);

        migrationService.processAllTheCases(TEST_USER_TOKEN, TEST_S2S_TOKEN, TEST_USER_ID, JURISDICTION_ID, CASE_TYPE);
        assertThat(migrationService.getTotalNumberOfCases(), is(0));
        assertThat(migrationService.getTotalMigrationsPerformed(), is(0));
        assertNull(migrationService.getFailedCases());
        assertNull(migrationService.getFailedCases());
    }

    @Test
    public void shouldProcessCasesFromCsvFile() throws IOException {
        setupMocksForCsvCases();
        URL csvFileResource = getClass().getClassLoader().getResource("csvExamples/cases.csv");
        String csvFilePath = Objects.requireNonNull(csvFileResource).getPath();

        migrationService.processCasesInFile(TEST_USER_TOKEN, TEST_S2S_TOKEN, csvFilePath);

        assertThat(migrationService.getTotalNumberOfCases(), is(9));
        assertThat(migrationService.getTotalMigrationsPerformed(), is(9));
    }

    @Test(expected = FileNotFoundException.class)
    public void shouldThrowFileNotFoundException() throws IOException {
        migrationService.processCasesInFile(TEST_USER_TOKEN, TEST_S2S_TOKEN, "FileDoesNotExist");
    }

    @Test
    public void shouldProcessEightCasesWhenOneIsNotFound() throws IOException {
        setupMocksForCsvCases();
        URL csvFileResource = getClass().getClassLoader().getResource("csvExamples/oneIncorrectCaseId.csv");
        String csvFilePath = Objects.requireNonNull(csvFileResource).getPath();

        migrationService.processCasesInFile(TEST_USER_TOKEN, TEST_S2S_TOKEN, csvFilePath);

        assertThat(migrationService.getTotalNumberOfCases(), is(8));
        assertThat(migrationService.getTotalMigrationsPerformed(), is(8));
    }

    @Test
    public void shouldProcessNoCasesWhenCsvIsEmpty() throws IOException {
        URL csvFileResource = getClass().getClassLoader().getResource("csvExamples/empty.csv");
        String csvFilePath = Objects.requireNonNull(csvFileResource).getPath();

        migrationService.processCasesInFile(TEST_USER_TOKEN, TEST_S2S_TOKEN, csvFilePath);

        assertThat(migrationService.getTotalNumberOfCases(), is(0));
        assertThat(migrationService.getTotalMigrationsPerformed(), is(0));
    }

    @Test
    public void shouldSkipDuplicateCaseIDs() throws IOException {
        setupMocksForCsvCases();
        URL csvFileResource = getClass().getClassLoader().getResource("csvExamples/duplicateIds.csv");
        String csvFilePath = Objects.requireNonNull(csvFileResource).getPath();

        migrationService.processCasesInFile(TEST_USER_TOKEN, TEST_S2S_TOKEN, csvFilePath);

        assertThat(migrationService.getTotalNumberOfCases(), is(9));
        assertThat(migrationService.getTotalMigrationsPerformed(), is(9));
    }

    private void setupMocks() {
        caseDetails1 = createCaseDetails(1111L, CASE_TYPE);
        caseDetails2 = createCaseDetails(1112L, CASE_TYPE);
        caseDetails3 = createCaseDetails(1113L, CASE_TYPE);

        final PaginatedSearchMetadata paginatedSearchMetadata = new PaginatedSearchMetadata();
        paginatedSearchMetadata.setTotalPagesCount(1);
        paginatedSearchMetadata.setTotalResultsCount(3);

        setupMocksForSearchCases(asList(caseDetails1, caseDetails2, caseDetails3), paginatedSearchMetadata);
    }

    private void setupFields(final boolean dryRun, final boolean debug) {
        final Field field = ReflectionUtils.findField(GeneralMigrationService.class, "dryRun");
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, migrationService, dryRun);
        if (debug) {
            final Field debugEnabled = ReflectionUtils.findField(GeneralMigrationService.class, "debugEnabled");
            ReflectionUtils.makeAccessible(debugEnabled);
            ReflectionUtils.setField(debugEnabled, migrationService, debug);
        }
    }

    private void setUpMockForUpdate(final CaseDetails caseDetails) {
        when(ccdUpdateService.update(caseDetails.getId().toString(),
                caseDetails.getData(),
                EVENT_ID,
                TEST_USER_TOKEN,
                EVENT_SUMMARY,
                EVENT_DESCRIPTION,
                CASE_TYPE)).thenReturn(caseDetails);
    }

    private void setupMocksForSearchCases(final List<CaseDetails> caseDetails,
                                          final PaginatedSearchMetadata paginatedSearchMetadata) {
        searchCriteriaForPagination = new HashMap<>();
        when(ccdApi.getPaginationInfoForSearchForCaseworkers(TEST_USER_TOKEN, TEST_S2S_TOKEN, TEST_USER_ID, JURISDICTION_ID,
                CASE_TYPE, searchCriteriaForPagination)).thenReturn(paginatedSearchMetadata);

        searchCriteriaForCaseWorker = new HashMap<>();
        searchCriteriaForCaseWorker.put("page", "1");

        when(ccdApi.searchForCaseworker(
            TEST_USER_TOKEN,
            TEST_S2S_TOKEN,
            TEST_USER_ID,
            JURISDICTION_ID,
            CASE_TYPE,
            searchCriteriaForCaseWorker))
            .thenReturn(caseDetails);
    }

    private void setupMocksForCsvCases() {
        List<String> ccdCaseIds = List.of(
                "1234567891",
                "1234567892",
                "1234567893",
                "1234567894",
                "1234567895",
                "1234567896",
                "1234567897",
                "1234567898",
                "1234567899"
        );

        for (String ccdCaseId : ccdCaseIds) {
            CaseDetails caseDetails = createCaseDetails(Long.parseLong(ccdCaseId), CASE_TYPE);

            when(ccdApi.getCase(TEST_USER_TOKEN, TEST_S2S_TOKEN, ccdCaseId))
                    .thenReturn(caseDetails);
        }
    }

    private CaseDetails createCaseDetails(long id, String caseType) {
        Map<String, Object> caseData = new HashMap<>();
        caseData.put("latestConsentOrder", "testMap");

        return CaseDetails.builder()
            .id(id)
            .caseTypeId(caseType)
            .data(caseData)
            .state("consentOrderMade")
            .build();
    }
}