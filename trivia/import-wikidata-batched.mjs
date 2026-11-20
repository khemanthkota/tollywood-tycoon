#!/usr/bin/env node
/** Resilient bounded Wikidata imports. Each language/year window is independent. */
import {mkdir,writeFile} from 'node:fs/promises';
const languages={te:'Q8097',hi:'Q1568',ta:'Q5885',ml:'Q36236',kn:'Q33673',en:'Q1860'};
const windows=[['1950','1969'],['1970','1989'],['1990','2004'],['2005','2014'],['2015','2019'],['2020','2026']];
const merged=new Map(),failures=[];
const wait=ms=>new Promise(resolve=>setTimeout(resolve,ms));
for(const [language,qid] of Object.entries(languages)) for(const [from,to] of windows){
 const query=`SELECT DISTINCT ?film ?filmLabel ?person ?personLabel ?role ?date WHERE {
 VALUES ?role { wdt:P161 wdt:P57 wdt:P162 wdt:P58 }
 ?film wdt:P364 wd:${qid}; ?role ?person; wdt:P577 ?date.
 FILTER(?date >= "${from}-01-01T00:00:00Z"^^xsd:dateTime && ?date < "${Number(to)+1}-01-01T00:00:00Z"^^xsd:dateTime)
 SERVICE wikibase:label { bd:serviceParam wikibase:language "en,mul". }
} LIMIT 900`;
 let data=null;
 for(let attempt=1;attempt<=2;attempt++){
  try{
   const url='https://query.wikidata.org/sparql?query='+encodeURIComponent(query);
   const res=await fetch(url,{headers:{Accept:'application/sparql-results+json','User-Agent':'CinemaTycoonTriviaImporter/0.2 (film research)'},signal:AbortSignal.timeout(25000)});
   if(!res.ok)throw Error('HTTP '+res.status);
   data=await res.json();break;
  }catch(e){if(attempt===2)failures.push({language,from,to,error:String(e)});else await wait(1200);}
 }
 if(data)for(const x of data.results.bindings){
  const filmId=x.film.value.split('/').pop(),personId=x.person.value.split('/').pop();
  const role=({'P161':'actor','P57':'director','P162':'producer','P58':'writer'})[x.role.value.split('/').pop()];
  if(!role)continue;
  const key=filmId+':'+personId;
  if(!merged.has(key))merged.set(key,{personId,personName:x.personLabel?.value||personId,filmId,title:x.filmLabel?.value||filmId,
   releaseDate:x.date.value.slice(0,10),kind:'feature',status:'released',roles:[],language,industry:language,
   sourceUrls:[x.film.value,x.person.value],reviewStatus:'pending',provenance:'Wikidata candidate; not independently verified'});
  const row=merged.get(key);if(!row.roles.includes(role))row.roles.push(role);
 }
 console.log(JSON.stringify({language,from,to,records:merged.size,failures:failures.length}));
 await wait(300);
}
await mkdir('trivia/candidates',{recursive:true});
await writeFile('trivia/candidates/wikidata-credits.json',JSON.stringify([...merged.values()],null,2)+'\n');
await writeFile('trivia/candidates/import-report.json',JSON.stringify({records:merged.size,failedWindows:failures},null,2)+'\n');
console.log(JSON.stringify({imported:merged.size,failedWindows:failures.length,verified:0}));
