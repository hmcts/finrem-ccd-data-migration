package uk.gov.hmcts.reform.finrem.ccddatamigration.service;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.util.List;

public interface MigrationService {

    boolean accepts(CaseDetails caseDetails);

    void processSingleCase(String userToken, String s2sToken, String caseId);

    void processAllTheCases(String userToken, String s2sToken, String userId, String jurisdictionId,
                            String caseType, boolean dryRun);
    String getFailedCases();

    int getTotalMigrationsPerformed();

    int getTotalNumberOfCases();
}
