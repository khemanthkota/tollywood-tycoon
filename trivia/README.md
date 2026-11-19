# Filmography count trivia: data quality and integration

Run `node --test trivia/filmography-counts.test.mjs` with Node 18+.

## Data model
Each credit identifies a stable person ID and film ID, release date, role(s), source URLs, and verification date. One released feature film is counted once per person in the total, even if that person has multiple roles. Role-specific counts can overlap. The mixed count counts films with two or more roles.

## Critical editorial rule
**A verified individual credit is not proof of a complete filmography.** Do not publish questions that claim lifetime totals or rankings until a reviewer has confirmed that each candidate's filmography is complete through the stated cutoff. The current generator is a development tool for curated, complete datasets only. A source URL's presence is not independent source verification.

## Pending
- Populate a comprehensive reviewed credits dataset for each supported industry.
- Add a person-level completeness certification and require it in the generator.
- Add language/industry filtering, appearance-type exclusions, milestone/debut questions, plausible distractors and review workflow.
- Connect question data to the actual trivia gameplay. The current Android MainActivity is a starter screen, not a trivia UI.
- Execute tests in CI and validate against authoritative filmography references.
