import test from 'node:test';
import assert from 'node:assert/strict';
import {summarizeFilmography,generateCountQuestions} from './filmography-counts.mjs';
const credit=(personId,filmId,roles,overrides={})=>({
 personId,personName:personId,filmId,title:filmId,releaseDate:'2020-01-01',
 kind:'feature',status:'released',roles,sourceUrls:['https://example.org/film'],
 verifiedAt:'2026-11-19',...overrides
});
test('one film with two roles is counted once in total, once per role',()=>{
 const [p]=summarizeFilmography([credit('A','F1',['actor','director'])]);
 assert.equal(p.total,1);assert.equal(p.actor,1);assert.equal(p.director,1);assert.equal(p.mixed,1);
});
test('duplicate records do not inflate counts',()=>{
 const c=credit('A','F1',['actor']);
 const [p]=summarizeFilmography([c,c]);
 assert.equal(p.total,1);assert.equal(p.actor,1);
});
test('separate credits for the same film combine roles',()=>{
 const [p]=summarizeFilmography([credit('A','F1',['actor']),credit('A','F1',['director'])]);
 assert.equal(p.total,1);assert.equal(p.mixed,1);
});
test('unverified, unreleased and post-cutoff credits are excluded',()=>{
 const rows=[credit('A','F1',['actor']),credit('A','F2',['actor'],{sourceUrls:[]}),
 credit('A','F3',['actor'],{status:'announced'}),
 credit('A','F4',['actor'],{releaseDate:'2027-01-01'})];
 assert.equal(summarizeFilmography(rows,'2026-11-19')[0].total,1);
});
test('question generation needs four distinct counts',()=>{
 const rows=['A','B','C','D'].flatMap((p,i)=>
 Array.from({length:i+1},(_,j)=>credit(p,p+j,['director'])));
 const q=generateCountQuestions(rows,'director');
 assert.equal(q.length,1);assert.equal(q[0].answer,'D');
 assert.equal(q[0].options.length,4);
});
test('tied counts cannot create ambiguous questions',()=>{
 const rows=['A','B','C','D'].map(p=>credit(p,p+'1',['director']));
 assert.equal(generateCountQuestions(rows,'director').length,0);
});
test('invalid role is rejected',()=>assert.throws(()=>generateCountQuestions([],'composer')));
