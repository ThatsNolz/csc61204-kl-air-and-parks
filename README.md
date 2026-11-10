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

Select a district and press **Refresh district** (or Ctrl+R). Overview displays the U.S. AQI, pollutant concentrations, model time and fetch time. **Compare all areas** refreshes the three pilot districts in sequence and shows their state in Monitoring. The trend plots distinct model hours from this session only, with an exact-value table next to it. A failed request shows the dated cache or explicitly says data is unavailable. Similar or equal readings can result from the same CAMS grid cell.

Advice is general guidance. Citizen reports stay on this device and are not sent to authorities. The report form blocks a second save while one is pending. Preferences and records live under `.kl-air-and-parks` in the user's home directory; keep it private. Alerts shown in the interface are session-only. The Data & impact tab explains provenance, SDG scope and keyboard controls. No application secrets or location permissions are required.

The standard build runs deterministic tests and fails below 75% JaCoCo line coverage. Test XML is under `target/surefire-reports`, and JaCoCo HTML/XML under `target/site/jacoco`. The test-only desktop harness uses a separate temporary store, makes one real request, then executes clearly labelled local HTTP fixtures. For Windows use a semicolon in the class path; on macOS/Linux use a colon.

```text
java -cp "target/kl-air-and-parks-1.0.0.jar;target/test-classes" cityair.DesktopEvidence evidence/gui
mvn -B -Pdocumented-limitation test
```

The last command intentionally fails one test. The current report limit uses Java UTF-16 units, so 500 supplementary Unicode characters do not behave like 500 ASCII characters. This profile is never used in ordinary CI. Keep the default verification and failure-run evidence separate.

If a live request fails, check HTTPS access to `air-quality-api.open-meteo.com`; a proxy or provider outage may prevent access. Run the build again before testing the packaged JAR if only `mvn test` has run. If Swing does not launch, check that a graphical desktop is available and that the local data folder is writable. A CI version probe checks the JAR entry point; it cannot validate GUI interaction.

Source follows MVC: `view` renders, `controller` coordinates Swing and I/O, `service` handles readings, `api` handles HTTP, and `model`, `strategy`, `observer` and `repository` keep independent responsibilities. See [requirements](REQUIREMENTS_MATRIX.md) and [design diagrams](diagrams/).
