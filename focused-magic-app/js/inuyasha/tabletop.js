(function(){
  'use strict';
  const E=window.InuYashaScoreEngine;
  const DB=window.InuYashaDeckData;
  const BUILD='0.6.9 / InuYasha rules 0.3.0';
  const Setup=window.InuYashaSetupEngine;
  const q=id=>document.getElementById(id);
  if(!E||!DB){console.error('InuYasha tabletop dependencies missing');return;}

  let state=null, selected=null, attackSource=null, attackTarget=null, inspectCard=null;
  const COLORS=Object.values(E.Color);
  function verifiedCharacter(card){return card?.cardType==='Character'&&card.colorValues&&Object.keys(card.colorValues).length>0;}
  function matchingColors(a,d){return verifiedCharacter(a)&&verifiedCharacter(d)?COLORS.filter(c=>Object.hasOwn(a.colorValues,c)&&Object.hasOwn(d.colorValues,c)):[];}
  function canAttackPlayer(){return !!state&&state.ai.play.every(c=>!c.faceUp||c.defeated||['Item','Location','Event'].includes(c.cardType));}
  function attackSelection(){const a=attackSource&&findCard(attackSource),direct=attackTarget==='P2',d=direct?null:attackTarget&&findCard(attackTarget);return {a,d,direct,colors:direct&&verifiedCharacter(a)&&canAttackPlayer()?COLORS.filter(c=>Object.hasOwn(a.colorValues,c)):matchingColors(a,d)};}
  const WINDOW_LABELS={start_of_setup:'Set Up starts',end_of_setup:'Set Up ends',when_character_played:'Character played',when_item_played:'Item played',when_location_played:'Location played',
    start_of_turn:'Turn starts',start_of_draw:'Draw starts',when_you_draw_cards:'Cards drawn',end_of_draw:'Draw ends',when_character_expended:'Expend',when_attacking:'When attacking',when_attacked:'When attacked',when_comparing_color_values:'Compare',when_character_defeated:'Defeated',when_jewel_shard_stolen:'Shard stolen',when_attack_ends:'Attack ends'
  };

  function clone(x){return JSON.parse(JSON.stringify(x));}
  function shuffle(a){a=[...a];for(let i=a.length-1;i>0;i--){const j=Math.floor(Math.random()*(i+1));[a[i],a[j]]=[a[j],a[i]];}return a;}
  function expandDeck(deck,owner){let n=1;const cards=[];for(const row of deck.cards){for(let i=0;i<row.quantity;i++){cards.push({...clone(row),instanceId:`${owner}-${deck.deckId}-${n++}`,owner,zone:'library',ready:true,faceUp:true,defeated:false});}}return cards;}
  function log(text){if(!state)return;state.log.push(text);if(state.log.length>120)state.log.shift();renderLog();}
  function assetId(raw){const m=String(raw||'').match(/api\.ccgtrader\.co\.uk\/_\/assets\/([A-Za-z0-9_-]{8,80})/);return m?m[1]:'';}
  function displayUrl(raw,mode='card-medium'){if(!raw)return '';if(!raw.includes('api.ccgtrader.co.uk/_/assets/'))return raw;const base=raw.split('?')[0];return mode?`${base}?key=${mode}`:base;}
  function imageAttempt(raw,step){const aid=assetId(raw);if(aid&&step===0)return `/inuyasha/catalog-image?asset=${encodeURIComponent(aid)}`;if(step<=(aid?1:0))return displayUrl(raw,'card-medium');if(step<=(aid?2:1))return displayUrl(raw,'directus-medium-contain');return displayUrl(raw,'');}
  function imageHtml(card){if(!card.imageUrl)return `<div class="iy-card-fallback"><b>${esc(card.name)}</b><span>Database card • image not linked</span></div>`;return `<img src="${esc(imageAttempt(card.imageUrl,0))}" alt="${esc(card.name)}" data-raw="${esc(card.imageUrl)}" data-step="0">`;}
  function esc(s){return String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
  function onImageError(img){const raw=img.dataset.raw||'';let step=Number(img.dataset.step||0)+1;img.dataset.step=String(step);const aid=assetId(raw);const max=aid?3:2;if(step<=max){img.src=imageAttempt(raw,step);return;}if(img.id==='iyInspectImage'){img.style.display='none';return;}img.replaceWith(Object.assign(document.createElement('div'),{className:'iy-card-fallback',innerHTML:`<b>${esc(img.alt)}</b><span>Database card • image host unavailable</span>`}));}
  window.InuYashaImageFallback=onImageError;

  function playerDeck(id){return DB.decks.find(d=>String(d.deckId)===String(id))||DB.decks[0];}
  function makeSide(owner,deck){const lib=shuffle(expandDeck(deck,owner));const hand=lib.splice(0,7);hand.forEach(c=>c.zone='hand');return {owner,deck,library:lib,hand,play:[],discard:[],shards:2};}
  function resetGame(){
    const u=playerDeck(q('iyUserDeck').value), a=playerDeck(q('iyAiDeck').value);
    state={user:makeSide('P1',u),ai:makeSide('P2',a),turn:'P1',log:[],lastWindows:[],setup:null};selected=null;attackSource=null;attackTarget=null;inspectCard=null;
    log(`Test match loaded: ${u.name} vs ${a.name}. Seven-card opening hands dealt from the actual database decklists.`);
    log('InuYasha rules v0.3: Draw, ordered Set Up, and preserved combat. Printed effects without handlers still require manual resolution.');
    render();
  }
  function side(owner){return owner==='P1'?state.user:state.ai;}
  function allCards(){return [...state.user.library,...state.user.hand,...state.user.play,...state.user.discard,...state.ai.library,...state.ai.hand,...state.ai.play,...state.ai.discard];}
  function findCard(id){return allCards().find(c=>c.instanceId===id)||null;}
  function moveCard(card,dest){const s=side(card.owner);for(const z of ['library','hand','play','discard']){const i=s[z].findIndex(c=>c.instanceId===card.instanceId);if(i>=0)s[z].splice(i,1);}s[dest].push(card);card.zone=dest;if(dest!=='play'){card.ready=true;card.faceUp=true;card.defeated=false;}render();}
  function startTurnDraw(){
    try{
      if(state.drawStepDone)throw new E.RuleError("This turn already drew. Finish the turn manually, then change the active player.");
      const players={};for(const owner of ['P1','P2']){const p=side(owner);players[owner]=new E.PlayerState(owner,[],{drawPile:p.library,hand:p.hand});}
      const game=new E.GameState({players,turnActivePlayerId:state.turn});game.startTurnAndDraw();
      for(const owner of ['P1','P2']){const p=side(owner);p.library=players[owner].drawPile;p.hand=players[owner].hand;for(const c of p.hand)c.zone='hand';}
      state.lastWindows=game.eventLog.map(e=>e.window);
      state.setup=new Setup.SetupStep({players:{P1:state.user,P2:state.ai},activePlayerId:state.turn,onWindow:event=>{state.lastWindows.push(event.window);log(`ARD ${event.window} — ${event.subjectPlayerId==='P1'?'you':'opponent'}`);}});state.setup.start(true);
      log(`${state.turn==='P1'?'You':'AI'} started the turn: active player drew 3, then opponent drew 3. Set Up started.`);
      state.drawStepDone=true;render();setStatus('Draw complete. Set Up: active player plays first.');
    }catch(e){setStatus(e.message);}
  }
  function draw(owner){const s=side(owner);if(!s.library.length){log(`${owner==='P1'?'You':'AI'} cannot draw: library empty.`);return;}const c=s.library.shift();c.zone='hand';s.hand.push(c);log(`${owner==='P1'?'You':'AI'} drew a card${owner==='P1'?`: ${c.name}`:''}.`);render();}
  async function playSelected(){
    const c=selected&&findCard(selected);if(!c||c.owner!=='P1'||c.zone!=='hand'){setStatus('Select a card in your hand first.');return;}
    try{state.setup?.requireActor('P1');if(!state.setup)throw Error('Start the turn and Draw Step first.');const current=state.setup;const opts=await chooseSetupPlay(c);if(!opts)return;if(state.setup!==current)throw Error('The match changed; choose the card again.');current.play('P1',c.instanceId,opts);log(`Played ${c.name}; paid deck cost ${c.printedDeckCost}. Characters ${current.counts.P1}/2.`);selected=null;render();}catch(e){setStatus(e.message);}
  }
  function chooseSetupPlay(card){
    return new Promise(resolve=>{
      const dialog=document.createElement('dialog');dialog.className='iy-setup-dialog';const form=document.createElement('form');form.innerHTML=`<h3>Play ${esc(card.name)}</h3>${imageHtml(card)}<p>Confirm missing printed details from the card. Resolve other printed effects manually.</p>`;
      const field=(label,input)=>{const row=document.createElement('label');row.textContent=label;row.append(input);form.append(row);return row;};
      const select=(choices,value='')=>{const el=document.createElement('select');for(const [id,text] of choices){const o=document.createElement('option');o.value=id;o.textContent=text;el.append(o);}el.value=value;return el;};
      const knownSetupTypes={"Chokyukai's Tiara":'Item','Following the Scent':'Event',"Shippo's Shroom":'Event',"Shippo's Item Technique":'Event'};const verifiedType=knownSetupTypes[card.name.replace(/’/g,"'")]||card.cardType;const kind=select(['Character','Item','Location','Event'].map(x=>[x,x]),verifiedType||'Character');kind.name='cardType';kind.disabled=!!verifiedType;field('Printed card type',kind);
      const cost=document.createElement('input');cost.name='deckCost';cost.type='number';cost.min=0;cost.step=1;cost.required=true;cost.value=card.printedDeckCost??'';if(Number.isInteger(card.printedDeckCost))cost.readOnly=true;field('Printed deck cost',cost);
      const unique=select([['','Check the printed trait'],['true','Unique'],['false','Not Unique']],typeof card.unique==='boolean'?String(card.unique):'');unique.name='unique';const ur=field('Unique trait',unique);
      const hosts=allCards().filter(c=>c.zone==='play'&&c.cardType==='Character'&&c.faceUp&&!c.defeated);const host=select([['','Choose a Character'],...hosts.map(c=>[c.instanceId,`${c.owner==='P1'?'You':'Opponent'} — ${c.name}`])]);host.name='hostId';const hr=field('Attach / move to',host);
      const ret=select([['','Choose a Character to return'],...hosts.filter(c=>c.owner==='P1').map(c=>[c.instanceId,c.name])]);ret.name='returnId';const rr=field('Return to hand',ret);
      const items=allCards().filter(c=>c.owner==='P1'&&c.zone==='play'&&c.cardType==='Item'&&c.attachedTo);const item=select([['','Choose an attached item'],...items.map(c=>[c.instanceId,c.name])]);item.name='itemId';const ir=field('Move this item',item);
      const searchZone=select([['library','Deck'],['discard','Discard']],'library');searchZone.name='searchZone';const zr=field('Search locations in',searchZone);const results=document.createElement('div');form.append(results);
      const populate=()=>{results.replaceChildren();for(const c of side('P1')[searchZone.value].filter(c=>c.cardType==='Location')){const input=document.createElement('input');input.type='checkbox';input.value=c.instanceId;const label=document.createElement('label');label.append(input,document.createTextNode(c.name));results.append(label);}};searchZone.onchange=populate;populate();
      const normalized=card.name.replace(/’/g,"'");const scent=normalized==='Following the Scent',shroom=normalized==="Shippo's Shroom",technique=normalized==="Shippo's Item Technique";
      const restriction=document.createElement('input');restriction.type='checkbox';const ar=field('I checked the printed attachment / play restrictions',restriction);
      const update=()=>{ur.hidden=!['Character','Item'].includes(kind.value);unique.required=!ur.hidden;hr.hidden=!(kind.value==='Item'||shroom||technique);host.required=!hr.hidden;rr.hidden=!scent;ret.required=scent;ir.hidden=!technique;item.required=technique;zr.hidden=!scent;results.hidden=!scent;ar.hidden=!(kind.value==='Item'||shroom||technique);restriction.required=!ar.hidden;};kind.onchange=update;update();
      const play=document.createElement('button');play.type='submit';play.textContent='Pay cost and play';const cancel=document.createElement('button');cancel.type='button';cancel.textContent='Cancel';cancel.onclick=()=>{dialog.close();dialog.remove();resolve(null);};form.append(play,cancel);form.onsubmit=e=>{e.preventDefault();if(!form.reportValidity())return;const opts={cardType:kind.value,deckCost:Number(cost.value),unique:unique.value?unique.value==='true':undefined,hostId:host.value,returnId:ret.value,itemId:item.value,searchZone:searchZone.value,searchIds:[...results.querySelectorAll('input:checked')].map(i=>i.value),shuffle};dialog.close();dialog.remove();resolve(opts);};dialog.oncancel=e=>{e.preventDefault();cancel.click();};dialog.append(form);document.body.append(dialog);dialog.showModal();
    });
  }
  function finishSetup(){try{if(!state.setup)throw Error('Start the turn first.');const actor=state.setup.actor;state.setup.finish(actor);log(`${actor==='P1'?'You':'Opponent'} finished Set Up.`);render();}catch(e){setStatus(e.message);}}
  function discardSelected(){if(!selected)return;const c=findCard(selected);if(!c||c.owner!=='P1')return;moveCard(c,'discard');log(`You moved ${c.name} to discard.`);selected=null;render();}
  function aiPlay(){try{if(!state.setup)throw Error('Start the turn first.');state.setup.requireActor('P2');const cards=state.ai.hand.filter(c=>c.cardType==='Character'&&Number.isInteger(c.printedDeckCost)&&!state.setup.controlled('P2').some(x=>x.name.split(/[,;]/)[0]===c.name.split(/[,;]/)[0]));for(const c of cards){try{state.setup.play('P2',c.instanceId,{unique:true});log(`Opponent played ${c.name}; paid ${c.printedDeckCost}.`);render();return;}catch(_){}}setStatus('Opponent has no verified legal Character play. Finish its Set Up block.');}catch(e){setStatus(e.message);}}
  function readyUser(){for(const c of state.user.play){if(!c.defeated)c.ready=true;}log('You readied your non-defeated characters for tabletop testing.');render();}

  function cardNode(card,zone,hidden=false){
    if(hidden){const b=document.createElement('div');b.className='iy-card-back';b.title='Hidden opponent card';return b;}
    const wrap=document.createElement('div');wrap.className='iy-card';wrap.dataset.cardId=card.instanceId;
    if(selected===card.instanceId)wrap.classList.add('selected');if(attackSource===card.instanceId)wrap.classList.add('attack-source');if(attackTarget===card.instanceId)wrap.classList.add('attack-target');if(!card.ready)wrap.classList.add('expended');if(card.defeated)wrap.classList.add('defeated');
    const face=card.faceUp===false?'<div class="iy-card-fallback"><b>Defeated</b><span>Facedown + expended</span></div>':imageHtml(card);wrap.innerHTML=`<div class="iy-card-face">${face}${card.cardType?`<span class="iy-card-type">${esc(card.cardType)}</span>`:''}${!card.ready?'<span class="iy-card-badge">EXPENDED</span>':''}</div><div class="iy-card-name">${esc(card.name)}</div>`;
    const img=wrap.querySelector('img');if(img)img.onerror=()=>onImageError(img);
    wrap.addEventListener('click',()=>{
      selected=card.instanceId;inspectCard=card.instanceId;
      if(zone==='play'){
        if(card.owner==='P1' && card.ready && card.faceUp && !card.defeated && verifiedCharacter(card)){attackSource=card.instanceId;}
        if(card.owner==='P2' && card.faceUp && !card.defeated && verifiedCharacter(card)){attackTarget=card.instanceId;}
      }
      render();
    });
    wrap.addEventListener('dblclick',()=>{if(card.owner==='P1'&&zone==='hand'){selected=card.instanceId;playSelected();}});
    return wrap;
  }

  function renderZone(id,cards,zone,hidden=false){const el=q(id);el.innerHTML='';if(hidden){for(const _ of cards)el.appendChild(cardNode({},zone,true));return;}for(const c of cards)el.appendChild(cardNode(c,zone,false));}
  function renderShards(id,count){const el=q(id);el.innerHTML='';for(let i=0;i<count;i++){const s=document.createElement('span');s.className='iy-shard';s.title='Opaque Jewel Shard';el.appendChild(s);}q(id+'Count').textContent=String(count);}
  function renderLog(){if(!q('iyLog')||!state)return;q('iyLog').textContent=state.log.slice().reverse().join('\n');}
  function renderWindows(){const el=q('iyWindows');el.innerHTML='';for(const w of (state.lastWindows.length?state.lastWindows.slice(-4):['when_character_expended','when_attacking','when_attacked','when_comparing_color_values','when_character_defeated','when_jewel_shard_stolen','when_attack_ends'])){const s=document.createElement('span');s.className='iy-window'+(state.lastWindows.includes(w)?' active':'');s.textContent=WINDOW_LABELS[w]||w.replaceAll('_',' ');el.appendChild(s);}}
  function renderInspector(){const box=q('iyInspector');const c=inspectCard?findCard(inspectCard):null;if(!c){box.classList.remove('open');return;}box.classList.add('open');q('iyInspectImage').src=c.imageUrl?imageAttempt(c.imageUrl,0):'';q('iyInspectImage').style.display=c.imageUrl?'block':'none';q('iyInspectImage').dataset.raw=c.imageUrl||'';q('iyInspectImage').dataset.step='0';q('iyInspectName').textContent=c.name;q('iyInspectMeta').textContent=[c.cardType||'Type not normalized in database',c.rarity||'rarity not normalized',c.evidenceStatus||''].filter(Boolean).join(' • ');q('iyInspectZone').textContent=`${c.owner==='P1'?'Your':'Opponent'} ${c.zone}`;}
  function renderAttackSummary(){const {a,d,direct,colors}=attackSelection();q('iyAttackSummary').innerHTML=a&&(d||direct)?`<b>${esc(a.name)}</b> → <b>${direct?'Opponent':esc(d.name)}</b><div class="sub">${colors.length?(direct?'The opponent controls no characters.':`Matching printed colors: ${colors.join(', ')}.`):'No matching printed color. This character attack is illegal; a mismatch does not permit a direct attack.'}</div>`:'Select your ready character and an opposing character, or attack the opponent when they control no characters.';q('iyResolveAttack').disabled=!(a&&colors.length&&a.ready&&!a.defeated&&(direct||d&&!d.defeated));q('iyDirectAttack').disabled=!(verifiedCharacter(a)&&a.ready&&a.faceUp&&!a.defeated&&canAttackPlayer());}
  function render(){if(!state)return;
    q('iyUserName').textContent=state.user.deck.name;q('iyAiName').textContent=state.ai.deck.name;
    q('iyUserLib').textContent=state.user.library.length;q('iyUserHandCount').textContent=state.user.hand.length;q('iyUserDiscard').textContent=state.user.discard.length;
    q('iyAiLib').textContent=state.ai.library.length;q('iyAiHandCount').textContent=state.ai.hand.length;q('iyAiDiscard').textContent=state.ai.discard.length;
    renderShards('iyUserShards',state.user.shards);renderShards('iyAiShards',state.ai.shards);
    renderZone('iyUserPlay',state.user.play,'play');renderZone('iyAiPlayZone',state.ai.play,'play');renderZone('iyUserHand',state.user.hand,'hand');renderZone('iyAiHand',state.ai.hand,'hand',true);
    renderAttackSummary();renderWindows();renderInspector();renderLog();const setup=state.setup;q('iyFinishSetup').disabled=!setup||setup.phase!=='PLAY';q('iyFinishSetup').textContent=setup?.actor==='P2'?'Finish opponent Set Up':'Finish your Set Up';q('iyPlaySelected').disabled=setup?.phase!=='PLAY'||setup.actor!=='P1';q('iyAiPlayButton').disabled=setup?.phase!=='PLAY'||setup.actor!=='P2';q('iyRulesVersion').textContent='InuYasha v0.3.0';for(const id of ['iyDraw','iyAiDraw','iyDiscardSelected','iyReady'])q(id).disabled=setup?.phase!=='BATTLE_START';setStatus(setup?`${setup.phase==='BATTLE_START'?'Battle Start':setup.actor==='P1'?'Your Set Up':'Opponent Set Up'} · Characters ${setup.counts.P1}/2 vs ${setup.counts.P2}/2`:'Start Turn to draw and begin Set Up.');
  }
  function setStatus(s){q('iyStatus').textContent=s;}

  function readRequiredCombatValue(id){
    const el=q(id);
    if(!el)return null;
    const raw=String(el.value??'').trim();
    // Fail closed before Number() so blank/whitespace can never become zero.
    if(!/^\d+$/.test(raw))return null;
    if(el.validity && !el.validity.valid)return null;
    const value=Number(raw);
    return Number.isSafeInteger(value)&&value>=0?value:null;
  }
  function syncAttackInputGate(){
    const av=readRequiredCombatValue('iyAttackValue'),dv=readRequiredCombatValue('iyDefendValue');
    const {a,d,direct,colors}=attackSelection(),color=q('iyAttackColor')?.value;
    const valid=!!(a&&a.zone==='play'&&a.owner==='P1'&&a.ready&&a.faceUp&&!a.defeated&&colors.includes(color)&&av===a.colorValues[color]&&(direct?canAttackPlayer():d&&d.zone==='play'&&d.owner==='P2'&&d.faceUp&&!d.defeated&&dv===d.colorValues[color]));
    const confirm=q('iyAttackConfirm');
    if(confirm){confirm.disabled=!valid;confirm.setAttribute('aria-disabled',String(!valid));}
    const err=q('iyAttackError');
    if(err)err.textContent=valid?'Printed values only. Card-text effects and modifiers are not automated.':'Choose a matching printed color on two eligible characters.';
    return valid;
  }
  function openAttackDialog(){if(state.setup?.phase!=='BATTLE_START'){setStatus('Finish Set Up before Battle.');return;}const source=attackSource&&findCard(attackSource);if(source&&state.setup.attachments(source).some(c=>c.name.replace(/’/g,"'")=="Chokyukai's Tiara")&&!state.setup.controlled('P1').some(c=>c.name.split(/[,;]/)[0].toLowerCase()==='chokyukai')){setStatus('This Character cannot attack without Chokyukai.');return;}const {a,d,direct,colors}=attackSelection();if(!a||!colors.length)return; q('iyAttackAttacker').textContent=a.name;q('iyAttackDefender').textContent=direct?'Opponent':d.name;q('iyDefendValue').closest('label').style.display=direct?'none':'';q('iyAttackNote').textContent=direct?'The opponent controls no characters. Choose any printed color on your ready character to attack directly. Card effects still require manual rules handling.':'Choose a color printed on both characters. Values come from the linked card printing and cannot be edited. This tabletop compares printed values; card effects and modifiers still require manual rules handling.';q('iyAttackModal').classList.add('open');q('iyAttackModal').setAttribute('aria-hidden','false');setAttackColor(colors[0]);}
  function setAttackColor(c){const {a,d,direct,colors}=attackSelection(),chosen=colors.includes(c)?c:'';q('iyAttackColor').value=chosen;q('iyAttackValue').value=chosen?a.colorValues[chosen]:'';q('iyDefendValue').value=chosen&&!direct?d.colorValues[chosen]:'';document.querySelectorAll('[data-iy-color]').forEach(b=>{b.disabled=!colors.includes(b.dataset.iyColor);b.classList.toggle('active',b.dataset.iyColor===chosen);});syncAttackInputGate();}
  function resolveAttack(event){
    event?.preventDefault?.();
    if(!syncAttackInputGate()){event?.stopImmediatePropagation?.();return false;}
    const {a,d,direct}=attackSelection();if(!a||!d&&!direct)return false;
    const av=readRequiredCombatValue('iyAttackValue'),dv=readRequiredCombatValue('iyDefendValue'),color=q('iyAttackColor').value;
    if(av===null||!direct&&dv===null){syncAttackInputGate();return false;}
    try{
      const cvA={...a.colorValues};
      const attachedA=allCards().filter(c=>c.attachedTo===a.instanceId),attachedD=d?allCards().filter(c=>c.attachedTo===d.instanceId):[];
      const ac=new E.CharacterState({attachments:attachedA.map(c=>new E.AttachmentState(c.name,{faceUp:c.faceUp,expended:!c.ready})),title:a.name,controllerId:'P1',colorValues:cvA,ready:a.ready,faceUp:a.faceUp,inPlay:true,defeated:a.defeated});
      const dc=direct?null:new E.CharacterState({attachments:attachedD.map(c=>new E.AttachmentState(c.name,{faceUp:c.faceUp,expended:!c.ready})),title:d.name,controllerId:'P2',colorValues:d.colorValues,ready:d.ready,faceUp:d.faceUp,inPlay:true,defeated:d.defeated});
      const p1=new E.PlayerState('P1',Array.from({length:state.user.shards},(_,i)=>new E.JewelShard(`P1-${i}`)));const p2=new E.PlayerState('P2',Array.from({length:state.ai.shards},(_,i)=>new E.JewelShard(`P2-${i}`)));
      const opponents=direct?state.ai.play.filter(c=>c.cardType==='Character').map(c=>new E.CharacterState({title:c.name,controllerId:'P2',colorValues:c.colorValues||{},ready:c.ready,faceUp:c.faceUp,inPlay:true,defeated:c.defeated})):[dc];
      const game=new E.GameState({players:{P1:p1,P2:p2},turnActivePlayerId:state.turn,characters:[ac,...opponents]});const ctx=direct?game.attackPlayer(ac,'P2',color):game.attack(ac,dc,color);
      a.ready=ac.ready;a.defeated=ac.defeated;a.faceUp=ac.faceUp;if(d){d.ready=dc.ready;d.defeated=dc.defeated;d.faceUp=dc.faceUp;}for(const [cards,model] of [[attachedA,ac],[attachedD,dc]])if(model)cards.forEach((c,i)=>{c.faceUp=model.attachments[i].faceUp;c.ready=!model.attachments[i].expended;});state.user.shards=game.player('P1').jewelShards.length;state.ai.shards=game.player('P2').jewelShards.length;
      state.lastWindows=game.eventLog.map(x=>x.window);log(direct?`${a.name} attacked the opponent directly using ${color.toUpperCase()}.${ctx.shardStolen?' One opaque Jewel Shard stolen.':''}`:`${a.name} attacked ${d.name} using ${color.toUpperCase()}: ${ctx.attackerFinalValue} vs ${ctx.defenderFinalValue}. ${ctx.defenderDefeatedByAttack?'Defender defeated.':'Defender survives.'}${ctx.shardStolen?' One opaque Jewel Shard stolen.':''}`);
      for(const ev of game.eventLog)log(`ARD window — ${WINDOW_LABELS[ev.window]}${ev.detail?` (${ev.detail})`:''}`);
      q('iyAttackModal').classList.remove('open');q('iyAttackModal').setAttribute('aria-hidden','true');attackSource=null;attackTarget=null;selected=null;render();return true;
    }catch(e){q('iyAttackError').textContent=e.message||String(e);return false;}
  }
  function attackGuardSelfTest(){
    const a=q('iyAttackValue'),d=q('iyDefendValue');
    if(!a||!d)return {ok:false,reason:'inputs missing',build:BUILD};
    const av=a.value,dv=d.value;
    a.value='';d.value='';
    const blankRejected=readRequiredCombatValue('iyAttackValue')===null&&readRequiredCombatValue('iyDefendValue')===null;
    a.value='1';d.value='6';
    const numericAccepted=readRequiredCombatValue('iyAttackValue')===1&&readRequiredCombatValue('iyDefendValue')===6;
    a.value=av;d.value=dv;syncAttackInputGate();
    return {ok:blankRejected&&numericAccepted,blankRejected,numericAccepted,build:BUILD};
  }

  function populateDeckSelectors(){for(const id of ['iyUserDeck','iyAiDeck']){const s=q(id);s.innerHTML='';for(const d of DB.decks){const o=document.createElement('option');o.value=d.deckId;o.textContent=`${d.name} (${d.cardCount})`;s.appendChild(o);}}const user=DB.decks.find(d=>Number(d.deckId)===6)||DB.decks[0];const ai=DB.decks.find(d=>Number(d.deckId)===5)||DB.decks.find(d=>d.deckId!==user.deckId)||DB.decks[0];q('iyUserDeck').value=String(user.deckId);q('iyAiDeck').value=String(ai.deckId);}
  function open(){q('iyGameModal').classList.add('open');q('iyGameModal').setAttribute('aria-hidden','false');if(!state)resetGame();else render();}
  function close(){q('iyGameModal').classList.remove('open');q('iyGameModal').setAttribute('aria-hidden','true');}
  function install(){
    populateDeckSelectors();q('iyGameClose').onclick=close;q('iyReset').onclick=resetGame;q('iyUserDeck').onchange=resetGame;q('iyAiDeck').onchange=resetGame;q('iyFinishSetup').onclick=finishSetup;q('iyNextTurn').onclick=()=>{if(state.setup?.phase!=='BATTLE_START'){setStatus('Finish both Set Up blocks before changing turns.');return;}state.setup=null;state.turn=state.turn==='P1'?'P2':'P1';state.drawStepDone=false;log('Active player changed manually. Other turn steps are not automated.');render();};q('iyTurnDraw').onclick=startTurnDraw;q('iyDraw').onclick=()=>draw('P1');q('iyPlaySelected').onclick=playSelected;q('iyDiscardSelected').onclick=discardSelected;q('iyAiDraw').onclick=()=>draw('P2');q('iyAiPlayButton').onclick=aiPlay;q('iyReady').onclick=readyUser;q('iyResolveAttack').onclick=openAttackDialog;q('iyDirectAttack').onclick=()=>{if(canAttackPlayer()){attackTarget='P2';openAttackDialog();}};q('iyClearAttack').onclick=()=>{attackSource=null;attackTarget=null;render();};q('iyInspectClose').onclick=()=>{inspectCard=null;renderInspector();};q('iyAttackCancel').onclick=()=>{q('iyAttackModal').classList.remove('open');q('iyAttackModal').setAttribute('aria-hidden','true');};q('iyAttackConfirm').onclick=null;q('iyAttackConfirm').addEventListener('click',e=>{if(!syncAttackInputGate()){e.preventDefault();e.stopImmediatePropagation();return false;}return resolveAttack(e);},{capture:true});for(const id of ['iyAttackValue','iyDefendValue'])for(const evt of ['input','change','keyup','blur'])q(id).addEventListener(evt,syncAttackInputGate);document.querySelectorAll('[data-iy-color]').forEach(b=>b.onclick=()=>setAttackColor(b.dataset.iyColor));q('iyInspectImage').onerror=function(){onImageError(this)};q('iyAttackConfirm').disabled=true;q('iyAttackConfirm').setAttribute('aria-disabled','true');window.addEventListener('pageshow',syncAttackInputGate);const diag=attackGuardSelfTest();if(!diag.ok)console.error('InuYasha attack guard self-test failed',diag);
    q('launchInuyasha')?.addEventListener('click',open);q('inuyashaBtn')?.addEventListener('click',open);
  }
  window.InuYashaTabletop={VERSION:BUILD,RULES_VERSION:'0.3.0',open,close,reset:resetGame,diagnostics:attackGuardSelfTest,get state(){return state;}};
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',install);else install();
})();
