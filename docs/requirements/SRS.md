# Software requirements specification

**Problem.** A Kuala Lumpur resident planning outdoor activity has to connect model or official air-quality information to the intended outing. The pilot combines timestamped model data and named public parks with audience-specific guidance and a local reporting form. The pilot is not a city deployment.

**Users and operators.** Residents and caregivers check district conditions; a local public-space team is a proposed operator; park operators and the Department of Environment are reference stakeholders, not project partners. City Centre, Titiwangsa and Perdana are representative pilot selections, not a claim about formal district boundaries.

| Requirement | User scenario and acceptance rule |
|---|---|
| FR-001 | A resident selects a district and refreshes. A valid response yields source, district, model and fetch times, U.S. AQI and available pollutant concentrations. District history is bounded to recent session samples. |
| FR-002 | A resident selects an audience. The display uses the bound Strategy and the currently available AirCategory; a missing reading does not generate context-specific advice. |
| FR-003 | A subscriber receives an alert on the first qualifying sample or on a fresh upward crossing of U.S. AQI 101. Repeated model time, older data and sustained exceedances do not retrigger. Opt-out stops delivery; history is session-only. |
| FR-004 | A resident sees verified parks associated with the selected pilot district. District model context must not be described as park-level sensing. |
| FR-005 | A resident supplies district, issue category and nonempty description. A valid record is stored on the local device and reloaded. An invalid input produces a friendly error and is not persisted. |
| FR-006 | The user can compare the latest available district readings, view recent distinct samples and select local preferences. |
| NFR-001 | A request has a bounded timeout. A failed fetch yields a correctly dated cache or an unavailable state. Missing optional pollutant values are labelled. |
| NFR-002 | Refresh and disk activity run away from the EDT. UI updates run on the EDT. Unavailable data and form validation never show a raw stack trace. |
| NFR-003 | Controller depends on transport and persistence via the relevant service boundaries. Named domain thresholds, public API documentation and measurable review evidence are maintained. |
| NFR-004 | Maven verify runs JUnit 5 and JaCoCo with the official minimum coverage. A separate failing profile remains opt-in. |
| NFR-005 | Verify all five Template 1 paths using evidence from real app execution; distinguish real internet calls from local fixtures. |

**Validation and boundaries.** The AQI must be present and valid; time must parse; a pollutant may be missing but cannot be negative. Reports include district, category, description and local timestamp, with no account, identifying fields or telemetry. Users should omit personal details in free text. There is no report submission to a government agency, background push, official warning, exact exposure estimate, health diagnosis or centrally hosted multi-user system. A running Java desktop and Internet access are assumed for live estimates.

**SDG acceptance proxies.** For 11.6, users should identify AQI source and time and interpret the right category in a timed pilot task. For 11.7, users should identify a verified park listing and explain the difference between district context and site accessibility. These are proposed pilot measures, not observed public impact. Component, test, evidence and assessment mappings are in the root matrix.
