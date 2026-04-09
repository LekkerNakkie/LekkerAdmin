# LekkerAdmin — bug/probleemscan (volledige codebase)

## Kritiek / functionele bugs

1. **`linklookup` feature lijkt niet bereikbaar vanuit het hoofdcommand.**
   - In `LekkerAdminCommand` staan alleen subcommands zoals `help`, `reload`, `invsee`, `enderchest`, `planrestart`, `cancelrestart`, `maintenance`.
   - Er is geen `linklookup` branch in de switch.
   - Gevolg: permissie bestaat, maar command-flow is niet gekoppeld.

2. **Package-naamfout in `LinkLookupSubCommand`.**
   - Bestand staat onder `command/subcommands`, maar package is `me.lekkernakkie.lekkeradmin.command.sub`.
   - Gevolg: inconsistentie, verwarrende imports, grotere kans op onderhouds- en refactorfouten.

3. **Meerdere subcommand-klassen lijken dood code (niet ge-ïnstantieerd).**
   - `HelpSubCommand`, `ReloadSubCommand`, `WhitelistSubCommand`, `PunishmentsSubCommand`, `LinkLookupSubCommand` komen wel voor als klassen, maar worden niet aangeroepen vanuit de hoofdcommand-handler.
   - Gevolg: code divergence (gedrag in klasses ≠ gedrag in live command-pad).

## Hoge prioriteit / reliability

4. **Offline invsee helper is nog placeholder/TODO.**
   - `load()` geeft altijd een lege inventory terug.
   - `save()` bevat een TODO en doet niks.
   - Gevolg: risico op dataverlies/vals gevoel van succesvolle offline edit, tenzij elders volledig omzeild.

5. **Main-thread dispatch via `FutureTask#get()` kan blijven hangen bij scheduler-problemen.**
   - `MinecraftWhitelistService#callSync` blokkeert achtergrondthread met `task.get()` zonder timeout.
   - Gevolg: potentieel hangende worker bij shutdown/race-condities.

## Middel / onderhoudbaarheid

6. **Command-architectuur is dubbel: deels inline in `LekkerAdminCommand`, deels aparte SubCommand-klassen.**
   - Dit veroorzaakt inconsistentie en maakt regressies waarschijnlijker.

7. **Configuratie/permissies suggereren features die niet in command-dispatch zitten.**
   - Voorbeeld: `lekkeradmin.linklookup` bestaat in `plugin.yml`, maar er is geen actieve dispatch route in `LekkerAdminCommand`.

8. **Build-verificatie in deze omgeving is niet reproduceerbaar door externe dependency-resolutie (Maven 403).**
   - Daardoor is compile/test-signaal nu niet betrouwbaar vanuit CI-achtige sandbox run.

## Aanbevolen volgorde van fixen

1. Herstel command-dispatch + package-naam van `LinkLookupSubCommand`.
2. Verwijder of koppel dode subcommands.
3. Maak offline-invsee gedrag expliciet (read-only of volledig implementeren).
4. Voeg timeout/fallback toe rond `task.get()` in `callSync`.
5. Voeg minimaal smoke-tests toe voor command-routing en permissies.
