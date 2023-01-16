![Deploy to dev and prod](https://github.com/navikt/syfohelsenettproxy/workflows/Deploy%20to%20dev%20and%20prod/badge.svg)

# Syfohelsenettproxy
Denne appen fungerer som et skjold helsenett hvor fæle apier med særnorske tegn regjerer. Målet er å tilby konkrete
REST-endepunkter som løser våre problemer, og holde ute datamodellene fra helsenett. 

## Teknologier
* Kotlin
* Ktor
* Gradle
* Kotest
* Jackson
* SOAP


## Flytkart
Dette er det eit oversiktsbilde av flyten i applikasjonen
```mermaid
  graph LR
  
      syfohelsenettproxy --- STS
      syfohelsenettproxy --- pep-gw    
      pep-gw --- nhn; 
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

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub

### For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #team-sykmelding