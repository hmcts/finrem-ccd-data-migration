package uk.gov.hmcts.reform.finrem.ccddatamigration.service;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import static uk.gov.hmcts.reform.finrem.ccddatamigration.MigrationConstants.CASE_TYPE_ID_CONSENTED;
import static uk.gov.hmcts.reform.finrem.ccddatamigration.MigrationConstants.CASE_TYPE_ID_CONTESTED;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonFunction {

    public static String nullToEmpty(Object o) {
        return o == null ? StringUtils.EMPTY : o.toString();
    }

    public static boolean isContestedCase(CaseDetails caseDetails) {

        return CASE_TYPE_ID_CONTESTED.equalsIgnoreCase(nullToEmpty(caseDetails.getCaseTypeId()));
    }

    public static boolean isConsentedCase(CaseDetails caseDetails) {

        return CASE_TYPE_ID_CONSENTED.equalsIgnoreCase(nullToEmpty(caseDetails.getCaseTypeId()));
    }

    public static boolean isCaseInCorrectState(CaseDetails caseDetails, String expectedState) {

        return expectedState.equalsIgnoreCase(nullToEmpty(caseDetails.getState()));
    }
}