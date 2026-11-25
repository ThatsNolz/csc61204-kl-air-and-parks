"""Sequence diagram for the primary use case: refresh one district (diagrams/refresh-sequence.svg).

Run from the repository root: python3 diagrams/sequence_svg.py
Then: rsvg-convert -z 2 diagrams/refresh-sequence.svg -o diagrams/refresh-sequence.png
"""
from pathlib import Path
from xml.sax.saxutils import escape

INK, MUTED, TEAL, RED, FRAME = "#14303B", "#5A707A", "#00796B", "#A1262F", "#9FB2BA"
LIVES = ["Resident", "MonitorPanel", "MonitorController", "ReadingService", "OpenMeteoClient", "ReadingCache",
         "AlertMonitor", "AlertBridge"]
NOTES = {"MonitorPanel": "EDT", "MonitorController": "EDT → worker", "ReadingService": "worker", "OpenMeteoClient": "worker",
         "AlertMonitor": "worker", "AlertBridge": "worker → EDT"}
GAP, LEFT, TOP, W = 145, 80, 40, 1170
X = {name: LEFT + i * GAP for i, name in enumerate(LIVES)}
parts, y = [], 120


def text(x, yy, value, size=11.5, colour=INK, anchor="middle", weight="normal"):
    parts.append(f'<text x="{x}" y="{yy}" font-size="{size}" fill="{colour}" text-anchor="{anchor}" '
                 f'font-weight="{weight}">{escape(value)}</text>')


def message(a, b, label, colour=INK, reply=False, step=34):
    global y
    dash = ' stroke-dasharray="6 4"' if reply else ""
    marker = {INK: "h", RED: "hr", TEAL: "ht", MUTED: "hm"}[colour]
    if a == b:
        x = X[a]
        parts.append(f'<path d="M{x},{y} h40 v18 h-36" fill="none" stroke="{colour}" stroke-width="1.4" marker-end="url(#{marker})"/>')
        text(x + 46, y + 4, label, colour=colour, anchor="start")
        y += step + 8
        return
    parts.append(f'<line x1="{X[a]}" y1="{y}" x2="{X[b]}" y2="{y}" stroke="{colour}" stroke-width="1.4"{dash} marker-end="url(#{marker})"/>')
    text((X[a] + X[b]) / 2, y - 6, label, colour=colour)
    y += step


def frame(label, guard, height, colour=FRAME):
    parts.append(f'<rect x="{LEFT + GAP - 70}" y="{y - 22}" width="{W - LEFT - GAP + 40}" height="{height}" fill="none" stroke="{colour}" stroke-width="1.2"/>')
    parts.append(f'<rect x="{LEFT + GAP - 70}" y="{y - 22}" width="46" height="18" fill="{colour}"/>')
    text(LEFT + GAP - 47, y - 9, label, 11, "#FFFFFF", weight="bold")
    text(LEFT + GAP - 16, y - 9, guard, 11, MUTED, anchor="start")


def divider(guard):
    global y
    parts.append(f'<line x1="{LEFT + GAP - 70}" y1="{y - 14}" x2="{W - 30}" y2="{y - 14}" stroke="{FRAME}" stroke-dasharray="5 4"/>')
    text(LEFT + GAP - 60, y + 2, guard, 11, MUTED, anchor="start")
    y += 20


def build():
    global y
    for name in LIVES:
        parts.append(f'<rect x="{X[name] - 62}" y="{TOP}" width="124" height="44" rx="5" fill="{"#E6F2F0" if name == "Resident" else "#FFFFFF"}" stroke="{MUTED}"/>')
        text(X[name], TOP + 20, name, 11.5, weight="bold")
        if name in NOTES:
            text(X[name], TOP + 36, NOTES[name], 10, MUTED)
    message("Resident", "MonitorPanel", "click Refresh district")
    message("MonitorPanel", "MonitorController", "refresh(district)")
    message("MonitorController", "MonitorController", "refreshing.compareAndSet(false, true)")
    message("MonitorController", "MonitorPanel", "loading(true): disable buttons", MUTED, reply=True)
    message("MonitorController", "ReadingService", "background.execute → refresh(district)")
    message("ReadingService", "OpenMeteoClient", "fetch(district)")
    message("OpenMeteoClient", "OpenMeteoClient", "HTTPS GET, checkStatus, parse JSON")
    y += 18
    start = y
    frame("alt", "[2xx and valid JSON]", 250)
    message("OpenMeteoClient", "ReadingService", "Reading", reply=True)
    message("ReadingService", "ReadingCache", "save(reading)")
    message("ReadingService", "AlertMonitor", "publish(reading)")
    frame_y = y
    left, right = X["MonitorPanel"] - 40, X["AlertBridge"] + 50
    parts.append(f'<rect x="{left}" y="{y - 22}" width="{right - left}" height="88" fill="none" stroke="{RED}" stroke-width="1"/>')
    parts.append(f'<rect x="{left}" y="{y - 22}" width="40" height="18" fill="{RED}"/>')
    text(left + 20, y - 9, "opt", 11, "#FFFFFF", weight="bold")
    text(left + 48, y - 9, "[AlertMonitor detects a fresh upward crossing of the threshold]", 10.5, RED, anchor="start")
    y += 22
    message("AlertMonitor", "AlertBridge", "onAlert(alert)", RED)
    message("AlertBridge", "MonitorPanel", "invokeLater → alert(history, alert)", RED, reply=True)
    y += 6
    message("ReadingService", "MonitorController", "Result(LIVE, reading)", reply=True)
    divider("[ApiFailure: 404 / 429 / 5xx / timeout / invalid JSON]")
    message("OpenMeteoClient", "ReadingService", "throw ApiFailure(kind, safe message)", RED, reply=True)
    message("ReadingService", "ReadingCache", "load(district)")
    message("ReadingService", "MonitorController", "Result(CACHED, last reading) or Result(UNAVAILABLE)", reply=True)
    parts[[i for i, p in enumerate(parts) if 'height="250"' in p][0]] = parts[[i for i, p in enumerate(parts) if 'height="250"' in p][0]].replace('height="250"', f'height="{y - start + 4}"')
    y += 16
    message("MonitorController", "MonitorPanel", "invokeLater → reading(district, result, history)", TEAL, reply=True)
    message("MonitorPanel", "MonitorPanel", "render Overview, Monitoring and Advice")
    message("MonitorController", "MonitorPanel", "invokeLater → loading(false)", TEAL, reply=True)
    bottom = y + 10
    for name in LIVES:
        parts.insert(0, f'<line x1="{X[name]}" y1="{TOP + 44}" x2="{X[name]}" y2="{bottom}" stroke="{FRAME}" stroke-dasharray="4 4"/>')
    defs = "".join(f'<marker id="{k}" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto">'
                   f'<path d="M0,0 L10,5 L0,10 z" fill="{c}"/></marker>' for k, c in (("h", INK), ("hr", RED), ("ht", TEAL), ("hm", MUTED)))
    h = bottom + 30
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{h}" viewBox="0 0 {W} {h}" '
            f'font-family="Helvetica, Arial, sans-serif"><rect width="{W}" height="{h}" fill="white"/><defs>{defs}</defs>'
            + "".join(parts) + "</svg>")


if __name__ == "__main__":
    Path("diagrams/refresh-sequence.svg").write_text(build(), encoding="utf-8")
