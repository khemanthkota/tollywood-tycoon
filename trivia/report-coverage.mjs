#!/usr/bin/env node
import {readFile,writeFile} from 'node:fs/promises';
const path=process.argv[2]||'trivia/candidates/wikidata-credits.json';
const rows=JSON.parse(await readFile(path,'utf8'));
const people=new Set(rows.map(x=>x.personId));
const films=new Set(rows.map(x=>x.filmId));
const roles={actor:0,director:0,producer:0,writer:0};
for(const row of rows)for(const role of new Set(row.roles||[]))if(role in roles)roles[role]++;
const report={candidateCredits:rows.length,people:people.size,films:films.size,
 roleCredits:roles,approvedCredits:rows.filter(x=>x.reviewStatus==='approved').length,
 playableVerifiedQuestions:0,
 note:'Import candidates are not verified lifetime filmographies or playable questions'};
console.log(JSON.stringify(report,null,2));
if(process.env.GITHUB_STEP_SUMMARY)await writeFile(process.env.GITHUB_STEP_SUMMARY,
 '## Filmography import coverage\n'+
 '|Metric|Count|\n|---|---:|\n'+
 Object.entries({candidateCredits:report.candidateCredits,people:report.people,
 films:report.films,approvedCredits:report.approvedCredits,
 playableVerifiedQuestions:report.playableVerifiedQuestions})
 .map(([k,v])=>`|${k}|${v}|`).join('\n')+'\n',
 {flag:'a'});
