#!/usr/bin/env node
/** Separate known problematic drafts; never promote unverified drafts to gameplay. */
import {readFile,writeFile} from 'node:fs/promises';
const [drafts,audits]=await Promise.all([
 readFile('trivia/candidates/bulk-drafts.json','utf8').then(JSON.parse),
 readFile('trivia/candidates/draft-audit-results.json','utf8').then(JSON.parse)
]);
const flags=new Map(audits.map(a=>[a.id,a.issues]));
const blocking=new Set(['answer-leak','multiple-options-supported-by-import','invalid-options','duplicate-prompt']);
const quarantine=[],reviewQueue=[];
for(const q of drafts){
 const issues=flags.get(q.id)||['missing-audit'];
 const item={...q,verified:false,playable:false,reviewStatus:'pending',auditIssues:issues};
 (issues.some(x=>blocking.has(x))?quarantine:reviewQueue).push(item);
}
await writeFile('trivia/candidates/quarantined-drafts.json',JSON.stringify(quarantine,null,2)+'\n');
await writeFile('trivia/candidates/fact-check-queue.json',JSON.stringify(reviewQueue,null,2)+'\n');
const report={total:drafts.length,quarantined:quarantine.length,queuedForIndependentFactCheck:reviewQueue.length,verified:0,playable:0};
await writeFile('trivia/candidates/remediation-report.json',JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify(report));
