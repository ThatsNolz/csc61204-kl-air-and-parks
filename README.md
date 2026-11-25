# Kuala Lumpur Air and Parks

CSC61204 Scenario 5 desktop pilot. The application combines modeled district air-quality estimates, audience-specific advice, park listings, local pollution reports and session alerts. Readings are Open-Meteo / Copernicus CAMS Global modeled estimates for representative coordinates, not Malaysian official API readings or measurements at a park.

## Run and verify

Requires a JDK 17 or newer, Maven 3.9 or compatible, and a desktop session for Swing. The project uses Maven Central on the first build. No API key is needed for the public non-commercial Open-Meteo endpoint. Review [provider terms and documentation](https://open-meteo.com/en/docs/air-quality-api) before operating at scale.

```text
mvn -B clean verify
java -jar target/kl-air-and-parks-1.0.0.jar --version
java -jar target/kl-air-and-parks-1.0.0.jar
java -jar target/kl-air-and-parks-1.0.0.jar --live-check
```

Select a district and press **Refresh district** (or Ctrl+R). Overview places the U.S. AQI on the EPA colour scale and shows each pollutant against its WHO guideline level, advice for the chosen audience, plain-language insights, a verified nearby park, model time and fetch time; the *All pilot areas* strip switches district with one click. **Compare all areas** refreshes the three pilot districts in sequence and shows their state in Monitoring. The trend plots distinct model hours from this session only, with an exact-value table next to it. A failed request shows the dated cache or explicitly says data is unavailable. Similar or equal readings can result from the same CAMS grid cell.

Activity planner scores a planned outing (who is going out, how hard, and for how long) against the reading on screen, shows the arithmetic behind the verdict instead of only its conclusion, and applies all three advice strategies (general public, children, older adults) to the same reading so they can be compared. Advice is general guidance. Citizen reports stay on this device and are not sent to authorities; the history table can be sorted and filtered by area, and the form blocks a second save while one is pending. Alert centre has one notification switch per area and an alert threshold (U.S. AQI 1–500, default 101) that is saved on this device. Lowering the threshold before a refresh is a quick way to see the Observer alert during a live demonstration. Preferences and records live under `.kl-air-and-parks` in the user's home directory; keep it private. Alerts shown in the interface are session-only. The Data & impact tab explains provenance, SDG scope and keyboard controls, and Settings holds the theme, reduced motion and navigation preference. No application secrets or location permissions are required.

The standard build runs deterministic tests and fails below 75% JaCoCo line coverage. Test XML is under `target/surefire-reports`, and JaCoCo HTML/XML under `target/site/jacoco`. The test-only desktop harness uses a separate temporary store, makes one real request, then executes clearly labelled local HTTP fixtures. For Windows use a semicolon in the class path; on macOS/Linux use a colon.

```text
java -cp "target/kl-air-and-parks-1.0.0.jar;target/test-classes" cityair.DesktopEvidence evidence/gui
java -cp "target/kl-air-and-parks-1.0.0.jar;target/test-classes" cityair.DesktopEvidence evidence/gui --fixtures-only
mvn -B -Pdocumented-limitation test
```

`--fixtures-only` skips the live provider (useful offline or behind a proxy) and captures every screen against the labelled local HTTP fixture instead.

The last command intentionally fails one test. The current report limit uses Java UTF-16 units, so 500 supplementary Unicode characters do not behave like 500 ASCII characters. This profile is never used in ordinary CI. Keep the default verification and failure-run evidence separate.

If a live request fails, check HTTPS access to `air-quality-api.open-meteo.com`; a proxy or provider outage may prevent access. Run the build again before testing the packaged JAR if only `mvn test` has run. If Swing does not launch, check that a graphical desktop is available and that the local data folder is writable. A CI version probe checks the JAR entry point; it cannot validate GUI interaction.

Diagrams are generated from the code so they cannot drift: `java scripts/UmlExtract.java src/main/java > diagrams/members.tsv`, then `python3 diagrams/uml_gen.py diagrams/members.tsv diagrams` and `dot -Tsvg` plus `dot -Tpng -Gdpi=192` for each `.dot` file. `diagrams/architecture_svg.py` and `diagrams/sequence_svg.py` draw the architecture and refresh sequence. `java scripts/JavaQuality.java src/main/java` prints method length and complexity.

Source follows MVC: `view` renders (one pane per screen over shared `view.design` tokens, `view.component` controls and `view.chart` plots), `controller` coordinates Swing and I/O, `service` handles readings, `api` handles HTTP, and `model`, `strategy`, `observer` and `repository` keep independent responsibilities. See [requirements](REQUIREMENTS_MATRIX.md) and [design diagrams](diagrams/).
