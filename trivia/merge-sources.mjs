#!/usr/bin/env node
/** Merge independently obtained credit records; never mark facts verified merely because they merged. */
import {readFile,writeFile,mkdir} from 'node:fs/promises';
const inputs=process.argv.slice(2);
if(!inputs.length)throw Error('Provide at least one source JSON path');
const merged=new Map(), sourceStats={};
for(const path of inputs){
 let rows;try{rows=JSON.parse(await readFile(path,'utf8'));}catch(e){if(e.code==='ENOENT'){console.warn('Missing optional source '+path);continue;}throw e;}
 if(!Array.isArray(rows))throw Error('Expected array in '+path);
 sourceStats[path]=rows.length;
 for(const row of rows){
  if(!row.personName||!row.title||!Array.isArray(row.roles))continue;
  const id=row.filmId&&row.personId?row.filmId+':'+row.personId:
    row.title.toLowerCase().trim()+':'+(row.releaseDate||'').slice(0,4)+':'+row.personName.toLowerCase().trim();
  if(!merged.has(id))merged.set(id,{...row,roles:[...new Set(row.roles)],sourceUrls:[...new Set(row.sourceUrls||[])],reviewStatus:'pending'});
  else{
   const old=merged.get(id);
   old.roles=[...new Set([...old.roles,...row.roles])];
   old.sourceUrls=[...new Set([...old.sourceUrls,...(row.sourceUrls||[])])];
   old.reviewStatus='pending';
  }
 }
}
await mkdir('trivia/candidates',{recursive:true});
await writeFile('trivia/candidates/merged-credits.json',JSON.stringify([...merged.values()],null,2)+'\n');
console.log(JSON.stringify({sources:sourceStats,mergedCredits:merged.size,verified:0}));
