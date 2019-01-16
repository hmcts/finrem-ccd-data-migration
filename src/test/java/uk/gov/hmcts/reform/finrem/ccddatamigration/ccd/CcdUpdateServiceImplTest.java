package uk.gov.hmcts.reform.finrem.ccddatamigration.ccd;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.ReflectionUtils;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.finrem.ccddatamigration.domain.UserDetails;
import uk.gov.hmcts.reform.finrem.ccddatamigration.idam.IdamUserService;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CcdUpdateServiceImplTest {

    private static final String EVENT_ID = "FR_migrateCase";
    private static final String CASE_TYPE = "FinancialRemedyMVP2";
    private static final String CASE_ID = "123456789";
    private static final String JURISDICTION_ID = "divorce";
    private static final String USER_ID = "30";
    private static final String CREATE = "create";
    private static final String AUTH_TOKEN = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJubGJoN";
    private static final String EVENT_TOKEN = "Bearer aaaadsadsasawewewewew";
    private static final String EVENT_SUMMARY = "Migrate Case";
    private static final String EVENT_DESC = "Migrate Case";

    @InjectMocks
    private CcdUpdateServiceImpl underTest;

    @Mock
    CoreCaseDataApi coreCaseDataApi;

    @Mock
    private IdamUserService idamUserService;

    @Mock
    private AuthTokenGenerator authTokenGenerator;


    @Before
    public void setUp() {
        Field field = ReflectionUtils.findField(CcdUpdateServiceImpl.class, "jurisdictionId");
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, underTest, JURISDICTION_ID);


        Field caseTypeField = ReflectionUtils.findField(CcdUpdateServiceImpl.class, "caseType");
        ReflectionUtils.makeAccessible(caseTypeField);
        ReflectionUtils.setField(caseTypeField, underTest, CASE_TYPE);

        Field createEventIdField = ReflectionUtils.findField(CcdUpdateServiceImpl.class, "createEventId");
        ReflectionUtils.makeAccessible(createEventIdField);
        ReflectionUtils.setField(createEventIdField, underTest, CREATE);

    }

    @Test
    public void shouldUpdateTheCase() {
        // given
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("solicitorEmail", "Padmaja.Ramisetti@hmcts.net");
        data.put("solicitorName", "PADMAJA");
        data.put("solicitorReference", "LL02");
        data.put("applicantLName", "Mamidi");
        data.put("applicantFMName", "Prashanth");
        data.put("appRespondentFMName", "TestRespondant");

        UserDetails userDetails = UserDetails.builder()
                .id("30")
                .email("test@test.com")
                .forename("Test")
                .surname("Surname")
                .build();

        setupMocks(userDetails, data);

        //when
        CaseDetails update = underTest.update(CASE_ID, data, EVENT_ID, AUTH_TOKEN,
                EVENT_SUMMARY, EVENT_DESC);
        //then
        assertThat(update.getId(), is(Long.parseLong(CASE_ID)));
        assertThat(update.getData().get("solicitorEmail"), is("Padmaja.Ramisetti@hmcts.net"));
        assertThat(update.getData().get("solicitorName"), is("PADMAJA"));
        assertThat(update.getData().get("solicitorReference"), is("LL02"));
        assertThat(update.getData().get("applicantLName"), is("Mamidi"));
        assertThat(update.getData().get("applicantFMName"), is("Prashanth"));
        assertThat(update.getData().get("appRespondentFMName"), is("TestRespondant"));
    }

    private void setupMocks(UserDetails userDetails, LinkedHashMap<String, Object> data) {
        when(idamUserService.retrieveUserDetails(AUTH_TOKEN)).thenReturn(userDetails);

        when(authTokenGenerator.generate()).thenReturn(AUTH_TOKEN);

        StartEventResponse startEventResponse = StartEventResponse.builder()
                .eventId(EVENT_ID)
                .token(EVENT_TOKEN)
                .build();

        when(coreCaseDataApi.startEventForCaseWorker(AUTH_TOKEN, AUTH_TOKEN, "30",
                JURISDICTION_ID, CASE_TYPE, CASE_ID, EVENT_ID))
                .thenReturn(startEventResponse);

        CaseDataContent caseDataContent = CaseDataContent.builder()
                .event(Event.builder()
                        .id(EVENT_ID)
                        .description(EVENT_DESC)
                        .summary(EVENT_SUMMARY)
                        .build())
                .eventToken(EVENT_TOKEN)
                .data(data)
                .ignoreWarning(false)
                .build();

        CaseDetails caseDetails = CaseDetails.builder()
                .id(123456789L)
                .data(data)
                .build();
        when(coreCaseDataApi.submitEventForCaseWorker(AUTH_TOKEN, AUTH_TOKEN, USER_ID, JURISDICTION_ID,
                CASE_TYPE, CASE_ID, true, caseDataContent)).thenReturn(caseDetails);
    }
}