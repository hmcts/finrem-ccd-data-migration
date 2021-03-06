# Financial Remedy Application to migrate data within CCD. 

## Getting started

This purpose of this application is to migrate data between CCD fields. This application produces a jar that is possible to run by command line.

### Prerequisites

- [JDK 11](https://www.oracle.com/java)
- Caseworker credentials for relevant environment
- OAUTH2_CLIENT_FINREM secret value
- TOTP_SECRET secret value

### Building

The project uses [Gradle](https://gradle.org) as a build tool but you don't have to install it locally since there is a `./gradlew` wrapper script.

To build project please execute the following command:

```bash
    ./gradlew build
```

** Make sure you are connected to VPN and no system proxy active; as this application has the proxy configured already(proxyout.reform.hmcts.net:8080)

- To access AAT, the proxy must be set on http / https need to proxyout.reform.hmcts.net:8080 which is in CCDDataMigrationApplication.java

### Running

After building the project, you can run the JAR file (finrem-1.0.0-SNAPSHOT.jar in ./build/libs) using the following commands:

You should run the migration in 'dry run' mode first - this will iterate over all relevant cases but not actually modify any date - this will ensure your migration logic has no issues.

- "OAUTH2_CLIENT_FINREM" = ‘idam-secret’ in FR Azure Key Vault
- "TOTP_SECRET" = ‘microservicekey-finrem-ccd-data-migrator’ in s2s Azure Key Vault
- "USER_NAME" = valid caseworker username for given environment
- "PASSWORD" = relevant caseworker password for given environment
- "FILE" = location to a CSV file that contains a list of Case ID's (See src/test/resources/csvExamples/cases.csv for example format)
- "PROXY_ENABLE" defaults to true to run smoothly across environments AAT/DEMO/PROD. You may set to false if you want to run against your local docker
- "MIGRATION_SPECIFICEVENT" to set the specific event name to run for migration

Using the "FILE" parameter will default to running the `migration.specificEvent` set on the `application.properties` configuration file.
This event calls the COS callback URL configured in definition file (if no `migration.specificEvent` is provided, the default event 
`FR_migrateCase` event hits the callback URL `/migrate`). If you want to change this to hit a particular event, you'll have to either 
set `migration.specificEvent` variable in `application.properties`file and rebuild the jar file or overrride the `MIGRATION_SPECIFICEVENT` 
environment variable to the specific event you want to run. 

Local dry run:
`java -jar ./build/libs/finrem-ccd-data-migration-1.0.0-SNAPSHOT.jar --DRYRUN=true`

AAT dry run:
`java -jar ./build/libs/finrem-ccd-data-migration-1.0.0-SNAPSHOT.jar --SPRING_PROFILE=aat --OAUTH2_CLIENT_FINREM=<idam_secret> --TOTP_SECRET=<<s2s_secret>> --USER_NAME=<<username>> --PASSWORD=<<password>> --DRYRUN=true`

Demo dry run:
`java -jar ./build/libs/finrem-ccd-data-migration-1.0.0-SNAPSHOT.jar --SPRING_PROFILE=demo --OAUTH2_CLIENT_FINREM=<idam_secret> --TOTP_SECRET=<<s2s_secret>> --USER_NAME=<<username>> --PASSWORD=<<password>> --DRYRUN=true`

Production dry run:
`java -jar finrem-ccd-data-migration-1.0.0-SNAPSHOT.jar --SPRING_PROFILE=prod --OAUTH2_CLIENT_FINREM=secret --TOTP_SECRET=secret --USER_NAME=username --PASSWORD=userpass --DRYRUN=true`


####The following statements are for fully running the migration against the relevant environment - simply insert the relevant credentials

AAT actual migration:
`java -jar ./build/libs/finrem-ccd-data-migration-1.0.0-SNAPSHOT.jar --SPRING_PROFILE=aat --OAUTH2_CLIENT_FINREM=<idam_secret> --TOTP_SECRET=<<s2s_secret>> --USER_NAME=<<username>> --PASSWORD=<<password>> --DRYRUN=false`

Demo actual migration:
`java -jar ./build/libs/finrem-ccd-data-migration-1.0.0-SNAPSHOT.jar --SPRING_PROFILE=demo --OAUTH2_CLIENT_FINREM=<idam_secret> --TOTP_SECRET=<<s2s_secret>> --USER_NAME=<<username>> --PASSWORD=<<password>> --DRYRUN=false`

Production actual migration (to only be done by DevOps):
`java -jar finrem-ccd-data-migration-1.0.0-SNAPSHOT.jar --SPRING_PROFILE=prod --OAUTH2_CLIENT_FINREM=<idam_secret> --TOTP_SECRET=<<s2s_secret>> --USER_NAME=<<username>> --PASSWORD=<<password>> --DRYRUN=false`

Adding additional parameters:

Our migration application allows us to add various parameters to adjust the migration.

It's possible to migrate a single case to validate your migration will work as expected by adding the "--CASEID=<<CaseId>>" parameter'. For example on AAT:
`java -jar ./build/libs/finrem-ccd-data-migration-1.0.0-SNAPSHOT.jar --SPRING_PROFILE=aat --OAUTH2_CLIENT_FINREM=<<idam_secret>> --TOTP_SECRET=<<s2s_secret>> --USER_NAME=<<username>> --PASSWORD=<<password>> --DRYRUN=true --CASEID=<<CaseId>>`

If you want to customize connection time out and socket timeout, append the following parameters to the command:
```--HTTP_READ_TIMEOUT=100 -Dhttp.connect.request.timeout=60000```

To enable debug mode:
``` --DEBUG=true ```

### How to adjust the application for your migration

The majority of customisation can be achieved by modifying the following files:
 
- DataMigrationProcessor.java
- GeneralMigrationService.java
- application.properties

Make sure to make use of common or previously used methods inside CommonFunction.java

This app can also be used to run a specific event for multiple cases in CCD rather than doing a specific data migration.
For example, if you need to re-run the sending of letters to multiple users, you can use this app to trigger the 'Send Order' event; simply replace 'EVENT_ID' inside GeneralMigrationService.java (just be sure to replace once done).  

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.