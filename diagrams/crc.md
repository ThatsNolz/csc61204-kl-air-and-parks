# Class Responsibility Collaborator cards

## MonitorController
| Responsibilities | Collaborators |
|---|---|
| Route district refresh and report actions; move I/O off the EDT; deliver completed results to the view. | View, ReadingService, AlertMonitor, ReportStore |

## ReadingService
| Responsibilities | Collaborators |
|---|---|
| Fetch current data; distinguish live, cache and unavailable results; maintain bounded in-memory observations. | AirQualityGateway, ReadingCache, District, Reading |

## OpenMeteoClient
| Responsibilities | Collaborators |
|---|---|
| Construct request, classify transport errors and translate validated JSON into a domain reading. | HttpClient, ObjectMapper, AirQualityGateway, Reading |

## AlertMonitor
| Responsibilities | Collaborators |
|---|---|
| Track fresh samples, detect threshold crossings and deliver one event to the subscribed observers for a district. | Reading, District, PollutionObserver |

## AdviceProfile
| Responsibilities | Collaborators |
|---|---|
| Bind each built-in audience to a distinct advice policy. | AdviceStrategy, AirCategory |

## MonitoringPane
| Responsibilities | Collaborators |
|---|---|
| Build read-only district and sample rows from the latest view snapshots; expose exact model times and missing values next to the trend. | MonitorPanel, TrendChart, Reading, ReadingService.Result |
