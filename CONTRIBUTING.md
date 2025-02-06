# Bidrag
Dette prosjektet er åpent for å akseptere funksjonsforespørsler og bidrag fra åpen kildekode-fellesskapet.
Vennligst fork repoet og start en ny brach å jobbe med.


## Bygge lokalt
Dette prosjektet bruker [Gradle](https://gradle.org/) som byggeverktøy.
En Gradle Wrapper er inkludert i koden, så du trenger ikke å administrere din egen installasjon.
For å kjøre et bygg, utfør ganske enkelt følgende:

``` bash
./gradlew clean build
```

Dette vil kjøre alle trinnene som er definert i `build.gradle.kts` filen.

## Testing
Hvis du legger til en ny funksjon eller feilretting, sørg for at det er riktig testdekning.

## Pull Request
Hvis du har en branch på ein forken som er klar til å slås merged, 
vennligst opprett en ny pull-request. Vedlikeholdsansvarlige vil gjennomgå for å sikre at retningslinjene ovenfor er fulgt, 
og hvis endringene er nyttige for alle bibliotekbrukere, vil de bli merged.