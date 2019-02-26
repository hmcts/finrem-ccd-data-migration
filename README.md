# Finrem Application to migrate data within CCD. 



## Getting started

This purpose of this application is to migrate data between ccd fields in aat env. This application produce a jar that is possible to run by command line.

### Prerequisites

- [JDK 8](https://www.oracle.com/java)

### Building

The project uses [Gradle](https://gradle.org) as a build tool but you don't have to install it locally since there is a
`./gradlew` wrapper script.

To build project please execute the following command:

```bash
    ./gradlew build
```

** Make sure you are connected to VPN and No Proxy active, as the application has the in-built to set the proxy.

proxyout.reform.hmcts.net:8080

- To access to aat, the proxy must be set on http / https need to proxyout.reform.hmcts.net:8080 which is in CCDDataMigrationApplication.java


### Running

- A Case Work user for aat is required

in ./build/libs locate finrem-1.0.0-SNAPSHOT.jar and run it with this commande line

** Ensure all the local dockers are running and healthy.

In Dryrun mode(default mode) for local:

`java -jar ./build/libs/finrem-ccd-data-migration-1.0.0-SNAPSHOT.jar --DRYRUN=true`

In Dryrun mode(default mode) for demo:

`java -jar ./build/libs/finrem-ccd-data-migration-1.0.0-SNAPSHOT.jar --SPRING_PROFILE=demo --OAUTH2_CLIENT_FINREM=<idam_secret> --TOTP_SECRET=<<s2s_secret>> --USER_NAME=<<username>> --PASSWORD=<<password>> --DRYRUN=true`

In Complete mode (demo):

`java -jar ./build/libs/finrem-ccd-data-migration-1.0.0-SNAPSHOT.jar --SPRING_PROFILE=demo --OAUTH2_CLIENT_FINREM=<idam_secret> --TOTP_SECRET=<<s2s_secret>> --USER_NAME=<<username>> --PASSWORD=<<password>> --DRYRUN=false`

To run for a single case:

`java -jar ./build/libs/finrem-ccd-data-migration-1.0.0-SNAPSHOT.jar --SPRING_PROFILE=demo --OAUTH2_CLIENT_FINREM=<<idam_secret>> --TOTP_SECRET=<<s2s_secret>> --USER_NAME=<<username>> --PASSWORD=<<password>> --DRYRUN=true --CASEID=<<CaseId>>`


If you want to customize connection time out and socket timeout, append the below to the command

```--HTTP_READ_TIMEOUT=100 -Dhttp.connect.request.timeout=60000```

To enable debug mode,

``` --DEBUG=true ```

WARNING: THIS WILL UPDATE ALL THE FR CASES IN CCD IN A FEW MINUTES!!!

### How to twist the application

The majority of the twist could be obtain by hacking one of these files:
 
- DataMigrationProcessor.java
- GeneralMigrationService.java
- application.properties



##  License
```The MIT License (MIT)

Copyright (c) 2018 HMCTS (HM Courts & Tribunals Service)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```
