#!/usr/bin/env node
/** Audit drafts without treating Wikidata imports as independent verification. */
import {readFile,writeFile} from 'node:fs/promises';
const [draftPath='trivia/candidates/bulk-drafts.json',creditPath='trivia/candidates/merged-credits.json']=process.argv.slice(2);
const [questions,credits]=await Promise.all([readFile(draftPath,'utf8').then(JSON.parse),readFile(creditPath,'utf8').then(JSON.parse)]);
const norm=s=>String(s||'').normalize('NFKC').toLowerCase().trim().replace(/\\s+/g,' ');
const roles=new Map(),byPerson=new Map();
for(const c of credits)for(const role of c.roles||[]){
 const key=[c.filmId,role].join('|');
 if(!roles.has(key))roles.set(key,new Set());
 roles.get(key).add(norm(c.personName));
 const personKey=[c.personId,role].join('|');
 if(!byPerson.has(personKey))byPerson.set(personKey,new Set());
 byPerson.get(personKey).add(c.filmId);
}
const results=[],counts={total:questions.length,structurallyValid:0,answerLeak:0,knownAmbiguity:0,sourceUnverified:0,lowDifficultyRisk:0,needsReview:0,readyToPlay:0};
const seen=new Set();
for(const q of questions){
 const issues=[],options=(q.options||[]).map(norm),answer=norm(q.answer),prompt=norm(q.prompt);
 if(options.length!==4||new Set(options).size!==4||options.filter(o=>o===answer).length!==1)issues.push('invalid-options');
 if(seen.has(prompt))issues.push('duplicate-prompt');seen.add(prompt);
 if(answer.length>3&&prompt.includes(answer))issues.push('answer-leak');
 const [filmId,personId]=q.sourceRecordIds||[];
 if(q.prompt?.startsWith('According to the imported credits,')){
  const possible=roles.get([filmId,q.role].join('|'))||new Set();
  if(options.filter(o=>possible.has(o)).length>1)issues.push('multiple-options-supported-by-import');
 }else if(q.prompt?.startsWith('Which of these films is listed')){
  const known=byPerson.get([personId,q.role].join('|'))||new Set();
  const optionTitles=new Set(options);
  const matches=credits.filter(c=>known.has(c.filmId)&&optionTitles.has(norm(c.title))).map(c=>norm(c.title));
  if(new Set(matches).size>1)issues.push('multiple-options-supported-by-import');
 }
 if(!q.sources?.length||new Set(q.sources.map(s=>{try{return new URL(s).hostname}catch{return ''}})).size<2)issues.push('no-independent-corroboration');
 if(q.difficulty==='draft'||/according to the imported credits|listed in the imported records/i.test(q.prompt))issues.push('difficulty-not-calibrated');
 if(!issues.some(x=>['invalid-options','duplicate-prompt'].includes(x)))counts.structurallyValid++;
 if(issues.includes('answer-leak'))counts.answerLeak++;
 if(issues.some(x=>x.startsWith('multiple-options')))counts.knownAmbiguity++;
 if(issues.includes('no-independent-corroboration'))counts.sourceUnverified++;
 if(issues.includes('difficulty-not-calibrated'))counts.lowDifficultyRisk++;
 counts.needsReview++;
 results.push({id:q.id,issues,reviewStatus:'pending',playable:false});
}
await writeFile('trivia/candidates/draft-audit-report.json',JSON.stringify({summary:counts,method:'Automated structural and imported-credit consistency checks only. No independent fact verification, difficulty calibration, or gameplay approval.',examples:results.filter(r=>r.issues.some(x=>x.startsWith('multiple-options'))).slice(0,50)},null,2)+'\\n');
await writeFile('trivia/candidates/draft-audit-results.json',JSON.stringify(results)+'\\n');
console.log(JSON.stringify(counts));
