#!/usr/bin/env node
/**
 * Import Wikidata film credits as UNREVIEWED candidates, never as playable trivia.
 * Usage: node trivia/import-wikidata.mjs Q12345 [Q67890 ...]
 * Produces trivia/candidates/wikidata-credits.json (local only; review before commit).
 * Requires Node 18+ and network access.
 */
import {mkdir,writeFile} from 'node:fs/promises';
const ids=process.argv.slice(2);
if(!ids.length || ids.some(id=>!/^Q[1-9]\d*$/.test(id))) {
 console.error('Usage: node trivia/import-wikidata.mjs Q12345 [Q67890 ...]');
 process.exitCode=2;
} else {
 const props={actor:'P161',director:'P57',producer:'P162',writer:'P58'};
 const query=`SELECT DISTINCT ?film ?filmLabel ?person ?personLabel ?role ?date ?languageLabel WHERE {
 VALUES ?person { ${ids.map(id=>'wd:'+id).join(' ')} }
 VALUES ?role { ${Object.values(props).map(p=>'wdt:'+p).join(' ')} }
 ?film ?role ?person; wdt:P31 wd:Q11424; wdt:P577 ?date.
 OPTIONAL { ?film wdt:P364 ?language. }
 SERVICE wikibase:label { bd:serviceParam wikibase:language "en,mul". }
} LIMIT 10000`;
 const url='https://query.wikidata.org/sparql?query='+encodeURIComponent(query);
 const res=await fetch(url,{headers:{Accept:'application/sparql-results+json',
  'User-Agent':'CinemaTycoonTriviaImporter/0.1 (educational; manual review required)'}});
 if(!res.ok) throw Error('Wikidata HTTP '+res.status);
 const data=await res.json();
 const rows=data.results.bindings.map(({film,filmLabel,person,personLabel,role,date,languageLabel})=>({
   personId:person.value.split('/').pop(),personName:personLabel?.value||'',
   filmId:film.value.split('/').pop(),title:filmLabel?.value||'',
   releaseDate:date.value.slice(0,10),kind:'feature',status:'released',
   roles:[Object.keys(props).find(k=>role.value.endsWith('/'+props[k]))].filter(Boolean),
   language:languageLabel?.value||null,industry:null,
   sourceUrls:[film.value,person.value],verifiedAt:null,reviewStatus:'pending',
   provenance:'Wikidata candidate — requires independent filmography review'
 }));
 // Merge all roles for the same person/film, without double counting.
 const merged=new Map();
 for(const row of rows){
  const key=row.personId+':'+row.filmId;
  if(!merged.has(key)) merged.set(key,row);
  else merged.get(key).roles=[...new Set([...merged.get(key).roles,...row.roles])];
 }
 await mkdir('trivia/candidates',{recursive:true});
 await writeFile('trivia/candidates/wikidata-credits.json',
  JSON.stringify([...merged.values()],null,2)+'\n');
 console.log('Saved '+merged.size+' pending-review credits. Playable verified questions: 0.');
}
