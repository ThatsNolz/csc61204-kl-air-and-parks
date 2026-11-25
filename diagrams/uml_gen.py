"""Generates the UML class diagrams (Graphviz DOT) from scripts/UmlExtract.java output.

Run from the repository root:
    java scripts/UmlExtract.java src/main/java > diagrams/members.tsv
    python3 diagrams/uml_gen.py diagrams/members.tsv diagrams
Members are taken from the source code, so the diagrams cannot drift from the implementation.
"""
import html
import sys
from collections import defaultdict
from pathlib import Path

FONT = "Helvetica"
INK, LINE_COLOUR = "#14303B", "#5A707A"
FILL = {"INTERFACE": "#E6F2F0", "ENUM": "#FFF4E0", "RECORD": "#EEF1FB", "CLASS": "#FFFFFF", "EXTERNAL": "#F2F2F2"}


def load(path):
    types, members, consts = {}, defaultdict(list), defaultdict(list)
    for line in Path(path).read_text(encoding="utf-8").splitlines():
        kind, owner, *rest = line.split("\t")
        if kind == "TYPE":
            types[owner] = (rest[0], rest[1] if len(rest) > 1 else "")
        elif kind == "CONST":
            consts[owner].append(rest[0])
        else:
            members[owner].append((kind, rest[0]))
    for owner, (kind, _) in types.items():
        if kind == "ENUM":  # javac models enum constants as static fields of the enum type
            simple = owner.split(".")[-1]
            keep = []
            for k, text in members[owner]:
                if k == "FIELD" and text.startswith("{static} +") and text.endswith(": " + simple):
                    consts[owner].append(text.split("+", 1)[1].split(":")[0])
                elif k == "METHOD" and text.startswith("~" + simple + "("):
                    keep.append((k, "-" + text[1:]))  # enum constructors are implicitly private
                else:
                    keep.append((k, text))
            members[owner] = keep
    return types, members, consts


WRAP = 58


def wrap(text):
    """Breaks a long signature after a comma so each diagram stays readable at page width."""
    if len(text) <= WRAP or "(" not in text:
        return [text]
    lines, current = [], ""
    for piece in text.replace(", ", ",\x00").split("\x00"):
        if current and len(current) + len(piece) > WRAP:
            lines.append(current)
            current = "        " + piece
        else:
            current += (" " if current and not current.endswith("(") and current.endswith(",") else "") + piece
    lines.append(current)
    return lines


def row(text):
    static = text.startswith("{static} ")
    text = text.replace("{static} ", "")
    abstract = text.startswith("{abstract} ")
    text = text.replace("{abstract} ", "")
    out = []
    for part in wrap(text):
        part = html.escape(part).replace("  ", "&#160; ")
        if static:
            part = f"<U>{part}</U>"
        if abstract:
            part = f"<I>{part}</I>"
        out.append(f'<TR><TD ALIGN="LEFT" BALIGN="LEFT">{part}</TD></TR>')
    return "".join(out)


def visibility(text):
    return text.replace("{static} ", "").replace("{abstract} ", "")[:1]


def node(name, types, members, consts, compact=False):
    kind, _ = types.get(name, ("EXTERNAL", ""))
    stereo = {"INTERFACE": "«interface»", "ENUM": "«enumeration»", "RECORD": "«record»"}.get(kind, "")
    title = html.escape(name.split(".")[-1] if "." in name and kind != "EXTERNAL" else name)
    title = f"<I>{title}</I>" if kind == "INTERFACE" else f"<B>{title}</B>"
    head = (f'<TR><TD><FONT POINT-SIZE="8">{stereo}</FONT><BR/>{title}</TD></TR>' if stereo
            else f"<TR><TD>{title}</TD></TR>")
    fields = [t for k, t in members.get(name, []) if k == "FIELD"]
    methods = [t for k, t in members.get(name, []) if k == "METHOD"]
    if name == "NavigationRailUI":  # framework hook overrides only; listed in the source, not the contract
        fields, methods = [], ["#paint…() / calculate…() overrides of BasicTabbedPaneUI"]
    elif compact:  # supporting classes: hide private members, keep the contract other classes use
        methods = [m for m in methods if not visibility(m) == "-"]
        fields = [f for f in fields if not visibility(f) == "-"]
    parts = [head]
    if consts.get(name):
        parts.append('<TR><TD ALIGN="LEFT" BALIGN="LEFT">' + "<BR/>".join(consts[name]) + "</TD></TR>")
    empty = not fields and not methods and kind in ("ENUM", "RECORD")
    if kind != "EXTERNAL" and not empty:
        parts.append('<TR><TD ALIGN="LEFT"><TABLE BORDER="0" CELLPADDING="0" CELLSPACING="0">'
                     + ("".join(row(f) for f in fields) or "<TR><TD> </TD></TR>") + "</TABLE></TD></TR>")
        parts.append('<TR><TD ALIGN="LEFT"><TABLE BORDER="0" CELLPADDING="0" CELLSPACING="0">'
                     + ("".join(row(m) for m in methods) or "<TR><TD> </TD></TR>") + "</TABLE></TD></TR>")
    table = (f'<TABLE BORDER="1" CELLBORDER="1" CELLSPACING="0" CELLPADDING="4" COLOR="{LINE_COLOUR}" '
             f'BGCOLOR="{FILL[kind]}">' + "".join(parts) + "</TABLE>")
    return f'  "{name}" [label=<{table}>];'


STYLE = {
    "realize": 'arrowhead=empty, style=dashed',
    "extend": 'arrowhead=empty',
    "aggregate": 'dir=back, arrowtail=odiamond',
    "compose": 'dir=back, arrowtail=diamond',
    "assoc": 'arrowhead=vee',
    "depend": 'arrowhead=vee, style=dashed',
}


def edge(a, b, kind, label="", tail="", headlabel=""):
    attrs = [STYLE[kind]]
    if label:
        attrs.append(f'label="{label}"')
    if tail:
        attrs.append(f'taillabel="{tail}"')
    if headlabel:
        attrs.append(f'headlabel="{headlabel}"')
    return f'  "{a}" -> "{b}" [{", ".join(attrs)}];'


def graph(title, nodes, edges, types, members, consts, compact=(), rankdir="TB", extra="", ranks=()):
    lines = [f'digraph "{title}" {{', f'  graph [rankdir={rankdir}, nodesep=0.35, ranksep=0.55, fontname="{FONT}", '
             f'bgcolor="white", pad=0.2, splines=spline, newrank=true];',
             f'  node [shape=plain, fontname="{FONT}", fontsize=9, fontcolor="{INK}"];',
             f'  edge [fontname="{FONT}", fontsize=8, color="{LINE_COLOUR}", fontcolor="{INK}", labeldistance=1.6];',
             extra]
    lines += [node(n, types, members, consts, n in compact) for n in nodes]
    lines += [edge(*e) for e in edges]
    for group in ranks:
        lines.append("  { rank=same; " + " ".join(f'"{n}";' for n in group) + " }")
    lines.append("}")
    return "\n".join(lines)


def main(tsv, out):
    types, members, consts = load(tsv)
    out = Path(out)
    service = ["ReadingService", "ReadingService.Result", "ReadingService.State", "ReadingCache", "Reading",
               "AirQualityGateway", "AlertMonitor"]
    service_edges = [
        ("ReadingService", "AirQualityGateway", "aggregate", "api", "1", "1"),
        ("ReadingService", "ReadingCache", "aggregate", "cache", "1", "1"),
        ("ReadingService", "AlertMonitor", "aggregate", "monitor", "1", "1"),
        ("ReadingService", "Reading", "assoc", "memory, history", "1", "0..*"),
        ("ReadingService", "ReadingService.Result", "depend", "«create»"),
        ("ReadingService.Result", "ReadingService.State", "assoc", "state", "", "1"),
        ("ReadingService.Result", "Reading", "assoc", "reading", "", "0..1"),
        ("ReadingCache", "Reading", "depend", "load / save"),
    ]
    (out / "class-service.dot").write_text(graph("Service", service, service_edges, types, members, consts,
        compact={"AlertMonitor", "AirQualityGateway"}, rankdir="LR"), encoding="utf-8")

    api = ["AirQualityGateway", "OpenMeteoClient", "ApiFailure", "ApiFailure.Kind", "Exception", "Reading"]
    api_edges = [
        ("OpenMeteoClient", "AirQualityGateway", "realize"),
        ("ApiFailure", "Exception", "extend"),
        ("OpenMeteoClient", "ApiFailure", "depend", "«throw»"),
        ("OpenMeteoClient", "Reading", "depend", "«create»"),
        ("ApiFailure", "ApiFailure.Kind", "assoc", "kind", "", "1"),
    ]
    (out / "class-api.dot").write_text(graph("API", api, api_edges, types, members, consts, compact={"Reading"},
                                             rankdir="LR"), encoding="utf-8")

    controller = ["MonitorController", "MonitorController.View", "AlertBridge", "ReadingService", "ReportStore",
                  "CitizenReport", "AlertMonitor"]
    controller_edges = [
        ("MonitorController", "AlertBridge", "compose", "listener", "1", "1"),
        ("MonitorController", "AlertMonitor", "aggregate", "alerts", "1", "1"),
        ("MonitorController", "ReadingService", "aggregate", "readings", "1", "1"),
        ("MonitorController", "ReportStore", "aggregate", "reports", "1", "1"),
        ("MonitorController", "MonitorController.View", "assoc", "view", "", "1"),
        ("ReportStore", "CitizenReport", "depend", "list / save"),
    ]
    (out / "class-controller.dot").write_text(graph("Controller", controller, controller_edges, types, members, consts,
        compact={"ReadingService", "AlertBridge", "AlertMonitor", "CitizenReport"}, rankdir="LR", ranks=[
        ]), 
        encoding="utf-8")

    observer = ["AlertMonitor", "AlertMonitor.Alert", "PollutionObserver", "AlertBridge", "MonitorController.View",
                "ReadingService"]
    observer_edges = [
        ("AlertBridge", "PollutionObserver", "realize"),
        ("ReadingService", "AlertMonitor", "assoc", "publish(reading)", "", "1"),
        ("AlertMonitor", "PollutionObserver", "aggregate", "subscriptions", "1", "0..*"),
        ("AlertMonitor", "AlertMonitor.Alert", "compose", "history", "1", "0..100"),
        ("AlertBridge", "AlertMonitor", "assoc", "monitor", "", "1"),
        ("AlertBridge", "MonitorController.View", "assoc", "view", "", "1"),
    ]
    (out / "class-observer.dot").write_text(graph("Observer", observer, observer_edges, types, members, consts,
        compact={"ReadingService", "MonitorController.View"}, ranks=[
        ["ReadingService", "AlertMonitor"], ["AlertMonitor.Alert", "PollutionObserver", "AlertBridge"]]),
        encoding="utf-8")

    strategy = ["AdviceProfile", "AdviceStrategy", "GeneralPublicAdvice", "ChildrenAdvice", "OlderAdultsAdvice",
                "AirCategory", "District", "CityCatalog", "CityCatalog.Park", "Reading"]
    strategy_edges = [
        ("GeneralPublicAdvice", "AdviceStrategy", "realize"),
        ("ChildrenAdvice", "AdviceStrategy", "realize"),
        ("OlderAdultsAdvice", "AdviceStrategy", "realize"),
        ("AdviceProfile", "AdviceStrategy", "compose", "strategy", "1", "1"),
        ("AdviceStrategy", "AirCategory", "depend", "advice(category)"),
        ("Reading", "District", "assoc", "district", "", "1"),
        ("Reading", "AirCategory", "depend", "category()"),
        ("CityCatalog", "CityCatalog.Park", "depend", "nearby() returns"),
        ("CityCatalog", "District", "depend", "keyed by"),
    ]
    (out / "class-strategy-domain.dot").write_text(
        graph("Strategy and domain", strategy, strategy_edges, types, members, consts, ranks=[
            ["AdviceProfile", "Reading"], ["AdviceStrategy", "District", "CityCatalog"],
            ["GeneralPublicAdvice", "ChildrenAdvice", "OlderAdultsAdvice"], ["AirCategory", "CityCatalog.Park"]]), encoding="utf-8")

    planner = ["PlannerPane", "ActivityPlanner", "ActivityPlanner.Plan", "ActivityPlanner.Verdict",
               "ActivityIntensity", "ActivityWindow", "AirCategory", "GuidelineLevel", "Reading"]
    planner_edges = [
        ("PlannerPane", "ActivityPlanner", "depend", "plan(...)"),
        ("ActivityPlanner", "ActivityPlanner.Plan", "depend", "«create»"),
        ("ActivityPlanner.Plan", "ActivityPlanner.Verdict", "assoc", "verdict", "", "1"),
        ("ActivityPlanner", "ActivityIntensity", "depend", "scores"),
        ("ActivityPlanner", "ActivityWindow", "depend", "scores"),
        ("ActivityPlanner", "AirCategory", "depend", "scores"),
        ("Reading", "AirCategory", "depend", "category()"),
        ("Reading", "GuidelineLevel", "assoc", "measured against", "", "0..*"),
    ]
    (out / "class-planner.dot").write_text(
        graph("Activity planning", planner, planner_edges, types, members, consts,
              compact={"PlannerPane", "Reading", "AirCategory", "GuidelineLevel"}, rankdir="LR", ranks=[
                  ["ActivityIntensity", "ActivityWindow", "AirCategory"],
                  ["ActivityPlanner.Plan", "ActivityPlanner.Verdict"]]), encoding="utf-8")

    views = ["MonitorPanel", "MonitorController.View", "Atmosphere", "JPanel", "BasicTabbedPaneUI", "TopBar",
             "StatusBar", "OverviewPane", "MonitoringPane", "PlannerPane", "ReportsPane", "AlertsPane", "AboutPane",
             "SettingsPane", "DistrictStrip", "InsightList", "ParkCard", "TrendChart", "ViewPreferences",
             "NavigationRailUI"]
    view_edges = [
        ("MonitorPanel", "MonitorController.View", "realize"),
        ("MonitorPanel", "Atmosphere", "extend"),
        ("Atmosphere", "JPanel", "extend"),
        ("MonitorPanel", "TopBar", "compose", "", "1", "1"),
        ("MonitorPanel", "StatusBar", "compose", "", "1", "1"),
        ("MonitorPanel", "OverviewPane", "compose", "", "1", "1"),
        ("MonitorPanel", "MonitoringPane", "compose", "", "1", "1"),
        ("MonitorPanel", "PlannerPane", "compose", "", "1", "1"),
        ("MonitorPanel", "ReportsPane", "compose", "", "1", "1"),
        ("MonitorPanel", "AlertsPane", "compose", "", "1", "1"),
        ("MonitorPanel", "AboutPane", "compose", "", "1", "1"),
        ("MonitorPanel", "SettingsPane", "compose", "", "1", "1"),
        ("MonitorPanel", "ViewPreferences", "compose", "settings", "1", "1"),
        ("MonitorPanel", "NavigationRailUI", "depend", "«uses»"),
        ("NavigationRailUI", "BasicTabbedPaneUI", "extend"),
        ("OverviewPane", "DistrictStrip", "compose", "", "1", "1"),
        ("OverviewPane", "InsightList", "compose", "", "1", "1"),
        ("OverviewPane", "ParkCard", "compose", "", "1", "1"),
        ("PlannerPane", "ParkCard", "compose", "", "1", "1"),
        ("MonitoringPane", "TrendChart", "compose", "", "1", "1"),
    ]
    compact = set(views)
    (out / "class-view.dot").write_text(
        graph("View layer", views, view_edges, types, members, consts, compact=compact, rankdir="LR", ranks=[
            ["TopBar", "StatusBar"],
            ["OverviewPane", "MonitoringPane", "PlannerPane", "ReportsPane", "AlertsPane", "AboutPane",
             "SettingsPane"],
            ["DistrictStrip", "InsightList", "ParkCard", "TrendChart", "ViewPreferences", "NavigationRailUI"]]),
        encoding="utf-8")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
