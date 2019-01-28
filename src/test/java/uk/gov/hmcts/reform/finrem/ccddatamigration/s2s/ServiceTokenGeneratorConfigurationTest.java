package uk.gov.hmcts.reform.finrem.ccddatamigration.s2s;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.finrem.ccddatamigration.CcdDataMigrationApplication;

import static org.junit.Assert.assertNotNull;


@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {CcdDataMigrationApplication.class})
@TestPropertySource("classpath:application.properties")
public class ServiceTokenGeneratorConfigurationTest {

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Test
    public void shouldReturnServiceAuthTokenGenerator() {
        assertNotNull(authTokenGenerator);
        assertNotNull(authTokenGenerator.generate());
    }
}