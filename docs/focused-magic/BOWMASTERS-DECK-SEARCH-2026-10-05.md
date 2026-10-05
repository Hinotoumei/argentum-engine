# Bowmasters targeting and deck search

ChooseTargetsDecision carries targetRequirements (with their own indices) and a separate legalTargets map. Both human and automatic adapters formerly read only requirement.validTargets and returned empty selections. They now read the indexed native map; impossible mandatory pools cause the automatic response to decline rather than submit an empty target list.

Live production scenario checks passed: AI casts Orcish Bowmasters, entry target resolves damage and amass, human priority returns; separate Faithless Looting draw-two scenario resolves both target decisions and amasses to at least two power. No provider errors occurred in these runs. 65 control regressions passed.

SearchLibraryDecision now provides a card-name search box. Chrome verifies filtering, zero results and retention of previously checked cards while changing the filter. Confirm returns both originally chosen cards. This search UI check is a supplied-decision fixture; native deck legality remains server authoritative.

InuYasha art uses portrait width derived from row height and contain rather than cropping. Both board layout checks passed at 1366x680 and 1093x544. New asset query revisions force browsers to fetch the changed scripts/styles after deployment; previously reused asset versions could serve stale cached files. Public deployment remains pending ZIP upload.
