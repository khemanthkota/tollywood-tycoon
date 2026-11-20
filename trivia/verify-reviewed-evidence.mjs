#!/usr/bin/env node
/**
 * Conservative evidence-based verification. Source URLs alone never prove claims.
 * Requires independently reviewed evidence for the correct credit AND explicit
 * negative-credit evidence for each distractor. Difficulty requires playtest data.
 */
import {readFile,writeFile,mkdir} from 'node:fs/promises';
const base='trivia/candidates/';
const load=async(p,fallback)=>{try{return JSON.parse(await readFile(p,'utf8'))}catch(e){if(e.code==='ENOENT')return fallback;throw e}};
const [queue,evidence]=await Promise.all([load(base+'fact-check-queue.json',[]),load('trivia/reviewed-evidence.json',[])]);
const byId=new Map(evidence.map(e=>[e.questionId,e]));
const host=u=>{try{const x=new URL(u);return x.protocol==='https:'?x.hostname.replace(/^www\\./,''):''}catch{return ''}};
const norm=x=>String(x||'').normalize('NFKC').trim().toLowerCase();
const accepted=[],pending=[],rejected=[];
for(const q of queue){
 const e=byId.get(q.id),issues=[];
 if(!e){pending.push({id:q.id,reasons:['no-reviewed-evidence']});continue}
 if(norm(e.answer)!==norm(q.answer))issues.push('reviewed-answer-mismatch');
 if(e.reviewerConfirmed!==true)issues.push('reviewer-confirmation-missing');
 if(!Array.isArray(e.positiveSources)||new Set(e.positiveSources.map(host).filter(Boolean)).size<2)issues.push('two-independent-positive-sources-required');
 if(!Array.isArray(e.distractorChecks)||e.distractorChecks.length!==3||e.distractorChecks.some(d=>!q.options.some(o=>norm(o)===norm(d.option))||norm(d.option)===norm(q.answer)||d.excluded!==true||!Array.isArray(d.sources)||d.sources.length===0||d.sources.some(s=>!host(s))))issues.push('distractor-evidence-incomplete');
 if(!Number.isInteger(e.playtestAttempts)||e.playtestAttempts<20||!Number.isInteger(e.playtestCorrect)||e.playtestCorrect<0||e.playtestCorrect>e.playtestAttempts)issues.push('difficulty-playtest-insufficient');
 if(issues.length){rejected.push({id:q.id,reasons:issues});continue}
 const accuracy=e.playtestCorrect/e.playtestAttempts;
 const difficulty=accuracy<0.35?'hard':accuracy<0.7?'medium':'easy';
 accepted.push({...q,verified:true,playable:true,reviewStatus:'approved',difficulty,verificationStatus:'reviewed-independent-evidence',evidence:e});
}
await mkdir(base,{recursive:true});
await writeFile(base+'verified-playable.json',JSON.stringify(accepted,null,2)+'\\n');
await writeFile(base+'evidence-verification-report.json',JSON.stringify({total:queue.length,approved:accepted.length,awaitingEvidence:pending.length,rejectedEvidence:rejected.length,examples:rejected.slice(0,30)},null,2)+'\\n');
console.log(JSON.stringify({total:queue.length,approved:accepted.length,awaitingEvidence:pending.length,rejectedEvidence:rejected.length}));
