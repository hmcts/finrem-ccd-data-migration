package uk.gov.hmcts.reform.finrem.ccddatamigration.service;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.util.List;

public interface MigrationService {

    List<CaseDetails> processData(List<CaseDetails> caseDetails);

    boolean accepts(CaseDetails caseDetails);

    void updateCase(String authorisation, CaseDetails cd) throws Exception;

    int getTotalMigrationsPerformed();

    int getTotalNumberOfCases();
}
