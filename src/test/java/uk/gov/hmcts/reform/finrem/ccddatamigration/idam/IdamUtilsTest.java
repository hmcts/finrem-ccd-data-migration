package uk.gov.hmcts.reform.finrem.ccddatamigration.idam;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class IdamUtilsTest {

    private static final String IDAM_CODE_URL = "/oauth2/authorize?response_type=code&"
        + "client_id=finrem&redirect_uri=callback";
    private static final String IDAM_ACCESS_TOKEN_URL = "/oauth2/token?code=y4Kjxhxwr48n7Jca&client_id=finrem"
        + "&client_secret=AAAAAAAAAAAAAAAA&redirect_uri=callback&grant_type=authorization_code";

    @InjectMocks
    private IdamUtils underTest;

    @ClassRule
    public static final WireMockRule mockIdamServer = new WireMockRule(8080);

    @Before
    public void setUp() {
        Field field = ReflectionUtils.findField(IdamUtils.class, "idamUserBaseUrl");
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, underTest, "http://localhost:8080");

        Field field1 = ReflectionUtils.findField(IdamUtils.class, "idamRedirectUri");
        ReflectionUtils.makeAccessible(field1);
        ReflectionUtils.setField(field1, underTest, "callback");

        Field field2 = ReflectionUtils.findField(IdamUtils.class, "idamSecret");
        ReflectionUtils.makeAccessible(field2);
        ReflectionUtils.setField(field2, underTest, "AAAAAAAAAAAAAAAA");

        Field field3 = ReflectionUtils.findField(IdamUtils.class, "clientId");
        ReflectionUtils.makeAccessible(field3);
        ReflectionUtils.setField(field3, underTest, "finrem");
    }

    @Test
    public void shouldGenerateUserTokenWithNoRoles() {
        mockIdamServer.stubFor(post(urlEqualTo(IDAM_CODE_URL))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n"
                            + "    \"code\": \"y4Kjxhxwr48n7Jca\"\n"
                            + "}")));
        mockIdamServer.stubFor(post(urlEqualTo(IDAM_ACCESS_TOKEN_URL))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n"
                            + "    \"access_token\": \"aaaasssswwwww\"\n"
                            + "}")));

        String token = underTest.generateUserTokenWithNoRoles("test@test.com", "testPassword");

        assertThat(token, is("Bearer aaaasssswwwww"));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowIllegalStateException() {
        mockIdamServer.stubFor(post(urlEqualTo(IDAM_CODE_URL))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withBody("{\n"
                            + "    \"error\": \"Could not able to generate token\"\n"
                            + "}")));
        underTest.generateUserTokenWithNoRoles("test@test.com", "testPassword");
    }
}