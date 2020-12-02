package uk.gov.hmcts.reform.finrem.ccddatamigration.service;

import java.io.IOException;

public interface MigrationService {

    void processSingleCase(String userToken, String s2sToken, String caseId);

    void processAllTheCases(String userToken, String s2sToken, String userId, String jurisdictionId, String caseType);

    void processCasesInFile(String userToken, String s2sToken, String file) throws IOException;

    String getFailedCases();

    String getSkippedCases();

    int getTotalNumberOfSkips();

    int getTotalNumberOfFails();

    int getTotalMigrationsPerformed();

    int getTotalNumberOfCases();

    String getMigratedCases();
}
