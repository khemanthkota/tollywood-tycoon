#!/usr/bin/env node
/** Optional TMDb importer. Configure TMDB_READ_TOKEN in GitHub Actions secrets. */
import {mkdir,writeFile} from 'node:fs/promises';
const token=process.env.TMDB_READ_TOKEN;
if(!token){console.log('TMDb skipped: TMDB_READ_TOKEN not configured');process.exit(0);}
const languages=['te','hi','ta','ml','kn','en'];
const rows=[];
async function get(path){
 const res=await fetch('https://api.themoviedb.org/3'+path,{headers:{Authorization:'Bearer '+token,Accept:'application/json'},signal:AbortSignal.timeout(25000)});
 if(!res.ok)throw Error('TMDb '+res.status+' for '+path);
 return res.json();
}
for(const language of languages){
 for(let page=1;page<=3;page++){
  const discovery=await get('/discover/movie?with_original_language='+language+'&sort_by=popularity.desc&include_adult=false&page='+page);
  for(const film of discovery.results||[]){
   if(!film.id||!film.release_date)continue;
   const credits=await get('/movie/'+film.id+'/credits');
   const people=[
    ...(credits.cast||[]).slice(0,12).map(x=>({person:x,role:'actor'})),
    ...(credits.crew||[]).filter(x=>['Director','Writer','Screenplay','Producer'].includes(x.job)).map(x=>({person:x,role:x.job==='Director'?'director':x.job==='Producer'?'producer':'writer'}))
   ];
   for(const {person,role} of people){
    if(!person.id||!person.name)continue;
    rows.push({personId:'tmdb-person-'+person.id,personName:person.name,
     filmId:'tmdb-film-'+film.id,title:film.title||film.original_title,
     releaseDate:film.release_date,kind:'feature',status:'released',roles:[role],
     industry:language,sourceUrls:['https://www.themoviedb.org/movie/'+film.id],
     reviewStatus:'pending',provenance:'TMDb API import; unverified'});
   }
  }
 }
}
await mkdir('trivia/candidates',{recursive:true});
await writeFile('trivia/candidates/tmdb-credits.json',JSON.stringify(rows,null,2)+'\n');
console.log(JSON.stringify({source:'TMDb',credits:rows.length,verified:0}));
