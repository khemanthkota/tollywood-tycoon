import test from 'node:test';
import assert from 'node:assert/strict';
import {buildCreditQuestions} from './credit-questions.mjs';
const mk=(personId,roles)=>({personId,personName:personId,filmId:'f1',
 title:'Example Film',releaseDate:'2020-01-01',kind:'feature',status:'released',
 industry:'Tollywood',roles,sourceUrls:['https://example.org/film'],
 reviewStatus:'approved'});
test('no invented negative distractors',()=>{
 const rows=[mk('A',['director']),mk('B',['actor']),mk('C',['actor']),
 mk('D',['actor'])];
 const q=buildCreditQuestions(rows);
 assert.equal(q.length,1);
 assert.equal(q[0].answer,'A');
});
test('pending review yields no questions',()=>{
 const rows=[mk('A',['director']),mk('B',['actor']),mk('C',['actor']),
 mk('D',['actor'])];
 rows[0].reviewStatus='pending';
 assert.equal(buildCreditQuestions(rows).length,0);
});
test('unreleased films excluded',()=>{
 const rows=[mk('A',['director']),mk('B',['actor']),mk('C',['actor']),
 mk('D',['actor'])];
 rows[0].status='announced';
 assert.equal(buildCreditQuestions(rows).length,0);
});
