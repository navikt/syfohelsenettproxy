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


## Forutsetninger
Sørg for at du har Java JDK 21 installert
Du kan sjekke hvilken versjon du har installert, ved å bruke denne kommandoen:
``` bash
java -version
```

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

#### Api dokumtentasjon
https://syfohelsenettproxy.intern.dev.nav.no/openapi

## Henvendelser
Dette prosjeket er vedlikeholdt av [navikt/teamsykmelding](CODEOWNERS)

Spørsmål knyttet til koden eller prosjektet kan stilles som
[issues](https://github.com/navikt/syfohelsenettproxy/issues) her på GitHub

### For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen [#team-sykmelding](https://nav-it.slack.com/archives/CMA3XV997)
