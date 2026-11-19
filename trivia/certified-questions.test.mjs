import test from 'node:test';
import assert from 'node:assert/strict';
import {buildCertifiedQuestions} from './certified-questions.mjs';
const credits=['A','B','C','D'].flatMap((personId,i)=>Array.from({length:i+1},(_,j)=>({
 personId,personName:personId,filmId:personId+j,releaseDate:'2020-01-01',
 kind:'feature',status:'released',roles:['director'],industry:'Tollywood',
 sourceUrls:['https://example.com/film'],verifiedAt:'2026-11-19'
})));
const certs=['A','B','C','D'].map(personId=>({
 personId,role:'director',industry:'Tollywood',completeThrough:'2026-11-19',
 reviewedAt:'2026-11-19',reviewStatus:'approved',reviewer:'editor',
 sources:['https://example.com/a','https://example.com/b']
}));
test('no lifetime questions without completeness certification',()=>{
 assert.equal(buildCertifiedQuestions(credits,[],{industry:'Tollywood'}).length,0);
});
test('complete certified filmographies yield one comparison',()=>{
 const q=buildCertifiedQuestions(credits,certs,{industry:'Tollywood'});
 assert.equal(q.length,1);assert.equal(q[0].answer,'D');
});
test('a stale or rejected certification blocks generation',()=>{
 const stale=certs.map((c,i)=>i===0?{...c,completeThrough:'2025-01-01'}:c);
 assert.equal(buildCertifiedQuestions(credits,stale,{industry:'Tollywood'}).length,0);
 const rejected=certs.map((c,i)=>i===0?{...c,reviewStatus:'pending'}:c);
 assert.equal(buildCertifiedQuestions(credits,rejected,{industry:'Tollywood'}).length,0);
});
test('certification for one industry cannot certify another',()=>{
 assert.equal(buildCertifiedQuestions(credits,certs,{industry:'Bollywood'}).length,0);
 assert.equal(buildCertifiedQuestions(credits,certs).length,0);
});
