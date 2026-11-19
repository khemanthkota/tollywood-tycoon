/**
 * Build reviewable film-credit questions from curated sourced records.
 * These ask about individual credits, NOT unverified lifetime totals.
 */
const roles = ['actor','director','producer','writer'];
const roleNames = {actor:'appeared in',director:'directed',producer:'produced',writer:'wrote'};
export function buildCreditQuestions(credits,{cutoff='2026-11-19',industry=null}={}) {
  const valid=credits.filter(c=>c && c.personId && c.personName && c.filmId && c.title &&
    c.releaseDate && c.releaseDate<=cutoff && c.kind==='feature' &&
    c.status==='released' && (!industry || c.industry===industry) &&
    Array.isArray(c.roles) && Array.isArray(c.sourceUrls) &&
    c.sourceUrls.some(s=>/^https:\/\//.test(s)) && c.reviewStatus==='approved');
  const people=[...new Map(valid.map(c=>[c.personId,c.personName])).entries()];
  const questions=[], seen=new Set();
  for(const c of valid) for(const role of c.roles) {
    if(!roles.includes(role)) continue;
    const key=`${c.filmId}:${c.personId}:${role}`;
    if(seen.has(key)) continue;
    seen.add(key);
    // Wrong choices must be known not to have this role in this film;
    // unknown credits cannot safely be used as negative distractors.
    const excluded=new Set(valid.filter(x=>x.filmId===c.filmId &&
      x.roles.includes(role)).map(x=>x.personId));
    const candidates=people.filter(([id])=>!excluded.has(id) &&
      valid.some(x=>x.personId===id && x.filmId===c.filmId &&
        x.reviewStatus==='approved' && !x.roles.includes(role) &&
        x.roles.every(r=>roles.includes(r))));
    if(candidates.length<3) continue;
    const options=[c.personName,...candidates.slice(0,3).map(x=>x[1])];
    questions.push({id:`credit-${key}`,category:'filmography',difficulty:'hard',
      prompt:`Who ${roleNames[role]} the film "${c.title}"?`,
      options,answer:c.personName,sourceUrls:c.sourceUrls,
      reviewStatus:'approved',industry:c.industry});
  }
  return questions;
}
