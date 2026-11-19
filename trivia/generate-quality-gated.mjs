#!/usr/bin/env node
/** Generate only quality-gated questions from imported credit records.
 * Unreviewed Wikidata records remain candidates, never playable content.
 */
import {readFile,mkdir,writeFile} from 'node:fs/promises';
import {buildCreditQuestions} from './credit-questions.mjs';
import {filterQuestionBatch} from './automated-quality-gate.mjs';
const input=process.argv[2]||'trivia/candidates/wikidata-credits.json';
const output=process.argv[3]||'trivia/candidates/quality-gated-questions.json';
const report=process.argv[4]||'trivia/candidates/quality-report.json';
let credits=[];
try { const parsed=JSON.parse(await readFile(input,'utf8')); if(!Array.isArray(parsed)) throw Error('Expected array'); credits=parsed; }
catch(e) { console.error('Cannot read credit input: '+e.message); process.exit(1); }
const approved=credits.filter(c=>c.reviewStatus==='approved');
const drafts=buildCreditQuestions(approved).map(q=>({
  ...q,
  explanation:`The credited role for ${q.answer} in this film is supported by the linked film-credit records.`,
  sources:q.sourceUrls,
  cutoff:'2026-11-19',
  verified:false
}));
const checked=filterQuestionBatch(drafts);
const reportData={importedCredits:credits.length,approvedCredits:approved.length,
  draftQuestions:drafts.length,...checked.summary,
  rejectedReasons:checked.rejected.reduce((a,x)=>{for(const r of x.reasons)a[r]=(a[r]||0)+1;return a;},{})};
await mkdir('trivia/candidates',{recursive:true});
await writeFile(output,JSON.stringify(checked.accepted,null,2)+'\n');
await writeFile(report,JSON.stringify(reportData,null,2)+'\n');
console.log(JSON.stringify(reportData));
