(ns statute.facts-test
  "Offline invariants for the USA-EPA compliance catalog.

  These complement `tools/verify_citations.cljs`, which is the LIVE gate and
  needs network. Nothing here re-checks the eCFR; these tests pin the shape and
  the substantive claims, so that a later edit cannot quietly drop the finding
  this catalog exists to carry.

  Every test below is written so that emptying the catalog makes it FAIL rather
  than pass vacuously. A suite that goes green on an empty catalog measures
  nothing."
  (:require [clojure.test :refer [deftest testing is]]
            [kotoba.lang.text :as str]
            [statute.facts :as f]))

(def iso "USA-EPA")
(def entries (f/entries iso))

;; ---------------------------------------------------------------- evidence --

(deftest catalog-is-not-empty
  (testing "the catalog carries citations at all (floor against vacuous pass)"
    (is (>= (count entries) 25)
        "USA-EPA must carry at least 25 citations; a shrunken catalog is a regression")))

(deftest unknown-jurisdiction-is-not-an-empty-valid-one
  (testing "an unknown iso code yields [] and is distinguishable from a real one"
    (is (= [] (f/entries "USA-ZZ")))
    (is (not= (f/entries "USA-ZZ") entries))))

;; ------------------------------------------------------------------- shape --

(deftest every-entry-is-well-formed
  (doseq [e entries]
    (testing (str (:statute/id e))
      (is (keyword? (:statute/id e)))
      (is (string? (:statute/title e)))
      (is (integer? (:statute/cfr-title e)))
      (is (set? (:statute/topic e)))
      (is (seq (:statute/topic e)) "an entry with no topic cannot be retrieved by topic")
      (is (string? (:statute/verified-label e)))
      (is (seq (:statute/verified-label e)))
      (is (str/starts-with? (:statute/url e) "https://www.ecfr.gov/"))
      (is (str/starts-with? (:statute/verified-via e)
                            "https://www.ecfr.gov/api/versioner/v1/structure/")
          "verification must go through the machine API, not the human page")
      (is (contains? #{:operative :stale-on-its-face :misread-on-its-face}
                     (:statute/status e)))
      (is (contains? #{:buyer :designator} (:statute/hat e))
          "every entry must say which of EPA's two roles it belongs to"))))

(deftest ids-are-unique
  (let [ids (map :statute/id entries)]
    (is (= (count ids) (count (distinct ids))))))

(deftest node-paths-descend
  (testing "each cfr-node is a non-empty vector of [type identifier] string pairs"
    (doseq [e entries]
      (let [n (:statute/cfr-node e)]
        (is (vector? n) (str (:statute/id e)))
        (is (seq n) (str (:statute/id e) " must not have an empty node path"))
        (doseq [step n]
          (is (= 2 (count step)) (str (:statute/id e) " step " (pr-str step)))
          (is (every? string? step) (str (:statute/id e) " step " (pr-str step))))))))

(deftest node-paths-are-unique
  (testing "two entries must not point at the same node"
    (let [nodes (map :statute/cfr-node entries)]
      (is (= (count nodes) (count (distinct nodes)))
          "a duplicated node path means one of the two entries is mislabelled"))))

(deftest every-title-has-a-declared-api-endpoint
  (testing "the live gate can actually resolve every title the catalog cites"
    (doseq [e entries]
      (is (contains? f/ecfr-structure-api (:statute/cfr-title e))
          (str (:statute/id e) " cites CFR title " (:statute/cfr-title e)
               " but no structure endpoint is declared for it")))))

(deftest verified-at-is-not-in-the-future
  (let [today (str (java.time.LocalDate/now java.time.ZoneOffset/UTC))]
    (doseq [e entries]
      (is (<= (compare (:statute/verified-at e) today) 0)
          (str (:statute/id e) " claims to have been verified at "
               (:statute/verified-at e) ", which is after today " today)))))

;; ------------------------------------------------------- the substantive claim

(deftest both-hats-are-populated
  (testing "the two-role split is the finding; neither side may be empty"
    (is (seq (f/by-hat iso :buyer))
        "EPA-as-buyer (EPAAR) entries must exist")
    (is (seq (f/by-hat iso :designator))
        "EPA-as-designator (FAR/40 CFR/2 CFR) entries must exist")
    (is (= (count entries)
           (+ (count (f/by-hat iso :buyer)) (count (f/by-hat iso :designator))))
        "every entry belongs to exactly one hat")))

(deftest buyer-entries-live-in-epas-own-chapter
  (testing "EPAAR entries are exactly the title-48 chapter-15 ones"
    (doseq [e (f/by-hat iso :buyer)]
      (is (= 48 (:statute/cfr-title e)) (str (:statute/id e)))
      (is (= ["chapter" "15"] (first (:statute/cfr-node e)))
          (str (:statute/id e) " is filed as :buyer but is not in 48 CFR chapter 15")))))

(deftest designator-entries-are-not-in-the-epaar
  (testing "the whole point: EPA's government-wide reach lives OUTSIDE its own chapter"
    (doseq [e (f/by-hat iso :designator)]
      (is (not= ["chapter" "15"] (first (:statute/cfr-node e)))
          (str (:statute/id e) " is filed as :designator but sits in the EPAAR")))))

(deftest far-23-108-is-the-hinge
  (testing "the FAR node that makes EPA's programs binding on every agency is cited"
    (let [e (first (filter #(= :epa/far-required-epa-programs (:statute/id %)) entries))]
      (is (some? e) "48 CFR 23.108 must be in the catalog")
      (is (= :designator (:statute/hat e)))
      (is (= "Required Environmental Protection Agency purchasing programs."
             (:statute/verified-label e)))
      (is (= ["chapter" "1"] (first (:statute/cfr-node e)))
          "23.108 is in FAR chapter 1, not EPA's chapter 15 -- that is the finding"))))

(deftest the-dbe-misreading-is-recorded
  (testing "40 CFR 33 is present AND marked as not governing EPA's own contracts"
    (let [part33 (first (filter #(= :epa/dbe-part (:statute/id %)) entries))]
      (is (some? part33) "the trap entry must exist")
      (is (= 40 (:statute/cfr-title part33)))
      (is (= :misread-on-its-face (:statute/status part33))
          "40 CFR 33 must not be presented as governing EPA contracts")
      (is (contains? (:statute/topic part33) :legacy-trap))))
  (testing "and the scope section that resolves it is cited separately"
    (let [s102 (first (filter #(= :epa/dbe-applicability (:statute/id %)) entries))]
      (is (some? s102))
      (is (= "When do the requirements of this part apply?"
             (:statute/verified-label s102))))))

(deftest legacy-traps-are-findable
  (testing "the entries that must never be cited alone are retrievable as a set"
    (let [traps (f/legacy-traps iso)]
      (is (seq traps) "the whole point of this catalog is the trap set; it must not be empty")
      (is (every? #(contains? (:statute/topic %) :legacy-trap) traps))
      (is (some #(= 40 (:statute/cfr-title %)) traps)
          "at least one trap must be in EPA's own title 40"))))

(deftest epa-only-compliance-surface-is-cited
  (testing "the obligations invisible from the FAR alone are all present"
    (let [ids (set (map :statute/id entries))]
      (doseq [required [:epa/oci-subpart
                        :epa/oci-limitation-future-contracting
                        :epa/fifra-cbi-access
                        :epa/tsca-cbi-access
                        :epa/scientific-integrity-clause]]
        (is (contains? ids required)
            (str required " is part of the EPA-only surface this leaf sells; dropping it"
                 " would leave the blueprint claim without a spec-basis"))))))

(deftest cbi-clauses-are-two-not-one
  (testing "FIFRA and TSCA are separate statutes with separate access clauses"
    (let [cbi (f/by-topic iso :cbi)]
      (is (= 2 (count cbi)) "collapsing FIFRA and TSCA into one entry loses a real obligation")
      (is (= #{"48 CFR 1552.235-73 -- Access to FIFRA Confidential Business Information"
               "48 CFR 1552.235-75 -- Access to TSCA Confidential Business Information"}
             (set (map :statute/title cbi)))))))

(deftest scientific-integrity-policy-and-clause-are-distinct-nodes
  (testing "same live label, different nodes -- keying on label would merge them"
    (let [both (filter #(contains? (:statute/topic %) :scientific-integrity) entries)]
      (is (= 2 (count both)))
      (is (= 1 (count (distinct (map :statute/verified-label both))))
          "they really do share a label; that is why the catalog keys on nodes")
      (is (= 2 (count (distinct (map :statute/cfr-node both))))))))

;; ---------------------------------------------------------------- absences --

(deftest every-absence-is-well-formed
  (is (seq f/absences) "absences are findings; an empty vector is a regression")
  (doseq [a f/absences]
    (testing (str (:absence/id a))
      (is (keyword? (:absence/id a)))
      (is (string? (:absence/claim a)))
      (is (boolean? (:absence/holds? a)))
      (is (string? (:absence/checked-at a)))
      (is (str/starts-with? (:absence/checked-via a)
                            "https://www.ecfr.gov/api/versioner/v1/structure/"))
      (is (some? (:absence/see-instead a))
          "an absence with no redirect leaves the operator nowhere to go"))))

(deftest structural-absences-are-machine-checkable
  (testing "at least the two removed parts carry :absence/absent-part so the live gate can scan for them"
    (let [structural (filter :absence/absent-part f/absences)]
      (is (>= (count structural) 2)
          "40 CFR 15 and 40 CFR 32 are structural absences; both must be scannable")
      (is (= #{"15" "32"}
             (set (map #(get-in % [:absence/absent-part :statute/part]) structural))))
      (doseq [a structural]
        (let [p (:absence/absent-part a)]
          (is (integer? (:statute/cfr-title p)))
          (is (vector? (:statute/under p)))
          (is (seq (:statute/under p))
              "an absence scoped to an empty root would scan the whole title and mean nothing"))))))

(deftest dbe-absence-points-at-the-rule-that-governs
  (testing "the DBE misreading is recorded as NOT holding, and redirects to EPAAR 1519"
    (let [a (first (filter #(= :epa/dbe-rule-does-not-govern-epa-contracts (:absence/id %))
                           f/absences))]
      (is (some? a))
      (is (false? (:absence/holds? a))
          "the claim `40 CFR 33 governs EPA contracts` must be recorded as false")
      (is (nil? (:absence/absent-part a))
          "part 33 EXISTS -- recording it as structurally absent would be wrong")
      (let [s (:absence/see-instead a)]
        (is (= 48 (:statute/cfr-title s)))
        (is (= [["chapter" "15"] ["subchapter" "D"] ["part" "1519"]] (:statute/cfr-node s)))
        (is (= "Small Business Programs" (:statute/verified-label s)))))))

(deftest removed-parts-redirect-somewhere-real
  (testing "both removed parts send the reader to a live address"
    (doseq [id [:epa/no-list-of-violating-facilities :epa/no-40-cfr-32]]
      (let [a (first (filter #(= id (:absence/id %)) f/absences))]
        (is (some? a) (str id " must be recorded"))
        (is (false? (:absence/holds? a)))
        (is (seq (get-in a [:absence/see-instead :statute/cfr-node]))
            (str id " must redirect to a resolvable node"))))))

(deftest certification-absence-is-semantic-not-structural
  (testing "the `EPA certifies products` claim is false without any part being missing"
    (let [a (first (filter #(= :epa/no-product-certification (:absence/id %)) f/absences))]
      (is (some? a))
      (is (false? (:absence/holds? a)))
      (is (nil? (:absence/absent-part a))
          "40 CFR 247 exists; the falsehood is the reading, not the absence of a rule")
      (is (str/includes? (get-in a [:absence/see-instead :statute/verified-label])
                         "Recommendations")
          "the redirect must land on the heading that says `Recommendations`"))))

;; ------------------------------------------------------------------ helpers --

(deftest by-topic-retrieves
  (is (seq (f/by-topic iso :procurement)))
  (is (seq (f/by-topic iso :oci)))
  (is (seq (f/by-topic iso :green-procurement)))
  (is (= [] (f/by-topic iso :no-such-topic))))

(deftest by-hat-rejects-unknown-hats
  (is (= [] (f/by-hat iso :neither))))

(deftest citation-count-matches
  (is (= (count entries) (f/citation-count iso))))

(deftest describe-renders-status-hat-and-path
  (let [e (first entries)]
    (is (str/includes? (f/describe e) (:statute/title e)))
    (is (str/includes? (f/describe e) (name (:statute/status e))))
    (is (str/includes? (f/describe e) (name (:statute/hat e))))))

(deftest readme-count-matches-catalog
  (testing "the README's advertised counts cannot drift from the data"
    (let [readme (slurp "README.md")
          claimed-citations (some-> (re-find #"\*\*(\d+) citations\*\*" readme) second parse-long)
          claimed-absences  (some-> (re-find #"\*\*(\d+) checked absences\*\*" readme) second parse-long)]
      (is (some? claimed-citations) "README must state a citation count")
      (is (some? claimed-absences) "README must state an absence count")
      (is (= (count entries) claimed-citations)
          (str "README says " claimed-citations " citations, catalog has " (count entries)))
      (is (= (count f/absences) claimed-absences)
          (str "README says " claimed-absences " absences, catalog has " (count f/absences))))))
