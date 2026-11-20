/**
 * Automated quality gate for imported trivia candidates.
 * Passing this gate is not a guarantee that source facts are correct.
 */
const NORMALIZE = s => String(s || '').normalize('NFKC').trim().toLowerCase().replace(/\s+/g,' ');
const SOURCE_URL = /^https:\/\/[^\s/]+\.[^\s/]+\//i;
const HOST = url => { try { return new globalThis.URL(url).hostname.replace(/^www\./,''); } catch { return ''; } };
export function assessQuestion(q, {seen=new Set(), requireIndependentSources=true}={}) {
  const reasons=[];
  if (!q || typeof q !== 'object') return {accepted:false,reasons:['invalid question']};
  const prompt=String(q.prompt||'').trim();
  const options=Array.isArray(q.options)?q.options:[];
  const normalized=options.map(NORMALIZE);
  const answer=NORMALIZE(q.answer);
  if(prompt.length<35 || prompt.length>350) reasons.push('prompt length');
  if(options.length!==4 || normalized.some(x=>!x) || new Set(normalized).size!==4) reasons.push('four distinct options required');
  if(normalized.filter(x=>x===answer).length!==1) reasons.push('answer must match exactly one option');
  if(answer.length>=4 && NORMALIZE(prompt).includes(answer)) reasons.push('answer leaked in prompt');
  if(!q.explanation || String(q.explanation).trim().length<20) reasons.push('insufficient explanation');
  const sources=Array.isArray(q.sources)?q.sources:[];
  if(!sources.length || sources.some(s=>typeof s!=='string' || !SOURCE_URL.test(s))) reasons.push('missing valid source URLs');
  if(requireIndependentSources && new Set(sources.map(HOST).filter(Boolean)).size<2) reasons.push('fewer than two source domains');
  if(/\b(how many|most films|total films|film count|credits|filmography)\b/i.test(prompt) && !/^\d{4}-\d{2}-\d{2}$/.test(q.cutoff||'')) reasons.push('film counts require cutoff date');
  const key=NORMALIZE(prompt).replace(/[^a-z0-9]+/g,' ')+'|'+[...normalized].sort().join('|');
  if(seen.has(key)) reasons.push('duplicate');
  if(!reasons.length) seen.add(key);
  return {accepted:reasons.length===0,reasons,key};
}
export function filterQuestionBatch(questions,opts={}) {
  const seen=new Set(), accepted=[],rejected=[];
  for(const q of questions) {
    const result=assessQuestion(q,{...opts,seen});
    (result.accepted?accepted:rejected).push(result.accepted?q:{question:q,reasons:result.reasons});
  }
  return {accepted,rejected,summary:{total:questions.length,accepted:accepted.length,rejected:rejected.length}};
}
