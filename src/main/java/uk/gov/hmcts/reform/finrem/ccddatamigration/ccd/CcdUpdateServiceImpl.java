package uk.gov.hmcts.reform.finrem.ccddatamigration.ccd;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.finrem.ccddatamigration.domain.UserDetails;

@Service
public class CcdUpdateServiceImpl extends BaseCcdCaseService implements CcdUpdateService {

    @Override
    public CaseDetails update(String caseId, Object data, String eventId, String authorisation,
                              String eventSummary, String eventDescription) {
        UserDetails userDetails = getUserDetails(authorisation);

        StartEventResponse startEventResponse = coreCaseDataApi.startEventForCaseWorker(
                getBearerUserToken(authorisation),
                getServiceAuthToken(),
                userDetails.getId(),
                jurisdictionId,
                caseType,
                caseId,
                eventId);

        CaseDataContent caseDataContent = CaseDataContent.builder()
                .eventToken(startEventResponse.getToken())
                .event(
                        Event.builder()
                                .id(startEventResponse.getEventId())
                                .summary(eventSummary)
                                .description(eventDescription)
                                .build()
                ).data(data)
                .build();

        return coreCaseDataApi.submitEventForCaseWorker(
                getBearerUserToken(authorisation),
                getServiceAuthToken(),
                userDetails.getId(),
                jurisdictionId,
                caseType,
                caseId,
                true,
                caseDataContent);
    }
}
