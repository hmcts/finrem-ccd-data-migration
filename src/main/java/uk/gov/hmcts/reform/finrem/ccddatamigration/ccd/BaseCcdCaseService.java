package uk.gov.hmcts.reform.finrem.ccddatamigration.ccd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.finrem.ccddatamigration.domain.UserDetails;
import uk.gov.hmcts.reform.finrem.ccddatamigration.idam.IdamUserService;
import uk.gov.hmcts.reform.finrem.ccddatamigration.s2s.AuthUtil;


class BaseCcdCaseService {
    @Value("${ccd.jurisdictionid}")
    String jurisdictionId;

    @Value("${ccd.casetype}")
    String caseType;

    @Value("${ccd.eventid.create}")
    String createEventId;

    @Autowired
    CoreCaseDataApi coreCaseDataApi;

    @Autowired
    private IdamUserService idamUserService;

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    UserDetails getUserDetails(String userToken) {
        return idamUserService.retrieveUserDetails(getBearerUserToken(userToken));
    }

    String getBearerUserToken(String userToken) {
        return AuthUtil.getBearToken(userToken);
    }

    String getServiceAuthToken() {
        return authTokenGenerator.generate();
    }
}
