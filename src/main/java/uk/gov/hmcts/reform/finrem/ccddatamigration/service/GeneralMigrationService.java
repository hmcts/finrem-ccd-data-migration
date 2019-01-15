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

import static org.springframework.util.StringUtils.isEmpty;


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
            deleteRedundantFields(caseDetail, getRedundantFields());
            log.debug("Case After  Migration " + caseDetail.toString().replace(System.getProperty("line.separator"), " "));
            totalNumberOfCases++;
        }
        return caseDetails;
    }

    private List getRedundantFields() {
       String[] fields = {"solicitorAddress1" };
// , "solicitorAddress2", "solicitorAddress3", "solicitorAddress4",
//                "solicitorAddress5", "solicitorAddress6", "rSolicitorAddress1", "rSolicitorAddress2",
//                "rSolicitorAddress3", "rSolicitorAddress4", "rSolicitorAddress5", "rSolicitorAddress6",
//                "respondentAddress1", "respondentAddress2", "respondentAddress3", "respondentAddress4",
//                "respondentAddress5", "respondentAddress6"};
        return Arrays.asList(fields);
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
        log.info("data {}" , data.toString());

        CaseDetails update = ccdUpdateService.update(
                caseId,
                data,
                "FR_migrateCase",
                authorisation,
                "Migrate Case",
                "Migrate Case"
        );
    }
    private void deleteRedundantFields(CaseDetails caseDetails, List<String> keysList) {
        Map<String, Object> data = caseDetails.getData();
        data.remove("solicitorAddress1");
//        keysList.stream()
//                .filter(data::containsKey).
//                forEach(key -> data.put(key, "value changed"));
        caseDetails.setData(data);
        totalMigrationsPerformed++;
    }
}
