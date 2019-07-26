package uk.gov.hmcts.reform.finrem.ccddatamigration.ccd;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

public interface CcdUpdateService {
    CaseDetails update(String caseId, Object data, String eventId, String authorisation,
                       String eventSummary, String eventDescription, String caseType);
}
