/**
 * Only certified, complete filmographies may be used for lifetime count trivia.
 * Certification is an editorial review, NOT inferred from individual credit URLs.
 */
import { summarizeFilmography, ROLES } from './filmography-counts.mjs';
const ISO = /^\d{4}-\d{2}-\d{2}$/;
const allowed = new Set([...ROLES, 'total', 'mixed']);
export function validateCertification(cert, role, cutoff) {
  return !!(cert && cert.personId && cert.role === role &&
    ISO.test(cert.completeThrough || '') && cert.completeThrough >= cutoff &&
    ISO.test(cert.reviewedAt || '') &&
    Array.isArray(cert.sources) && cert.sources.length >= 2 &&
    cert.sources.every(s => /^https:\/\//.test(s)) &&
    cert.reviewStatus === 'approved' && cert.reviewer);
}
export function buildCertifiedQuestions(credits, certifications, {
  role='director', cutoff='2026-11-19', industry=null
}={}) {
  if (!allowed.has(role)) throw new Error('Unknown role');
  if (!ISO.test(cutoff)) throw new Error('Invalid cutoff');
  // For mixed-industry counts, certifications must cover the person's global filmography.
  const scoped = industry ? credits.filter(c => c.industry === industry) : credits;
  const stats = summarizeFilmography(scoped, cutoff);
  const certified = stats.filter(p => p[role] > 0 && certifications.some(c =>
    c.personId === p.personId && c.industry === (industry || 'ALL') &&
    validateCertification(c,role,cutoff)));
  certified.sort((a,b)=>b[role]-a[role] || a.personId.localeCompare(b.personId));
  const questions=[];
  for(let i=0;i+3<certified.length;i+=4) {
    const group=certified.slice(i,i+4);
    if(new Set(group.map(p=>p[role])).size!==4) continue;
    questions.push({
      id:`certified-${industry || 'all'}-${role}-${cutoff}-${i}`,
      category:'filmography',difficulty:'hard',industry:industry||'Mix & Match',
      cutoff,role,
      prompt:`Through ${cutoff}, which person has the most released feature-film credits as ${role}?`,
      options:group.map(p=>p.name),answer:group[0].name,
      explanation:group.map(p=>`${p.name}: ${p[role]}`).join('; '),
      verified:true, certificationRequired:true
    });
  }
  return questions;
}
