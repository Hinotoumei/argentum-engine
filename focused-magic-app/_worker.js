/* Focused Magic v0.6.9 — restricted InuYasha catalog-image bridge.
 * Not a general proxy. Accepts only opaque CCGTrader asset IDs carried by the
 * canonical SECOND_DRAW_INUYASHA_DATABASE_CURRENT data used by the test match.
 */
function json(status, data) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {'content-type':'application/json; charset=utf-8','cache-control':'no-store'}
  });
}
function assetId(value) {
  const v = String(value || '').trim();
  return /^[A-Za-z0-9_-]{8,80}$/.test(v) ? v : '';
}
async function inuyashaCatalogImage(value) {
  const id = assetId(value);
  if (!id) throw new Error('Invalid InuYasha catalog asset id.');
  const base = `https://api.ccgtrader.co.uk/_/assets/${id}`;
  const urls = [`${base}?key=card-medium`, `${base}?key=directus-medium-contain`, base];
  let last = '';
  for (const target of urls) {
    try {
      const r = await fetch(target, {
        redirect: 'follow',
        headers: {
          accept: 'image/avif,image/webp,image/apng,image/*,*/*;q=0.8',
          'user-agent': 'FocusedMagic-InuYasha/0.6.9'
        }
      });
      const type = String(r.headers.get('content-type') || '').toLowerCase();
      if (r.ok && type.startsWith('image/')) {
        return new Response(r.body, {
          status: 200,
          headers: {
            'content-type': type,
            'cache-control': 'public, max-age=86400',
            'x-focused-magic-inuyasha-catalog': 'v0.6.9'
          }
        });
      }
      last = `HTTP ${r.status} ${type || 'unknown-content-type'}`;
    } catch (e) {
      last = String(e?.message || e);
    }
  }
  throw new Error(`Could not read InuYasha catalog image. ${last}`);
}
export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname === '/inuyasha/catalog-image') {
      if (request.method.toUpperCase() !== 'GET') return json(405, {ok:false,error:'GET required.'});
      try { return await inuyashaCatalogImage(url.searchParams.get('asset')); }
      catch (e) { return json(502, {ok:false,error:'InuYasha catalog image unavailable.',detail:String(e?.message || e)}); }
    }
    return env.ASSETS.fetch(request);
  }
};
