Focused Magic v0.6.9 — October 4 direct-source release

FRONTEND UPLOAD
Upload this replacement ZIP's root contents to the existing seconddrawfocusedmagic Cloudflare Pages project. index.html is at the archive root. This release adds the manual opening-hand startup repair: the AI waits for both mulligan confirmations and fresh server state. The public deployment of this repair has not yet been verified. After upload, pause at the human opening-hand choice, click Keep, and verify the October profile, hand images, life totals, and exact deck versus AI.

CURRENT HOSTING
Argentum remains on the existing Render Pro service:
  service: focused-magic-argentum-067-pro
  service id: srv-datrs2rncjis739q2t00
  provider: https://focused-magic-argentum-067-pro.onrender.com
  websocket: wss://focused-magic-argentum-067-pro.onrender.com/game
No hosting migration or second paid Render service was created.

FRONTEND LINEAGE
The full playable v0.6.6/InuYasha foundation remains the frontend baseline. Valid provider/backend work from v0.6.7+ is ported forward; the stripped v0.6.7 frontend is not a valid full-app baseline.

INUYASHA
The complete InuYasha test-match surface is preserved. The user reported that it works. This MTG repair batch does not change InuYasha code.

ARGENTUM PROVIDER
The frontend defaults to the dedicated Render provider and uses storage key focused_magic_argentum_v069_render_pro. Constructed checks exact provider card identities before launch. Exclusive Nightclub is canonicalized to provider-native Oscorp Industries only for provider traffic; Gemstone Caverns remains exact.

CURRENT RENDER BACKEND
Repository: Hinotoumei/argentum-engine
Existing branch: focused-magic
Verified live backend revision: c4ce24869bbca3bbb11d315a9f8d5621938aa1be
Live deploy: dep-db1aitbncjis73bubmog
The backend builds direct fork source. The original patch archive is preserved as recovery evidence and is not a build input.
The latest batch has 83 passing dedicated scenarios; broader regression evidence is recorded in docs/focused-magic on GitHub.
The updated local frontend against the deployed server passed all three exact deck starts, confirmed AI actions, real life totals, hand-state rendering, and decoding all 75 distinct deck images. This does not establish public Cloudflare acceptance or a complete-match result.

CORS
Read-only GET/OPTIONS /api/cards/** and /api/printings access passed real HTTP regressions and deployed browser acceptance. Split and comma-containing names use the appropriate printing endpoint.

REMAINING ACCEPTANCE
Upload the frontend package to the existing Cloudflare Pages project and verify its public deployment. Browser dashboard access was unavailable in this session. Backend acceptance and the public frontend deployment are separate records.
