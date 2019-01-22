package uk.gov.hmcts.reform.finrem.ccddatamigration.idam;

public interface IdamUserClient {

    String generateUserTokenWithNoRoles(String username, String password);

}
