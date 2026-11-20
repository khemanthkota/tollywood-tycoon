#!/usr/bin/env node
// Export all 18,000 draft questions as compact offline Android assets for beta testing.
// Unverified drafts are intentionally included; this is not a certified question set.
import {readFile,writeFile,mkdir} from 'node:fs/promises';
const src=JSON.parse(await readFile('trivia/candidates/bulk-drafts.json','utf8'));
if(src.length!==18000)throw Error('Expected 18000 drafts, got '+src.length);
const out=src.map(q=>({id:q.id,industry:q.industry||'Mixed',prompt:q.prompt,options:q.options,answer:q.answer,verified:false}));
for(const q of out){
 if(!q.id||!q.prompt||!Array.isArray(q.options)||q.options.length!==4||!q.options.includes(q.answer))throw Error('Invalid draft '+q.id);
}
await mkdir('app/src/main/assets',{recursive:true});
await writeFile('app/src/main/assets/trivia-beta-18000.json',JSON.stringify(out)+'\n');
console.log('Exported '+out.length+' unverified beta questions');
