# Business Model: Independent EPA Environmental-Procurement Compliance Service — United States

## Classification

- Repository: `cloud-itonami-iso3166-usa-epa`
- ISO 3166 (agency-level): `USA-EPA`, parent `USA`
- Ooyake cross-reference: `gov.usa.epa` (Environmental Protection Agency)
- Activity: EPA acquisition eligibility and environmental-compliance documentation

## Customer

- an operator already using `cloud-itonami-iso3166-usa` whose contract
  touches Environmental Protection Agency rules or buying channels
- a foreign SME entering a Environmental Protection Agency-specific public program for the first time

## Offer

- walkthrough and evidence checklist for: EPA acquisition eligibility and environmental-compliance documentation
- ongoing regulatory-change monitoring for this body's public sources
- compliance-audit export package

## Trust Controls

- `:filing/submit` never auto-commits at any phase
- fabricated regulatory claims are HARD holds
- not legal advice — cite https://www.epa.gov/

## Boundary

- **`cloud-itonami-iso3166-usa`**: country coordinator (general U.S. market entry)
- **`com-etzhayyim-ooyake`**: read-only civic atlas (never acts as the body)
