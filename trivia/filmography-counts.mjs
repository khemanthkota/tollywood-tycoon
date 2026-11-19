/**
 * Filmography-count trivia engine. Input is a curated list of film credits, not raw
 * person-profile headline counts. Never publish unverified questions.
 *
 * Credit: {personId, personName, filmId, title, releaseDate, kind:"feature",
 * status:"released", roles:["actor","director"], sourceUrls:["https://..."],
 * verifiedAt:"2026-11-19", industry:"Tollywood", appearance?:"cameo"}
 */
export const ROLES = ["actor", "director", "producer", "writer"];
const isVerified = c => c && c.personId && c.filmId &&
  c.kind === "feature" && c.status === "released" &&
  /^\\d{4}-\\d{2}-\\d{2}$/.test(c.releaseDate || "") &&
  Array.isArray(c.sourceUrls) && c.sourceUrls.some(u => /^https:\\/\\//.test(u)) &&
  /^\\d{4}-\\d{2}-\\d{2}$/.test(c.verifiedAt || "") &&
  Array.isArray(c.roles) && c.roles.some(r => ROLES.includes(r));
export function summarizeFilmography(credits, cutoff = "9999-12-31") {
  const byPerson = new Map();
  for (const c of credits) {
    if (!isVerified(c) || c.releaseDate > cutoff) continue;
    if (!byPerson.has(c.personId)) byPerson.set(c.personId, {
      personId:c.personId, name:c.personName, films:new Map()
    });
    const p = byPerson.get(c.personId);
    if (!p.films.has(c.filmId)) p.films.set(c.filmId, new Set());
    c.roles.filter(r => ROLES.includes(r)).forEach(r => p.films.get(c.filmId).add(r));
  }
  return [...byPerson.values()].map(p => {
    const counts = Object.fromEntries(ROLES.map(r => [r,0]));
    let mixed = 0;
    for (const roles of p.films.values()) {
      for (const role of roles) counts[role]++;
      if (roles.size > 1) mixed++;
    }
    return {personId:p.personId, name:p.name, total:p.films.size,
      ...counts, mixed};
  });
}
/** Returns comparison questions only when all 4 options have verified, distinct totals. */
export function generateCountQuestions(credits, role = "director", cutoff = "2026-11-19") {
  if (![...ROLES,"total","mixed"].includes(role)) throw Error("Unknown role");
  const people = summarizeFilmography(credits, cutoff)
    .filter(p => p[role] > 0).sort((a,b) => b[role]-a[role] || a.name.localeCompare(b.name));
  const questions = [];
  for (let i=0; i+3<people.length; i+=4) {
    const options = people.slice(i,i+4);
    if (new Set(options.map(p => p[role])).size !== 4) continue;
    questions.push({
      id:`film-count-${role}-${cutoff}-${i}`,
      category:"filmography", difficulty:"hard", cutoff,
      prompt:`As of ${cutoff}, who has the most verified released feature-film credits as ${role} among these four?`,
      options:options.map(p=>p.name), answer:options[0].name,
      explanation:options.map(p=>`${p.name}: ${p[role]}`).join("; "),
      verified:true
    });
  }
  return questions;
}
