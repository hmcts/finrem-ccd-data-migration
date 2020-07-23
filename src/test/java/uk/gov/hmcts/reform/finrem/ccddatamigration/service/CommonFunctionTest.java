package uk.gov.hmcts.reform.finrem.ccddatamigration.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.hmcts.reform.finrem.ccddatamigration.MigrationConstants.CASE_TYPE_ID_CONSENTED;
import static uk.gov.hmcts.reform.finrem.ccddatamigration.MigrationConstants.CASE_TYPE_ID_CONTESTED;
import static uk.gov.hmcts.reform.finrem.ccddatamigration.service.CommonFunction.isCaseInCorrectState;
import static uk.gov.hmcts.reform.finrem.ccddatamigration.service.CommonFunction.isConsentedCase;
import static uk.gov.hmcts.reform.finrem.ccddatamigration.service.CommonFunction.isContestedCase;

@RunWith(MockitoJUnitRunner.class)
public class CommonFunctionTest {

    @Test
    public void isConsentedCaseShouldReturnTrueWheCaseTypeIsSetToConsentedCaseType() {
        CaseDetails caseDetails = createCaseDetails(CASE_TYPE_ID_CONSENTED, "expectedState");

        assertThat(isConsentedCase(caseDetails), is(true));
    }

    @Test
    public void isConsentedCaseShouldReturnFalseWhenCaseTypeIsSetToContested() {
        CaseDetails caseDetails = createCaseDetails(CASE_TYPE_ID_CONTESTED, "expectedState");

        assertThat(isConsentedCase(caseDetails), is(false));
    }

    @Test
    public void isConsentedCaseShouldReturnFalseWhenCaseTypeIsNull() {
        CaseDetails caseDetails = createCaseDetails(null, "expectedState");

        assertThat(isConsentedCase(caseDetails), is(false));
    }

    @Test
    public void isContestedCaseShouldReturnTrueWheCaseTypeIsSetToContested() {
        CaseDetails caseDetails = createCaseDetails(CASE_TYPE_ID_CONTESTED, "expectedState");

        assertThat(isContestedCase(caseDetails), is(true));
    }

    @Test
    public void isContestedCaseShouldReturnFalseWheCaseTypeIsSetToConsented() {
        CaseDetails caseDetails = createCaseDetails(CASE_TYPE_ID_CONSENTED, "expectedState");

        assertThat(isContestedCase(caseDetails), is(false));
    }

    @Test
    public void isContestedCaseShouldReturnFalseWheCaseTypeIsSetToNull() {
        CaseDetails caseDetails = createCaseDetails(null, "expectedState");

        assertThat(isContestedCase(caseDetails), is(false));
    }

    @Test
    public void isInCorrectCaseStateShouldReturnTrueWhenCaseIsInExpectedState() {
        CaseDetails caseDetails = createCaseDetails(CASE_TYPE_ID_CONSENTED, "expectedState");

        assertThat(isCaseInCorrectState(caseDetails, "expectedState"), is(true));
    }

    @Test
    public void isInCorrectCaseStateShouldReturnFalseWhenCaseIsInUnexpectedState() {
        CaseDetails caseDetails = createCaseDetails(CASE_TYPE_ID_CONSENTED, "expectedState");

        assertThat(isCaseInCorrectState(caseDetails, "unexpectedState"), is(false));
    }

    @Test
    public void isInCorrectCaseStateShouldReturnFalseWhenCaseIsNull() {
        CaseDetails caseDetails = createCaseDetails(CASE_TYPE_ID_CONSENTED, null);

        assertThat(isCaseInCorrectState(caseDetails, "expectedState"), is(false));
    }

    @Test
    public void isInCorrectCaseStateShouldReturnTrueWhenCaseIsInEitherExpectedState() {
        CaseDetails caseDetails = createCaseDetails(CASE_TYPE_ID_CONSENTED, "expectedState");

        assertThat(isCaseInCorrectState(caseDetails, "expectedState", "redundantSecondState"), is(true));
    }

    @Test
    public void isInCorrectCaseStateShouldReturnTrueWhenCaseIsInOneExpectedStateAndOtherIsNull() {
        CaseDetails caseDetails = createCaseDetails(CASE_TYPE_ID_CONSENTED, "expectedState");

        assertThat(isCaseInCorrectState(caseDetails, "expectedState", null), is(true));
    }

    @Test
    public void isInCorrectCaseStateShouldReturnFalseWhenCaseIsInNeitherExpectedState() {
        CaseDetails caseDetails = createCaseDetails(CASE_TYPE_ID_CONSENTED, "expectedState");

        assertThat(isCaseInCorrectState(caseDetails, "unexpectedState", "unexpectedState2"), is(false));
    }

    private CaseDetails createCaseDetails(String caseType, String state) {
        Map<String, Object> caseData = new HashMap<>();

        return CaseDetails.builder()
            .id(1234123412341234L)
            .caseTypeId(caseType)
            .data(caseData)
            .state(state)
            .build();
    }
}