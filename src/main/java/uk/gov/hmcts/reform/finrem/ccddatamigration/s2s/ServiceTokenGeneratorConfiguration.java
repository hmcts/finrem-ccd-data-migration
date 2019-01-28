package uk.gov.hmcts.reform.finrem.ccddatamigration.s2s;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import static uk.gov.hmcts.reform.authorisation.generators.AuthTokenGeneratorFactory.createDefaultGenerator;

@Configuration
@Lazy
@EnableFeignClients(basePackageClasses = ServiceAuthorisationApi.class)
public class ServiceTokenGeneratorConfiguration {

    @Bean
    public AuthTokenGenerator serviceAuthTokenGenerator(
            @Value("${idam.s2s-auth.totp_secret}") final String secret,
            @Value("${idam.s2s-auth.microservice}") final String microService,
            final ServiceAuthorisationApi serviceAuthorisationApi
    ) {

        return createDefaultGenerator(secret,
                        microService,
                        serviceAuthorisationApi);
    }

}
