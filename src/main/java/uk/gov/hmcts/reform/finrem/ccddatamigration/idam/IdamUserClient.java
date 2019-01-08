package uk.gov.hmcts.reform.finrem.ccddatamigration.idam;

public interface IdamUserClient {

    public String generateUserTokenWithNoRoles(String username, String password);

}
