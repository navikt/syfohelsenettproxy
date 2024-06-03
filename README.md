# Syfohelsenettproxy
Denne appen fungerer som et skjold helsenett hvor fæle apier med særnorske tegn regjerer. Målet er å tilby konkrete
REST-endepunkter som løser våre problemer, og holde ute datamodellene fra helsenett. 

## Teknologier
* Kotlin
* Ktor
* Gradle
* Junit
* Jackson
* SOAP
* Docker


## Flytkart
Dette er det eit oversiktsbilde av flyten i applikasjonen
```mermaid
  flowchart LR
  
      syfohelsenettproxy <---> azure-ad
      syfohelsenettproxy <--> redis
      syfohelsenettproxy <--> nhn;
 ```

## Bygg & Deploy
På grunn av særnorske tegn i klassenavn / encoding i WSDLen som vi baserer oss på bygger ikke dette prosjektet ut av 
boksen på Windows. Det er en kjent feil, men vi har ikke funnet en god løsning på det enda. Appen bygges og deployes fra GHA


### Hente github-package-registry pakker NAV-IT
Noen pakker som brukes i denne repoen lastes opp til GitHub Package Registry som krever autentisering. 
Det kan for eksempel løses slik i Gradle:
```
val githubUser: String by project
val githubPassword: String by project
repositories {
    maven {
        credentials {
            username = githubUser
            password = githubPassword
        }
        setUrl("https://maven.pkg.github.com/navikt/syfosm-common")
    }
}
```

`githubUser` og `githubPassword` kan legges inn i en egen fil `~/.gradle/gradle.properties` med følgende innhold:

```                                                     
githubUser=x-access-token
githubPassword=[token]
```

Erstatt `[token]` med en personal access token med omfang `read:packages`.
Se GitHubs guide [creating-a-personal-access-token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token) på
hvordan lage et personlig tilgangstoken.

Alternativt kan variablene konfigureres via miljøvariabler:
* `ORG_GRADLE_PROJECT_githubUser`
* `ORG_GRADLE_PROJECT_githubPassword`

eller kommandolinjen:

``` bash
./gradlew -PgithubUser=x-access-token -PgithubPassword=[token]
```

#### Gradle kommandoer for bygg og test
For å bygge lokalt og kjøre integrasjonstestene kan du ganske enkelt kjøre 
``` bash
./gradlew clean build
```
eller på Windows
`gradlew.bat clean build`

## Oppgradering av gradle wrapper
Finn nyeste versjon av gradle her: https://gradle.org/releases/

``` bash
./gradlew wrapper --gradle-version $gradleVersjon
```

#### Api doc
https://syfohelsenettproxy.intern.dev.nav.no/openapi


## Henvendelser
Dette prosjeket er vedlikeholdt av [navikt/teamsykmelding](CODEOWNERS)

Spørsmål knyttet til koden eller prosjektet kan stilles som
[issues](https://github.com/navikt/syfohelsenettproxy/issues) her på GitHub

### For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen [#team-sykmelding](https://nav-it.slack.com/archives/CMA3XV997)
