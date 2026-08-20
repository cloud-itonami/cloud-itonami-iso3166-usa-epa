(ns statute.facts
  "Agency-level compliance catalog for **USA-EPA** (United States Environmental
  Protection Agency) -- the spec-basis behind this leaf's blueprint claim that
  an independent operator can run an EPA-procurement / environmental-compliance
  navigation service.

  Scope. This is the EPA-specific layer only. Government-wide U.S. federal
  statutes live in the country coordinator `cloud-itonami-iso3166-usa`'s
  `statute.facts` and are NOT duplicated here; the two catalogs compose, keyed
  `USA-EPA` -> `USA`. Sibling agency leaves (`USA-VA`, `USA-SBA`, `USA-FTC`,
  `USA-DOE`) hold their own chapters.

  Provenance. Every entry cites the official eCFR (Electronic Code of Federal
  Regulations, GPO/Office of the Federal Register) address for the smallest
  stable unit that was independently confirmed. Nothing here is fabricated:
  each `:statute/verified-label` below is the byte-exact `label_description`
  returned by the eCFR versioner API on `:statute/verified-at`, and
  `tools/verify_citations.cljs` re-fetches that API and fails if any label
  drifts.

  Why the citation and the verification URL differ. `:statute/url` is the
  canonical human address a person should open. It is deliberately NOT the
  URL that was machine-verified: fetching www.ecfr.gov from an automated
  client can return HTTP 200 with a `Federal Register :: Request Access`
  interstitial rather than the regulation, so a status-code check against it
  would report success while proving nothing. We therefore verify through the
  documented machine API (`:statute/verified-via`) and record both. Do not
  `curl` the `:statute/url` and treat a 200 as confirmation -- it is not.

  THE TRAP THIS CATALOG EXISTS TO PIN DOWN. **EPA wears two hats, and the rule
  that carries EPA's name is usually not the rule that governs the operator's
  transaction.** Every other agency leaf in this fleet has one hat: it buys.
  EPA buys *and* it is the government-wide designator whose programs every
  other agency must buy from -- that second role lives in the FAR
  (48 CFR 23.108, `Required Environmental Protection Agency purchasing
  programs`) and in title 40, not in EPA's own acquisition chapter. Three
  concrete consequences, all recorded as checked negatives in `absences`:

  1. **40 CFR part 33 does not govern selling to EPA.** Its live title is
     `Participation by Disadvantaged Business Enterprises in United States
     Environmental Protection Agency Programs`, which reads exactly like the
     rule for EPA's own contracts. It is not. 40 CFR 33.102 -- live heading
     `When do the requirements of this part apply?` -- limits the part to
     procurement under EPA **financial assistance agreements**, i.e. what a
     grantee or loan recipient does with EPA money. A firm bidding on an EPA
     contract is governed by FAR part 19 and EPAAR part 1519 instead. This is
     the same shape as the USA-VA trap: the rule named after the agency is
     about somebody else's procurement.

  2. **40 CFR part 15 no longer exists.** That part implemented the Clean Air
     Act sec. 306 / Clean Water Act sec. 508 `List of Violating Facilities`,
     under which a facility in violation was ineligible for federal contracts.
     There is now no part numbered 15 anywhere in title 40 -- the numbering
     runs 14 (`Employee Personal Property Claims`) straight to 16
     (`Implementation of Privacy Act of 1974`) -- and no node anywhere in
     titles 2, 40 or 48 whose label mentions violating facilities. Contractor
     exclusion runs through FAR subpart 9.4 and, for assistance, 2 CFR part
     1532.

  3. **40 CFR part 32 no longer exists.** EPA's debarment/suspension part is
     gone from title 40 (the numbering runs 29 to 33); nonprocurement
     debarment is 2 CFR part 1532 (`Nonprocurement Debarment and Suspension`)
     and EPA's own procurement procedure is EPAAR subpart 1509.4.

  A fourth confusion is semantic rather than structural, so it is carried by
  the CPG entries' notes rather than by `absences`: **EPA does not certify or
  approve products.** 40 CFR 247 *designates item categories* that contain
  recovered materials; the duty to buy them falls on the procuring agency
  under RCRA sec. 6002 (42 U.S.C. 6962), and the content-level guidance sits
  in non-binding Recovered Materials Advisory Notices that are not in the CFR
  at all. `EPA-certified recycled content` is not a thing that exists.

  What is genuinely EPA-only, and is the operator's actual product. EPA runs
  the most aggressive organizational-conflict-of-interest regime of any
  civilian FAR-supplement agency (EPAAR subpart 1509.5 plus five clauses at
  1552.209-70..75, including `Limitation of future contracting`), and it is
  the only agency whose contractors routinely need statute-specific access to
  confidential business information under FIFRA and TSCA (1552.235-73 and
  1552.235-75). Those, plus scientific integrity (1503.1070 / 1552.203-72),
  human subjects, laboratory animals and dual-use research, are the compliance
  surface a bidder actually has to clear. None of them are visible from the
  FAR alone.

  Extending. A regulation not in this table has NO spec-basis, full stop.
  Extend `catalog` with a real, API-confirmed citation; never invent an id,
  a URL, or a label."
  (:require [clojure.string :as str]))

(def ecfr-structure-api
  "eCFR versioner structure endpoints these entries were verified against.
  Keyed by CFR title. The date is the title's `up_to_date_as_of` at
  verification time, so the call is reproducible rather than `current`."
  {2  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-2.json"
   40 "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-40.json"
   48 "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json"})

(def catalog
  "iso3166 code -> vector of regulation entries.

  `USA-EPA` is an agency-level key (parent `USA`), matching `blueprint.edn`'s
  `:itonami.blueprint/iso3166`.

  `:statute/cfr-node` is the path from the CFR title down to the cited node,
  as [type identifier] pairs. The live gate walks the eCFR structure tree by
  this path -- it does not string-match the URL, because hierarchical
  identifiers nest as substrings of one another (`1552.209-7` is a prefix of
  `1552.209-70`, and part `15` is a prefix of part `1523`). Walking the tree by
  explicit [type identifier] steps is the only check that cannot pass by
  accident.

  `:statute/hat` says which of EPA's two roles the entry belongs to:
    :buyer      -- EPA acquiring goods and services (EPAAR, 48 CFR chapter 15)
    :designator -- EPA setting rules other agencies and grantees must follow
                   (FAR chapter 1, 40 CFR, 2 CFR chapter XV)
  Conflating these is the failure this catalog exists to prevent, so it is a
  required field rather than a note."
  {"USA-EPA"
   ;; ---------------------------------------------------------------- buyer --
   [{:statute/id            :epa/epaar-chapter
     :statute/topic         #{:procurement :epaar}
     :statute/hat           :buyer
     :statute/title         "48 CFR Chapter 15 -- Environmental Protection Agency (EPAAR)"
     :statute/cfr-title     48
     :statute/cfr-node      [["chapter" "15"]]
     :statute/url           "https://www.ecfr.gov/current/title-48/chapter-15"
     :statute/verified-label "Environmental Protection Agency"
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "EPA is a FAR-supplement agency: it has its own acquisition chapter, the
      EPAAR. This is the root of everything a bidder must clear that the FAR
      does not already say. Contrast USA-SBA, which has no chapter at all."}

    {:statute/id            :epa/contractor-qualifications-part
     :statute/topic         #{:procurement :epaar :debarment}
     :statute/hat           :buyer
     :statute/title         "48 CFR Part 1509 -- Contractor Qualifications"
     :statute/cfr-title     48
     :statute/cfr-node      [["chapter" "15"] ["subchapter" "B"] ["part" "1509"]]
     :statute/url           "https://www.ecfr.gov/current/title-48/chapter-15/subchapter-B/part-1509"
     :statute/verified-label "Contractor Qualifications"
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "Holds both EPA's debarment procedure (1509.4) and its OCI regime (1509.5).
      The two are separate: debarment is about who may contract at all, OCI is
      about which work a qualified firm may take."}

    {:statute/id            :epa/debarment-subpart
     :statute/topic         #{:procurement :debarment}
     :statute/hat           :buyer
     :statute/title         "48 CFR Subpart 1509.4 -- Debarment, Suspension and Ineligibility"
     :statute/cfr-title     48
     :statute/cfr-node      [["chapter" "15"] ["subchapter" "B"] ["part" "1509"]
                             ["subpart" "1509.4"]]
     :statute/url           "https://www.ecfr.gov/current/title-48/chapter-15/subchapter-B/part-1509/subpart-1509.4"
     :statute/verified-label "Debarment, Suspension and Ineligibility"
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "EPA's PROCUREMENT debarment procedure, supplementing FAR subpart 9.4.
      Note the spelling difference from the FAR heading (`Debarment, Suspension
      and Ineligibility` here, `Debarment, Suspension, and Ineligibility` in
      FAR 9.4) -- recorded byte-exactly because a fuzzy match would hide a
      future retitling. For assistance agreements the rule is 2 CFR 1532
      instead; see `absences`."}

    {:statute/id            :epa/oci-subpart
     :statute/topic         #{:procurement :oci}
     :statute/hat           :buyer
     :statute/title         "48 CFR Subpart 1509.5 -- Organizational Conflicts of Interests"
     :statute/cfr-title     48
     :statute/cfr-node      [["chapter" "15"] ["subchapter" "B"] ["part" "1509"]
                             ["subpart" "1509.5"]]
     :statute/url           "https://www.ecfr.gov/current/title-48/chapter-15/subchapter-B/part-1509/subpart-1509.5"
     :statute/verified-label "Organizational Conflicts of Interests"
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "THE single largest EPA-specific barrier to entry. EPA contracts routinely
      inform rulemaking and enforcement, so the agency polices OCI harder than
      the FAR baseline: a firm that does site assessment can be barred from the
      remediation that follows. Read with 1552.209-71..75."}

    {:statute/id            :epa/oci-applicability
     :statute/topic         #{:procurement :oci}
     :statute/hat           :buyer
     :statute/title         "48 CFR 1509.502 -- OCI applicability"
     :statute/cfr-title     48
     :statute/cfr-node      [["chapter" "15"] ["subchapter" "B"] ["part" "1509"]
                             ["subpart" "1509.5"] ["section" "1509.502"]]
     :statute/url           "https://www.ecfr.gov/current/title-48/chapter-15/subchapter-B/part-1509/subpart-1509.5/section-1509.502"
     :statute/verified-label "Applicability."
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "The scope section. A one-word heading (`Applicability.`) that decides
      whether the whole subpart bites -- cited separately so an operator's
      answer can point at it rather than at the subpart as a whole."}

    {:statute/id            :epa/oci-clause
     :statute/topic         #{:procurement :oci :clause}
     :statute/hat           :buyer
     :statute/title         "48 CFR 1552.209-71 -- Organizational conflicts of interest"
     :statute/cfr-title     48
     :statute/cfr-node      [["chapter" "15"] ["subchapter" "H"] ["part" "1552"]
                             ["subpart" "1552.2"] ["section" "1552.209-71"]]
     :statute/url           "https://www.ecfr.gov/current/title-48/chapter-15/subchapter-H/part-1552/subpart-1552.2/section-1552.209-71"
     :statute/verified-label "Organizational conflicts of interest."
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "The operative contract clause. Presence of this clause in a solicitation
      is the signal that the OCI subpart applies to that acquisition."}

    {:statute/id            :epa/oci-limitation-future-contracting
     :statute/topic         #{:procurement :oci :clause}
     :statute/hat           :buyer
     :statute/title         "48 CFR 1552.209-74 -- Limitation of future contracting"
     :statute/cfr-title     48
     :statute/cfr-node      [["chapter" "15"] ["subchapter" "H"] ["part" "1552"]
                             ["subpart" "1552.2"] ["section" "1552.209-74"]]
     :statute/url           "https://www.ecfr.gov/current/title-48/chapter-15/subchapter-H/part-1552/subpart-1552.2/section-1552.209-74"
     :statute/verified-label "Limitation of future contracting."
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "The clause with the largest commercial consequence in this catalog:
      accepting one EPA contract can forfeit eligibility for later, larger ones.
      An operator who prices a bid without reading this is pricing the wrong
      thing."}

    {:statute/id            :epa/oci-annual-certification
     :statute/topic         #{:procurement :oci :clause :recurring-obligation}
     :statute/hat           :buyer
     :statute/title         "48 CFR 1552.209-75 -- Annual certification"
     :statute/cfr-title     48
     :statute/cfr-node      [["chapter" "15"] ["subchapter" "H"] ["part" "1552"]
                             ["subpart" "1552.2"] ["section" "1552.209-75"]]
     :statute/url           "https://www.ecfr.gov/current/title-48/chapter-15/subchapter-H/part-1552/subpart-1552.2/section-1552.209-75"
     :statute/verified-label "Annual certification."
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "A recurring obligation, not a one-off at award. Carried here because a
      compliance calendar that only models award-time filings will silently
      miss it."}

    {:statute/id            :epa/env-part
     :statute/topic         #{:procurement :environment :epaar}
     :statute/hat           :buyer
     :statute/title         "48 CFR Part 1523 -- Environmental, Conservation, Occupational Safety, and Drug-Free Workplace"
     :statute/cfr-title     48
     :statute/cfr-node      [["chapter" "15"] ["subchapter" "D"] ["part" "1523"]]
     :statute/url           "https://www.ecfr.gov/current/title-48/chapter-15/subchapter-D/part-1523"
     :statute/verified-label "Environmental, Conservation, Occupational Safety, and Drug-Free Workplace"
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "EPA's own supplement to FAR part 23. Note the asymmetry the whole catalog
      turns on: this is EPA constraining ITSELF as a buyer, while FAR 23.108 is
      EPA constraining everyone else."}

    {:statute/id            :epa/epp-subpart
     :statute/topic         #{:procurement :environment :green-procurement}
     :statute/hat           :buyer
     :statute/title         "48 CFR Subpart 1523.7 -- Contracting for Environmentally Preferable Products and Services"
     :statute/cfr-title     48
     :statute/cfr-node      [["chapter" "15"] ["subchapter" "D"] ["part" "1523"]
                             ["subpart" "1523.7"]]
     :statute/url           "https://www.ecfr.gov/current/title-48/chapter-15/subchapter-D/part-1523/subpart-1523.7"
     :statute/verified-label "Contracting for Environmentally Preferable Products and Services"
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "`Environmentally preferable` is a term of art defined in the FAR, not a
      marketing adjective, and not an EPA certification. See the CPG entries."}

    {:statute/id            :epa/epp-meetings
     :statute/topic         #{:procurement :environment :green-procurement}
     :statute/hat           :buyer
     :statute/title         "48 CFR 1523.703-1 -- Environmentally preferable meeting and conference facilities"
     :statute/cfr-title     48
     :statute/cfr-node      [["chapter" "15"] ["subchapter" "D"] ["part" "1523"]
                             ["subpart" "1523.7"] ["section" "1523.703-1"]]
     :statute/url           "https://www.ecfr.gov/current/title-48/chapter-15/subchapter-D/part-1523/subpart-1523.7/section-1523.703-1"
     :statute/verified-label "Acquisition of environmentally preferable meeting and conference facilities and services."
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "An unusually narrow, unusually concrete obligation: EPA regulates how it
      buys its own conference venues. Included because an operator serving
      event and logistics vendors would never find it from the FAR."}

    {:statute/id            :epa/green-meetings-clause
     :statute/topic         #{:procurement :environment :green-procurement :clause}
     :statute/hat           :buyer
     :statute/title         "48 CFR 1552.223-71 -- EPA Green Meetings and Conferences"
     :statute/cfr-title     48
     :statute/cfr-node      [["chapter" "15"] ["subchapter" "H"] ["part" "1552"]
                             ["subpart" "1552.2"] ["section" "1552.223-71"]]
     :statute/url           "https://www.ecfr.gov/current/title-48/chapter-15/subchapter-H/part-1552/subpart-1552.2/section-1552.223-71"
     :statute/verified-label "EPA Green Meetings and Conferences."
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "The clause that carries 1523.703-1 into the contract."}

    {:statute/id            :epa/human-subjects-clause
     :statute/topic         #{:procurement :research :clause}
     :statute/hat           :buyer
     :statute/title         "48 CFR 1552.223-70 -- Protection of human subjects"
     :statute/cfr-title     48
     :statute/cfr-node      [["chapter" "15"] ["subchapter" "H"] ["part" "1552"]
                             ["subpart" "1552.2"] ["section" "1552.223-70"]]
     :statute/url           "https://www.ecfr.gov/current/title-48/chapter-15/subchapter-H/part-1552/subpart-1552.2/section-1552.223-70"
     :statute/verified-label "Protection of human subjects."
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "EPA funds human-exposure research; a contractor inherits IRB-shaped duties
      that have no analogue in an ordinary services contract."}

    {:statute/id            :epa/lab-animals-clause
     :statute/topic         #{:procurement :research :clause}
     :statute/hat           :buyer
     :statute/title         "48 CFR 1552.223-72 -- Use and care of laboratory animals"
     :statute/cfr-title     48
     :statute/cfr-node      [["chapter" "15"] ["subchapter" "H"] ["part" "1552"]
                             ["subpart" "1552.2"] ["section" "1552.223-72"]]
     :statute/url           "https://www.ecfr.gov/current/title-48/chapter-15/subchapter-H/part-1552/subpart-1552.2/section-1552.223-72"
     :statute/verified-label "Use and care of laboratory animals."
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "Paired with 1523.303-72 in the EPAAR body. Both are cited so an operator
      can distinguish the policy section from the clause that binds."}

    {:statute/id            :epa/durc-clause
     :statute/topic         #{:procurement :research :clause :biosecurity}
     :statute/hat           :buyer
     :statute/title         "48 CFR 1552.235-82 -- Institutional oversight of life sciences dual use research of concern"
     :statute/cfr-title     48
     :statute/cfr-node      [["chapter" "15"] ["subchapter" "H"] ["part" "1552"]
                             ["subpart" "1552.2"] ["section" "1552.235-82"]]
     :statute/url           "https://www.ecfr.gov/current/title-48/chapter-15/subchapter-H/part-1552/subpart-1552.2/section-1552.235-82"
     :statute/verified-label "Institutional oversight of life sciences dual use research of concern."
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "Biosecurity oversight inside an environmental agency's acquisition
      regulation -- the kind of obligation that is invisible unless somebody
      has read the chapter end to end."}

    {:statute/id            :epa/fifra-cbi-access
     :statute/topic         #{:procurement :cbi :clause :criminal-exposure}
     :statute/hat           :buyer
     :statute/title         "48 CFR 1552.235-73 -- Access to FIFRA Confidential Business Information"
     :statute/cfr-title     48
     :statute/cfr-node      [["chapter" "15"] ["subchapter" "H"] ["part" "1552"]
                             ["subpart" "1552.2"] ["section" "1552.235-73"]]
     :statute/url           "https://www.ecfr.gov/current/title-48/chapter-15/subchapter-H/part-1552/subpart-1552.2/section-1552.235-73"
     :statute/verified-label "Access to Federal Insecticide, Fungicide, and Rodenticide Act Confidential Business Information (APR 1996)."
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "EPA-only. FIFRA CBI carries statutory penalties for wrongful disclosure
      that attach to the individual employee, not just to the firm. Note the
      date-in-heading convention (`(APR 1996)`), recorded byte-exactly: EPAAR
      clause headings carry their revision date, so a heading match is also a
      version match."}

    {:statute/id            :epa/tsca-cbi-access
     :statute/topic         #{:procurement :cbi :clause :criminal-exposure}
     :statute/hat           :buyer
     :statute/title         "48 CFR 1552.235-75 -- Access to TSCA Confidential Business Information"
     :statute/cfr-title     48
     :statute/cfr-node      [["chapter" "15"] ["subchapter" "H"] ["part" "1552"]
                             ["subpart" "1552.2"] ["section" "1552.235-75"]]
     :statute/url           "https://www.ecfr.gov/current/title-48/chapter-15/subchapter-H/part-1552/subpart-1552.2/section-1552.235-75"
     :statute/verified-label "Access to Toxic Substances Control Act Confidential Business Information (APR 1996)."
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "The TSCA twin of 1552.235-73. Two separate statutes, two separate access
      regimes, two separate clauses -- a contractor touching both needs both."}

    {:statute/id            :epa/scientific-integrity-policy
     :statute/topic         #{:procurement :scientific-integrity}
     :statute/hat           :buyer
     :statute/title         "48 CFR 1503.1070 -- Scientific integrity"
     :statute/cfr-title     48
     :statute/cfr-node      [["chapter" "15"] ["subchapter" "A"] ["part" "1503"]
                             ["subpart" "1503.10"] ["section" "1503.1070"]]
     :statute/url           "https://www.ecfr.gov/current/title-48/chapter-15/subchapter-A/part-1503/subpart-1503.10/section-1503.1070"
     :statute/verified-label "Scientific integrity."
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "Filed under `Contractor Code of Business Ethics and Conduct` -- EPA treats
      scientific integrity as an ethics obligation, not a technical one. That
      placement is the finding: it means the duty survives regardless of whether
      the contract is a research contract."}

    {:statute/id            :epa/scientific-integrity-clause
     :statute/topic         #{:procurement :scientific-integrity :clause}
     :statute/hat           :buyer
     :statute/title         "48 CFR 1552.203-72 -- Scientific integrity (clause)"
     :statute/cfr-title     48
     :statute/cfr-node      [["chapter" "15"] ["subchapter" "H"] ["part" "1552"]
                             ["subpart" "1552.2"] ["section" "1552.203-72"]]
     :statute/url           "https://www.ecfr.gov/current/title-48/chapter-15/subchapter-H/part-1552/subpart-1552.2/section-1552.203-72"
     :statute/verified-label "Scientific integrity."
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "Same live label as 1503.1070, different node. Recorded separately on
      purpose: a catalog keyed by label rather than by node would collapse the
      policy and the clause into one entry and lose the distinction between
      what EPA must do and what the contractor signs."}

    {:statute/id            :epa/small-business-part
     :statute/topic         #{:procurement :small-business}
     :statute/hat           :buyer
     :statute/title         "48 CFR Part 1519 -- Small Business Programs (EPAAR)"
     :statute/cfr-title     48
     :statute/cfr-node      [["chapter" "15"] ["subchapter" "D"] ["part" "1519"]]
     :statute/url           "https://www.ecfr.gov/current/title-48/chapter-15/subchapter-D/part-1519"
     :statute/verified-label "Small Business Programs"
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "THIS is the small-business path for selling to EPA -- not 40 CFR part 33.
      Its live label is identical to FAR part 19's (`Small Business Programs`),
      which is why the catalog keys on nodes: the two are distinguishable only
      by chapter."}

    {:statute/id            :epa/clauses-part
     :statute/topic         #{:procurement :clause :epaar}
     :statute/hat           :buyer
     :statute/title         "48 CFR Part 1552 -- Solicitation Provisions and Contract Clauses"
     :statute/cfr-title     48
     :statute/cfr-node      [["chapter" "15"] ["subchapter" "H"] ["part" "1552"]]
     :statute/url           "https://www.ecfr.gov/current/title-48/chapter-15/subchapter-H/part-1552"
     :statute/verified-label "Solicitation Provisions and Contract Clauses"
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "The clause bank. Every EPA-specific obligation in this catalog that binds
      a contractor does so through a section of this part."}

    ;; ----------------------------------------------------------- designator --
    {:statute/id            :epa/far-required-epa-programs
     :statute/topic         #{:green-procurement :government-wide}
     :statute/hat           :designator
     :statute/title         "48 CFR 23.108 -- Required Environmental Protection Agency purchasing programs"
     :statute/cfr-title     48
     :statute/cfr-node      [["chapter" "1"] ["subchapter" "D"] ["part" "23"]
                             ["subpart" "23.1"] ["section" "23.108"]]
     :statute/url           "https://www.ecfr.gov/current/title-48/chapter-1/subchapter-D/part-23/subpart-23.1/section-23.108"
     :statute/verified-label "Required Environmental Protection Agency purchasing programs."
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "THE HINGE OF THIS CATALOG. EPA's programs become mandatory for every
      federal buyer here, in the FAR -- in chapter 1, not EPA's chapter 15. A
      vendor selling water-efficient or chemically-intensive products to ANY
      agency is in an EPA-designated market without ever touching the EPAAR."}

    {:statute/id            :epa/far-recovered-materials
     :statute/topic         #{:green-procurement :recovered-materials :government-wide}
     :statute/hat           :designator
     :statute/title         "48 CFR 23.107-1 -- Products containing recovered materials"
     :statute/cfr-title     48
     :statute/cfr-node      [["chapter" "1"] ["subchapter" "D"] ["part" "23"]
                             ["subpart" "23.1"] ["section" "23.107-1"]]
     :statute/url           "https://www.ecfr.gov/current/title-48/chapter-1/subchapter-D/part-23/subpart-23.1/section-23.107-1"
     :statute/verified-label "Products containing recovered materials."
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "The procurement-side counterpart to 40 CFR 247. The duty to buy lives
      here and in RCRA sec. 6002; the list of what counts lives in title 40.
      Citing either alone is half an answer."}

    {:statute/id            :epa/far-ecolabels
     :statute/topic         #{:green-procurement :ecolabel :government-wide}
     :statute/hat           :designator
     :statute/title         "48 CFR 23.108-3 -- EPA Recommendations of Specifications, Standards, and Ecolabels"
     :statute/cfr-title     48
     :statute/cfr-node      [["chapter" "1"] ["subchapter" "D"] ["part" "23"]
                             ["subpart" "23.1"] ["section" "23.108-3"]]
     :statute/url           "https://www.ecfr.gov/current/title-48/chapter-1/subchapter-D/part-23/subpart-23.1/section-23.108-3"
     :statute/verified-label "Products and services that are subject to EPA Recommendations of Specifications, Standards, and Ecolabels."
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-48.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "`Recommendations` is doing real work in that heading. EPA recommends
      third-party ecolabels; it does not issue them and it does not certify
      products against them. A vendor claiming to be `EPA certified` is making
      a claim no rule in this catalog supports."}

    {:statute/id            :epa/cpg-part
     :statute/topic         #{:green-procurement :recovered-materials :government-wide}
     :statute/hat           :designator
     :statute/title         "40 CFR Part 247 -- Comprehensive Procurement Guideline for Products Containing Recovered Materials"
     :statute/cfr-title     40
     :statute/cfr-node      [["chapter" "I"] ["subchapter" "I"] ["part" "247"]]
     :statute/url           "https://www.ecfr.gov/current/title-40/chapter-I/subchapter-I/part-247"
     :statute/verified-label "Comprehensive Procurement Guideline for Products Containing Recovered Materials"
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-40.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "A DESIGNATION rule, not a certification rule. It names item categories;
      the buying duty is RCRA sec. 6002 (42 U.S.C. 6962) and FAR 23.107-1, and
      the content-level guidance sits in non-binding Recovered Materials
      Advisory Notices that are not in the CFR at all."}

    {:statute/id            :epa/cpg-applicability
     :statute/topic         #{:green-procurement :recovered-materials :threshold}
     :statute/hat           :designator
     :statute/title         "40 CFR 247.2 -- CPG applicability"
     :statute/cfr-title     40
     :statute/cfr-node      [["chapter" "I"] ["subchapter" "I"] ["part" "247"]
                             ["subpart" "A"] ["section" "247.2"]]
     :statute/url           "https://www.ecfr.gov/current/title-40/chapter-I/subchapter-I/part-247/subpart-A/section-247.2"
     :statute/verified-label "Applicability."
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-40.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "Carries the threshold that decides whether the CPG bites at all, and
      extends it to State and local agencies spending appropriated federal
      funds and to their contractors. That reach -- beyond federal buyers -- is
      why this leaf's market is larger than `firms that sell to EPA`."}

    {:statute/id            :epa/dbe-part
     :statute/topic         #{:small-business :financial-assistance :legacy-trap}
     :statute/hat           :designator
     :statute/title         "40 CFR Part 33 -- Participation by Disadvantaged Business Enterprises in EPA Programs"
     :statute/cfr-title     40
     :statute/cfr-node      [["chapter" "I"] ["subchapter" "B"] ["part" "33"]]
     :statute/url           "https://www.ecfr.gov/current/title-40/chapter-I/subchapter-B/part-33"
     :statute/verified-label "Participation by Disadvantaged Business Enterprises in United States Environmental Protection Agency Programs"
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-40.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :misread-on-its-face
     :statute/note
     "THE TRAP. The live title says `in United States Environmental Protection
      Agency Programs`, which reads as though it covers EPA's contracts. It
      does not -- see 33.102 and `absences`. This entry is operative law; what
      is false is the reading, not the rule. Never cite it to a firm bidding on
      an EPA contract without also citing EPAAR part 1519."}

    {:statute/id            :epa/dbe-applicability
     :statute/topic         #{:small-business :financial-assistance :legacy-trap}
     :statute/hat           :designator
     :statute/title         "40 CFR 33.102 -- When the DBE requirements apply"
     :statute/cfr-title     40
     :statute/cfr-node      [["chapter" "I"] ["subchapter" "B"] ["part" "33"]
                             ["subpart" "A"] ["section" "33.102"]]
     :statute/url           "https://www.ecfr.gov/current/title-40/chapter-I/subchapter-B/part-33/subpart-A/section-33.102"
     :statute/verified-label "When do the requirements of this part apply?"
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-40.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "The section that resolves the trap: it limits part 33 to procurement
      under EPA FINANCIAL ASSISTANCE agreements. Cited as its own entry so an
      operator's answer can quote the scope rule rather than assert the scope."}

    {:statute/id            :epa/nonprocurement-debarment
     :statute/topic         #{:debarment :financial-assistance}
     :statute/hat           :designator
     :statute/title         "2 CFR Part 1532 -- Nonprocurement Debarment and Suspension"
     :statute/cfr-title     2
     :statute/cfr-node      [["subtitle" "B"] ["chapter" "XV"] ["part" "1532"]]
     :statute/url           "https://www.ecfr.gov/current/title-2/subtitle-B/chapter-XV/part-1532"
     :statute/verified-label "Nonprocurement Debarment and Suspension"
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-2.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "Where EPA's old 40 CFR part 32 went. Two exclusion regimes now run in
      parallel -- this one for assistance, FAR 9.4 plus EPAAR 1509.4 for
      contracts -- and an exclusion under one is not automatically an exclusion
      under the other."}

    {:statute/id            :epa/uniform-requirements
     :statute/topic         #{:financial-assistance :grants}
     :statute/hat           :designator
     :statute/title         "2 CFR Part 1500 -- Uniform Administrative Requirements (EPA)"
     :statute/cfr-title     2
     :statute/cfr-node      [["subtitle" "B"] ["chapter" "XV"] ["part" "1500"]]
     :statute/url           "https://www.ecfr.gov/current/title-2/subtitle-B/chapter-XV/part-1500"
     :statute/verified-label "Uniform Administrative Requirements, Cost Principles, and Audit Requirements for Federal Awards"
     :statute/verified-via  "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-2.json"
     :statute/verified-at   "2026-08-19"
     :statute/status        :operative
     :statute/note
     "EPA's adoption of the government-wide grants rule. Note the number
      collision that the node-keyed design exists to survive: `part 1500` in
      title 2 is this, while `part 1500` in title 48 chapter 15 is
      `[Reserved]`. Identifier alone is not an address."}]})

(def absences
  "Checked NEGATIVES. An absence is a finding, not a gap in the catalog: each
  entry below was looked for and confirmed not to exist in the stated place,
  and carries the address that governs instead.

  These are the most valuable records here, because a missing rule is
  indistinguishable from an unsearched one unless somebody writes down that
  they searched.

  Two shapes appear here and they are checked differently:

    :absence/absent-part -- a STRUCTURAL absence. The named part does not exist
      anywhere under the given root. The live gate scans that subtree and FAILS
      if the part is found, so re-adoption by EPA breaks the build instead of
      passing quietly.

    (no :absence/absent-part) -- a SEMANTIC absence. The rule exists; what is
      false is a reading of it. Only `see-instead` can be machine-checked, so
      the offline suite carries the rest."
  [{:absence/id          :epa/dbe-rule-does-not-govern-epa-contracts
    :absence/claim       "40 CFR part 33 sets the DBE requirements for firms contracting with EPA."
    :absence/holds?      false
    :absence/checked-at  "2026-08-19"
    :absence/checked-via "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-40.json"
    :absence/see-instead
    {:statute/title          "48 CFR Part 1519 -- Small Business Programs (EPAAR)"
     :statute/cfr-title      48
     :statute/cfr-node       [["chapter" "15"] ["subchapter" "D"] ["part" "1519"]]
     :statute/url            "https://www.ecfr.gov/current/title-48/chapter-15/subchapter-D/part-1519"
     :statute/verified-label "Small Business Programs"}
    :absence/note
    "Part 33 exists and is NOT reserved -- so an existence check against it
     succeeds and proves the wrong thing, exactly as 38 CFR 74 does in the
     USA-VA leaf. The negative recorded here is not `part 33 is missing`; it is
     `part 33 is not about EPA's own contracts`. 40 CFR 33.102 confines it to
     procurement under EPA financial assistance agreements. Those differ, and
     only the second is true."}

   {:absence/id          :epa/no-list-of-violating-facilities
    :absence/claim       "40 CFR part 15 maintains the Clean Air Act sec. 306 / Clean Water Act sec. 508 List of Violating Facilities, which makes a violating facility ineligible for federal contracts."
    :absence/holds?      false
    :absence/checked-at  "2026-08-19"
    :absence/checked-via "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-40.json"
    :absence/absent-part {:statute/cfr-title 40
                          :statute/under     [["chapter" "I"]]
                          :statute/part      "15"}
    :absence/see-instead
    {:statute/title          "48 CFR Subpart 1509.4 -- Debarment, Suspension and Ineligibility"
     :statute/cfr-title      48
     :statute/cfr-node       [["chapter" "15"] ["subchapter" "B"] ["part" "1509"]
                              ["subpart" "1509.4"]]
     :statute/url            "https://www.ecfr.gov/current/title-48/chapter-15/subchapter-B/part-1509/subpart-1509.4"
     :statute/verified-label "Debarment, Suspension and Ineligibility"}
    :absence/note
    "There is no part numbered 15 anywhere in title 40: the numbering runs 14
     (`Employee Personal Property Claims`) straight to 16 (`Implementation of
     Privacy Act of 1974`). Guidance and secondary sources that still cite
     `40 CFR 15` for contractor listing are citing a part that is not there.
     Exclusion now runs through FAR subpart 9.4 with EPA's procedure at EPAAR
     1509.4, and through 2 CFR 1532 for assistance."}

   {:absence/id          :epa/no-40-cfr-32
    :absence/claim       "EPA's debarment and suspension rules are at 40 CFR part 32."
    :absence/holds?      false
    :absence/checked-at  "2026-08-19"
    :absence/checked-via "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-40.json"
    :absence/absent-part {:statute/cfr-title 40
                          :statute/under     [["chapter" "I"]]
                          :statute/part      "32"}
    :absence/see-instead
    {:statute/title          "2 CFR Part 1532 -- Nonprocurement Debarment and Suspension"
     :statute/cfr-title      2
     :statute/cfr-node       [["subtitle" "B"] ["chapter" "XV"] ["part" "1532"]]
     :statute/url            "https://www.ecfr.gov/current/title-2/subtitle-B/chapter-XV/part-1532"
     :statute/verified-label "Nonprocurement Debarment and Suspension"}
    :absence/note
    "Title 40 runs 29 (`Intergovernmental Review of Environmental Protection
     Agency Programs and Activities`) straight to 33. EPA's nonprocurement
     debarment moved to 2 CFR chapter XV with the government-wide
     consolidation. Answering `40 CFR 32` to a grantee sends them to nothing."}

   {:absence/id          :epa/no-product-certification
    :absence/claim       "EPA certifies or approves products as containing recovered materials, and a vendor can hold an EPA certification."
    :absence/holds?      false
    :absence/checked-at  "2026-08-19"
    :absence/checked-via "https://www.ecfr.gov/api/versioner/v1/structure/2026-08-18/title-40.json"
    :absence/see-instead
    {:statute/title          "48 CFR 23.108-3 -- EPA Recommendations of Specifications, Standards, and Ecolabels"
     :statute/cfr-title      48
     :statute/cfr-node       [["chapter" "1"] ["subchapter" "D"] ["part" "23"]
                              ["subpart" "23.1"] ["section" "23.108-3"]]
     :statute/url            "https://www.ecfr.gov/current/title-48/chapter-1/subchapter-D/part-23/subpart-23.1/section-23.108-3"
     :statute/verified-label "Products and services that are subject to EPA Recommendations of Specifications, Standards, and Ecolabels."}
    :absence/note
    "A semantic absence, not a structural one: 40 CFR 247 is real and operative,
     but it DESIGNATES item categories rather than certifying products, and the
     FAR heading that governs the ecolabel side says `Recommendations`. No rule
     in this catalog creates an EPA product certification. This one cannot be
     machine-checked by absence -- a heading cannot prove a negative about the
     world -- so the offline suite pins the reading instead."}])

(defn entries
  "All catalog entries for an iso3166 code. Returns [] for an unknown code --
  callers must not treat an unknown jurisdiction as an empty-but-valid one."
  [iso]
  (get catalog iso []))

(defn by-topic
  "Entries under `iso` carrying `topic`."
  [iso topic]
  (filterv #(contains? (:statute/topic %) topic) (entries iso)))

(defn by-hat
  "Entries under `iso` belonging to one of EPA's two roles, `:buyer` or
  `:designator`. Retrieving by hat is the operation this catalog exists to make
  possible: an answer that mixes the two is the failure mode."
  [iso hat]
  (filterv #(= hat (:statute/hat %)) (entries iso)))

(defn legacy-traps
  "Entries whose live text reads as governing something it does not. These are
  the entries a compliance answer must never cite without also citing
  `absences`."
  [iso]
  (filterv #(contains? (:statute/topic %) :legacy-trap) (entries iso)))

(defn citation-count
  "Number of catalog entries under `iso`."
  [iso]
  (count (entries iso)))

(defn- node->str [node]
  (str/join " > " (map (fn [[t i]] (str t ":" i)) node)))

(defn describe
  "Human-readable one-line rendering of an entry, for operator output."
  [e]
  (str (:statute/title e)
       " [" (name (:statute/status e)) "/" (name (:statute/hat e)) "]"
       " (" (node->str (:statute/cfr-node e)) ")"))
