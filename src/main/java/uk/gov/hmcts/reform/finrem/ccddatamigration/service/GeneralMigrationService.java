package uk.gov.hmcts.reform.finrem.ccddatamigration.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.finrem.ccddatamigration.ccd.CcdUpdateService;

import java.util.List;
import java.util.Map;

import static org.springframework.util.StringUtils.isEmpty;


@Slf4j
@Component("generalMigrationService")
public class GeneralMigrationService implements MigrationService {
    private static final String EVENT_ID = "FR_migrateCase";

    @Getter
    private int totalMigrationsPerformed;

    @Getter
    private int totalNumberOfCases;

    @Autowired
    private CcdUpdateService ccdUpdateService;

    @Value("${idam.username}")
    private String idamUserName;

    @Value("${idam.userpassword}")
    private String idamUserPassword;

    @Value("${ccd.jurisdictionid}")
    private String jurisdictionId;

    @Value("${ccd.casetype}")
    private String caseType;

    @Override
    public List<CaseDetails> processData(List<CaseDetails> caseDetails) {
        for (CaseDetails caseDetail : caseDetails) {
            log.debug("Case Before Migration " + caseDetail.toString()
                    .replace(System.getProperty("line.separator"), " "));
            totalNumberOfCases++;
        }
        return caseDetails;
    }

    @Override
    public boolean accepts(CaseDetails caseDetails) {
        if (caseDetails == null || caseDetails.getData() == null) {
            return false;
        }
        Map<String, Object> data = caseDetails.getData();
        if (!isEmpty(data.get("solicitorAddress1"))) {
            return true;
        }
        return false;
    }

    @Override
    public void updateCase(String authorisation, CaseDetails cd) {
        String caseId = cd.getId().toString();
        Object data = cd.getData();
        log.info("data {}", data.toString());

        CaseDetails update = ccdUpdateService.update(
                caseId,
                data,
                EVENT_ID,
                authorisation,
                "Migrate Case",
                "Migrate Case"
        );
        totalMigrationsPerformed++;
    }
}
