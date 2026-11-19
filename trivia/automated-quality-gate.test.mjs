import test from 'node:test';
import assert from 'node:assert/strict';
import {assessQuestion,filterQuestionBatch} from './automated-quality-gate.mjs';
const q={prompt:'Which director was responsible for the 1960 film Psycho?',options:['Alfred Hitchcock','Charlie Chaplin','Orson Welles','Billy Wilder'],answer:'Alfred Hitchcock',explanation:'The director credit is supported by independent published film records.',sources:['https://www.bfi.org.uk/films-tv-people/','https://www.britannica.com/biography/Alfred-Hitchcock']};
test('accepts well-formed sourced question',()=>assert.equal(assessQuestion(q).accepted,true));
test('rejects leaked answer',()=>assert.ok(assessQuestion({...q,prompt:'Did Alfred Hitchcock direct the 1960 film Psycho?'}).reasons.includes('answer leaked in prompt')));
test('rejects same-domain sources',()=>assert.ok(assessQuestion({...q,sources:['https://www.bfi.org.uk/a','https://www.bfi.org.uk/b']}).reasons.includes('fewer than two source domains')));
test('rejects repeated question',()=>assert.equal(filterQuestionBatch([q,q]).summary.rejected,1));
