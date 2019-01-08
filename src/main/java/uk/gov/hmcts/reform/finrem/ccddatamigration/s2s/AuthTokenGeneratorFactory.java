package uk.gov.hmcts.reform.finrem.ccddatamigration.s2s;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.authorisation.generators.AutorefreshingJwtAuthTokenGenerator;
import uk.gov.hmcts.reform.authorisation.generators.BearerTokenGenerator;
import uk.gov.hmcts.reform.authorisation.generators.ServiceAuthTokenGenerator;

import java.time.Duration;

public class AuthTokenGeneratorFactory {

    public static AuthTokenGenerator createDefaultGenerator(
            String secret,
            String microService,
            ServiceAuthorisationApi serviceAuthorisationApi
    ) {
        return new BearerTokenGenerator(
                new AutorefreshingJwtAuthTokenGenerator(
                        new ServiceAuthTokenGenerator(secret, microService, serviceAuthorisationApi)
                )
        );
    }

    public static AuthTokenGenerator createDefaultGenerator(
            String secret,
            String microService,
            ServiceAuthorisationApi serviceAuthorisationApi,
            Duration refreshTimeDelta
    ) {
        return new BearerTokenGenerator(
                new AutorefreshingJwtAuthTokenGenerator(
                        new ServiceAuthTokenGenerator(secret, microService, serviceAuthorisationApi),
                        refreshTimeDelta
                )
        );
    }

    private AuthTokenGeneratorFactory() {
        // Static factory class
    }

    @Configuration
    @Lazy
    @EnableFeignClients(basePackageClasses = ServiceAuthorisationApi.class)
    public static class ServiceTokenGeneratorConfiguration {

        @Bean
        public AuthTokenGenerator serviceAuthTokenGenerator(
                @Value("${idam.s2s-auth.totp_secret}") final String secret,
                @Value("${idam.s2s-auth.microservice}") final String microService,
                final ServiceAuthorisationApi serviceAuthorisationApi
        ) {

            return uk.gov.hmcts.reform.authorisation
                    .generators
                    .AuthTokenGeneratorFactory
                    .createDefaultGenerator(secret,
                            microService,
                            serviceAuthorisationApi);
        }

    }
}
