package uk.gov.hmcts.reform.finrem.ccddatamigration.service;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.util.List;

public interface MigrationService {

    List<CaseDetails> processData(List<CaseDetails> caseDetails);

    boolean accepts(CaseDetails caseDetails);

    void updateCase(String authorisation, CaseDetails cd) throws Exception;

    int getTotalMigrationsPerformed();

    int getTotalNumberOfCases();

    void processSingleCase(String userToken, String s2sToken, String caseId);

    void updateOneCase(String authorisation, CaseDetails cd);

    void processAllTheCases(String userToken, String s2sToken, String userId);

    String getFailedCases();
}
