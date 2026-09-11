# cloud-itonami-iso3166-usa-epa

Open ISO 3166 **agency-level** Blueprint for **USA-EPA**: Environmental Protection Agency
(parent country: **USA**).

This leaf designs a forkable OSS business for an independent operator
navigating **Environmental Protection Agency**-specific public-procurement / regulatory compliance
(EPA acquisition eligibility and environmental-compliance documentation), composing with the country coordinator
`cloud-itonami-iso3166-usa`.

## The catalog

`src/statute/facts.cljk` carries **30 citations** and **4 checked absences** for
`USA-EPA`, every one of them confirmed against the official eCFR versioner API
rather than transcribed from a secondary source. Each entry records the
byte-exact `label_description` the API returned, the date it returned it, and
the machine endpoint it came from.

```clojure
(require '[statute.facts :as f])

(f/citation-count "USA-EPA")          ;=> 30
(f/by-hat "USA-EPA" :buyer)           ;=> EPAAR obligations (48 CFR chapter 15)
(f/by-hat "USA-EPA" :designator)      ;=> what EPA makes everyone else do
(f/by-topic "USA-EPA" :oci)           ;=> the organizational-conflict regime
(f/legacy-traps "USA-EPA")            ;=> entries that must never be cited alone
f/absences                            ;=> claims that were checked and are false
```

### EPA wears two hats, and that is the whole finding

Every other agency leaf in this fleet has one role: it buys. EPA buys **and** it
is the government-wide designator whose programs every other federal buyer must
follow. Those two roles live in different titles of the CFR, so the rule that
carries EPA's name is usually not the rule that governs a given transaction.
`:statute/hat` is a required field on every entry for exactly this reason.

| hat | where it lives | example |
|---|---|---|
| `:buyer` | 48 CFR chapter 15 (EPAAR) | `1552.209-74` Limitation of future contracting |
| `:designator` | 48 CFR chapter 1 (FAR), 40 CFR, 2 CFR chapter XV | `23.108` Required Environmental Protection Agency purchasing programs |

### The four checked absences

An absence is a finding, not a gap: somebody looked, and wrote down that they
looked. Two of these are **structural** — the part is not there, and the live
gate scans for it on every run, so re-adoption breaks the build rather than
leaving a stale negative sitting in the catalog looking verified.

1. **40 CFR part 33 does not govern selling to EPA.** Its title reads as though
   it does; `33.102` confines it to procurement under EPA *financial assistance*
   agreements. Bidders are governed by FAR part 19 and EPAAR part 1519.
2. **40 CFR part 15 is gone** (structural). The Clean Air Act §306 / Clean Water
   Act §508 List of Violating Facilities is not in title 40 — the numbering runs
   14 straight to 16. Exclusion runs through FAR subpart 9.4 and EPAAR 1509.4.
3. **40 CFR part 32 is gone** (structural). Nonprocurement debarment is
   2 CFR part 1532.
4. **EPA does not certify products.** 40 CFR 247 *designates* item categories;
   FAR 23.108-3 says *Recommendations*. "EPA-certified" is not a status that
   exists.

### What the operator actually sells

The obligations that are invisible from the FAR alone, and that a bidder must
clear anyway: EPA's organizational-conflict-of-interest regime (subpart 1509.5
plus five clauses at `1552.209-70`…`-75`, including one that can forfeit
eligibility for follow-on work), FIFRA and TSCA confidential-business-information
access (`1552.235-73` and `-75`, two separate statutes and two separate clauses),
scientific integrity filed under contractor ethics (`1503.1070` / `1552.203-72`),
human subjects, laboratory animals, and dual-use research oversight.

## Gates

Two, and they check different things. Neither can pass by accident.

```bash
# offline: shape and substance. Fails if the catalog is emptied, if an entry
# loses its hat, if the DBE trap stops being marked, or if README counts drift.
clojure -M:test

# live: re-fetches the eCFR API. Confirms every citation byte-exactly AND that
# every part recorded as absent is still absent.
#   exit 0 verified / 1 drifted / 2 could-not-answer
nbb tools/verify_citations.cljk
```

The live gate refuses to report a pass it did not earn. A run that reached zero
citations, or that carries no scannable absence, exits **2** — deliberately
neither 0 nor 1, because "nothing was checked" and "nothing was wrong" must not
share an exit code.

## What this is NOT

- **Not Environmental Protection Agency.** Commercial compliance navigation only.
- **Not legal advice.** Cite official sources; route licensed work to counsel.
- **Not a certification.** Neither this repo nor EPA certifies any product or firm.

## Official surface

- https://www.epa.gov/
- https://www.ecfr.gov/current/title-48/chapter-15 — EPAAR
- https://www.ecfr.gov/current/title-40/chapter-I/subchapter-I/part-247 — CPG
- https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json — the machine endpoint the catalog was verified against

## Capability layer

Resolves via `kotoba-lang/iso3166` (`USA-EPA`, parent `USA`).

## License

AGPL-3.0-or-later.
