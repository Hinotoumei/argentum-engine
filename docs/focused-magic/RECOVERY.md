# Focused Magic source recovery

Recovered on 2026-09-29 from the user-supplied v0.6.9 archive.

- Original ZIP: `recovery/original-v0.6.9.zip`
- ZIP SHA-256: `81bec2d917515286dc55c04784003918bab584a07af697d27b6d5a3284a272e1`
- Decoded deployed payload: `recovery/deployed-payload/`
- Decoded archive SHA-256: `5dffa810521a6ac588f552eae186521e787328c326b440c6d4921d8c59de777d`
- Original deployment pin: `e4fcc1621812670c58c001a80df9c71eced69a1c`
- Fork starting commit: `842023e60cfd25075c9a6a7f77945515112496d8`
- Destination: existing `Hinotoumei/argentum-engine` branch `focused-magic`.

The original archive preserves the frontend, exact deck fixtures, historical deployment recipe, and original patches. The decoded deployed payload preserves the exact backend patch and ten overrides. Those backend changes have been reconstructed as direct source changes in this checkout. The original Base64 payload is evidence only; it is not a build input for the fork.

Recovery is not gameplay acceptance. The recovered Gemstone implementation contains card-specific engine logic and an unreported counter mutation that require repair. Scalding Tarn and Faithless Looting already have native definitions; their gameplay and Focused Magic adapter decisions still require behavioral tests. Scope remains the exact Modern 60 versus Dimir Tempo 60 main decks, with 42 unique identities, as recorded in the latest checkpoint.

No upstream writes or Render changes have been made. The recovered source has not yet passed compilation or gameplay verification.
