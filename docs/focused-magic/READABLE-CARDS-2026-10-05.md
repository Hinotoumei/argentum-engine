# Readable hand cards and preview

Hand cards now retain 118 x 166 pixels (110 x 154 on short windows). Crowded rows overlap instead of shrinking horizontally. Battlefield height follows its available row. Hover or focus shows a large picture and rules preview; clicking pins it until closed. The preview leaves controls clickable. Empty action areas no longer consume board height.

Verified 65 provider regression tests, zero failures. Browser checks at 1366 x 680 and 1093 x 544 verified hand minimum widths, panel and card bounds, loaded card images, hover preview, pinned click preview and close. Both InuYasha boards remained inside the viewport. Twenty copied battlefield cards exercised layout only, not native gameplay. Previous Bowmasters and deck-search fixes are retained.

This is a frontend package for manual Cloudflare Pages upload. It does not establish deployment or full deck acceptance. Native backend unchanged.
