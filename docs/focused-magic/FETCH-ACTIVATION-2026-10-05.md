# Fetch activation client cost repair

The visual adapter incorrectly rejected provider-advertised SacrificeSelf costs. For ActivateAbility only, when the provider identifies exactly the source itself as the sole sacrifice, the adapter now merges that ID into costPayment.sacrificedPermanents. This follows the native web client payment shape. Human and AI paths share the helper; ambiguous costs remain blocked. Native engine still validates and pays tap/life/sacrifice costs.

67 frontend regression tests passed. An isolated live Render scenario using the updated visual adapter activated Scalding Tarn: life 20 to 19, Tarn to graveyard, pictured land chooser selected Island, Island onto battlefield, zero server errors. This verifies the reported activation path, not full deck acceptance. Backend unchanged. Public Cloudflare upload still required.
