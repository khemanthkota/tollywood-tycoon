#!/usr/bin/env python3
"""Validate the Cinema Trivia bank before adding new questions.

Usage: python3 scripts/validate_trivia_bank.py
Only editorially reviewed questions should be marked verified=true.
"""
import collections, json, pathlib, re, sys
path=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/assets/trivia-lan-bank.json"
questions=json.loads(path.read_text(encoding="utf-8"))
issues=[]; seen=set(); by_industry=collections.Counter(); by_level=collections.Counter(); verified=0
for i,q in enumerate(questions):
    ident=f"#{i+1} {q.get('industry','?')}"
    industry=q.get("industry"); level=q.get("difficulty")
    by_industry[industry]+=1; by_level[level]+=1
    if q.get("verified") is True: verified+=1
    options=q.get("options",[])
    if len(options)!=4 or len(set(str(v).strip().casefold() for v in options))!=4: issues.append(f"{ident}: options not four unique choices"); continue
    correct=q.get("correct")
    if not isinstance(correct,int) or not 0<=correct<4: issues.append(f"{ident}: invalid correct index");continue
    question=q.get("question","").strip()
    if not question: issues.append(f"{ident}: blank question")
    if level not in ("easy","medium","hard","expert"): issues.append(f"{ident}: invalid difficulty")
    if industry not in ("tollywood","bollywood","hollywood","kollywood","mollywood","sandalwood"): issues.append(f"{ident}: invalid industry")
    key=(industry,re.sub(r"\W+"," ",question.casefold()).strip(),tuple(sorted(v.casefold() for v in options)))
    if key in seen: issues.append(f"{ident}: duplicate question")
    seen.add(key)
    answer=options[correct].casefold()
    if len(answer)>3 and re.search(r"(?<!\w)"+re.escape(answer)+r"(?!\w)",question.casefold()):
        issues.append(f"{ident}: answer appears in question")
    if level in ("hard","expert") and (question.startswith("What was the theatrical release year") or question.startswith("Which of these films was released in")):
        issues.append(f"{ident}: release-year recall alone is not sufficient evidence for {level} difficulty")
    if q.get("type") in ("scene","dialogue") and not q.get("source"):
        issues.append(f"{ident}: missing asset/scene source")
print(f"Total {len(questions):,} / 9,000 target; editorially verified: {verified:,}")
print("By industry:",dict(by_industry));print("By difficulty:",dict(by_level))
print(f"Review flags: {len(issues)}")
for issue in issues[:80]:print(" -",issue)
if len(issues)>80:print(f" ... {len(issues)-80} more")
sys.exit(1 if issues else 0)
