package uk.gov.hmcts.reform.finrem.ccddatamigration.service;

public interface MigrationService {

    void processSingleCase(String userToken, String s2sToken, String caseId);

    void processAllTheCases(String userToken, String s2sToken, String userId, String jurisdictionId, String caseType);

    String getFailedCases();

    int getTotalMigrationsPerformed();

    int getTotalNumberOfCases();

    String getMigratedCases();
}
