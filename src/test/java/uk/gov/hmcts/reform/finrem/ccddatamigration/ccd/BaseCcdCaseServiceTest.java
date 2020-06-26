package uk.gov.hmcts.reform.finrem.ccddatamigration.ccd;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.finrem.ccddatamigration.domain.UserDetails;
import uk.gov.hmcts.reform.finrem.ccddatamigration.idam.IdamUserService;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class BaseCcdCaseServiceTest {

    @InjectMocks
    private BaseCcdCaseService underTest;

    private static final String AUTH_TOKEN = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJubGJoN";

    @Mock
    private IdamUserService idamUserService;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @Before
    public void setUp() {
        UserDetails userDetails = UserDetails.builder()
                .id("30")
                .email("test@test.com")
                .forename("Test")
                .surname("Surname")
                .build();

        when(idamUserService.retrieveUserDetails(AUTH_TOKEN)).thenReturn(userDetails);
    }

    @Test
    public void shouldReturnUserDetails() {

        UserDetails userDetails = underTest.getUserDetails(AUTH_TOKEN);
        assertThat(userDetails.getId(), Is.is("30"));
    }

    @Test
    public void shouldGetAuthToken() {
        String bearerUserToken = underTest.getBearerUserToken(AUTH_TOKEN);
        assertThat(bearerUserToken, is(AUTH_TOKEN));
    }

    @Test
    public void shouldGenerateAuthToken() {
        when(authTokenGenerator.generate()).thenReturn(AUTH_TOKEN);
        assertThat(underTest.getServiceAuthToken(), is(AUTH_TOKEN));
    }
}