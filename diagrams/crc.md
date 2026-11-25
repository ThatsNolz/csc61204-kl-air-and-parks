# Class Responsibility Collaborator cards

Class names match the UML class diagrams generated from the source (`diagrams/class-*.svg`).

## MonitorController
| Responsibilities | Collaborators |
|---|---|
| Accept view actions; run refreshes and report saves on one background worker; hand results back on the UI executor; refuse overlapping refreshes and duplicate saves; apply threshold and subscription changes. | ReadingService, ReportStore, AlertMonitor, AlertBridge, View |

## ReadingService
| Responsibilities | Collaborators |
|---|---|
| Fetch current data; decide between live, cached and unavailable results; keep 12 distinct model hours per district; publish live readings for alerting. | AirQualityGateway, ReadingCache, AlertMonitor, Reading, Result |

## OpenMeteoClient
| Responsibilities | Collaborators |
|---|---|
| Build the request, classify HTTP and transport errors and translate validated JSON into a domain reading. | HttpClient, ObjectMapper, AirQualityGateway, Reading, ApiFailure |

## ReadingCache
| Responsibilities | Collaborators |
|---|---|
| Load and save one validated reading per district on disk with an atomic file replacement; leave the source decision to the service. | ReadingService, Reading, District |

## AlertMonitor (Observer subject)
| Responsibilities | Collaborators |
|---|---|
| Hold a validated threshold; track the latest model hour per district; detect a fresh upward crossing; notify that district's observers outside its lock; keep the last 100 alerts. | Reading, District, PollutionObserver, Alert |

## AlertBridge (concrete observer)
| Responsibilities | Collaborators |
|---|---|
| Receive an alert on the worker thread and forward it, with the current history, to the view on the UI executor. | AlertMonitor, View |

## AdviceProfile
| Responsibilities | Collaborators |
|---|---|
| Offer the three audiences to the view and bind each one to its own advice strategy object. | AdviceStrategy, GeneralPublicAdvice, ChildrenAdvice, OlderAdultsAdvice |

## GeneralPublicAdvice / ChildrenAdvice / OlderAdultsAdvice (concrete strategies)
| Responsibilities | Collaborators |
|---|---|
| Map each AQI category to outdoor-activity guidance for one audience. | AirCategory |

## MonitorPanel
| Responsibilities | Collaborators |
|---|---|
| Implement the View contract on the Swing event dispatch thread; own the top bar, status bar and session state; delegate each screen to its pane; persist choices. | MonitorController, TopBar, StatusBar, OverviewPane, MonitoringPane, PlannerPane, ReportsPane, AlertsPane, SettingsPane, ViewPreferences |

## MonitoringPane
| Responsibilities | Collaborators |
|---|---|
| Build read-only district and sample rows; colour each category cell; plot distinct model hours against the alert threshold; show the session's trend statistics. | TrendChart, ComparisonBars, AreaPlot, CategoryPalette, TrendSummary, Reading, Result |

## ActivityPlanner
| Responsibilities | Collaborators |
|---|---|
| Score a planned activity from the AQI category, intensity, duration and audience; publish the arithmetic and the precautions behind each verdict rather than only its conclusion. | AirCategory, ActivityIntensity, ActivityWindow, AdviceProfile, PlannerPane |

## PlannerPane
| Responsibilities | Collaborators |
|---|---|
| Collect the activity choices; show the verdict with the score that produced it; run the same reading through all three advice strategies side by side; name one verified nearby park. | ActivityPlanner, AdviceProfile, ParkCard, CityCatalog, Reading |

## InsightEngine
| Responsibilities | Collaborators |
|---|---|
| Turn the current readings into ranked plain-language observations (comparisons between areas, guideline exceedances, data gaps) without deciding how they are displayed. | District, Reading, GuidelineLevel, InsightList |
