# Syfohelsenettproxy
This app acts as a shield for the *Helsenett* (Norwegian health network),
where nasty APIs—riddled with unique Norwegian characters—reign supreme. The goal is to provide concrete REST endpoints that solve our problems, while keeping the *Helsenett* data models at bay.

## Technologies used
* Kotlin
* Ktor
* Gradle
* Junit
* Jackson
* SOAP
* Docker


## Requirements for developing the application
#### Java
Make sure you have the Java JDK 25 installed
You can check which version you have installed using this command:
``` bash
java -version
```
or install it: https://sdkman.io/

#### Docker
Make sure you have docker installed using this command:
You can check which version you have installed using this command:
``` bash
docker --version
```
or install it: https://docs.docker.com/engine/install/


## Flowchart
This is an overview of the flow in the application.
```mermaid
  flowchart LR
      syfohelsenettproxy <-- API --> random-application
      syfohelsenettproxy <---> azure-ad
      syfohelsenettproxy <--> valkey
      syfohelsenettproxy <--> nhn;
 ```


### Building the application
To build the application locally and run tests you can simply run
``` bash
./gradlew clean build
 ```
or on Windows
``` bash
gradlew.bat clean build
 ```

### Upgrading the Gradle wrapper version to latest
``` bash
./gradlew :wrapper --gradle-version latest
```

#### API documentation
https://syfohelsenettproxy.intern.dev.nav.no/openapi

#### NHN’s documentation of the SOAP interface
 - Flrexport: https://register-web.test.nhn.no/docs/api/NHN.DtoContracts.Flr.Service.IFlrExportOperations.html
 - HPR: https://register-web.test.nhn.no/docs/api/NHN.Palantir.WebServices.Contracts.HPRv2.Service.IHPR2Service.html

## Contributing

To get started, fork the repository and create a new branch.

See details in [CONTRIBUTING.md](CONTRIBUTING.md)


### Contact

This project is maintained by [navikt/teamsykmelding](CODEOWNERS)

Questions and/or feature requests? Please create an [issue](https://github.com/navikt/syfohelsenettproxy/issues)

If you work in [@navikt](https://github.com/navikt) you can reach us at the Slack
channel [#team-sykmelding](https://nav-it.slack.com/archives/CMA3XV997)