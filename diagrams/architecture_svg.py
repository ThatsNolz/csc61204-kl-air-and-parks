"""Draws the layered architecture diagram as a hand-placed SVG (diagrams/architecture.svg).

Run from the repository root: python3 diagrams/architecture_svg.py
Then: rsvg-convert -z 2 diagrams/architecture.svg -o diagrams/architecture.png
"""
from pathlib import Path
from xml.sax.saxutils import escape

W, H = 1200, 860
INK, MUTED, TEAL, LINE, RED = "#14303B", "#5A707A", "#00796B", "#9FB2BA", "#A1262F"
parts = []


def text(x, y, value, size=13, weight="normal", colour=INK, anchor="middle", italic=False):
    style = "italic" if italic else "normal"
    parts.append(f'<text x="{x}" y="{y}" font-size="{size}" font-weight="{weight}" font-style="{style}" '
                 f'fill="{colour}" text-anchor="{anchor}">{escape(value)}</text>')


def box(x, y, w, h, title, lines=(), fill="#FFFFFF", stroke=MUTED, bold=True, radius=6, width=1.2):
    parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{radius}" fill="{fill}" stroke="{stroke}" '
                 f'stroke-width="{width}"/>')
    top = y + h / 2 - (len(lines) * 16) / 2 + (5 if not lines else -2)
    text(x + w / 2, top, title, 13, "bold" if bold else "normal")
    for i, line in enumerate(lines):
        text(x + w / 2, top + 18 + i * 16, line, 11.5, colour=MUTED)


def band(y, h, title, subtitle, fill="#F2F5F7", stroke=TEAL):
    parts.append(f'<rect x="150" y="{y}" width="1030" height="{h}" rx="10" fill="{fill}" stroke="{stroke}" stroke-width="1.2"/>')
    parts.append(f'<rect x="20" y="{y}" width="120" height="{h}" rx="10" fill="{stroke}"/>')
    text(80, y + h / 2 - 4, title, 13, "bold", "#FFFFFF")
    text(80, y + h / 2 + 13, subtitle, 10.5, colour="#E3F2EF")


def arrow(x1, y1, x2, y2, label="", colour=MUTED, dashed=False, label_dx=8, anchor="start"):
    dash = ' stroke-dasharray="6 4"' if dashed else ""
    marker = "headRed" if colour == RED else ("headTeal" if colour == TEAL else "head")
    parts.append(f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{colour}" stroke-width="1.6"{dash} '
                 f'marker-end="url(#{marker})"/>')
    if label:
        text((x1 + x2) / 2 + label_dx, (y1 + y2) / 2 + 4, label, 11, colour=colour, anchor=anchor)


def path(points, colour=MUTED, dashed=False):
    dash = ' stroke-dasharray="6 4"' if dashed else ""
    marker = "headRed" if colour == RED else ("headTeal" if colour == TEAL else "head")
    d = " ".join(("M" if i == 0 else "L") + f"{x},{y}" for i, (x, y) in enumerate(points))
    parts.append(f'<path d="{d}" fill="none" stroke="{colour}" stroke-width="1.6"{dash} marker-end="url(#{marker})"/>')


def build():
    parts.append(f'<rect width="{W}" height="{H}" fill="white"/>')
    # Actor
    box(470, 16, 320, 50, "Resident or caregiver", ["selects area, refreshes, reports, sets threshold"], fill="#E6F2F0", radius=25)
    # Bands
    band(90, 150, "View", "Swing EDT")
    band(270, 110, "Controller", "EDT → worker")
    band(410, 150, "Service", "worker thread")
    band(590, 130, "API and data", "worker thread")
    box(180, 108, 300, 58, "MonitorPanel", ["shell · implements View · binds actions",
                                              "owns the TopBar and StatusBar"], width=2)
    box(500, 108, 380, 58, "Seven screen panes", ["Overview · Monitoring · Planner · Reports",
                                                    "Alerts · Data & impact · Settings"])
    box(900, 108, 250, 58, "ViewPreferences", ["district, audience, alerts, threshold"])
    box(180, 176, 970, 46, "view.design tokens · view.component controls · view.chart plots · CategoryPalette (EPA colours) · NavigationRailUI", bold=False)
    box(180, 290, 430, 70, "MonitorController", ["schedules I/O on one worker; AtomicBoolean guards",
                                                  "stop overlapping refreshes and duplicate saves"], width=2)
    box(640, 290, 250, 70, "AlertBridge  «ConcreteObserver»", ["forwards alerts to the EDT"])
    box(920, 290, 230, 70, "View  «interface»", ["output contract; MonitorPanel", "and test fakes implement it"])
    box(180, 430, 430, 70, "ReadingService", ["chooses LIVE, CACHED or UNAVAILABLE; publishes live", "readings to AlertMonitor; caches validated ones"], width=2)
    box(640, 430, 250, 70, "AlertMonitor  «Subject»", ["fresh upward crossing of", "a configurable threshold"])
    box(920, 430, 230, 70, "AdviceProfile → AdviceStrategy", ["«Strategy»: General public,", "Children, Older adults"])
    box(180, 512, 970, 34, "Result record: state + validated Reading (or none) + user-safe message", bold=False)
    box(180, 610, 430, 90, "AirQualityGateway  «interface»", ["OpenMeteoClient: java.net.http.HttpClient,",
                                                               "10 s timeout, Jackson tree validation,",
                                                               "ApiFailure.Kind for 404 / 429 / 5xx / timeout"], width=2)
    box(640, 610, 250, 90, "ReadingCache", ["one validated reading per area", "atomic JSON replace"], fill="#FFF4E0")
    box(920, 610, 230, 90, "ReportStore", ["reports.json, newest first", "never drops older entries"], fill="#FFF4E0")
    # Domain and external
    box(180, 760, 560, 70, "Domain model (immutable records and enums)", ["Reading · CitizenReport · District · AirCategory · CityCatalog",
                                                                         "validation in constructors; no Swing or I/O"], fill="#EEF1FB")
    box(800, 760, 350, 70, "Open-Meteo Air Quality API", ["Copernicus CAMS Global model",
                                                          "GET /v1/air-quality · HTTPS · JSON"], fill="#FFFFFF", stroke=TEAL, width=2)
    # Flows
    arrow(630, 64, 330, 106, "user input", label_dx=-10, anchor="end")
    arrow(330, 222, 330, 288, "action calls", label_dx=8)
    arrow(395, 360, 395, 428, "refresh(area) on worker", label_dx=8)
    path([(580, 360), (580, 395), (1166, 395), (1166, 598), (1035, 598), (1035, 607)])
    text(905, 390, "save / list reports on worker", 11, colour=MUTED, anchor="start")
    arrow(330, 500, 330, 608, "fetch(area)", label_dx=8)
    arrow(610, 465, 638, 465)
    arrow(610, 655, 638, 655)
    arrow(765, 430, 765, 362, "", colour=RED, dashed=True)
    text(757, 420, "onAlert(alert)", 11, colour=RED, anchor="end")
    arrow(700, 290, 470, 168, "alert(...) via invokeLater", colour=RED, dashed=True, label_dx=12)
    arrow(250, 288, 250, 168, "", colour=TEAL, dashed=True)
    text(258, 236, "Result → reading(...)", 11, colour=TEAL, anchor="start")
    text(258, 250, "via invokeLater", 11, colour=TEAL, anchor="start")
    path([(560, 700), (560, 736), (1000, 736), (1000, 758)], colour=TEAL)
    text(780, 731, "HTTPS GET latitude, longitude, current=us_aqi,pm2_5,pm10,nitrogen_dioxide,ozone", 10.5, colour=TEAL)
    path([(1100, 758), (1100, 748), (600, 748), (600, 702)], colour=TEAL, dashed=True)
    text(1108, 745, "JSON or", 10, colour=TEAL, anchor="start")
    text(1108, 757, "HTTP error", 10, colour=TEAL, anchor="start")
    arrow(300, 700, 300, 758, "maps to Reading", label_dx=8)
    # Legend
    text(1180, 852, "Solid grey: call or data flow   |   Dashed red: Observer notification   |   Dashed teal: hand-back to the Swing EDT",
         10.5, colour=MUTED, anchor="end")
    defs = "".join(f'<marker id="{name}" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" '
                   f'orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" fill="{colour}"/></marker>'
                   for name, colour in (("head", MUTED), ("headRed", RED), ("headTeal", TEAL)))
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}" '
            f'font-family="Helvetica, Arial, sans-serif"><defs>{defs}</defs>' + "".join(parts) + "</svg>")


if __name__ == "__main__":
    Path("diagrams/architecture.svg").write_text(build(), encoding="utf-8")
