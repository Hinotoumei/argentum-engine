(function () {
  'use strict';

  const VERSION = '0.6.9';
  const ARGENTUM_COMMIT = 'e4fcc1621812670c58c001a80df9c71eced69a1c';
  const DEFAULT_PROVIDER_ORIGIN = 'https://focused-magic-argentum-067-pro.onrender.com';
  const DEFAULT_PROVIDER_WS = 'wss://focused-magic-argentum-067-pro.onrender.com/game';
  const STORAGE_KEY = 'focused_magic_argentum_v069_render_pro';
  const LEGACY_PROVIDER_STORAGE_KEYS = ['focused_magic_argentum_v065','focused_magic_argentum_v067','focused_magic_argentum_v067_render_pro','focused_magic_argentum_v068_render_pro'];

  const OCTOBER_DECK = `2 Bilbo, Thief in the Night
1 Blood Crypt
4 Blood Scrivener
1 Commit // Memory
3 Currency Converter
1 Demonfire
2 Erebos's Intervention
3 Exclusive Nightclub
3 Gamble
1 Geier Reach Sanitarium
2 Infernal Tutor
1 Island
1 Keldon Megaliths
3 Library of Leng
3 Lotus Petal
3 Monument to Endurance
1 Mountain
1 Notion Thief
2 One with Nothing
3 Page, Loose Leaf
4 Prismari Command
1 Reforge the Soul
3 Scalding Tarn
1 Sire of Insanity
2 Starting Town
1 Steam Vents
1 Swamp
1 The Biblioplex
2 Visionary's Dance
3 Xander's Lounge`;

  const MANDATORY_DECK = `2 Blood Crypt
2 Bloodstained Mire
4 Desperate Ritual
3 Exclusive Nightclub
3 Faithless Looting
4 Gemstone Caverns
2 Infernal Tutor
4 Jace's Machinations
4 Kasmina, Enigma Sage
2 Keldon Megaliths
4 Manamorphose
4 Monument to Endurance
3 One with Nothing
4 Prismari Command
4 Reweave
2 Scalding Tarn
1 Sire of Insanity
2 Starting Town
2 Steam Vents
4 Way of the Pyromancer`;

  const PAGE_LEGACY_DECK = `2 Blood Crypt
4 Blood Scrivener
1 Chandra, Spark Hunter
2 Currency Converter
2 Cutthroat il-Dal
1 Demonfire
2 Gamble
2 Gathan Raiders
1 Geier Reach Sanitarium
2 Infernal Tutor
1 Island
3 Izzet Charm
3 Jagged Poppet
1 Keldon Megaliths
3 Library of Leng
3 Lotus Petal
1 Mountain
1 One with Nothing
2 Oscorp Industries
3 Page, Loose Leaf
2 Persist
2 Prismari Command
4 Scalding Tarn
1 Sire of Insanity
4 Steam Vents
1 Swamp
4 Taste for Mayhem
1 The Biblioplex
2 Visionary's Dance

SIDEBOARD:
2 Surgical Extraction`;

  const DIMIR_TEMPO_DECK = `2 Barrowgoyf
4 Brainstorm
3 Daze
3 Fatal Push
3 Flow State
1 Force of Negation
4 Force of Will
1 Island
2 Kaito, Bane of Nightmares
2 Misty Rainforest
2 Murktide Regent
4 Orcish Bowmasters
4 Polluted Delta
4 Ponder
1 Scalding Tarn
1 Sheoldred's Edict
1 Swamp
4 Tamiyo, Inquisitive Student
4 Thoughtseize
1 Undercity Sewers
4 Underground Sea
1 Verdant Catacombs
4 Wasteland

SIDEBOARD:
1 Barrowgoyf
3 Consign to Memory
2 Dauthi Voidwalker
1 Engineered Explosives
2 Force of Negation
2 Hydroblast
1 Long Goodbye
1 Null Rod
1 Sheoldred's Edict
1 Toxic Deluge`;

  const CORE_DECK = `24 Mountain
4 Mons's Goblin Raiders
4 Goblin Piker
4 Gray Ogre
4 Hurloon Minotaur
4 Hill Giant
4 Shock
4 Lightning Strike
4 Volcanic Hammer
4 Lava Axe`;

  const FIXTURES = Object.freeze({
    october: { label: 'October 3 uploaded deck', user: OCTOBER_DECK, ai: DIMIR_TEMPO_DECK, expected: [60, 0, 60, 15] },
    mandatory: { label: 'v0.6.5 mandatory deck', user: MANDATORY_DECK, ai: DIMIR_TEMPO_DECK, expected: [60, 0, 60, 15] },
    legacy: { label: 'Page, Loose Leaf legacy', user: PAGE_LEGACY_DECK, ai: DIMIR_TEMPO_DECK, expected: [61, 2, 60, 15] },
    core: { label: 'Core Match', user: CORE_DECK, ai: CORE_DECK, expected: [60, 0, 60, 0] },
  });

  const $ = (id) => document.getElementById(id);
  const esc = (s) => String(s ?? '').replace(/[&<>"']/g, c => ({ '&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;' }[c]));
  const clone = (x) => JSON.parse(JSON.stringify(x));
  const cardImageCache = new Map();

  // Focused Magic canonical-name bridge. The deck/UI may use a Universes Within /
  // alternate-printing name while upstream Argentum registers the mechanically identical
  // card under its original name. This is a name bridge only; card behavior remains server-authoritative.
  const PROVIDER_NAME_ALIASES = Object.freeze({
    'Exclusive Nightclub': 'Oscorp Industries',
  });

  function providerCardName(name) { return PROVIDER_NAME_ALIASES[name] || name; }
  function providerDeckMap(deck) {
    const out = {};
    for (const [name, qty] of Object.entries(deck || {})) {
      const providerName = providerCardName(name);
      out[providerName] = (out[providerName] || 0) + Number(qty || 0);
    }
    return out;
  }

  function providerDeckSpec(spec) {
    return { ...spec, main: providerDeckMap(spec?.main), sideboard: providerDeckMap(spec?.sideboard) };
  }

  function isPublicUpstream(_cfg) {
    // Legacy public upstream is deliberately unreachable from this build.
    return false;
  }

  function parseDecklist(text) {
    const main = new Map(), sideboard = new Map();
    let target = main;
    for (const raw of String(text || '').replace(/^\uFEFF/, '').split(/\r?\n/)) {
      let line = raw.trim();
      if (!line) continue;
      if (/^sideboard\s*:?$/i.test(line)) { target = sideboard; continue; }
      if (/^sb\s*:/i.test(line)) { target = sideboard; line = line.replace(/^sb\s*:\s*/i, ''); }
      const m = line.match(/^(\d+)\s+(.+?)(?:\s+\[[^\]]+\])?(?:\s+\([^)]*\)\s*\d+)?$/);
      if (!m) throw new Error(`Cannot parse deck line: ${line}`);
      const qty = Number(m[1]);
      const name = m[2].replace(/\s*<[^>]+>\s*/g, ' ').trim();
      if (!Number.isInteger(qty) || qty < 1 || qty > 99 || !name) throw new Error(`Invalid deck line: ${line}`);
      target.set(name, (target.get(name) || 0) + qty);
    }
    const asObject = (m) => Object.fromEntries([...m.entries()]);
    return {
      main: asObject(main),
      sideboard: asObject(sideboard),
      mainCount: [...main.values()].reduce((a,b)=>a+b,0),
      sideboardCount: [...sideboard.values()].reduce((a,b)=>a+b,0),
      uniqueNames: [...new Set([...main.keys(), ...sideboard.keys()])],
    };
  }

  function countsFromCards(cards) {
    const out = {};
    for (const c of cards || []) {
      const n = typeof c === 'string' ? c : c?.def?.name || c?.name;
      if (!n) continue;
      out[n] = (out[n] || 0) + 1;
    }
    return out;
  }

  function assertExactFixture(key) {
    const f = FIXTURES[key];
    if (!f) throw new Error(`Unknown fixture ${key}`);
    const u = parseDecklist(f.user), a = parseDecklist(f.ai);
    const got = [u.mainCount,u.sideboardCount,a.mainCount,a.sideboardCount];
    if (got.some((n,i)=>n!==f.expected[i])) throw new Error(`${f.label} exact-count check failed: ${got.join('/')} expected ${f.expected.join('/')}`);
    return { fixture:key, label:f.label, user:u, ai:a, counts:got };
  }

  function clearLegacyProviderConfig() {
    try {
      for (const key of LEGACY_PROVIDER_STORAGE_KEYS) localStorage.removeItem(key);
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ origin: DEFAULT_PROVIDER_ORIGIN, ws: DEFAULT_PROVIDER_WS, enabled: true, locked: true }));
    } catch (_e) {}
  }

  function providerConfig() {
    // TIKUNARI authority: the Focused Magic provider is pinned to the current Render service.
    // Do not accept stale localStorage, injected globals, or a prior WingedSheep origin.
    clearLegacyProviderConfig();
    return { origin: DEFAULT_PROVIDER_ORIGIN, ws: DEFAULT_PROVIDER_WS, enabled: true, locked: true };
  }

  function saveConfig(_cfg) {
    // Compatibility entry point retained for callers, but endpoint mutation is deliberately disabled.
    clearLegacyProviderConfig();
    return providerConfig();
  }

  function ensureUI() {
    if ($('argentumModal')) return;
    const modal = document.createElement('div');
    modal.id = 'argentumModal';
    modal.className = 'modal argentum-modal';
    modal.setAttribute('aria-hidden','true');
    modal.innerHTML = `
      <div class="modal-panel argentum-panel">
        <div class="argentum-head">
          <div><h2>Focused Magic v0.6.9 — Argentum Match Engine</h2><p class="sub">Focused Magic rules server • server-authoritative rules, legal actions, decisions, combat, hidden information and AI</p></div>
          <button id="argentumClose" type="button">Close</button>
        </div>
        <div class="argentum-config">
          <label>Provider origin<input id="argentumOrigin" spellcheck="false"></label>
          <label>WebSocket<input id="argentumWs" spellcheck="false"></label>
          <label class="argentum-check"><input id="argentumEnabled" type="checkbox"> Use Argentum for Magic matches</label>
          <button id="argentumSave" type="button">Save provider</button>
          <button id="argentumReconnect" type="button">Reconnect</button>
          <label class="argentum-check"><input id="argentumAutopilot" type="checkbox"> Autopilot my seat</label>
        </div>
        <div class="argentum-assessment">
          <button id="argentumAssessAll" class="primary" type="button">Run mandatory Constructed assessment</button>
          <button id="argentumPlayMandatory" class="good" type="button">Play new deck vs Dimir</button>
          <button id="argentumPlayLegacy" class="good" type="button">Play Page legacy vs Dimir</button>
          <span id="argentumStatus" class="status">Provider idle.</span>
        </div>
        <div id="argentumAssessmentReport" class="argentum-report"></div>
        <div id="argentumGame" class="argentum-game hidden">
          <div id="argentumScore" class="argentum-score"></div>
          <section><h3>Opponent</h3><div id="argentumOpponentZones" class="argentum-zones"></div></section>
          <section><h3>Stack</h3><div id="argentumStack" class="argentum-zone"></div></section>
          <section><h3>You</h3><div id="argentumMyZones" class="argentum-zones"></div></section>
          <section class="argentum-controls"><h3>Legal actions</h3><div id="argentumActions" class="argentum-actions"></div></section>
          <section class="argentum-controls"><h3>Decision</h3><div id="argentumDecision" class="argentum-decision sub">No pending decision.</div></section>
          <details><summary>Provider event log</summary><pre id="argentumLog" class="argentum-log"></pre></details>
        </div>
      </div>`;
    document.body.appendChild(modal);
    const cfg = providerConfig();
    $('argentumOrigin').value = cfg.origin;
    $('argentumWs').value = cfg.ws;
    $('argentumEnabled').checked = true;
    $('argentumClose').onclick = closeUI;
    modal.addEventListener('click', e => { if (e.target === modal) closeUI(); });
    $('argentumSave').onclick = () => { saveConfig(); setStatus('Provider is locked to the current Focused Magic Render service.'); };
    $('argentumReconnect').onclick = () => active?.reconnect();
    $('argentumAssessAll').onclick = runMandatoryAssessment;
    $('argentumPlayMandatory').onclick = () => startFixture('mandatory');
    $('argentumPlayLegacy').onclick = () => startFixture('legacy');

    const tb = document.querySelector('.toolbar');
    if (tb && !$('argentumBtn')) {
      const b = document.createElement('button'); b.id='argentumBtn'; b.className='good'; b.textContent='Argentum Engine'; b.onclick=openUI;
      const status = $('status'); tb.insertBefore(b, status || null);
    }
  }

  function ensureTabletopUI() {
    if ($('argentumTabletopModal')) return;
    const modal=document.createElement('div');
    modal.id='argentumTabletopModal';
    modal.className='modal argentum-tabletop-modal';
    modal.setAttribute('aria-hidden','true');
    modal.innerHTML=`
      <div class="modal-panel argentum-tabletop-panel">
        <div class="argentum-tabletop-topbar">
          <div><h2>Focused Magic — Constructed</h2><div id="argentumTabletopStatus" class="status">Connecting to Argentum…</div></div>
          <div class="actions"><button id="argentumTabletopDebug" type="button">Diagnostics</button><button id="argentumTabletopClose" type="button">Close</button></div>
        </div>
        <div id="argentumTabletopScore" class="argentum-score"></div>
        <div class="argentum-tabletop-board">
          <section class="argentum-tabletop-player opponent"><div class="argentum-tabletop-playerhead"><b id="argentumTabletopOppName">Opponent</b><span>Life <b id="argentumTabletopOppLife">?</b></span><span>Hand <b id="argentumTabletopOppHandCount">?</b></span><span>Library <b id="argentumTabletopOppLibCount">?</b></span></div><div class="argentum-tabletop-zone-title">Opponent battlefield</div><div id="argentumTabletopOppBattlefield" class="argentum-tabletop-cards"></div></section>
          <section class="argentum-tabletop-stack"><div class="argentum-tabletop-zone-title">Stack</div><div id="argentumTabletopStack" class="argentum-tabletop-cards compact"></div></section>
          <section class="argentum-tabletop-player self"><div class="argentum-tabletop-playerhead"><b id="argentumTabletopMyName">You</b><span>Life <b id="argentumTabletopMyLife">?</b></span><span>GY <b id="argentumTabletopMyGyCount">0</b></span><span>Library <b id="argentumTabletopMyLibCount">?</b></span></div><div class="argentum-tabletop-zone-title">Your battlefield</div><div id="argentumTabletopMyBattlefield" class="argentum-tabletop-cards"></div></section>
          <aside id="argentumTabletopPiles" class="argentum-tabletop-piles"></aside><section class="argentum-tabletop-hand"><div class="argentum-tabletop-zone-title">Your hand — tap a card to play/cast</div><div id="argentumTabletopMyHand" class="argentum-tabletop-cards hand"></div></section>
        </div>
        <div id="argentumTabletopPlayerEffects" class="sub"></div>
        <section id="argentumTabletopCardActions" class="argentum-tabletop-actions"><span class="sub">Select a card or use Pass/Continue.</span></section>
        <section id="argentumTabletopTurnActions" class="argentum-tabletop-actions"></section>
        <section class="argentum-tabletop-decision"><div id="argentumTabletopDecision" class="sub">No pending decision.</div></section>
        <div class="argentum-tabletop-footer"><button id="argentumTabletopPass" class="good" type="button">Pass / Continue</button><span class="sub">Choose a card to act. Click a pile to inspect it.</span></div>
      </div>`;
    document.body.appendChild(modal);
    $('argentumTabletopClose').onclick=()=>{active?.close();closeTabletopUI();};
    $('argentumTabletopDebug').onclick=()=>openUI();
    $('argentumTabletopPass').onclick=()=>{
      const info=(active?.legalActions||[]).find(a=>/PassPriority/i.test(a.actionType||''));
      if(info) submitLegalAction(info); else setTabletopStatus('Pass is not currently legal.',true);
    };
  }

  function openTabletopUI(){ensureTabletopUI();$('argentumTabletopModal').classList.add('open');$('argentumTabletopModal').setAttribute('aria-hidden','false');}
  function closeTabletopUI(){hideCardPreview(true);$('argentumTabletopModal')?.classList.remove('open');$('argentumTabletopModal')?.setAttribute('aria-hidden','true');}
  function setTabletopStatus(text,bad=false){ensureTabletopUI();const el=$('argentumTabletopStatus');el.textContent=text;el.classList.toggle('bad-text',!!bad);}
  function takeTestChoice() {
    const q = window.__ARGENTUM_TEST_CHOICES;
    return Array.isArray(q) ? q.shift() : undefined;
  }
  function hasNativeDialog(dialog) { return typeof dialog.showModal === 'function'; }
  function noPromptFallback(message='This browser cannot show the Focused Magic chooser.') { throw new Error(message); }
  async function ensureCardImage(name) {
    if(!name||cardImageCache.has(name))return cardImageCache.get(name)||'';
    const cfg=providerConfig();
    try{
      const canonical=providerCardName(name);
      // Spring's batch binding splits a single query value on commas; the path route
      // preserves those names. Split-card slashes require the query route instead.
      const single=canonical.includes(',')&&!canonical.includes('/');
      const route=single?`/api/cards/${encodeURIComponent(canonical)}/printings`:`/api/printings?names=${encodeURIComponent(canonical)}`;
      const r=await fetch(`${cfg.origin}${route}`,{headers:{Accept:'application/json'}});
      if(!r.ok)throw new Error(`HTTP ${r.status}`);
      const payload=await r.json();
      const rows=single?payload:(payload[canonical]||[]);
      const row=Array.isArray(rows)?rows.find(r=>r.imageUri||r.image_uri||r.imageURL||r.imageUrl):rows;
      const uri=row?.imageUri||row?.image_uri||row?.imageURL||row?.imageUrl||row?.normalImageUri||row?.card_faces?.[0]?.image_uri||'';
      cardImageCache.set(name,uri||'');
      return uri||'';
    }catch(_e){cardImageCache.set(name,'');return '';}
  }

  function stateZone(state,owner,type){return state?.zones?.find(z=>zoneOwner(z)===owner&&String(zoneType(z)).toUpperCase()===type)||null;}
  function recursiveContainsId(v,id,depth=0){
    if(depth>8||v==null)return false;
    if(String(v)===String(id))return true;
    if(Array.isArray(v))return v.some(x=>recursiveContainsId(x,id,depth+1));
    if(typeof v==='object')return Object.values(v).some(x=>recursiveContainsId(x,id,depth+1));
    return false;
  }
  function actionsForCard(id){
    return (active?.legalActions||[]).filter(info=>{
      if(recursiveContainsId(info?.action,id))return true;
      if(recursiveContainsId(info?.sourceId,id)||recursiveContainsId(info?.cardId,id)||recursiveContainsId(info?.sourceEntityId,id))return true;
      return false;
    });
  }
  function providerTableCard(state,id,zone){
    const c=cardById(state,id);
    const b=document.createElement('button');b.type='button';b.className='argentum-tabletop-card';b.dataset.cardId=id;
    if(!c){b.classList.add('hidden-card');b.textContent='Hidden';b.disabled=true;return b;}
    const art=c.imageUri||c.imageURL||c.imageUrl||cardImageCache.get(c.name)||'';
    b.innerHTML=art?`<img src="${esc(art)}" alt="${esc(c.name)}"><span><b>${esc(c.name)}</b></span>`:`<span class="argentum-tabletop-cardname"><b>${esc(c.name)}</b><small>${esc(c.manaCost||'')} ${esc(c.typeLine||'')}</small></span>`;
    if(!art&&c.name)ensureCardImage(c.name).then(uri=>{if(uri&&document.body.contains(b)&&!b.querySelector('img')){const img=document.createElement('img');img.src=uri;img.alt=c.name;b.prepend(img);const label=b.querySelector('.argentum-tabletop-cardname');if(label)label.innerHTML=`<b>${esc(c.name)}</b>`;}});
    // Display the provider's projected values, including X=0; never derive card rules here.
    if(c.power!=null&&c.toughness!=null)b.innerHTML+=`<small class="argentum-tabletop-stat">${esc(c.power)}/${esc(c.toughness)}</small>`;
    if(c.chosenX!=null)b.innerHTML+=`<small class="argentum-tabletop-x">X=${esc(c.chosenX)}</small>`;
    for(const [type,count] of Object.entries(c.counters||{})){
      if(count>0)b.innerHTML+=`<small class="argentum-tabletop-counter">${esc(type.toLowerCase().replaceAll('_',' '))}: ${esc(count)}</small>`;
    }
    b.title=c.stackText||c.oracleText||c.name||'';
    if(c.isTapped)b.classList.add('tapped');
    b.onmouseenter=()=>showCardPreview(c);b.onfocus=()=>showCardPreview(c);b.onmouseleave=()=>hideCardPreview();b.onblur=()=>hideCardPreview();
    b.onclick=()=>{showCardActions(id,zone);showCardPreview(c,true);};
    return b;
  }
  let previewPinned=false;
  function hideCardPreview(force=false){if(previewPinned&&!force)return;previewPinned=false;const preview=$('argentumCardPreview');if(preview)preview.hidden=true;}
  function showCardPreview(card,pinned=false){
    if(!card||(previewPinned&&!pinned))return;let preview=$('argentumCardPreview');
    if(!preview){preview=document.createElement('aside');preview.id='argentumCardPreview';preview.className='argentum-card-preview';document.body.append(preview);}
    previewPinned=pinned;preview.replaceChildren();preview.hidden=false;
    const close=document.createElement('button');close.type='button';close.textContent='Close card preview';close.onclick=()=>hideCardPreview(true);preview.append(close);
    const name=document.createElement('h3');name.textContent=card.name;preview.append(name);
    const art=card.imageUri||card.imageURL||card.imageUrl||cardImageCache.get(card.name);
    if(art){const img=document.createElement('img');img.src=art;img.alt=card.name;preview.append(img);}
    const rules=document.createElement('p');rules.textContent=card.oracleText||card.stackText||'';preview.append(rules);
  }
  function fitCardRow(el){
    const cards=[...el.querySelectorAll('.argentum-tabletop-card')];
    if(!cards.length)return;
    for(const card of cards)card.style.marginLeft='0px';
    const width=cards[0].getBoundingClientRect().width;
    const available=el.clientWidth-12;
    const overlap=cards.length>1?Math.min(0,(available-cards.length*width)/(cards.length-1)-5):0;
    cards.forEach((card,index)=>{if(index)card.style.marginLeft=overlap+'px';});
  }
  function fillTableZone(id,state,zone){const el=$(id);if(!el)return;el.innerHTML='';if(!zone){el.innerHTML='<span class="sub">—</span>';return;}if(zone.isVisible===false){el.innerHTML=`<span class="sub">${zone.size??0} hidden cards</span>`;return;}for(const cardId of zone.cardIds||[])el.appendChild(providerTableCard(state,cardId,zoneType(zone)));if(typeof requestAnimationFrame==='function')requestAnimationFrame(()=>fitCardRow(el));if(!(zone.cardIds||[]).length)el.innerHTML='<span class="sub">Empty</span>';}
  if(typeof window.addEventListener==='function')window.addEventListener('resize',()=>document.querySelectorAll('.argentum-tabletop-cards').forEach(fitCardRow));
  function showCardActions(id,zone){
    ensureTabletopUI();if(active)active.selectedCard={id,zone};const box=$('argentumTabletopCardActions');const c=cardById(active?.state,id);const actions=actionsForCard(id).filter(a=>a.isAffordable!==false);
    box.innerHTML=`<div class="argentum-tabletop-action-title"><b>${esc(c?.name||id)}</b><span class="sub">${esc(zone||'')}</span></div>`;
    if(c?.name==='Currency Converter'){const help=document.createElement('span');help.className='sub';help.textContent='Discard → choose to exile it from your graveyard. Then tap Converter to return an exiled land for a Treasure, or a nonland for a Rogue. Drawing/discarding taps Converter, so its other tap ability needs it untapped again.';box.append(help);}
    if(!actions.length){const note=document.createElement('span');note.className='sub';note.textContent='No provider-advertised legal action for this card right now.';box.appendChild(note);return;}
    for(const info of actions){const b=document.createElement('button');b.type='button';b.className='good';b.textContent=info.description||info.actionType||'Use action';b.onclick=()=>submitLegalAction(info);box.appendChild(b);}
  }
  function renderTabletopDecision(){
    const box=$('argentumTabletopDecision');if(!box)return;const d=active?.pendingDecision;
    if(active?.user?.mulliganPrompt){const prompt=active.user.mulliganPrompt;box.innerHTML='<b>Opening hand decision</b> ';const keep=document.createElement('button');keep.className='good';keep.textContent='Keep';keep.onclick=()=>active.submitOpening(active.user,'keepHand',null,prompt);const mul=document.createElement('button');mul.textContent='Mulligan';mul.onclick=()=>active.submitOpening(active.user,'mulligan',null,prompt);box.append(keep,mul);return;}
    if(active?.user?.bottomPrompt){active.renderBottomCards(active.user.bottomPrompt,'argentumTabletopDecision');return;}
    if(active?.user?.openingRequestPending){box.textContent='Waiting for the new opening hand…';return;}
    if(!d){box.textContent='';return;}
    box.innerHTML=`<b>${esc(d.prompt||d.type)}</b> `;const b=document.createElement('button');b.className='primary';b.textContent='Resolve decision';b.onclick=async()=>{try{const r=await decisionResponse(d,'human');if(r)active.submitDecision(r);}catch(e){setTabletopStatus(e.message,true)}};box.appendChild(b);
  }
  function playerEffectsSummary(player){
    const limit=player?.maxHandSize;
    const limitText=limit===null?'No maximum hand size':Number.isFinite(limit)?`Hand limit: ${limit}`:'Hand limit: unknown';
    const effects=(player?.activeEffects||[]).map(effect=>[effect.name,effect.description].filter(Boolean).join(': '));
    return [limitText,...effects].join(' • ');
  }
  function stateIsRenderable(s){return !!(s?.viewingPlayerId&&Array.isArray(s.players)&&Array.isArray(s.zones));}
  function renderTabletop(){
    ensureTabletopUI();const s=active?.state;openTabletopUI();
    if(!stateIsRenderable(s)){
      fillTableZone('argentumTabletopMyHand',{cards:{}},null);
      fillTableZone('argentumTabletopMyBattlefield',{cards:{}},null);
      fillTableZone('argentumTabletopOppBattlefield',{cards:{}},null);
      fillTableZone('argentumTabletopStack',{cards:{}},null);
      $('argentumTabletopMyName').textContent='You';$('argentumTabletopMyLife').textContent='—';$('argentumTabletopOppName').textContent='Opponent';$('argentumTabletopOppLife').textContent='—';
      $('argentumTabletopMyLibCount').textContent='—';$('argentumTabletopMyGyCount').textContent='—';$('argentumTabletopOppHandCount').textContent='—';$('argentumTabletopOppLibCount').textContent='—';$('argentumTabletopScore').textContent='Waiting for Argentum opening state…';
      $('argentumTabletopCardActions').innerHTML='<span class="sub">Waiting for provider state.</span>';$('argentumTabletopPlayerEffects').textContent='';$('argentumTabletopTurnActions').innerHTML='';
      renderTabletopDecision();setTabletopStatus(active?.user?.mulliganPrompt?'Opening hand ready. Choose keep or mulligan.':'Waiting for Argentum state…');return;
    }
    const me=s.viewingPlayerId,my=s.players?.find(p=>p.playerId===me),opp=s.players?.find(p=>p.playerId!==me);
    $('argentumTabletopMyName').textContent=my?.name||'You';$('argentumTabletopMyLife').textContent=my?.life??'?';$('argentumTabletopOppName').textContent=opp?.name||'Opponent';$('argentumTabletopOppLife').textContent=opp?.life??'?';
    const myHand=stateZone(s,me,'HAND'),myLib=stateZone(s,me,'LIBRARY'),myGy=stateZone(s,me,'GRAVEYARD');const oppHand=opp?stateZone(s,opp.playerId,'HAND'):null,oppLib=opp?stateZone(s,opp.playerId,'LIBRARY'):null;
    $('argentumTabletopMyLibCount').textContent=myLib?.size??myLib?.cardIds?.length??'?';$('argentumTabletopMyGyCount').textContent=myGy?.size??myGy?.cardIds?.length??0;$('argentumTabletopOppHandCount').textContent=oppHand?.size??oppHand?.cardIds?.length??'?';$('argentumTabletopOppLibCount').textContent=oppLib?.size??oppLib?.cardIds?.length??'?';
    $('argentumTabletopScore').textContent=`Turn ${s.turnNumber??'?'} • ${s.currentPhase||''}${s.currentStep?'/'+s.currentStep:''} • Priority: ${s.players?.find(p=>p.playerId===s.priorityPlayerId)?.name||s.priorityPlayerId||'—'}`;
    fillTableZone('argentumTabletopMyHand',s,myHand);fillTableZone('argentumTabletopMyBattlefield',s,stateZone(s,me,'BATTLEFIELD'));if(opp)fillTableZone('argentumTabletopOppBattlefield',s,stateZone(s,opp.playerId,'BATTLEFIELD'));fillTableZone('argentumTabletopStack',s,s.zones?.find(z=>String(zoneType(z)).toUpperCase()==='STACK'));
    $('argentumTabletopCardActions').innerHTML='';
    if(active.selectedCard&&s.cards?.[active.selectedCard.id])showCardActions(active.selectedCard.id,active.selectedCard.zone);
    $('argentumTabletopPlayerEffects').textContent=playerEffectsSummary(my);
    const turnActions=$('argentumTabletopTurnActions');turnActions.innerHTML='';
    if(!active.pendingDecision)for(const info of active.legalActions||[]){
      if(!/DeclareAttackers|DeclareBlockers/.test(info.actionType||''))continue;
      const button=document.createElement('button');button.type='button';button.className='primary';
      button.textContent=/DeclareAttackers/.test(info.actionType)?'Choose attackers':'Choose blockers';
      button.onclick=()=>submitLegalAction(info);turnActions.appendChild(button);
    }
    const piles=$('argentumTabletopPiles');piles.replaceChildren();
    for(const [owner,label] of [[opp?.playerId,'Opponent'],[me,'You']])for(const type of ['LIBRARY','GRAVEYARD','EXILE']){
      if(!owner)continue;const z=stateZone(s,owner,type),ids=z?.cardIds||[],count=z?.size??ids.length;
      const pile=document.createElement('button');pile.type='button';pile.className='argentum-pile';
      pile.textContent=`${label} · ${type.toLowerCase()} (${count})`;
      const knownTopIndex=type==='LIBRARY'?(z?.positions||[]).indexOf(0):-1;
      const top=s.cards?.[type==='LIBRARY'?ids[knownTopIndex]:ids[ids.length-1]],art=top?.imageUri||top?.imageUrl;
      if(type==='LIBRARY'&&top){const known=document.createElement('small');known.textContent=`Known top: ${top.name}`;pile.append(known);}
      if(type!=='LIBRARY'&&art){const img=document.createElement('img');img.src=art;img.alt=top.name;pile.prepend(img);}
      pile.onclick=()=>{const dialog=document.createElement('dialog');dialog.className='argentum-target-dialog';
        const heading=document.createElement('h3');heading.textContent=`${label} · ${type.toLowerCase()} (${count})`;dialog.append(heading);
        const cards=document.createElement('div');cards.className='argentum-pile-cards';
        if(type==='LIBRARY'){
          const note=document.createElement('p');note.textContent=`${count} cards face down. Only cards the game allows you to know are listed below.`;cards.append(note);
          for(const [i,id] of ids.entries()){const known=document.createElement('div'),position=(z.positions||[])[i];
            const caption=document.createElement('p');caption.textContent=position===0?'Known top card':`Known card · position ${position+1}`;
            known.append(caption,providerTableCard(s,id,type));cards.append(known);}
        }else if(z?.isVisible===false)cards.textContent=`${count} cards face down`;
        else if(!ids.length)cards.textContent='Empty';else for(const id of ids)cards.append(providerTableCard(s,id,type));
        dialog.append(cards);const close=document.createElement('button');close.textContent='Back to game';close.onclick=()=>dialog.close();dialog.append(close);
        dialog.addEventListener('close',()=>dialog.remove());document.body.append(dialog);dialog.showModal();};piles.append(pile);
    }
    $('argentumTabletopModal').classList.toggle('opening-hand',!!active.user.mulliganPrompt||!!active.user.bottomPrompt);
    const pass=$('argentumTabletopPass'),canPass=(active.legalActions||[]).some(a=>/PassPriority/i.test(a.actionType||''));
    pass.disabled=!canPass||!!active.pendingDecision;pass.textContent=active.pendingDecision?'Choose cards':canPass?'Pass priority':'Waiting…';
    renderTabletopDecision();setTabletopStatus(active.pendingDecision?'Complete your choice to continue.':canPass?'Your move.':'Waiting for the opponent…');
  }

  function openUI() { ensureUI(); $('argentumModal').classList.add('open'); $('argentumModal').setAttribute('aria-hidden','false'); }
  function closeUI() { $('argentumModal')?.classList.remove('open'); $('argentumModal')?.setAttribute('aria-hidden','true'); }
  function setStatus(text, bad=false) { ensureUI(); const el=$('argentumStatus'); el.textContent=text; el.classList.toggle('bad-text',!!bad); if($('argentumTabletopModal'))setTabletopStatus(text,bad); }
  function log(text) {
    ensureUI();
    const el=$('argentumLog');
    const stamp=new Date().toLocaleTimeString();
    el.textContent = `${stamp} ${text}\n${el.textContent}`.slice(0,30000);
  }

  function zoneType(z) { return z?.zoneId?.zoneType || z?.zoneType || ''; }
  function zoneOwner(z) { return z?.zoneId?.ownerId || z?.ownerId || null; }
  function cardName(state,id) { return state?.cards?.[id]?.name || String(id); }
  function cardById(state,id) { return state?.cards?.[id] || null; }

  function renderCard(state,id) {
    const c=cardById(state,id);
    if (!c) return `<span class="argentum-card hidden-card">Hidden</span>`;
    const pt=(c.power!=null||c.toughness!=null)?` <small>${c.power??''}/${c.toughness??''}</small>`:'';
    const flags=[c.isTapped?'tapped':'',c.isAttacking?'attacking':'',c.isBlocking?'blocking':''].filter(Boolean).join(' • ');
    return `<button class="argentum-card" type="button" data-argentum-card="${esc(id)}" title="${esc(c.oracleText||'')}"><b>${esc(c.name)}</b>${pt}<span>${esc(c.manaCost||'')} ${esc(c.typeLine||'')}</span>${flags?`<small>${esc(flags)}</small>`:''}</button>`;
  }

  function renderZone(state, z, title) {
    const ids=z?.cardIds||[];
    const visible=z?.isVisible!==false;
    return `<div class="argentum-zone"><h4>${esc(title)} <small>${z?.size ?? ids.length}</small></h4><div>${visible?ids.map(id=>renderCard(state,id)).join(''):`<span class="sub">${z?.size??0} hidden cards</span>`}</div></div>`;
  }

  function renderState() {
    ensureUI();
    const s=active?.state;
    if (!s) { $('argentumGame').classList.add('hidden'); return; }
    $('argentumGame').classList.remove('hidden');
    const me=s.viewingPlayerId;
    const my=s.players?.find(p=>p.playerId===me);
    const opp=s.players?.find(p=>p.playerId!==me);
    $('argentumScore').innerHTML=`<b>Turn ${esc(s.turnNumber)}</b> • ${esc(s.currentPhase)}/${esc(s.currentStep)} • Priority: ${esc(s.players?.find(p=>p.playerId===s.priorityPlayerId)?.name||s.priorityPlayerId)} • You ${esc(my?.life??'?')} — ${esc(opp?.life??'?')} ${esc(opp?.name||'Opponent')}`;
    const stack=s.zones?.find(z=>String(zoneType(z)).toLowerCase()==='stack');
    $('argentumStack').innerHTML=stack?renderZone(s,stack,'Stack'):'<span class="sub">Empty</span>';
    const renderFor=(owner,isMe)=>{
      const wanted=['BATTLEFIELD','HAND','GRAVEYARD','EXILE','COMMAND','SIDEBOARD'];
      return wanted.map(t=>{
        const z=s.zones?.find(x=>zoneOwner(x)===owner && String(zoneType(x)).toUpperCase()===t);
        if (!z) return '';
        const title=t[0]+t.slice(1).toLowerCase();
        if (!isMe && t==='HAND' && z.isVisible===false) return renderZone(s,z,title);
        return renderZone(s,z,title);
      }).join('');
    };
    $('argentumMyZones').innerHTML=renderFor(me,true);
    $('argentumOpponentZones').innerHTML=opp?renderFor(opp.playerId,false):'<span class="sub">Waiting for opponent.</span>';
    renderActions(); renderDecision();
    if(active?.spec?.presentation==='tabletop')renderTabletop();
  }

  function renderActions() {
    const box=$('argentumActions'); if(!box)return;
    box.innerHTML='';
    if(!active?.state){box.innerHTML='<span class="sub">No live state.</span>';return;}
    if(active.pendingDecision){box.innerHTML='<span class="sub">Resolve the pending decision first.</span>';return;}
    const acts=active.legalActions||[];
    if(!acts.length){box.innerHTML='<span class="sub">No legal action is currently available to this seat.</span>';return;}
    for(const [i,info] of acts.entries()){
      const b=document.createElement('button'); b.type='button'; b.className=info.actionType==='PassPriority'?'':'good';
      b.textContent=info.description||info.actionType||`Action ${i+1}`;
      b.disabled=info.isAffordable===false;
      b.onclick=()=>submitLegalAction(info);
      box.appendChild(b);
    }
  }

  function entityLabel(id, decisionCardInfo) {
    const s=active?.state;
    const p=s?.players?.find(x=>x.playerId===id); if(p)return `${p.name} (${p.life})`;
    const c=s?.cards?.[id];
    const z=s?.zones?.find(x=>(x.cardIds||[]).includes(id));
    const location=c?.zone||z;
    const zone=String(zoneType(location)).toLowerCase().replaceAll('_',' ');
    const ownerId=c?.ownerId||zoneOwner(location);
    const owner=s?.players?.find(x=>x.playerId===ownerId)?.name;
    const name=c?.name||decisionCardInfo?.name;
    if(name)return `${name}${zone?` — ${zone}${owner?` (${owner})`:''}`:''} [${id}]`;
    return String(id);
  }

  function chosenTarget(id) {
    const s=active?.state; if(!s) throw new Error('No state for target resolution.');
    if(s.players?.some(p=>p.playerId===id)) return {type:'Player',playerId:id};
    const z=s.zones?.find(x=>(x.cardIds||[]).includes(id));
    const t=String(zoneType(z)).toUpperCase();
    if(t==='STACK') return {type:'Spell',spellEntityId:id};
    if(t==='BATTLEFIELD') return {type:'Permanent',entityId:id};
    const c=s.cards?.[id];
    return {type:'Card',cardId:id,ownerId:c?.ownerId||zoneOwner(z)||s.viewingPlayerId,zone:zoneType(z)};
  }

  function idsFromChoice(raw, ids, min, max) {
    if(raw==null)throw new Error('Action cancelled.');
    if(Array.isArray(raw))return raw.filter(id=>ids.includes(id));
    const text=String(raw).trim();
    if(!text&&min===0)return [];
    const picked=[...new Set(text.split(/[ ,]+/).filter(Boolean).map(x=>ids[Number(x)-1]).filter(Boolean))];
    if(picked.length<min||picked.length>max)throw new Error(`Choose ${min===max?min:`${min}-${max}`} target(s).`);
    return picked;
  }
  function pickIds(promptText, ids, min=1, max=1, cardInfo={}) {
    if(!ids?.length && min>0) throw new Error(`${promptText}: no legal targets.`);
    if(max===0)return [];
    const test=takeTestChoice();
    if(test!==undefined)return idsFromChoice(test,ids,min,max);
    noPromptFallback();
  }

  async function pickActionTargets(promptText, ids, min=1, max=1, cardInfo={}, {searchable=false}={}) {
    const options=[...new Set(ids||[])];
    if(options.length<min)throw new Error(`${promptText}: no legal targets.`);
    if(max===0)return [];
    const dialog=document.createElement('dialog');
    if(!hasNativeDialog(dialog))return pickIds(promptText,options,min,max,cardInfo);
    dialog.className='argentum-target-dialog';
    const heading=document.createElement('h3');heading.textContent=promptText;dialog.appendChild(heading);
    if(searchable){const search=document.createElement('input');search.type='search';search.placeholder='Search your deck by card name';search.setAttribute('aria-label','Search your deck');search.className='argentum-deck-search';search.oninput=()=>{const query=search.value.trim().toLocaleLowerCase();for(const row of dialog.querySelectorAll('label'))row.hidden=!row.dataset.searchName.includes(query);};dialog.append(search);}
    const help=document.createElement('p');help.textContent=`Choose ${min===max?min:`${min}-${max}`} card(s). Nothing is selected automatically.`;dialog.appendChild(help);
    return await new Promise((resolve,reject)=>{
      const selected=new Set();
      const confirmButton=document.createElement('button');confirmButton.type='button';confirmButton.textContent=/discard/i.test(promptText)?'Discard selected cards':'Confirm selected cards';confirmButton.disabled=min>0;
      const finish=value=>{dialog.close();dialog.remove();resolve(value);};
      for(const id of options){
        const label=document.createElement('label');
        const input=document.createElement('input');input.type='checkbox';
        const details=cardInfo[id]||active?.state?.cards?.[id]||{};label.dataset.searchName=(details.name||entityLabel(id,details)).toLocaleLowerCase();
        const text=document.createElement('span');text.textContent=entityLabel(id,details).replace(/ \[[^\]]+\]$/,'');
        const art=details.imageUri||details.imageUrl||cardImageCache.get(details.name);
        if(art){const img=document.createElement('img');img.src=art;img.alt=details.name||'Card';img.className='argentum-choice-art';label.appendChild(img);}
        if(details.oracleText){const rules=document.createElement('small');rules.textContent=details.oracleText;text.appendChild(rules);} 
        input.onchange=()=>{
          if(input.checked && max===1){for(const other of dialog.querySelectorAll('input'))if(other!==input)other.checked=false;selected.clear();}
          if(input.checked)selected.add(id);else selected.delete(id);
          confirmButton.disabled=selected.size<min||selected.size>max;
        };
        label.appendChild(input);label.appendChild(text);dialog.appendChild(label);
      }
      confirmButton.onclick=()=>{if(selected.size>=min&&selected.size<=max)finish(options.filter(id=>selected.has(id)));};
      const cancel=document.createElement('button');cancel.type='button';cancel.textContent='Cancel';
      const abort=()=>{dialog.close();dialog.remove();reject(new Error('Action cancelled.'));};
      cancel.onclick=abort;dialog.addEventListener('cancel',event=>{event.preventDefault();abort();});
      dialog.appendChild(confirmButton);dialog.appendChild(cancel);document.body.appendChild(dialog);dialog.showModal();
    });
  }

  async function pickActionNumber(promptText, min, max) {
    if(!Number.isSafeInteger(min)||!Number.isSafeInteger(max)||min<0||max<min)throw new Error('Invalid provider number bounds.');
    const valid=raw=>raw!==''&&Number.isSafeInteger(Number(raw))&&Number(raw)>=min&&Number(raw)<=max;
    const test=takeTestChoice();
    if(test!==undefined){if(test==null)throw new Error('Action cancelled.');if(!valid(String(test).trim()))throw new Error(`Choose a whole number from ${min} to ${max}.`);return Number(test);}
    const dialog=document.createElement('dialog');
    if(!hasNativeDialog(dialog))noPromptFallback();
    dialog.className='argentum-target-dialog';
    const heading=document.createElement('h3');heading.textContent=promptText;dialog.appendChild(heading);
    const label=document.createElement('label');label.textContent=`Whole number (${min}–${max})`;
    const input=document.createElement('input');input.type='number';input.min=String(min);input.max=String(max);input.step='1';input.value=String(min);
    label.appendChild(input);dialog.appendChild(label);
    return await new Promise((resolve,reject)=>{
      const confirm=document.createElement('button');confirm.type='button';confirm.textContent='Confirm number';
      input.oninput=()=>{confirm.disabled=!valid(input.value);};
      confirm.onclick=()=>{if(valid(input.value)){const value=Number(input.value);dialog.close();dialog.remove();resolve(value);}};
      const abort=()=>{dialog.close();dialog.remove();reject(new Error('Action cancelled.'));};
      const cancel=document.createElement('button');cancel.type='button';cancel.textContent='Cancel';cancel.onclick=abort;
      dialog.addEventListener('cancel',event=>{event.preventDefault();abort();});
      dialog.appendChild(confirm);dialog.appendChild(cancel);document.body.appendChild(dialog);dialog.showModal();input.focus();
    });
  }

  async function pickActionModes(enumeration) {
    const available=(enumeration.modes||[]).filter(m=>m.available!==false && !(enumeration.unavailableIndices||[]).includes(m.index));
    const min=Math.max(1,enumeration.minChooseCount??1), max=Math.max(min,enumeration.chooseCount??min);
    if(!available.length||(!enumeration.allowRepeat&&available.length<min))throw new Error('Provider offered a modal spell without enough available modes.');
    const test=takeTestChoice();
    if(test!==undefined){
      if(test==null)throw new Error('Action cancelled.');
      let picks=String(test).split(/[ ,]+/).filter(Boolean).map(x=>available[Number(x)-1]).filter(Boolean).map(m=>m.index);
      if(!enumeration.allowRepeat)picks=[...new Set(picks)];
      if(picks.length<min||picks.length>max)throw new Error(`Choose ${min===max?min:`${min}-${max}`} legal mode(s).`);
      return picks;
    }
    const dialog=document.createElement('dialog'); if(!hasNativeDialog(dialog))noPromptFallback();
    dialog.className='argentum-target-dialog';
    const heading=document.createElement('h3');heading.textContent=`Choose ${min===max?min:`${min}-${max}`} mode${max===1?'':'s'}`;dialog.appendChild(heading);
    return await new Promise((resolve,reject)=>{
      const selected=[];
      const confirm=document.createElement('button');confirm.type='button';confirm.textContent='Confirm modes';confirm.disabled=true;
      const refresh=()=>{confirm.disabled=selected.length<min||selected.length>max;};
      for(const mode of available){
        const label=document.createElement('label');const input=document.createElement('input');input.type=enumeration.allowRepeat?'number':'checkbox';input.min='0';input.max=String(max);input.value=enumeration.allowRepeat?'0':'';
        const text=document.createElement('span');text.textContent=mode.description||`Mode ${mode.index+1}`;
        input.onchange=()=>{selected.length=0;for(const row of dialog.querySelectorAll('[data-mode-index]')){const idx=Number(row.dataset.modeIndex);const field=row.querySelector('input');const count=field.type==='number'?Number(field.value||0):(field.checked?1:0);for(let i=0;i<count;i++)selected.push(idx);}refresh();};
        label.dataset.modeIndex=String(mode.index);label.append(input,text);dialog.appendChild(label);
      }
      const finish=()=>{dialog.close();dialog.remove();resolve([...selected]);};
      const abort=()=>{dialog.close();dialog.remove();reject(new Error('Action cancelled.'));};
      confirm.onclick=()=>{if(!confirm.disabled)finish();};const cancel=document.createElement('button');cancel.type='button';cancel.textContent='Cancel';cancel.onclick=abort;
      dialog.addEventListener('cancel',event=>{event.preventDefault();abort();});dialog.append(confirm,cancel);document.body.appendChild(dialog);dialog.showModal();
    });
  }

  async function pickActionColor(promptText, colors) {
    const opts=colors?.length?colors:['WHITE','BLUE','BLACK','RED','GREEN'];
    const test=takeTestChoice();
    if(test!==undefined){if(test==null)throw new Error('Action cancelled.');return String(test).toUpperCase();}
    const dialog=document.createElement('dialog'); if(!hasNativeDialog(dialog))noPromptFallback();
    dialog.className='argentum-target-dialog';
    const heading=document.createElement('h3');heading.textContent=promptText||'Choose mana color';dialog.appendChild(heading);
    return await new Promise((resolve,reject)=>{
      for(const color of opts){const b=document.createElement('button');b.type='button';b.textContent=color;b.onclick=()=>{dialog.close();dialog.remove();resolve(String(color).toUpperCase());};dialog.appendChild(b);}
      const abort=()=>{dialog.close();dialog.remove();reject(new Error('Action cancelled.'));};const cancel=document.createElement('button');cancel.type='button';cancel.textContent='Cancel';cancel.onclick=abort;dialog.addEventListener('cancel',event=>{event.preventDefault();abort();});dialog.appendChild(cancel);document.body.appendChild(dialog);dialog.showModal();
    });
  }

  async function pickYesNo(promptText, defaultChoice=true) {
    const test=takeTestChoice();
    if(test!==undefined){if(test==null)throw new Error('Action cancelled.');return /^(true|yes|y|1)$/i.test(String(test));}
    const dialog=document.createElement('dialog'); if(!hasNativeDialog(dialog))noPromptFallback();
    dialog.className='argentum-target-dialog';
    const heading=document.createElement('h3');heading.textContent=promptText;dialog.appendChild(heading);
    return await new Promise((resolve,reject)=>{
      const yes=document.createElement('button');yes.type='button';yes.className=defaultChoice?'primary':'';yes.textContent='Yes';yes.onclick=()=>{dialog.close();dialog.remove();resolve(true);};
      const no=document.createElement('button');no.type='button';no.className=!defaultChoice?'primary':'';no.textContent='No';no.onclick=()=>{dialog.close();dialog.remove();resolve(false);};
      const abort=()=>{dialog.close();dialog.remove();reject(new Error('Action cancelled.'));};
      dialog.addEventListener('cancel',event=>{event.preventDefault();abort();});dialog.append(yes,no);document.body.appendChild(dialog);dialog.showModal();
    });
  }

  async function pickOptionIndex(promptText, options) {
    const test=takeTestChoice();
    if(test!==undefined){if(test==null)throw new Error('Action cancelled.');return Math.max(0,Number(test)-1);}
    const dialog=document.createElement('dialog'); if(!hasNativeDialog(dialog))noPromptFallback();
    dialog.className='argentum-target-dialog';
    const heading=document.createElement('h3');heading.textContent=promptText;dialog.appendChild(heading);
    return await new Promise((resolve,reject)=>{
      for(const [i,opt] of (options||[]).entries()){const b=document.createElement('button');b.type='button';b.textContent=String(opt?.description||opt);b.onclick=()=>{dialog.close();dialog.remove();resolve(i);};dialog.appendChild(b);}
      const abort=()=>{dialog.close();dialog.remove();reject(new Error('Action cancelled.'));};const cancel=document.createElement('button');cancel.type='button';cancel.textContent='Cancel';cancel.onclick=abort;dialog.addEventListener('cancel',event=>{event.preventDefault();abort();});dialog.appendChild(cancel);document.body.appendChild(dialog);dialog.showModal();
    });
  }

  async function pickModeDecisionIndexes(promptText, options, min, max) {
    const test=takeTestChoice();
    if(test!==undefined){if(test==null)throw new Error('Action cancelled.');return String(test).split(/[ ,]+/).map(x=>Number(x)-1).filter(x=>x>=0).slice(0,max);}
    const dialog=document.createElement('dialog'); if(!hasNativeDialog(dialog))noPromptFallback();
    dialog.className='argentum-target-dialog';
    const heading=document.createElement('h3');heading.textContent=promptText;dialog.appendChild(heading);
    if(searchable){const search=document.createElement('input');search.type='search';search.placeholder='Search your deck by card name';search.setAttribute('aria-label','Search your deck');search.className='argentum-deck-search';search.oninput=()=>{const query=search.value.trim().toLocaleLowerCase();for(const row of dialog.querySelectorAll('label'))row.hidden=!row.dataset.searchName.includes(query);};dialog.append(search);}
    const help=document.createElement('p');help.textContent=`Choose ${min===max?min:`${min}-${max}`} mode${max===1?'':'s'}.`;dialog.appendChild(help);
    return await new Promise((resolve,reject)=>{
      const selected=new Set();const confirm=document.createElement('button');confirm.type='button';confirm.textContent='Confirm modes';confirm.disabled=min>0;
      const refresh=()=>{confirm.disabled=selected.size<min||selected.size>max;};
      for(const [i,opt] of (options||[]).entries()){const label=document.createElement('label');const input=document.createElement('input');input.type='checkbox';input.onchange=()=>{if(input.checked)selected.add(i);else selected.delete(i);refresh();};const text=document.createElement('span');text.textContent=String(opt?.description||opt);label.append(input,text);dialog.appendChild(label);}
      const abort=()=>{dialog.close();dialog.remove();reject(new Error('Action cancelled.'));};
      confirm.onclick=()=>{if(!confirm.disabled){dialog.close();dialog.remove();resolve([...selected]);}};const cancel=document.createElement('button');cancel.type='button';cancel.textContent='Cancel';cancel.onclick=abort;dialog.addEventListener('cancel',event=>{event.preventDefault();abort();});dialog.append(confirm,cancel);document.body.appendChild(dialog);dialog.showModal();
    });
  }

  function collectModalTargets(action, enumeration, chooseIds, toTarget) {
    const picks=action.chosenModes||[];
    const min=enumeration.minChooseCount??1, max=enumeration.chooseCount??min;
    if(picks.length<min||picks.length>max)throw new Error('Choose the required number of modes.');
    if(!enumeration.allowRepeat && new Set(picks).size!==picks.length)throw new Error('A mode cannot be chosen twice.');
    const allIds=[], groups=[];
    for(const modeIndex of picks){
      const mode=(enumeration.modes||[]).find(m=>m.index===modeIndex);
      if(!mode||mode.available===false||(enumeration.unavailableIndices||[]).includes(modeIndex))throw new Error('That mode is unavailable.');
      const targets=[];
      for(const req of mode.targetRequirements||[]){
        const valid=[...new Set(req.validTargets||[])].filter(id=>!req.mustDifferFromEarlier||!allIds.includes(id));
        const minTargets=req.minTargets??1,maxTargets=req.maxTargets??minTargets;
        if(valid.length<minTargets)throw new Error('A selected mode has no legal target selection.');
        const ids=chooseIds({...req,validTargets:valid},mode);
        if(!Array.isArray(ids)||ids.length<minTargets||ids.length>maxTargets||new Set(ids).size!==ids.length||ids.some(id=>!valid.includes(id)))throw new Error('Choose legal targets for each selected mode.');
        allIds.push(...ids);targets.push(...ids.map(toTarget));
      }
      groups.push(targets);
    }
    action.modeTargetsOrdered=groups;
    action.targets=groups.flat();
  }

  async function collectHumanModalTargets(action, enumeration) {
    const selections=[],earlier=[];
    for(const index of action.chosenModes||[]){
      const mode=(enumeration.modes||[]).find(m=>m.index===index);
      if(!mode)throw new Error('That mode is unavailable.');
      for(const req of mode.targetRequirements||[]){
        const valid=[...new Set(req.validTargets||[])].filter(id=>!req.mustDifferFromEarlier||!earlier.includes(id));
        const ids=await pickActionTargets(`${mode.description||'Selected mode'}: ${req.description||'Choose target'}`,valid,req.minTargets??1,req.maxTargets??req.minTargets??1);
        selections.push(ids);earlier.push(...ids);
      }
    }
    let next=0;
    collectModalTargets(action,enumeration,()=>selections[next++],chosenTarget);
  }

  function fixedSelfSacrifice(info){
    const cost=info?.additionalCostInfo,action=info?.action;
    return cost?.costType==='SacrificeSelf' && action?.type==='ActivateAbility' && (cost.sacrificeCount??1)===1 && cost.validSacrificeTargets?.length===1 && cost.validSacrificeTargets[0]===action.sourceId;
  }
  function applySelfSacrifice(action,info){
    if(fixedSelfSacrifice(info))action.costPayment={...(action.costPayment||{}),sacrificedPermanents:[action.sourceId]};
  }
  async function submitLegalAction(info) {
    try {
      if(!active?.interactionEpoch)throw new Error('No live interaction epoch; request a resync first.');
      const actionSession=active, actionEpoch=JSON.stringify(active.interactionEpoch);
      const action=clone(info.action);
      applySelfSacrifice(action,info);
      if(/DeclareAttackers/.test(info.actionType||action.type||'')){
        const valid=info.validAttackers||[],mandatory=info.mandatoryAttackers||[];
        const chosen=await pickActionTargets('Choose attackers (leave empty to attack with none)',valid,mandatory.length,valid.length);
        if(mandatory.some(id=>!chosen.includes(id)))throw new Error('Choose every mandatory attacker.');
        action.attackers={};
        for(const id of chosen){
          const targets=await pickActionTargets(`Choose attack target for ${entityLabel(id)}`,info.validAttackTargets||[],1,1);
          action.attackers[id]=targets[0];
        }
      }
      if(/DeclareBlockers/.test(info.actionType||action.type||'')){
        action.blockers=clone(info.mandatoryBlockerAssignments||{});
        const optional=(info.validBlockers||[]).filter(id=>!Object.hasOwn(action.blockers,id));
        if(optional.length){
          const selected=await pickActionTargets('Choose optional blockers (leave empty for none)',optional,0,optional.length);
          const attackers=(active.state?.combat?.attackers||[]).map(a=>a.creatureId);
          for(const id of selected){
            const max=Math.min(info.blockerMaxBlockCounts?.[id]??1,attackers.length);
            action.blockers[id]=await pickActionTargets(`Choose attackers blocked by ${entityLabel(id)}`,attackers,1,max);
          }
        }
      }
      if(info.modalEnumeration && action.type==='CastSpell') {
        action.chosenModes=await pickActionModes(info.modalEnumeration);
      }
      if(info.hasXCost)action.xValue=await pickActionNumber('Choose X',info.minX??0,info.maxAffordableX??0);
      if(info.modalEnumeration && action.type==='CastSpell') {
        await collectHumanModalTargets(action,info.modalEnumeration);
      }
      if(info.additionalCostInfo?.costType==='PayXLife') {
        const max=info.additionalCostInfo.payXLifeMaxX??0;
        const amount=await pickActionNumber('Choose X: pay that much life',0,max);
        action.additionalCostPayment={...(action.additionalCostPayment||{}),payXLifeAmount:amount};
      }
      if(info.requiresTargets && !info.modalEnumeration){
        const chosen=[];
        const reqs=info.targetRequirements?.length?info.targetRequirements:[{description:info.targetDescription||'Choose target',minTargets:info.minTargets??info.targetCount??1,maxTargets:info.targetCount??1,validTargets:info.validTargets||[]}];
        for(const req of reqs){const ids=await pickActionTargets(req.description||'Choose target',req.validTargets||[],req.minTargets??1,req.maxTargets??1);chosen.push(...ids.map(chosenTarget));}
        action.targets=chosen;
      }
      if(info.requiresManaColorChoice)action.manaColorChoice=await pickActionColor('Choose mana color',info.availableManaColors);
      // The provider advertises complex alternative/additional costs separately. If the template
      // still requires a resource choice, do not invent one: expose the exact action JSON editor.
      if(info.additionalCostInfo && !action.additionalCostPayment && !action.costPayment){
        throw new Error('This provider-defined additional cost is not playable on the visual tabletop yet.');
      }
      if(active!==actionSession||JSON.stringify(active.interactionEpoch)!==actionEpoch)throw new Error('The game changed during selection; choose the action again.');
      active.submitAction(action);
    }catch(e){setStatus(e.message,true);log(`Action blocked: ${e.message}`);}
  }

  function automaticCardSelection(decision,state){
    const options=decision.options||[],min=decision.minSelections??decision.minCards??0,max=decision.maxSelections??decision.maxCards??options.length;
    const source=state?.cards?.[decision.context?.sourceId];
    const librarySearch=decision.type==='SearchLibraryDecision' ||
      (max===1 && /search your library/i.test(source?.oracleText||'') && options.length>0 &&
       options.every(id=>String(state?.cards?.[id]?.zone?.zoneType).toUpperCase()==='LIBRARY'));
    const count=librarySearch?Math.min(max,Math.max(min,1)):min;
    return options.slice(0,count);
  }
  async function decisionResponse(decision, mode='human') {
    const type=decision?.type||''; const id=decision?.id;
    const ai = mode==='auto';
    if(type==='YesNoDecision') return {type:'YesNoResponse',decisionId:id,choice: ai ? (decision.defaultChoice ?? true) : await pickYesNo(decision.prompt||'Yes?', decision.defaultChoice ?? true)};
    if(type==='BatchYesNoDecision') return {type:'BatchYesNoResponse',decisionId:id,choice:true,applyToAll:true};
    if(type==='ChooseColorDecision'){
      const colors=decision.colors||decision.options||['WHITE','BLUE','BLACK','RED','GREEN'];
      const raw=ai?colors[0]:await pickActionColor(decision.prompt||'Choose color',colors); if(raw==null)return null;
      return {type:'ColorChosenResponse',decisionId:id,color:String(raw).toUpperCase(),colors:decision.maxColors>1?[String(raw).toUpperCase()]:[]};
    }
    if(type==='ChooseNumberDecision'){
      const min=decision.min??decision.minValue??0,max=decision.max??decision.maxValue??20; const val=ai?min:await pickActionNumber(decision.prompt||'Choose number',min,max); if(!Number.isFinite(val))return null;
      return {type:'NumberChosenResponse',decisionId:id,number:Math.max(min,Math.min(max,val))};
    }
    if(type==='ChooseOptionDecision'){
      const opts=decision.options||[]; const ix=ai?0:await pickOptionIndex(decision.prompt||'Choose option',opts);
      return {type:'OptionChosenResponse',decisionId:id,optionIndex:Math.min(Math.max(ix,0),Math.max(0,opts.length-1))};
    }
    if(type==='ChooseModeDecision'){
      const opts=decision.modes||decision.options||[]; const min=decision.minModes??decision.chooseCount??1,max=decision.maxModes??decision.chooseCount??1;
      const picks=ai?[0]:await pickModeDecisionIndexes(decision.prompt||'Choose mode',opts,min,max);
      return {type:'ModesChosenResponse',decisionId:id,selectedModes:picks.length>=min?picks:[0]};
    }
    if(type==='SelectCardsDecision' || type==='SearchLibraryDecision'){
      const options=decision.options||[]; const min=decision.minSelections??decision.minCards??0,max=decision.maxSelections??decision.maxCards??options.length;
      const prompt=[decision.context?.sourceName,decision.prompt||'Choose cards'].filter(Boolean).join(' — ');
      const picks=ai?automaticCardSelection(decision,active?.state):await pickActionTargets(prompt,options,min,max,decision.cardInfo||{},{searchable:type==='SearchLibraryDecision'});
      return {type:'CardsSelectedResponse',decisionId:id,selectedCards:picks};
    }
    if(type==='ChooseTargetsDecision'){
      const reqs=decision.requirements||decision.targetRequirements||[]; const selectedTargets={};
      for(const [i,r] of reqs.entries()){const index=r.index??i,valid=decision.legalTargets?.[index]||r.validTargets||[];selectedTargets[index]=ai?valid.slice(0,r.minTargets??1):await pickActionTargets(r.description||decision.prompt||'Choose targets',valid,r.minTargets??1,r.maxTargets??1);}
      return {type:'TargetsResponse',decisionId:id,selectedTargets};
    }
    if(type==='OrderObjectsDecision' || type==='ReorderLibraryDecision') return {type:'OrderedResponse',decisionId:id,orderedObjects:[...(decision.objects||decision.cards||[])]};
    if(type==='DistributeDecision'){
      const targets=decision.targets||[];let left=decision.totalAmount||0;const distribution={};for(const [i,t] of targets.entries()){const min=decision.minPerTarget||0;distribution[t]=min;left-=min;if(i===targets.length-1)distribution[t]+=Math.max(0,left);}return {type:'DistributionResponse',decisionId:id,distribution};
    }
    if(type==='AssignDamageDecision') return {type:'DamageAssignmentResponse',decisionId:id,assignments:decision.defaultAssignments||{}};
    if(type==='SelectManaSourcesDecision') return {type:'ManaSourcesSelectedResponse',decisionId:id,selectedSources:[],autoPay:true,waterbendPermanents:[]};
    if(type==='SplitPilesDecision') {const cards=decision.cards||[];return {type:'PilesSplitResponse',decisionId:id,piles:[cards.filter((_,i)=>i%2===0),cards.filter((_,i)=>i%2===1)]};}
    if(type==='BudgetModalDecision') return {type:'BudgetModalResponse',decisionId:id,selectedModeIndices: decision.modes?.length?[0]:[]};
    if(type==='ChooseReplacementDecision') return {type:'ReplacementChosenResponse',decisionId:id,fromIndex:decision.defaultFromIndex??0,toIndex:0};
    return null;
  }

  function renderDecision() {
    const box=$('argentumDecision'); if(!box)return;
    if(active?.user?.mulliganPrompt){active.renderMulligan(active.user.mulliganPrompt);return;}
    if(active?.user?.bottomPrompt){active.renderBottomCards(active.user.bottomPrompt);return;}
    const d=active?.pendingDecision;
    if(!d){box.innerHTML='<span class="sub">No pending decision.</span>';return;}
    box.innerHTML=`<p><b>${esc(d.prompt||d.type)}</b></p><button id="argentumResolveDecision" class="primary" type="button">Resolve decision</button><details><summary>Advanced response JSON</summary><textarea id="argentumDecisionJson"></textarea><button id="argentumSubmitDecisionJson" type="button">Submit response JSON</button></details>`;
    const suggested=decisionResponseSafeForState(d,'auto',active?.state);
    $('argentumDecisionJson').value=suggested?JSON.stringify(suggested,null,2):JSON.stringify({type:'',decisionId:d.id},null,2);
    $('argentumResolveDecision').onclick=async()=>{try{const response=await decisionResponse(d,'human');if(response)active.submitDecision(response);}catch(e){setStatus(e.message,true)}};
    $('argentumSubmitDecisionJson').onclick=()=>{try{active.submitDecision(JSON.parse($('argentumDecisionJson').value));}catch(e){setStatus(`Decision JSON error: ${e.message}`,true)}};
  }
  function decisionResponseSafe(d,m){try{return m==='auto'?decisionResponseSafeForState(d,m,active?.state):null}catch(_e){return null}}

  function decisionResponseSafeForState(d,m,state){
    if(m!=='auto')return decisionResponseSafe(d,m);
    try {
      const type=d?.type||'', id=d?.id;
      if(type==='YesNoDecision')return {type:'YesNoResponse',decisionId:id,choice:d.defaultChoice??true};
      if(type==='BatchYesNoDecision')return {type:'BatchYesNoResponse',decisionId:id,choice:true,applyToAll:true};
      if(type==='ChooseColorDecision'){const colors=d.colors||d.options||['WHITE','BLUE','BLACK','RED','GREEN'];return {type:'ColorChosenResponse',decisionId:id,color:String(colors[0]).toUpperCase(),colors:d.maxColors>1?[String(colors[0]).toUpperCase()]:[]};}
      if(type==='ChooseNumberDecision'){const min=d.min??d.minValue??0;return {type:'NumberChosenResponse',decisionId:id,number:min};}
      if(type==='ChooseOptionDecision')return {type:'OptionChosenResponse',decisionId:id,optionIndex:0};
      if(type==='ChooseModeDecision')return {type:'ModesChosenResponse',decisionId:id,selectedModes:[0]};
      if(type==='SelectCardsDecision'||type==='SearchLibraryDecision'){const options=d.options||[],min=d.minSelections??d.minCards??0;return {type:'CardsSelectedResponse',decisionId:id,selectedCards:automaticCardSelection(d,state)};}
      if(type==='ChooseTargetsDecision'){const reqs=d.requirements||d.targetRequirements||[],selectedTargets={};for(const [i,r] of reqs.entries()){const index=r.index??i,valid=d.legalTargets?.[index]||r.validTargets||[],min=r.minTargets??1;if(valid.length<min)return null;selectedTargets[index]=valid.slice(0,min);}return {type:'TargetsResponse',decisionId:id,selectedTargets};}
      if(type==='OrderObjectsDecision'||type==='ReorderLibraryDecision')return {type:'OrderedResponse',decisionId:id,orderedObjects:[...(d.objects||d.cards||[])]};
      if(type==='DistributeDecision'){const targets=d.targets||[];let left=d.totalAmount||0,distribution={};for(const [i,t] of targets.entries()){const min=d.minPerTarget||0;distribution[t]=min;left-=min;if(i===targets.length-1)distribution[t]+=Math.max(0,left);}return {type:'DistributionResponse',decisionId:id,distribution};}
      if(type==='AssignDamageDecision')return {type:'DamageAssignmentResponse',decisionId:id,assignments:d.defaultAssignments||{}};
      if(type==='SelectManaSourcesDecision')return {type:'ManaSourcesSelectedResponse',decisionId:id,selectedSources:[],autoPay:true,waterbendPermanents:[]};
      if(type==='SplitPilesDecision'){const cards=d.cards||[];return {type:'PilesSplitResponse',decisionId:id,piles:[cards.filter((_,i)=>i%2===0),cards.filter((_,i)=>i%2===1)]};}
      if(type==='BudgetModalDecision')return {type:'BudgetModalResponse',decisionId:id,selectedModeIndices:d.modes?.length?[0]:[]};
      if(type==='ChooseReplacementDecision')return {type:'ReplacementChosenResponse',decisionId:id,fromIndex:d.defaultFromIndex??0,toIndex:0};
    } catch(_e) {}
    return null;
  }

  function actionNeedsUnsupportedAutomation(info) {
    // Cast-time modes and targets come from the provider's enumeration. Resource-heavy
    // payments stay fail-closed until a generic chooser can complete their payloads.
    return !!((info?.additionalCostInfo && info.additionalCostInfo.costType!=='PayXLife' && !fixedSelfSacrifice(info)) || info?.hasConvoke || info?.hasDelve || info?.hasHarmonize || info?.hasTapForGeneric || info?.requiresDamageDistribution);
  }

  function completeActionForBot(info, state) {
    if (!info || info.isAffordable === false || actionNeedsUnsupportedAutomation(info)) return null;
    const action=clone(info.action);
      applySelfSacrifice(action,info);
    if (!action || !state) return null;
    const me=state.viewingPlayerId;
    const opp=state.players?.find(p=>p.playerId!==me)?.playerId;

    if (info.modalEnumeration && action.type==='CastSpell') {
      const e=info.modalEnumeration;
      const available=(e.modes||[]).filter(m=>m.available!==false && !(e.unavailableIndices||[]).includes(m.index));
      const min=Math.max(1,e.minChooseCount??1), max=Math.max(min,e.chooseCount??min);
      if(!available.length||(!e.allowRepeat&&available.length<min))return null;
      action.chosenModes=Array.from({length:min},(_,i)=>available[e.allowRepeat?i%available.length:i].index);
      if(e.additionalCostPerExtraMode&&action.chosenModes.length>1)return null;
      if(action.chosenModes.some(index=>available.find(m=>m.index===index)?.additionalCostInfo))return null;
    }
    if (info.hasXCost) action.xValue=Math.max(info.minX??0, Math.min(info.maxAffordableX??0, info.minX??0));
    if (info.requiresManaColorChoice) action.manaColorChoice=(info.availableManaColors?.[0]||'BLUE');

    if (info.additionalCostInfo?.costType==='PayXLife') {
      action.additionalCostPayment={...(action.additionalCostPayment||{}),payXLifeAmount:0};
    }
    if (info.modalEnumeration && action.type==='CastSpell') {
      try {
        collectModalTargets(action,info.modalEnumeration,
          req=>(req.validTargets||[]).slice(0,req.minTargets??1),id=>chosenTargetForState(state,id));
      } catch(_e) {return null;}
    }
    if (info.requiresTargets && !info.modalEnumeration) {
      const reqs=info.targetRequirements?.length ? info.targetRequirements : [{
        minTargets:info.minTargets??info.targetCount??1,
        maxTargets:info.targetCount??1,
        validTargets:info.validTargets||[],
      }];
      const chosen=[];
      for (const req of reqs) {
        const min=req.minTargets??1, max=req.maxTargets??min, valid=req.validTargets||[];
        if (valid.length < min) return null;
        const preferred=[...valid].sort((a,b)=>{
          const ap=a===opp?0:state.cards?.[a]?.controllerId===opp?1:a===me?4:2;
          const bp=b===opp?0:state.cards?.[b]?.controllerId===opp?1:b===me?4:2;
          return ap-bp;
        });
        chosen.push(...preferred.slice(0,Math.max(min,Math.min(max,min))).map(id=>chosenTargetForState(state,id)));
      }
      action.targets=chosen;
    }

    if (/DeclareAttackers/i.test(info.actionType||'')) {
      const attackers=info.validAttackers||[];
      const target=(info.validAttackTargets||[]).find(x=>x===opp) || (info.validAttackTargets||[])[0] || opp;
      if (!target || !attackers.length) return null;
      action.attackers=Object.fromEntries(attackers.map(id=>[id,target]));
    }
    if (/DeclareBlockers/i.test(info.actionType||'')) {
      // Blocking is optional unless the provider marks mandatory assignments. Preserve mandatory ones;
      // otherwise an empty assignment is a real legal choice and avoids inventing combat heuristics.
      action.blockers=clone(info.mandatoryBlockerAssignments||{});
    }
    return action;
  }

  function chosenTargetForState(state,id) {
    if(state.players?.some(p=>p.playerId===id)) return {type:'Player',playerId:id};
    const z=state.zones?.find(x=>(x.cardIds||[]).includes(id));
    const t=String(zoneType(z)).toUpperCase();
    if(t==='STACK') return {type:'Spell',spellEntityId:id};
    if(t==='BATTLEFIELD') return {type:'Permanent',entityId:id};
    const c=state.cards?.[id];
    return {type:'Card',cardId:id,ownerId:c?.ownerId||zoneOwner(z)||state.viewingPlayerId,zone:zoneType(z)};
  }

  class ArgentumSession {
    constructor(spec={}) {
      this.spec=spec;
      this.user=this.newRole('user','Focused Magic');
      this.ai=this.newRole('ai',spec.aiLabel||'Dimir Tempo');
      this.state=null; this.legalActions=[]; this.pendingDecision=null; this.interactionEpoch=null;
      this.gameStarted=false; this.intentionalClose=false; this.aiMeaningful=[]; this.seats=[]; this.assessment=spec.assessment||null;
      this.sessionId=null; this.aiConfirmedActionCount=0; this.userConfirmedActionCount=0; this.fallbackStarted=false;
    }
    newRole(kind,name){return {kind,name,ws:null,connected:false,playerId:null,token:null,state:null,legalActions:[],pendingDecision:null,mulliganPrompt:null,bottomPrompt:null,openingRequestPending:false,mulliganComplete:false,playReady:false,interactionEpoch:null,lastVersion:0,resyncTimer:null,autoTimer:null,pendingMeaningful:null};}
    connect(){
      const cfg=providerConfig();
      if(!cfg.origin || !cfg.ws)throw new Error('Patched Argentum provider is not configured. Open Diagnostics and set the deployed provider origin/WebSocket.');
      if(!cfg.enabled)throw new Error('Argentum provider is disabled.');
      this.intentionalClose=false; setStatus(`Connecting to ${cfg.ws} …`); log(`Connect user seat ${cfg.ws}`);
      this.openRole(this.user,cfg.ws);
    }
    openRole(role,wsUrl){
      try{role.ws=new WebSocket(wsUrl);}catch(e){this.fail(`${role.kind} WebSocket creation failed: ${e.message}`);return;}
      role.ws.onopen=()=>{role.connected=true;log(`${role.kind} socket open`);this.sendRole(role,{type:'connect',playerName:role.name});};
      role.ws.onmessage=e=>{try{this.onRoleMessage(role,JSON.parse(e.data));}catch(err){this.fail(`${role.kind} provider message parse failed: ${err.message}`)}};
      role.ws.onerror=()=>this.fail(`${role.kind} Argentum WebSocket error.`);
      role.ws.onclose=e=>{role.connected=false;if(!this.intentionalClose)setStatus(`Argentum ${role.kind} seat disconnected (${e.code}).`,true);};
    }
    reconnect(){this.close();setTimeout(()=>this.connect(),50)}
    close(){
      this.intentionalClose=true;
      for(const role of [this.user,this.ai]){clearTimeout(role.resyncTimer);clearTimeout(role.autoTimer);try{role.ws?.close()}catch(_e){}role.ws=null;}
    }
    sendRole(role,msg){if(!role.ws||role.ws.readyState!==WebSocket.OPEN)throw new Error(`${role.kind} provider socket is not open.`);role.ws.send(JSON.stringify(msg));log(`${role.kind} → ${msg.type}`);}
    send(msg){this.sendRole(this.user,msg);}
    fail(msg){
      setStatus(msg,true);log(`FAIL ${msg}`);
      if(this.assessment){this.assessment.fail?.(msg);return;}
      if(this.spec.onProviderFail && !this.fallbackStarted){
        this.fallbackStarted=true;
        const cb=this.spec.onProviderFail;
        this.close();
        setTimeout(()=>{try{cb(msg)}catch(e){setStatus(`Fallback failed: ${e.message}`,true);}},0);
      }
    }
    onRoleMessage(role,msg){
      log(`${role.kind} ← ${msg.type}${msg.message?`: ${msg.message}`:''}`);
      if(msg.type==='error'){
        role.pendingMeaningful=null;
        if(msg.code==='INVALID_ACTION'){
          if(role.kind==='ai'&&role.lastAutomaticAction){role.rejectedActions=role.rejectedActions||new Set();role.rejectedActions.add(JSON.stringify(role.lastAutomaticAction));role.lastSubmittedStateKey=null;}
          setStatus(msg.message||'That action cannot be performed now.',true);
          log(`Action rejected for ${role.kind}; keeping the match open.`);
          clearTimeout(role.resyncTimer);role.resyncTimer=setTimeout(()=>{try{this.sendRole(role,{type:'requestResync'})}catch(_e){}},10);return;
        }
        this.fail(`${role.kind} ${msg.code||'ERROR'}: ${msg.message}`);return;
      }
      if(msg.type==='connected' || msg.type==='reconnected'){
        role.playerId=msg.playerId||role.playerId; role.token=msg.token||role.token;
        if(role.kind==='user' && !this.sessionId){
          setStatus('Argentum connected. Submitting exact player deck…');
          const providerUser=providerDeckSpec(this.spec.user); this.sendRole(role,{type:'createGame',deckList:providerUser.main,sideboard:providerUser.sideboard});
        } else if(role.kind==='ai' && this.sessionId && !role.joinSent) {
          role.joinSent=true;
          const providerAi=providerDeckSpec(this.spec.ai); this.sendRole(role,{type:'joinGame',sessionId:this.sessionId,deckList:providerAi.main,sideboard:providerAi.sideboard});
        }
        return;
      }
      if(role.kind==='user' && msg.type==='gameCreated'){
        this.sessionId=msg.sessionId;
        setStatus('Exact player deck accepted. Connecting exact Dimir 60+15 seat…');
        const cfg=providerConfig(); this.openRole(this.ai,cfg.ws); return;
      }
      if(msg.type==='gameStarted'){
        role.gameStarted=true; role.seats=msg.players||[];
        if(role.kind==='user') this.seats=msg.players||[];
        clearTimeout(role.resyncTimer);
        role.resyncTimer=setTimeout(()=>{try{this.sendRole(role,{type:'requestResync'})}catch(_e){}},20);
        if(this.user.gameStarted&&this.ai.gameStarted&&!this.gameStarted){
          this.gameStarted=true;setStatus(`Argentum exact-deck game started: ${this.spec.label}.`);this.assessment?.started(this);
          for(const seat of [this.user,this.ai]){
            clearTimeout(seat.resyncTimer);
            seat.resyncTimer=setTimeout(()=>{try{this.sendRole(seat,{type:'requestResync'})}catch(_e){}},30);
          }
        }
        return;
      }
      if(msg.type==='mulliganDecision'){
        role.openingRequestPending=false;
        role.mulliganComplete=false;role.playReady=false;clearTimeout(role.autoTimer);
        role.mulliganPrompt=msg; role.bottomPrompt=null;
        if(role.kind==='user' && msg.state && !this.state){this.state=msg.state;role.state=msg.state;}
        if(role.kind==='user' && !(this.spec.autopilot||this.assessment)){ this.renderMulligan(msg); if(this.spec.presentation==='tabletop')renderTabletop(); }
        else { this.submitOpening(role,'keepHand',null,msg); }
        if(role.kind==='user'&&!(this.spec.autopilot||this.assessment))this.sendRole(role,{type:'requestResync'});
        return;
      }
      if(msg.type==='chooseBottomCards'){
        role.openingRequestPending=false;
        role.mulliganComplete=false;role.playReady=false;clearTimeout(role.autoTimer);
        role.bottomPrompt=msg; role.mulliganPrompt=null;
        if(role.kind==='user' && !(this.spec.autopilot||this.assessment)){this.renderBottomCards(msg);if(this.spec.presentation==='tabletop')renderTabletop();}
        else { const ids=(msg.hand||msg.cardIds||[]).slice(0,msg.cardsToPutOnBottom||0); this.submitOpening(role,'chooseBottomCards',ids,msg); }
        return;
      }
      if(msg.type==='mulliganComplete'){
        role.openingRequestPending=false;
        role.mulliganComplete=true;role.mulliganPrompt=null;role.bottomPrompt=null;
        // Completion is per seat; an opening-state resync can expose legal actions
        // while the other player is still deciding. Refresh after both confirmations.
        if(this.user.mulliganComplete&&this.ai.mulliganComplete){
          for(const seat of [this.user,this.ai])this.sendRole(seat,{type:'requestResync'});
        }
        return;
      }
      if(msg.type==='waitingForOpponentMulligan'){
        role.playReady=false;clearTimeout(role.autoTimer);return;
      }
      if(msg.type==='stateUpdate'){
        const nextKey=JSON.stringify({state:msg.state,pendingDecision:msg.pendingDecision||null,legalActions:msg.legalActions||[]});
        if(role.playStateKey!==nextKey)role.rejectedActions=new Set();
        role.playStateKey=nextKey;
        role.playReady=this.user.mulliganComplete&&this.ai.mulliganComplete;
        role.state=msg.state;role.legalActions=msg.legalActions||[];role.pendingDecision=msg.pendingDecision||null;role.interactionEpoch=msg.interactionEpoch||null;role.lastVersion=msg.stateVersion||role.lastVersion;
        if(role.pendingMeaningful&&role.playStateKey!==role.lastSubmittedStateKey){
          const m=role.pendingMeaningful; role.pendingMeaningful=null;
          if(role.kind==='ai'){
            this.aiMeaningful.push(m);this.aiConfirmedActionCount++;log(`AI authoritative action confirmed: ${m.type}${m.name?` — ${m.name}`:''}`);
          } else this.userConfirmedActionCount++;
        }
        if(role.kind==='user'){
          this.state=role.state;this.legalActions=role.legalActions;this.pendingDecision=role.pendingDecision;this.interactionEpoch=role.interactionEpoch;
          renderState();this.assessment?.state(this,msg);
          if(this.spec.autopilot||this.assessment)this.autoRole(role);
        } else {
          this.assessment?.aiState?.(this,msg);
          this.autoRole(role);
        }
        return;
      }
      if(msg.type==='stateDeltaUpdate'){
        role.legalActions=msg.legalActions||role.legalActions;role.pendingDecision=msg.pendingDecision||null;role.interactionEpoch=msg.interactionEpoch||role.interactionEpoch;
        clearTimeout(role.resyncTimer);role.resyncTimer=setTimeout(()=>{try{this.sendRole(role,{type:'requestResync'})}catch(_e){}},10);return;
      }
      if(msg.type==='gameOver'){
        if(role.kind==='user'){setStatus(`Game over: ${msg.reason}.`);this.assessment?.gameOver(this,msg);}return;
      }
    }
    renderMulligan(msg){
      ensureUI(); $('argentumGame').classList.remove('hidden'); this.user.mulliganPrompt=msg;
      $('argentumDecision').innerHTML=`<p><b>Opening hand</b> • mulligans ${msg.mulliganCount||0}</p><button id="argentumKeep" class="good">Keep</button> <button id="argentumMulligan">Mulligan</button>`;
      $('argentumKeep').onclick=()=>this.submitOpening(this.user,'keepHand',null,msg);
      $('argentumMulligan').onclick=()=>this.submitOpening(this.user,'mulligan',null,msg);
    }
    renderBottomCards(msg,hostId='argentumDecision'){
      ensureUI(); $('argentumGame').classList.remove('hidden'); this.user.bottomPrompt=msg;
      const ids=msg.hand||msg.cardIds||[], need=msg.cardsToPutOnBottom||0;
      const choicesId=hostId+'BottomChoices',confirmId=hostId+'BottomConfirm';
      $(hostId).innerHTML=`<p><b>London mulligan</b> • choose exactly ${need} card${need===1?'':'s'} to put on the bottom.</p><div id="${choicesId}" class="choose-grid"></div><button id="${confirmId}" class="good" type="button">Put selected on bottom</button>`;
      const box=$(choicesId);
      for(const id of ids){const b=document.createElement('button');b.type='button';b.className='choose-card';b.dataset.id=id;b.textContent=msg.cards?.[id]?.name||cardName(this.user.state,id);b.onclick=()=>b.classList.toggle('selected');box.appendChild(b);}
      $(confirmId).onclick=()=>{const chosen=[...box.querySelectorAll('.choose-card.selected')].map(b=>b.dataset.id);if(chosen.length!==need){setStatus(`Choose exactly ${need} card${need===1?'':'s'} to bottom.`,true);return;}this.submitOpening(this.user,'chooseBottomCards',chosen,msg);};
    }
    submitOpening(role,type,cardIds,prompt){
      const current=type==='chooseBottomCards'?role.bottomPrompt:role.mulliganPrompt;
      if(role.openingRequestPending||role.mulliganComplete||!current||current!==prompt)return false;
      role.openingRequestPending=true;role.mulliganPrompt=null;role.bottomPrompt=null;role.playReady=false;clearTimeout(role.autoTimer);
      this.sendRole(role,{type,...(cardIds?{cardIds}: {})});
      if(role.kind==='user'){
        for(const id of ['argentumKeep','argentumMulligan','argentumDecisionBottomConfirm','argentumTabletopDecisionBottomConfirm']){const button=$(id);if(button)button.disabled=true;}
        if(this.spec.presentation==='tabletop')renderTabletopDecision();
      }
      return true;
    }
    canPlay(role){return role.playReady&&this.user.mulliganComplete&&this.ai.mulliganComplete&&!role.mulliganPrompt&&!role.bottomPrompt;}
    submitAction(action){if(!this.canPlay(this.user))throw new Error('Wait for both players to finish their opening hands.');this.sendRole(this.user,{type:'submitAction',action,interactionEpoch:this.user.interactionEpoch});}
    submitDecision(response){if(!this.user.state)throw new Error('No state.');this.submitAction({type:'SubmitDecision',playerId:this.user.pendingDecision?.playerId||this.user.state.viewingPlayerId,response});}
    submitRoleAction(role,action,info){
      if(!this.canPlay(role)||role.lastSubmittedStateKey===role.playStateKey)return;
      role.lastSubmittedStateKey=role.playStateKey;
      role.lastAutomaticAction=clone(action);
      this.sendRole(role,{type:'submitAction',action,interactionEpoch:role.interactionEpoch});
      const t=info?.actionType||action?.type||'';
      if(!/PassPriority|DeclareBlockers/i.test(t)) role.pendingMeaningful={type:t,name:info?.description||''};
    }
    submitRoleDecision(role,response){
      if(!this.canPlay(role)||role.lastSubmittedStateKey===role.playStateKey)return;
      const playerId=role.pendingDecision?.playerId||role.state?.viewingPlayerId;
      if(!playerId)return;
      role.lastSubmittedStateKey=role.playStateKey;
      this.sendRole(role,{type:'submitAction',action:{type:'SubmitDecision',playerId,response},interactionEpoch:role.interactionEpoch});
    }
    autoRole(role){
      clearTimeout(role.autoTimer);role.autoTimer=setTimeout(()=>{
        try{
          if(!this.canPlay(role)||!role.state||!role.interactionEpoch)return;
          if(role.pendingDecision){const r=decisionResponseSafeForState(role.pendingDecision,'auto',role.state);if(r)this.submitRoleDecision(role,r);return;}
          const me=role.state.viewingPlayerId;if(role.state.priorityPlayerId!==me)return;
          const candidates=(role.legalActions||[]).filter(a=>a.isAffordable!==false);
          const rank=a=>/PlayLand/i.test(a.actionType)?0:/CastSpell/i.test(a.actionType)?1:/ActivateAbility/i.test(a.actionType)&&!a.isManaAbility?2:/DeclareAttackers/i.test(a.actionType)?3:/DeclareBlockers/i.test(a.actionType)?4:/PassPriority/i.test(a.actionType)?99:20;
          candidates.sort((a,b)=>rank(a)-rank(b));
          let picked=null,action=null;
          for(const c of candidates){const completed=completeActionForBot(c,role.state);if(completed&&!role.rejectedActions?.has(JSON.stringify(completed))){picked=c;action=completed;break;}}
          if(!action){picked=(role.legalActions||[]).find(a=>/PassPriority/i.test(a.actionType)&&!role.rejectedActions?.has(JSON.stringify(a.action)));action=picked?clone(picked.action):null;}
          if(action)this.submitRoleAction(role,action,picked);
        }catch(e){log(`${role.kind} autopilot: ${e.message}`);}
      },role.kind==='ai'?80:110);
    }
  }

  let active=null;

  function launch(spec) {
    ensureUI();
    try{active?.close();}catch(_e){}
    active=new ArgentumSession(spec);
    if(spec?.presentation==='tabletop')openTabletopUI();else openUI();
    active.connect();return active;
  }

  function startFixture(key, opts={}) {
    ensureUI();
    const chk=assertExactFixture(key), f=FIXTURES[key];
    $('argentumAutopilot').checked=!!opts.autopilot;
    return launch({label:f.label,user:chk.user,ai:chk.ai,aiLabel:key==='core'?'Core opponent':'Dimir Tempo',autopilot:!!opts.autopilot,assessment:opts.assessment||null,onProviderFail:opts.onProviderFail||null});
  }

  function startText(userText, aiText, opts={}) {
    const user=parseDecklist(userText), ai=parseDecklist(aiText);
    if(user.mainCount<40||ai.mainCount<40) throw new Error('Argentum match decks must contain at least 40 main-deck cards.');
    if(user.sideboardCount>15||ai.sideboardCount>15) throw new Error('Argentum constructed sideboards may contain at most 15 cards.');
    return launch({label:opts.label||'Focused Magic constructed',user,ai,aiLabel:opts.aiLabel||'Dimir Tempo',autopilot:!!opts.autopilot,assessment:opts.assessment||null,onProviderFail:opts.onProviderFail||null,presentation:opts.presentation||'tabletop'});
  }

  function startDynamic(userCards, aiCards, opts={}) {
    const user={main:countsFromCards(userCards),sideboard:{},mainCount:(userCards||[]).length,sideboardCount:0};
    const ai={main:countsFromCards(aiCards),sideboard:{},mainCount:(aiCards||[]).length,sideboardCount:0};
    return launch({label:opts.label||'Focused Magic match',user,ai,aiLabel:opts.aiLabel||'AI opponent',autopilot:!!opts.autopilot,onProviderFail:opts.onProviderFail||null});
  }

  async function catalogCoverage(requestedNames) {
    const cfg=providerConfig();
    const allNames=[...new Set(requestedNames||Object.values(FIXTURES).flatMap(f=>{const u=parseDecklist(f.user),a=parseDecklist(f.ai);return [...u.uniqueNames,...a.uniqueNames]}))];
    const r=await fetch(`${cfg.origin}/api/cards`,{headers:{Accept:'application/json'}});
    if(!r.ok)throw new Error(`Catalog HTTP ${r.status}`);
    const rows=await r.json();const have=new Set((rows||[]).map(x=>x.name));
    const missing=allNames.filter(n=>!have.has(providerCardName(n)));
    const needsFocusedPatch=allNames.filter(n=>n==='Gemstone Caverns' && !have.has('Gemstone Caverns'));
    return {total:allNames.length,missing,needsFocusedPatch,publicUpstream:isPublicUpstream(cfg)};
  }

  function reportAssessment(lines) {
    ensureUI();$('argentumAssessmentReport').innerHTML=lines.map(x=>`<div class="argentum-checkrow ${x.ok===true?'pass':x.ok===false?'fail':'pending'}"><b>${x.ok===true?'PASS':x.ok===false?'FAIL':'…'}</b> ${esc(x.text)}</div>`).join('');
  }

  async function runMandatoryAssessment() {
    ensureUI();openUI();$('argentumAutopilot').checked=true;
    const lines=[];const push=(ok,text)=>{lines.push({ok,text});reportAssessment(lines)};
    try{
      const n=assertExactFixture('mandatory'),l=assertExactFixture('legacy'),d=parseDecklist(DIMIR_TEMPO_DECK);
      push(true,`Exact fixtures locked: new ${n.user.mainCount}; Page legacy ${l.user.mainCount}+${l.user.sideboardCount}; Dimir ${d.mainCount}+${d.sideboardCount}.`);
    }catch(e){push(false,e.message);setStatus('Mandatory assessment stopped on fixture integrity.',true);return;}
    try{const c=await catalogCoverage();if(c.missing.length){const patchNote=c.publicUpstream&&c.needsFocusedPatch?.length?' Public upstream Argentum does not contain the Focused Magic Gemstone patch; deploy/configure the patched backend before gameplay validation.':'';push(false,`Provider catalog is reachable but missing ${c.missing.length} required names after canonical aliasing: ${c.missing.join(', ')}.${patchNote}`);setStatus('Mandatory assessment FAILED: configured Argentum registry is incompatible with this Focused Magic deck.',true);return;}push(true,`Provider catalog contains all ${c.total} distinct required card identities after canonical aliasing.`);}catch(e){push(null,`Catalog preflight unavailable (${e.message}); continuing to the authoritative exact-deck game-admission gate.`);}

    const queue=['mandatory','legacy'];let ix=0;
    const next=()=>{
      if(ix>=queue.length){push(true,'Mandatory runtime gate complete: both registered decks started exactly and Dimir produced a meaningful game action.');setStatus('v0.6.9 mandatory assessment PASSED.');return;}
      const key=queue[ix++];let done=false,updates=0;
      const ctl={
        started(){push(null,`${FIXTURES[key].label}: server accepted exact user deck + exact Dimir deck; waiting for AI liveness.`);},
        state(session){updates++;if(!done&&session.aiMeaningful.length>0){done=true;lines.pop();push(true,`${FIXTURES[key].label}: exact-deck game started and Dimir acted (${session.aiMeaningful[0].type}${session.aiMeaningful[0].name?` — ${session.aiMeaningful[0].name}`:''}).`);try{session.send({type:'concede'})}catch(_e){};setTimeout(next,250);}else if(updates>240&&!done){done=true;lines.pop();push(false,`${FIXTURES[key].label}: AI liveness failed — no meaningful Dimir play after ${updates} authoritative updates.`);setStatus('Mandatory assessment FAILED on Dimir liveness.',true);session.close();}},
        fail(msg){if(done)return;done=true;push(false,`${FIXTURES[key].label}: ${msg}`);setStatus('Mandatory assessment FAILED.',true);},
        gameOver(){if(!done){done=true;push(false,`${FIXTURES[key].label}: game ended before Dimir demonstrated a meaningful action.`);setStatus('Mandatory assessment FAILED.',true);}}
      };
      startFixture(key,{autopilot:true,assessment:ctl});
    };
    next();
  }

  function installConstructedProfile() {
    const toolbar=document.querySelector('#constructedModal .constructed-toolbar');
    if(!toolbar||$('constructedProfile'))return;
    const label=document.createElement('label');
    label.innerHTML=`Deck profile<select id="constructedProfile"><option value="october" selected>October 3 uploaded deck</option><option value="mandatory">v0.6.5 mandatory</option><option value="legacy">Page legacy</option></select>`;
    toolbar.prepend(label);
    const apply=()=>{
      const key=$('constructedProfile').value;
      $('constructedUserDeck').value=FIXTURES[key].user;
      $('constructedAiDeck').value=DIMIR_TEMPO_DECK;
      const heading=$('constructedUserDeck')?.closest('.constructed-deckbox')?.querySelector('h3');if(heading)heading.textContent=`You — ${FIXTURES[key].label}`;
      try{window.validateConstructedUI?.()}catch(_e){}
    };
    $('constructedProfile').onchange=apply;apply();
  }

  function providerEnabled(){const cfg=providerConfig();return Boolean(cfg.origin && cfg.ws && cfg.enabled!==false);}

  window.FocusedMagicArgentum={
    VERSION,ARGENTUM_COMMIT,FIXTURES,parseDecklist,assertExactFixture,providerConfig,saveConfig,providerEnabled,
    open:openUI,openTabletop:openTabletopUI,closeTabletop:closeTabletopUI,startFixture,startText,startDynamic,runMandatoryAssessment,catalogCoverage,providerCardName,providerDeckMap,get active(){return active;}
  };

  window.addEventListener('DOMContentLoaded',()=>{ensureUI();installConstructedProfile();});
})();
