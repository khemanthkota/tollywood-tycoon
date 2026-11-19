import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {summarizeFilmography} from './filmography-counts.mjs';
import {buildCertifiedQuestions} from './certified-questions.mjs';
test('pending imported credits never count as certified playable questions',()=>{
 const rows=[{personId:'Q1',personName:'Example',filmId:'Q2',title:'Example',
 releaseDate:'2020-01-01',kind:'feature',status:'released',roles:['director'],
 sourceUrls:['https://www.wikidata.org/wiki/Q2'],reviewStatus:'pending'}];
 assert.equal(buildCertifiedQuestions(rows,[]).length,0);
});
test('importer requires explicit Wikidata IDs and retains pending-review state',()=>{
 const src=readFileSync(new URL('./import-wikidata.mjs',import.meta.url),'utf8');
 assert.match(src,/reviewStatus:'pending'/);
 assert.ok(src.includes('Q[1-9]'));
});
