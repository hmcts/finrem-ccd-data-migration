package uk.gov.hmcts.reform.finrem.ccddatamigration.s2s;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import static uk.gov.hmcts.reform.finrem.ccddatamigration.MigrationConstants.BEARER;

@SuppressWarnings("squid:S1118")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthUtil {

    public static String getBearToken(String token) {
        if (StringUtils.isBlank(token)) {
            return token;
        }

        return token.startsWith(BEARER) ? token : BEARER.concat(token);
    }
}

