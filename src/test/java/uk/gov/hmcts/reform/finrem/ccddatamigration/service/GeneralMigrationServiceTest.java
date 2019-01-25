package uk.gov.hmcts.reform.finrem.ccddatamigration.service;

import org.hamcrest.core.Is;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.PaginatedSearchMetadata;
import uk.gov.hmcts.reform.finrem.ccddatamigration.ccd.CcdUpdateService;

import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class GeneralMigrationServiceTest {

    private static final String USER_TOKEN = "Bearer eeeejjjttt";
    private static final String S2S_TOKEN = "eeeejjjttt";
    private static final String CASE_ID = "11111";
    private static final String USER_ID = "30";
    private static final String JURISDICTION_ID = "divorce";
    private static final String CASE_TYPE = "FinancialRemedyMVP2";
    @InjectMocks
    private GeneralMigrationService migrationService;

    @Mock
    private CcdUpdateService ccdUpdateService;

    @Mock
    private CoreCaseDataApi ccdApi;

    @Test
    public void shouldReturnTrueWhenAcceptTheCaseWithSolicitorAddress1() {
        Map<String, Object> data = new HashMap<>();
        data.put("solicitorAddress1", "188 cityView");
        data.put("solicitorAddress2", "Ilford");
        CaseDetails caseDetails = CaseDetails.builder()
                .data(data)
                .build();

        assertThat(migrationService.accepts(caseDetails), Is.is(true));
    }

    @Test
    public void shouldReturnFalseWhenAcceptTheCaseWithSolicitorAddress1() {
        Map<String, Object> data = new HashMap<>();
        data.put("solicitorAddress2", "188 cityView");
        data.put("solicitorAddress3", "Ilford");
        CaseDetails caseDetails = CaseDetails.builder()
                .data(data)
                .build();

        assertThat(migrationService.accepts(caseDetails), Is.is(false));
    }

    @Test
    public void ShouldProcessASingleCase() {
        CaseDetails caseDetails = CaseDetails.builder()
                .id(1111L).build();
        when(ccdApi.getCase(USER_TOKEN, S2S_TOKEN, CASE_ID))
                .thenReturn(caseDetails);
        migrationService.processSingleCase(USER_TOKEN, S2S_TOKEN, CASE_ID);
        verify(ccdApi, times(1)).getCase(USER_TOKEN, S2S_TOKEN, CASE_ID);
    }

    @Test
    public void shouldProcessOnlyOneCandidateCase_whenDryRunIsTrue() {
        setupMocks();
        migrationService.processAllTheCases(USER_TOKEN, S2S_TOKEN, USER_ID, JURISDICTION_ID, CASE_TYPE, true);
        assertThat(migrationService.getTotalNumberOfCases(), Is.is(1));
        assertThat(migrationService.getTotalMigrationsPerformed(), Is.is(1));
    }

    @Test
    public void shouldProcessOnlyOneCandidateCase_whenDryRunIsFalse() {
        setupMocks();
        migrationService.processAllTheCases(USER_TOKEN, S2S_TOKEN, USER_ID, JURISDICTION_ID, CASE_TYPE, false);
        assertThat(migrationService.getTotalNumberOfCases(), Is.is(2));
        assertThat(migrationService.getTotalMigrationsPerformed(), Is.is(2));
    }

    private void setupMocks() {
        Map<String, Object> data1 = new HashMap<>();
        data1.put("solicitorAddress1", "188 cityView");
        CaseDetails caseDetails1 = CaseDetails.builder()
                .id(1111L)
                .data(data1)
                .build();
        Map<String, Object> data2 = new HashMap<>();
        data2.put("solicitorAddress1", "189 cityView");
        CaseDetails caseDetails2 = CaseDetails.builder()
                .id(1112L)
                .data(data2)
                .build();

        PaginatedSearchMetadata paginatedSearchMetadata = new PaginatedSearchMetadata();
        paginatedSearchMetadata.setTotalPagesCount(1);
        paginatedSearchMetadata.setTotalResultsCount(2);

        Map<String, String> searchCriteria = new HashMap<>();
        when(ccdApi.getPaginationInfoForSearchForCaseworkers(USER_TOKEN, S2S_TOKEN, USER_ID, JURISDICTION_ID,
                CASE_TYPE, searchCriteria)).thenReturn(paginatedSearchMetadata);

        Map<String, String> searchCriteria1 = new HashMap<>();
        searchCriteria1.put("page", "1");

        when(ccdApi.searchForCaseworker(
                USER_TOKEN,
                S2S_TOKEN,
                USER_ID,
                JURISDICTION_ID,
                CASE_TYPE,
                searchCriteria1))
                .thenReturn(asList(caseDetails1, caseDetails2));
    }
}