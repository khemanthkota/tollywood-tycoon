import test from 'node:test';
import assert from 'node:assert/strict';
import {mkdtemp,readFile,writeFile} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {execFileSync} from 'node:child_process';
test('generator handles unreviewed imports without publishing questions',async()=>{
 const dir=await mkdtemp(join(tmpdir(),'trivia-gate-'));
 const input=join(dir,'credits.json'),out=join(dir,'questions.json'),report=join(dir,'report.json');
 await writeFile(input,JSON.stringify([{personId:'Q1',personName:'Test',filmId:'Q2',title:'Example',reviewStatus:'pending'}]));
 execFileSync(process.execPath,['trivia/generate-quality-gated.mjs',input,out,report],{cwd:process.cwd()});
 assert.deepEqual(JSON.parse(await readFile(out,'utf8')),[]);
 const summary=JSON.parse(await readFile(report,'utf8'));
 assert.equal(summary.importedCredits,1);
 assert.equal(summary.accepted,0);
});
