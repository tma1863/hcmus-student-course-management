#!/usr/bin/env python3
"""Generate a user-friendly HTML report from a Major Program curriculum report.

The input is the JSON returned by:
    GET /api/majors/{majorId}/major-programs/{id}/report

JSON shape (see *ReportResponse records on the server side):
    {
      "id": 1,
      "majorId": 10,
      "majorCode": "CS",
      "name": "Computer Science 2023",
      "entryYear": "2023",
      "totalRequiredCredit": 120,
      "totalOptionalCredit": 18,
      "semesters": [
        {
          "semester": 1,
          "courses": [
            {
              "courseId": "CS101",
              "name": "Intro to Programming",
              "credits": 4,
              "isRequired": true,
              "prerequisites": [{"courseId": "MATH100", "name": "Calculus I"}]
            }
          ]
        }
      ]
    }

Usage:
    # From a JSON file
    python report_to_html.py report.json -o report.html

    # Straight from the API via stdin
    curl -s http://localhost:8080/api/majors/10/major-programs/1/report \
        | python report_to_html.py - -o report.html
"""

import argparse
import html
import json
import sys
from datetime import datetime


def esc(value):
    """HTML-escape a value, rendering None as an empty string."""
    if value is None:
        return ""
    return html.escape(str(value))


def render_prerequisites(prerequisites):
    if not prerequisites:
        return '<span class="muted">—</span>'
    chips = []
    for pre in prerequisites:
        code = esc(pre.get("courseId"))
        name = esc(pre.get("name"))
        chips.append(f'<span class="chip" title="{name}">{code}</span>')
    return "".join(chips)


def render_course_row(course):
    is_required = course.get("isRequired")
    badge_class = "badge-required" if is_required else "badge-optional"
    badge_text = "Required" if is_required else "Optional"
    return f"""
        <tr>
          <td class="mono">{esc(course.get("courseId"))}</td>
          <td>{esc(course.get("name"))}</td>
          <td class="center">{esc(course.get("credits"))}</td>
          <td class="center"><span class="badge {badge_class}">{badge_text}</span></td>
          <td>{render_prerequisites(course.get("prerequisites"))}</td>
        </tr>"""


def render_semester(semester):
    courses = semester.get("courses") or []
    sem_credits = sum((c.get("credits") or 0) for c in courses)
    required_count = sum(1 for c in courses if c.get("isRequired"))
    optional_count = len(courses) - required_count

    rows = "".join(render_course_row(c) for c in courses) or """
        <tr><td colspan="5" class="muted center">No courses in this semester</td></tr>"""

    return f"""
      <section class="semester">
        <div class="semester-header">
          <h2>Semester {esc(semester.get("semester"))}</h2>
          <div class="semester-meta">
            <span>{len(courses)} course(s)</span>
            <span>{required_count} required · {optional_count} optional</span>
            <span class="sem-credits">{sem_credits} credits</span>
          </div>
        </div>
        <table class="courses">
          <thead>
            <tr>
              <th style="width:12%">Course ID</th>
              <th style="width:38%">Course Name</th>
              <th style="width:10%" class="center">Credits</th>
              <th style="width:14%" class="center">Type</th>
              <th style="width:26%">Prerequisites</th>
            </tr>
          </thead>
          <tbody>{rows}
          </tbody>
        </table>
      </section>"""


def render_html(report):
    semesters = sorted(
        report.get("semesters") or [],
        key=lambda s: (s.get("semester") is None, s.get("semester")),
    )

    total_courses = sum(len(s.get("courses") or []) for s in semesters)
    total_credits = sum(
        (c.get("credits") or 0)
        for s in semesters
        for c in (s.get("courses") or [])
    )

    semesters_html = "".join(render_semester(s) for s in semesters) or """
      <p class="muted center">This program has no curriculum courses yet.</p>"""

    generated = datetime.now().strftime("%Y-%m-%d %H:%M")

    return f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>{esc(report.get("name"))} — Curriculum Report</title>
  <style>
    :root {{
      --bg: #f4f6fb;
      --card: #ffffff;
      --ink: #1f2933;
      --muted: #7b8794;
      --line: #e4e7eb;
      --accent: #2563eb;
      --accent-soft: #eff4ff;
    }}
    * {{ box-sizing: border-box; }}
    body {{
      margin: 0;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      background: var(--bg);
      color: var(--ink);
      line-height: 1.5;
    }}
    .wrap {{ max-width: 1080px; margin: 0 auto; padding: 32px 20px 64px; }}
    header.report-head {{
      background: var(--card);
      border: 1px solid var(--line);
      border-radius: 14px;
      padding: 28px 32px;
      box-shadow: 0 1px 3px rgba(0,0,0,.05);
    }}
    .program-code {{
      display: inline-block;
      font-size: 12px;
      font-weight: 600;
      letter-spacing: .06em;
      text-transform: uppercase;
      color: var(--accent);
      background: var(--accent-soft);
      padding: 4px 10px;
      border-radius: 999px;
      margin-bottom: 10px;
    }}
    header.report-head h1 {{ margin: 0 0 6px; font-size: 26px; }}
    .sub {{ color: var(--muted); font-size: 14px; }}
    .stats {{
      display: flex;
      flex-wrap: wrap;
      gap: 14px;
      margin-top: 22px;
    }}
    .stat {{
      flex: 1 1 150px;
      background: var(--accent-soft);
      border-radius: 10px;
      padding: 14px 16px;
    }}
    .stat .num {{ font-size: 22px; font-weight: 700; color: var(--accent); }}
    .stat .label {{ font-size: 12px; color: var(--muted); text-transform: uppercase; letter-spacing: .04em; }}
    .semester {{
      background: var(--card);
      border: 1px solid var(--line);
      border-radius: 14px;
      margin-top: 22px;
      overflow: hidden;
      box-shadow: 0 1px 3px rgba(0,0,0,.04);
    }}
    .semester-header {{
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: 8px;
      padding: 16px 24px;
      border-bottom: 1px solid var(--line);
      background: #fafbfc;
    }}
    .semester-header h2 {{ margin: 0; font-size: 18px; }}
    .semester-meta {{ display: flex; gap: 16px; font-size: 13px; color: var(--muted); flex-wrap: wrap; }}
    .sem-credits {{ font-weight: 600; color: var(--accent); }}
    table.courses {{ width: 100%; border-collapse: collapse; }}
    table.courses th {{
      text-align: left;
      font-size: 12px;
      text-transform: uppercase;
      letter-spacing: .04em;
      color: var(--muted);
      padding: 12px 24px;
      border-bottom: 1px solid var(--line);
    }}
    table.courses td {{
      padding: 12px 24px;
      border-bottom: 1px solid var(--line);
      font-size: 14px;
      vertical-align: top;
    }}
    table.courses tbody tr:last-child td {{ border-bottom: none; }}
    table.courses tbody tr:hover {{ background: #fafbff; }}
    .center {{ text-align: center; }}
    .mono {{ font-family: "SF Mono", ui-monospace, "Cascadia Code", Menlo, monospace; font-size: 13px; }}
    .muted {{ color: var(--muted); }}
    .badge {{
      display: inline-block;
      font-size: 11px;
      font-weight: 600;
      padding: 3px 9px;
      border-radius: 999px;
    }}
    .badge-required {{ background: #e6f4ea; color: #1e7e34; }}
    .badge-optional {{ background: #fdecea; color: #b94a48; }}
    .chip {{
      display: inline-block;
      font-family: "SF Mono", ui-monospace, Menlo, monospace;
      font-size: 12px;
      background: #f0f2f5;
      color: var(--ink);
      padding: 2px 8px;
      border-radius: 6px;
      margin: 2px 4px 2px 0;
    }}
    footer {{ margin-top: 28px; text-align: center; font-size: 12px; color: var(--muted); }}
  </style>
</head>
<body>
  <div class="wrap">
    <header class="report-head">
      <span class="program-code">{esc(report.get("majorCode"))} · Major #{esc(report.get("majorId"))}</span>
      <h1>{esc(report.get("name"))}</h1>
      <div class="sub">Entry year {esc(report.get("entryYear"))} · Program #{esc(report.get("id"))}</div>
      <div class="stats">
        <div class="stat"><div class="num">{esc(report.get("totalRequiredCredit"))}</div><div class="label">Required Credits</div></div>
        <div class="stat"><div class="num">{esc(report.get("totalOptionalCredit"))}</div><div class="label">Optional Credits</div></div>
        <div class="stat"><div class="num">{len(semesters)}</div><div class="label">Semesters</div></div>
        <div class="stat"><div class="num">{total_courses}</div><div class="label">Courses</div></div>
        <div class="stat"><div class="num">{total_credits}</div><div class="label">Total Course Credits</div></div>
      </div>
    </header>
    {semesters_html}
    <footer>Generated {generated}</footer>
  </div>
</body>
</html>"""


def main():
    parser = argparse.ArgumentParser(
        description="Convert a Major Program curriculum report JSON into an HTML page."
    )
    parser.add_argument(
        "input",
        help="Path to the JSON file, or '-' to read from stdin.",
    )
    parser.add_argument(
        "-o", "--output",
        help="Output HTML file path (default: stdout).",
    )
    args = parser.parse_args()

    if args.input == "-":
        raw = sys.stdin.read()
    else:
        with open(args.input, "r", encoding="utf-8") as f:
            raw = f.read()

    try:
        report = json.loads(raw)
    except json.JSONDecodeError as exc:
        sys.exit(f"Error: input is not valid JSON: {exc}")

    page = render_html(report)

    if args.output:
        with open(args.output, "w", encoding="utf-8") as f:
            f.write(page)
        print(f"Wrote {args.output}", file=sys.stderr)
    else:
        sys.stdout.write(page)


if __name__ == "__main__":
    main()
