package uk.gov.hmcts.reform.finrem.ccddatamigration.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.finrem.ccddatamigration.ccd.CcdUpdateService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@Component("generalMigrationService")
public class GeneralMigrationService implements MigrationService {
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

            log.debug("Case Before Migration " + caseDetail.toString().replace(System.getProperty("line.separator"), " "));

           //TODO: need to implement the logic for data migration.
            log.debug("Case After  Migration " + caseDetail.toString().replace(System.getProperty("line.separator"), " "));
            totalNumberOfCases++;
        }

        return caseDetails;
    }

    @Override
    public boolean accepts(CaseDetails caseDetails) {
        if(caseDetails == null || caseDetails.getData() == null){
            return false;
        }
        return true;
    }

    @Override
    public void updateCase(String authorisation, CaseDetails cd) {
        String caseId = cd.getId().toString();
        Object data = cd.getData();

        CaseDetails update = ccdUpdateService.update(
                caseId,
                data,
                "eventId",
                authorisation,
                "",
                ""
        );
    }
}
