(function () {
  'use strict';

  const VERSION = '0.6.9';
  const ARGENTUM_COMMIT = 'e4fcc1621812670c58c001a80df9c71eced69a1c';
  const DEFAULT_PROVIDER_ORIGIN = 'https://focused-magic-argentum-067-pro.onrender.com';
  const DEFAULT_PROVIDER_WS = 'wss://focused-magic-argentum-067-pro.onrender.com/game';
  const STORAGE_KEY = 'focused_magic_argentum_v069_render_pro';
  const LEGACY_PROVIDER_STORAGE_KEYS = ['focused_magic_argentum_v065','focused_magic_argentum_v067','focused_magic_argentum_v067_render_pro','focused_magic_argentum_v068_render_pro'];

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
    mandatory: { label: 'v0.6.5 mandatory deck', user: MANDATORY_DECK, ai: DIMIR_TEMPO_DECK, expected: [60, 0, 60, 15] },
    legacy: { label: 'Page, Loose Leaf legacy', user: PAGE_LEGACY_DECK, ai: DIMIR_TEMPO_DECK, expected: [61, 2, 60, 15] },
    core: { label: 'Core Match', user: CORE_DECK, ai: CORE_DECK, expected: [60, 0, 60, 0] },
  });

  const $ = (id) => document.getElementById(id);
  const esc = (s) => String(s ?? '').replace(/[&<>"']/g, c => ({ '&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;' }[c]));
  const clone = (x) => JSON.parse(JSON.stringify(x));

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
          <div><h2>Focused Magic v0.6.9 — Argentum Match Engine</h2><p class="sub">Pinned provider ${ARGENTUM_COMMIT.slice(0,12)} • server-authoritative rules, legal actions, decisions, combat, hidden information and AI</p></div>
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
          <section class="argentum-tabletop-hand"><div class="argentum-tabletop-zone-title">Your hand — tap a card to play/cast</div><div id="argentumTabletopMyHand" class="argentum-tabletop-cards hand"></div></section>
        </div>
        <div id="argentumTabletopPlayerEffects" class="sub"></div>
        <section id="argentumTabletopCardActions" class="argentum-tabletop-actions"><span class="sub">Select a card or use Pass/Continue.</span></section>
        <section id="argentumTabletopTurnActions" class="argentum-tabletop-actions"></section>
        <section class="argentum-tabletop-decision"><div id="argentumTabletopDecision" class="sub">No pending decision.</div></section>
        <div class="argentum-tabletop-footer"><button id="argentumTabletopPass" class="good" type="button">Pass / Continue</button><span class="sub">Argentum is authoritative. Only provider-advertised legal actions are shown.</span></div>
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
  function closeTabletopUI(){$('argentumTabletopModal')?.classList.remove('open');$('argentumTabletopModal')?.setAttribute('aria-hidden','true');}
  function setTabletopStatus(text,bad=false){ensureTabletopUI();const el=$('argentumTabletopStatus');el.textContent=text;el.classList.toggle('bad-text',!!bad);}

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
    const art=c.imageUri||c.imageURL||c.imageUrl||'';
    b.innerHTML=art?`<img src="${esc(art)}" alt="${esc(c.name)}"><span><b>${esc(c.name)}</b></span>`:`<span class="argentum-tabletop-cardname"><b>${esc(c.name)}</b><small>${esc(c.manaCost||'')} ${esc(c.typeLine||'')}</small></span>`;
    // Display the provider's projected values, including X=0; never derive card rules here.
    if(c.power!=null&&c.toughness!=null)b.innerHTML+=`<small class="argentum-tabletop-stat">${esc(c.power)}/${esc(c.toughness)}</small>`;
    if(c.chosenX!=null)b.innerHTML+=`<small class="argentum-tabletop-x">X=${esc(c.chosenX)}</small>`;
    for(const [type,count] of Object.entries(c.counters||{})){
      if(count>0)b.innerHTML+=`<small class="argentum-tabletop-counter">${esc(type.toLowerCase().replaceAll('_',' '))}: ${esc(count)}</small>`;
    }
    b.title=c.stackText||c.oracleText||c.name||'';
    if(c.isTapped)b.classList.add('tapped');
    b.onclick=()=>showCardActions(id,zone);
    return b;
  }
  function fillTableZone(id,state,zone){const el=$(id);if(!el)return;el.innerHTML='';if(!zone){el.innerHTML='<span class="sub">—</span>';return;}if(zone.isVisible===false){el.innerHTML=`<span class="sub">${zone.size??0} hidden cards</span>`;return;}for(const cardId of zone.cardIds||[])el.appendChild(providerTableCard(state,cardId,zoneType(zone)));if(!(zone.cardIds||[]).length)el.innerHTML='<span class="sub">Empty</span>';}
  function showCardActions(id,zone){
    ensureTabletopUI();const box=$('argentumTabletopCardActions');const c=cardById(active?.state,id);const actions=actionsForCard(id).filter(a=>a.isAffordable!==false);
    box.innerHTML=`<div class="argentum-tabletop-action-title"><b>${esc(c?.name||id)}</b><span class="sub">${esc(zone||'')}</span></div>`;
    if(!actions.length){const note=document.createElement('span');note.className='sub';note.textContent='No provider-advertised legal action for this card right now.';box.appendChild(note);return;}
    for(const info of actions){const b=document.createElement('button');b.type='button';b.className='good';b.textContent=info.description||info.actionType||'Use action';b.onclick=()=>submitLegalAction(info);box.appendChild(b);}
  }
  function renderTabletopDecision(){
    const box=$('argentumTabletopDecision');if(!box)return;const d=active?.pendingDecision;
    if(active?.user?.mulliganPrompt){box.innerHTML='<b>Opening hand decision</b> ';const keep=document.createElement('button');keep.className='good';keep.textContent='Keep';keep.onclick=()=>{active.user.mulliganPrompt=null;active.sendRole(active.user,{type:'keepHand'});};const mul=document.createElement('button');mul.textContent='Mulligan';mul.onclick=()=>{active.user.mulliganPrompt=null;active.sendRole(active.user,{type:'mulligan'});};box.append(keep,mul);return;}
    if(active?.user?.bottomPrompt){box.innerHTML='<b>London mulligan:</b> use Diagnostics for bottom-card selection in this WIP.';return;}
    if(!d){box.textContent='No pending decision.';return;}
    box.innerHTML=`<b>${esc(d.prompt||d.type)}</b> `;const b=document.createElement('button');b.className='primary';b.textContent='Resolve decision';b.onclick=()=>{try{const r=decisionResponse(d,'human');if(r)active.submitDecision(r);}catch(e){setTabletopStatus(e.message,true)}};box.appendChild(b);
  }
  function playerEffectsSummary(player){
    const limit=player?.maxHandSize;
    const limitText=limit===null?'No maximum hand size':Number.isFinite(limit)?`Hand limit: ${limit}`:'Hand limit: unknown';
    const effects=(player?.activeEffects||[]).map(effect=>[effect.name,effect.description].filter(Boolean).join(': '));
    return [limitText,...effects].join(' • ');
  }
  function renderTabletop(){
    ensureTabletopUI();const s=active?.state;if(!s)return;openTabletopUI();
    const me=s.viewingPlayerId,my=s.players?.find(p=>p.playerId===me),opp=s.players?.find(p=>p.playerId!==me);
    $('argentumTabletopMyName').textContent=my?.name||'You';$('argentumTabletopMyLife').textContent=my?.life??'?';$('argentumTabletopOppName').textContent=opp?.name||'Opponent';$('argentumTabletopOppLife').textContent=opp?.life??'?';
    const myHand=stateZone(s,me,'HAND'),myLib=stateZone(s,me,'LIBRARY'),myGy=stateZone(s,me,'GRAVEYARD');const oppHand=opp?stateZone(s,opp.playerId,'HAND'):null,oppLib=opp?stateZone(s,opp.playerId,'LIBRARY'):null;
    $('argentumTabletopMyLibCount').textContent=myLib?.size??myLib?.cardIds?.length??'?';$('argentumTabletopMyGyCount').textContent=myGy?.size??myGy?.cardIds?.length??0;$('argentumTabletopOppHandCount').textContent=oppHand?.size??oppHand?.cardIds?.length??'?';$('argentumTabletopOppLibCount').textContent=oppLib?.size??oppLib?.cardIds?.length??'?';
    $('argentumTabletopScore').textContent=`Turn ${s.turnNumber??'?'} • ${s.currentPhase||''}${s.currentStep?'/'+s.currentStep:''} • Priority: ${s.players?.find(p=>p.playerId===s.priorityPlayerId)?.name||s.priorityPlayerId||'—'}`;
    fillTableZone('argentumTabletopMyHand',s,myHand);fillTableZone('argentumTabletopMyBattlefield',s,stateZone(s,me,'BATTLEFIELD'));if(opp)fillTableZone('argentumTabletopOppBattlefield',s,stateZone(s,opp.playerId,'BATTLEFIELD'));fillTableZone('argentumTabletopStack',s,s.zones?.find(z=>String(zoneType(z)).toUpperCase()==='STACK'));
    $('argentumTabletopCardActions').innerHTML='<span class="sub">Select a card or use Pass/Continue.</span>';
    $('argentumTabletopPlayerEffects').textContent=playerEffectsSummary(my);
    const turnActions=$('argentumTabletopTurnActions');turnActions.innerHTML='';
    if(!active.pendingDecision)for(const info of active.legalActions||[]){
      if(!/DeclareAttackers|DeclareBlockers/.test(info.actionType||''))continue;
      const button=document.createElement('button');button.type='button';button.className='primary';
      button.textContent=/DeclareAttackers/.test(info.actionType)?'Choose attackers':'Continue with required blocks only';
      button.onclick=()=>submitLegalAction(info);turnActions.appendChild(button);
    }
    renderTabletopDecision();setTabletopStatus('Argentum game active.');
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

  function pickIds(promptText, ids, min=1, max=1, cardInfo={}) {
    const opts=(ids||[]).map((id,i)=>`${i+1}. ${entityLabel(id,cardInfo[id])}`).join('\n');
    if(!ids?.length && min>0) throw new Error(`${promptText}: no legal targets.`);
    if(max===0)return [];
    const d=(ids||[]).slice(0,min).map(id=>String((ids||[]).indexOf(id)+1)).join(',');
    const raw=window.prompt(`${promptText}\n${opts}\nEnter ${min===max?min:`${min}-${max}`} number(s), comma separated:`,d);
    if(raw==null)throw new Error('Action cancelled.');
    const picked=[...new Set(raw.split(/[ ,]+/).filter(Boolean).map(x=>ids[Number(x)-1]).filter(Boolean))];
    if(picked.length<min||picked.length>max)throw new Error(`Choose ${min===max?min:`${min}-${max}`} target(s).`);
    return picked;
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

  async function submitLegalAction(info) {
    try {
      if(!active?.interactionEpoch)throw new Error('No live interaction epoch; request a resync first.');
      const action=clone(info.action);
      if(/DeclareAttackers/.test(info.actionType||action.type||'')){
        const valid=info.validAttackers||[],mandatory=info.mandatoryAttackers||[];
        const chosen=pickIds('Choose attackers (leave empty to attack with none)',valid,mandatory.length,valid.length);
        if(mandatory.some(id=>!chosen.includes(id)))throw new Error('Choose every mandatory attacker.');
        action.attackers={};
        for(const id of chosen){
          const targets=pickIds(`Choose attack target for ${entityLabel(id)}`,info.validAttackTargets||[],1,1);
          action.attackers[id]=targets[0];
        }
      }
      if(/DeclareBlockers/.test(info.actionType||action.type||'')){
        action.blockers=clone(info.mandatoryBlockerAssignments||{});
      }
      if(info.modalEnumeration && action.type==='CastSpell') {
        const e=info.modalEnumeration;
        const available=(e.modes||[]).filter(m=>m.available!==false && !(e.unavailableIndices||[]).includes(m.index));
        const min=Math.max(1,e.minChooseCount??1), max=Math.max(min,e.chooseCount??min);
        if(!available.length||(!e.allowRepeat&&available.length<min))throw new Error('Provider offered a modal spell without enough available modes.');
        const listing=available.map((m,i)=>`${i+1}. ${m.description||`Mode ${m.index+1}`}`).join('\n');
        const def=Array.from({length:min},(_,i)=>String((e.allowRepeat?i%available.length:i)+1)).join(',');
        const raw=prompt(`Choose ${min===max?min:`${min}-${max}`} mode(s):\n${listing}`,def);
        if(raw==null)return;
        let picks=raw.split(/[ ,]+/).filter(Boolean).map(x=>available[Number(x)-1]).filter(Boolean).map(m=>m.index);
        if(!e.allowRepeat)picks=[...new Set(picks)];
        if(picks.length<min||picks.length>max)throw new Error(`Choose ${min===max?min:`${min}-${max}`} legal mode(s).`);
        action.chosenModes=picks;
      }
      if(info.hasXCost){const max=info.maxAffordableX??20;const raw=prompt(`Choose X (${info.minX??0}–${max})`,String(info.minX??0));if(raw==null)return;action.xValue=Math.max(info.minX??0,Math.min(max,Number(raw)||0));}
      if(info.modalEnumeration && action.type==='CastSpell') {
        collectModalTargets(action,info.modalEnumeration,
          (req,mode)=>pickIds(`${mode.description||'Selected mode'}: ${req.description||'Choose target'}`,req.validTargets,req.minTargets??1,req.maxTargets??1),chosenTarget);
      }
      if(info.additionalCostInfo?.costType==='PayXLife') {
        const max=info.additionalCostInfo.payXLifeMaxX??0;
        const raw=prompt(`Choose X: pay that much life (0–${max})`,'0');
        if(raw==null)return;
        const amount=Number(raw);
        if(!String(raw).trim()||!Number.isSafeInteger(amount)||amount<0||amount>max)throw new Error(`Choose a whole number of life from 0 to ${max}.`);
        action.additionalCostPayment={...(action.additionalCostPayment||{}),payXLifeAmount:amount};
      }
      if(info.requiresTargets && !info.modalEnumeration){
        const chosen=[];
        const reqs=info.targetRequirements?.length?info.targetRequirements:[{description:info.targetDescription||'Choose target',minTargets:info.minTargets??info.targetCount??1,maxTargets:info.targetCount??1,validTargets:info.validTargets||[]}];
        for(const req of reqs){const ids=pickIds(req.description||'Choose target',req.validTargets||[],req.minTargets??1,req.maxTargets??1);chosen.push(...ids.map(chosenTarget));}
        action.targets=chosen;
      }
      if(info.requiresManaColorChoice){const colors=info.availableManaColors?.length?info.availableManaColors:['WHITE','BLUE','BLACK','RED','GREEN'];const raw=prompt(`Choose mana color: ${colors.join(', ')}`,colors[0]);if(raw==null)return;action.manaColorChoice=String(raw).toUpperCase();}
      // The provider advertises complex alternative/additional costs separately. If the template
      // still requires a resource choice, do not invent one: expose the exact action JSON editor.
      if(info.additionalCostInfo && !action.additionalCostPayment && !action.costPayment){
        const raw=prompt('This action has a provider-defined additional cost. Edit the exact action JSON if needed; Cancel to abort.',JSON.stringify(action));
        if(raw==null)return; Object.assign(action,JSON.parse(raw));
      }
      active.submitAction(action);
    }catch(e){setStatus(e.message,true);log(`Action blocked: ${e.message}`);}
  }

  function decisionResponse(decision, mode='human') {
    const type=decision?.type||''; const id=decision?.id;
    const ai = mode==='auto';
    if(type==='YesNoDecision') return {type:'YesNoResponse',decisionId:id,choice: ai ? (decision.defaultChoice ?? true) : confirm(decision.prompt||'Yes?')};
    if(type==='BatchYesNoDecision') return {type:'BatchYesNoResponse',decisionId:id,choice:true,applyToAll:true};
    if(type==='ChooseColorDecision'){
      const colors=decision.colors||decision.options||['WHITE','BLUE','BLACK','RED','GREEN'];
      const raw=ai?colors[0]:prompt(`${decision.prompt||'Choose color'}: ${colors.join(', ')}`,colors[0]); if(raw==null)return null;
      return {type:'ColorChosenResponse',decisionId:id,color:String(raw).toUpperCase(),colors:decision.maxColors>1?[String(raw).toUpperCase()]:[]};
    }
    if(type==='ChooseNumberDecision'){
      const min=decision.min??decision.minValue??0,max=decision.max??decision.maxValue??20; const val=ai?min:Number(prompt(`${decision.prompt||'Choose number'} (${min}–${max})`,String(min))); if(!Number.isFinite(val))return null;
      return {type:'NumberChosenResponse',decisionId:id,number:Math.max(min,Math.min(max,val))};
    }
    if(type==='ChooseOptionDecision'){
      const opts=decision.options||[]; const ix=ai?0:Math.max(0,Number(prompt(`${decision.prompt||'Choose option'}\n${opts.map((x,i)=>`${i+1}. ${x}`).join('\n')}`,'1'))-1);
      return {type:'OptionChosenResponse',decisionId:id,optionIndex:Math.min(Math.max(ix,0),Math.max(0,opts.length-1))};
    }
    if(type==='ChooseModeDecision'){
      const opts=decision.modes||decision.options||[]; const min=decision.minModes??decision.chooseCount??1,max=decision.maxModes??decision.chooseCount??1;
      const picks=ai?[0]:String(prompt(`${decision.prompt||'Choose mode'}\n${opts.map((x,i)=>`${i+1}. ${x.description||x}`).join('\n')}`,'1')||'1').split(/[ ,]+/).map(x=>Number(x)-1).filter(x=>x>=0).slice(0,max);
      return {type:'ModesChosenResponse',decisionId:id,selectedModes:picks.length>=min?picks:[0]};
    }
    if(type==='SelectCardsDecision' || type==='SearchLibraryDecision'){
      const options=decision.options||[]; const min=decision.minSelections??decision.minCards??0,max=decision.maxSelections??decision.maxCards??options.length;
      const picks=ai?options.slice(0,min):pickIds(decision.prompt||'Choose cards',options,min,max,decision.cardInfo||{});
      return {type:'CardsSelectedResponse',decisionId:id,selectedCards:picks};
    }
    if(type==='ChooseTargetsDecision'){
      const reqs=decision.requirements||decision.targetRequirements||[]; const selectedTargets={};
      reqs.forEach((r,i)=>{selectedTargets[i]=ai?(r.validTargets||[]).slice(0,r.minTargets??1):pickIds(r.description||decision.prompt||'Choose targets',r.validTargets||[],r.minTargets??1,r.maxTargets??1)});
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
    const suggested=decisionResponseSafe(d,'auto');
    $('argentumDecisionJson').value=suggested?JSON.stringify(suggested,null,2):JSON.stringify({type:'',decisionId:d.id},null,2);
    $('argentumResolveDecision').onclick=()=>{try{const response=decisionResponse(d,'human');if(response)active.submitDecision(response);}catch(e){setStatus(e.message,true)}};
    $('argentumSubmitDecisionJson').onclick=()=>{try{active.submitDecision(JSON.parse($('argentumDecisionJson').value));}catch(e){setStatus(`Decision JSON error: ${e.message}`,true)}};
  }
  function decisionResponseSafe(d,m){try{return decisionResponse(d,m)}catch(_e){return null}}

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
      if(type==='SelectCardsDecision'||type==='SearchLibraryDecision'){const options=d.options||[],min=d.minSelections??d.minCards??0;return {type:'CardsSelectedResponse',decisionId:id,selectedCards:options.slice(0,min)};}
      if(type==='ChooseTargetsDecision'){const reqs=d.requirements||d.targetRequirements||[],selectedTargets={};reqs.forEach((r,i)=>{selectedTargets[i]=(r.validTargets||[]).slice(0,r.minTargets??1)});return {type:'TargetsResponse',decisionId:id,selectedTargets};}
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
    return !!((info?.additionalCostInfo && info.additionalCostInfo.costType!=='PayXLife') || info?.hasConvoke || info?.hasDelve || info?.hasHarmonize || info?.hasTapForGeneric || info?.requiresDamageDistribution);
  }

  function completeActionForBot(info, state) {
    if (!info || info.isAffordable === false || actionNeedsUnsupportedAutomation(info)) return null;
    const action=clone(info.action);
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
    newRole(kind,name){return {kind,name,ws:null,connected:false,playerId:null,token:null,state:null,legalActions:[],pendingDecision:null,mulliganPrompt:null,bottomPrompt:null,interactionEpoch:null,lastVersion:0,resyncTimer:null,autoTimer:null,pendingMeaningful:null};}
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
        if(this.user.gameStarted&&this.ai.gameStarted&&!this.gameStarted){
          this.gameStarted=true;setStatus(`Argentum exact-deck game started: ${this.spec.label}.`);this.assessment?.started(this);
        }
        return;
      }
      if(msg.type==='mulliganDecision'){
        role.mulliganPrompt=msg; role.bottomPrompt=null;
        if(role.kind==='user' && !(this.spec.autopilot||this.assessment)) this.renderMulligan(msg);
        else { role.mulliganPrompt=null; this.sendRole(role,{type:'keepHand'}); }
        return;
      }
      if(msg.type==='chooseBottomCards'){
        role.bottomPrompt=msg; role.mulliganPrompt=null;
        if(role.kind==='user' && !(this.spec.autopilot||this.assessment)) this.renderBottomCards(msg);
        else { const ids=(msg.hand||msg.cardIds||[]).slice(0,msg.cardsToPutOnBottom||0); role.bottomPrompt=null; this.sendRole(role,{type:'chooseBottomCards',cardIds:ids}); }
        return;
      }
      if(msg.type==='stateUpdate'){
        role.state=msg.state;role.legalActions=msg.legalActions||[];role.pendingDecision=msg.pendingDecision||null;role.interactionEpoch=msg.interactionEpoch||null;role.lastVersion=msg.stateVersion||role.lastVersion;
        if(role.pendingMeaningful){
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
      $('argentumKeep').onclick=()=>{this.user.mulliganPrompt=null;this.sendRole(this.user,{type:'keepHand'});};
      $('argentumMulligan').onclick=()=>{this.user.mulliganPrompt=null;this.sendRole(this.user,{type:'mulligan'});};
    }
    renderBottomCards(msg){
      ensureUI(); $('argentumGame').classList.remove('hidden'); this.user.bottomPrompt=msg;
      const ids=msg.hand||msg.cardIds||[], need=msg.cardsToPutOnBottom||0;
      $('argentumDecision').innerHTML=`<p><b>London mulligan</b> • choose exactly ${need} card${need===1?'':'s'} to put on the bottom.</p><div id="argentumBottomChoices" class="choose-grid"></div><button id="argentumBottomConfirm" class="good" type="button">Put selected on bottom</button>`;
      const box=$('argentumBottomChoices');
      for(const id of ids){const b=document.createElement('button');b.type='button';b.className='choose-card';b.dataset.id=id;b.textContent=cardName(this.user.state,id);b.onclick=()=>b.classList.toggle('selected');box.appendChild(b);}
      $('argentumBottomConfirm').onclick=()=>{const chosen=[...box.querySelectorAll('.choose-card.selected')].map(b=>b.dataset.id);if(chosen.length!==need){setStatus(`Choose exactly ${need} card${need===1?'':'s'} to bottom.`,true);return;}this.user.bottomPrompt=null;this.sendRole(this.user,{type:'chooseBottomCards',cardIds:chosen});};
    }
    submitAction(action){this.sendRole(this.user,{type:'submitAction',action,interactionEpoch:this.user.interactionEpoch});}
    submitDecision(response){if(!this.user.state)throw new Error('No state.');this.submitAction({type:'SubmitDecision',playerId:this.user.pendingDecision?.playerId||this.user.state.viewingPlayerId,response});}
    submitRoleAction(role,action,info){
      this.sendRole(role,{type:'submitAction',action,interactionEpoch:role.interactionEpoch});
      const t=info?.actionType||action?.type||'';
      if(!/PassPriority|DeclareBlockers/i.test(t)) role.pendingMeaningful={type:t,name:info?.description||''};
    }
    submitRoleDecision(role,response){
      const playerId=role.pendingDecision?.playerId||role.state?.viewingPlayerId;
      if(!playerId)return;
      this.sendRole(role,{type:'submitAction',action:{type:'SubmitDecision',playerId,response},interactionEpoch:role.interactionEpoch});
    }
    autoRole(role){
      clearTimeout(role.autoTimer);role.autoTimer=setTimeout(()=>{
        try{
          if(!role.state||!role.interactionEpoch)return;
          if(role.pendingDecision){const r=decisionResponseSafeForState(role.pendingDecision,'auto',role.state);if(r)this.submitRoleDecision(role,r);return;}
          const me=role.state.viewingPlayerId;if(role.state.priorityPlayerId!==me)return;
          const candidates=(role.legalActions||[]).filter(a=>a.isAffordable!==false);
          const rank=a=>/PlayLand/i.test(a.actionType)?0:/CastSpell/i.test(a.actionType)?1:/ActivateAbility/i.test(a.actionType)&&!a.isManaAbility?2:/DeclareAttackers/i.test(a.actionType)?3:/DeclareBlockers/i.test(a.actionType)?4:/PassPriority/i.test(a.actionType)?99:20;
          candidates.sort((a,b)=>rank(a)-rank(b));
          let picked=null,action=null;
          for(const c of candidates){const completed=completeActionForBot(c,role.state);if(completed){picked=c;action=completed;break;}}
          if(!action){picked=(role.legalActions||[]).find(a=>/PassPriority/i.test(a.actionType));action=picked?clone(picked.action):null;}
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

  async function catalogCoverage() {
    const cfg=providerConfig();
    const allNames=[...new Set(Object.values(FIXTURES).flatMap(f=>{const u=parseDecklist(f.user),a=parseDecklist(f.ai);return [...u.uniqueNames,...a.uniqueNames]}))];
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
    label.innerHTML=`Deck profile<select id="constructedProfile"><option value="mandatory" selected>v0.6.5 mandatory</option><option value="legacy">Page legacy</option></select>`;
    toolbar.prepend(label);
    const apply=()=>{
      const key=$('constructedProfile').value;
      $('constructedUserDeck').value=FIXTURES[key].user;
      $('constructedAiDeck').value=DIMIR_TEMPO_DECK;
      const heading=$('constructedUserDeck')?.closest('.constructed-deckbox')?.querySelector('h3');if(heading)heading.textContent=key==='mandatory'?'You — v0.6.5 mandatory deck':'You — Page, Loose Leaf legacy';
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
