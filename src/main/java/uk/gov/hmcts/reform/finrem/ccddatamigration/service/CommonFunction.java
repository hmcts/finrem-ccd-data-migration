package uk.gov.hmcts.reform.finrem.ccddatamigration.service;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import static uk.gov.hmcts.reform.finrem.ccddatamigration.MigrationConstants.CASE_TYPE_ID_CONSENTED;
import static uk.gov.hmcts.reform.finrem.ccddatamigration.MigrationConstants.CASE_TYPE_ID_CONTESTED;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonFunction {

    public static boolean isContestedCase(CaseDetails caseDetails) {
        return caseDetails.getCaseTypeId().equals(CASE_TYPE_ID_CONTESTED);
    }

    public static boolean isConsentedCase(CaseDetails caseDetails) {
        return caseDetails.getCaseTypeId().equals(CASE_TYPE_ID_CONSENTED);
    }

    public static boolean isCaseInCorrectState(CaseDetails caseDetails, String expectedState) {

        return caseDetails.getState().equals(expectedState);
    }
}