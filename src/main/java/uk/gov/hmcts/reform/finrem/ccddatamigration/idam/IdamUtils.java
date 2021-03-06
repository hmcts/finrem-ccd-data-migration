package uk.gov.hmcts.reform.finrem.ccddatamigration.idam;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;

import static uk.gov.hmcts.reform.finrem.ccddatamigration.MigrationConstants.AUTHORIZATION_HEADER;

@Component
public class IdamUtils implements IdamUserClient {

    @Value("${idam.api.url}")
    private String idamUserBaseUrl;

    @Value("${idam.whitelist.url}")
    private String idamRedirectUri;


    @Value("${idam.api.secret}")
    private String idamSecret;

    @Value("${client.id}")
    private String clientId;

    public String generateUserTokenWithNoRoles(String username, String password) {
        String userLoginDetails = String.join(":", username, password);
        final String authHeader = "Basic " + new String(Base64.getEncoder().encode((userLoginDetails).getBytes()));

        Response response = RestAssured.given()
                .header(AUTHORIZATION_HEADER, authHeader)
                .relaxedHTTPSValidation()
                .post(idamCodeUrl());
        if (response.getStatusCode() >= 300) {
            throw new IllegalStateException("Token generation failed with code: " + response.getStatusCode()
                    + " body: " + response.getBody().prettyPrint());
        }

        response = RestAssured.given()
                .relaxedHTTPSValidation()
                .post(idamTokenUrl(response.getBody().path("code")));

        String token = response.getBody().path("access_token");
        return "Bearer " + token;
    }

    private String idamCodeUrl() {
        return idamUserBaseUrl + "/oauth2/authorize"
                + "?response_type=code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + idamRedirectUri;
    }

    private String idamTokenUrl(String code) {
        return idamUserBaseUrl + "/oauth2/token"
                + "?code=" + code
                + "&client_id=" + clientId
                + "&client_secret=" + idamSecret
                + "&redirect_uri=" + idamRedirectUri
                + "&grant_type=authorization_code";
    }
}
