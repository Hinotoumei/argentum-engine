# Focused Magic frontend source

This directory preserves the prepared September 30, 2026 frontend from the verified TIKUNARI full repair packet. Its provider is locked to the existing Focused Magic Render service. The backend is implemented directly in this repository; the archived Base64 overlay is not an active build input.

The original packet and handoff are preserved under `docs/focused-magic/recovery/2026-09-30`. Historical assessment and deployment notes inside the frontend package describe earlier builds and are not proof of the current deployment.

Local checks completed during import: provider lock and unique DOM IDs, both JavaScript syntax checks, 11 Python attack-reference tests, and 11 JavaScript parity tests. These checks do not establish user-live Resolve Attack acceptance.

Serve this directory over HTTP to inspect the app. Backend catalog admission, actual card gameplay, and InuYasha user-live A–H acceptance remain separate validation gates.
