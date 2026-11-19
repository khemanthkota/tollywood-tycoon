#!/usr/bin/env node
/**
 * Bulk DRAFT generation from imported film-credit candidates.
 * Produces up to 9000 unique, non-playable questions; never pads with invented facts.
 * Source records are hypotheses until independently checked.
 */
import {readFile,writeFile,mkdir} from 'node:fs/promises';
const target=9000;
const input=process.argv[2]||'trivia/candidates/wikidata-credits.json';
const output=process.argv[3]||'trivia/candidates/bulk-drafts.json';
const reportPath=process.argv[4]||'trivia/candidates/bulk-draft-report.json';
const records=JSON.parse(await readFile(input,'utf8'));
if(!Array.isArray(records)) throw Error('Expected credit array');
const roleLabels={actor:'appeared in',director:'directed',producer:'produced',writer:'wrote'};
const clean=records.filter(c=>c && c.personId && c.personName && c.filmId && c.title && Array.isArray(c.roles) && c.status==='released');
const unique=new Map();
for(const c of clean) for(const role of c.roles) if(roleLabels[role]){
 const key=[c.personId,c.filmId,role].join(':');
 if(!unique.has(key)) unique.set(key,{...c,role});
}
const rows=[...unique.values()].sort((a,b)=>a.filmId.localeCompare(b.filmId)||a.personId.localeCompare(b.personId)||a.role.localeCompare(b.role));
const questions=[],seen=new Set();
function add(q){
 const key=q.prompt.toLowerCase().replace(/\s+/g,' ').trim();
 if(seen.has(key)||questions.length>=target)return;
 seen.add(key);questions.push({...q,id:'bulk-draft-'+String(questions.length+1).padStart(5,'0'),verified:false,playable:false,verificationStatus:'needs-independent-fact-check'});
}
for(const c of rows){
 if(questions.length>=target)break;
 const others=[...new Map(rows.filter(x=>x.role===c.role && x.filmId!==c.filmId && x.personId!==c.personId).map(x=>[x.personId,x])).values()].slice(0,3);
 if(others.length!==3)continue;
 const options=[c.personName,...others.map(x=>x.personName)];
 if(new Set(options.map(x=>x.toLowerCase())).size!==4)continue;
 add({category:'filmography',difficulty:'draft',industry:c.industry||'Unclassified',
  prompt:`According to the imported credits, who ${roleLabels[c.role]} the film "${c.title}"?`,
  options,answer:c.personName,
  explanation:`The imported record attributes the ${c.role} credit for "${c.title}" to ${c.personName}; this attribution has not been independently verified.`,
  sources:c.sourceUrls||[],sourceRecordIds:[c.filmId,c.personId],role:c.role});
}
await mkdir('trivia/candidates',{recursive:true});
await writeFile(output,JSON.stringify(questions,null,2)+'\n');
const report={target,generated:questions.length,shortfall:target-questions.length,sourceCredits:records.length,distinctRoleCredits:rows.length,verified:0,playable:0,note:'Draft-only; do not import into gameplay without factual and ambiguity checks'};
await writeFile(reportPath,JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify(report));
