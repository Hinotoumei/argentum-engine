'use strict';
const assert=require('node:assert/strict');
const {test}=require('node:test');
const fs=require('node:fs');
const vm=require('node:vm');
function harness(answers=[]){
  const elements=Object.fromEntries(['argentumModal','argentumStatus','argentumLog'].map(id=>[id,{textContent:'',classList:{toggle(){}}}]));
  const prompts=[],sent=[];
  const window={addEventListener(){},__ARGENTUM_TEST_CHOICES:answers};
  const context={window,document:{body:{contains(){return false}},getElementById:id=>elements[id]||null,createElement:()=>({dataset:{},classList:{add(){},toggle(){}},innerHTML:'',querySelector(){return null}})},console,setTimeout,clearTimeout};
  vm.createContext(context);
  const source=fs.readFileSync(process.env.ARGENTUM_TEST_SOURCE||require.resolve('../js/provider/argentum.js'),'utf8').replace('  window.FocusedMagicArgentum={','  window.__test={decisionResponseSafeForState,decisionResponse,providerTableCard,playerEffectsSummary,submitLegalAction,completeActionForBot,setActive(value){active=value;}};\n  window.FocusedMagicArgentum={');
  vm.runInContext(source,context);
  const state={viewingPlayerId:'me',players:[{playerId:'me',life:20},{playerId:'opp',life:20}],cards:{bear:{name:'Bear',controllerId:'opp'},spell:{name:'Spell',controllerId:'opp'}},zones:[{zoneId:{zoneType:'BATTLEFIELD'},cardIds:['bear']},{zoneId:{zoneType:'STACK'},cardIds:['spell']}]};
  window.__test.setActive({interactionEpoch:{id:'epoch'},state,submitAction:action=>sent.push(JSON.parse(JSON.stringify(action)))});
  return {api:window.__test,state,prompts,sent,elements};
}
const target=(ids,min=1,max=1,extra={})=>({description:'Choose target',validTargets:ids,minTargets:min,maxTargets:max,...extra});
const modal=modes=>({action:{type:'CastSpell',cardId:'command',playerId:'me'},modalEnumeration:{minChooseCount:2,chooseCount:2,allowRepeat:false,modes,unavailableIndices:[]},requiresTargets:true});
const mode=(index,reqs=[])=>({index,description:`Mode ${index}`,available:true,targetRequirements:reqs});

test('tabletop preserves an unlimited hand and its server-provided emblem text',()=>{
 const h=harness();
 assert.equal(h.api.playerEffectsSummary({maxHandSize:null,activeEffects:[{name:'Tamiyo Emblem',description:'You have no maximum hand size.'}]}),'No maximum hand size • Tamiyo Emblem: You have no maximum hand size.');
});
test('tabletop displays a later server limit even while an emblem remains',()=>{
 const h=harness();
 assert.equal(h.api.playerEffectsSummary({maxHandSize:0,activeEffects:[{name:'Emblem'}]}),'Hand limit: 0 • Emblem');
});

test('human attack selection preserves each provider-advertised attack target',async()=>{
 const h=harness(['1,2','1','2']);
 await h.api.submitLegalAction({actionType:'DeclareAttackers',action:{type:'DeclareAttackers',playerId:'me',attackers:{}},validAttackers:['bear','spell'],validAttackTargets:['opp','bear']});
 assert.deepEqual(h.sent[0].attackers,{bear:'opp',spell:'bear'});
});
test('human can explicitly declare no attackers',async()=>{
 const h=harness(['']);
 await h.api.submitLegalAction({actionType:'DeclareAttackers',action:{type:'DeclareAttackers',playerId:'me',attackers:{}},validAttackers:['bear'],validAttackTargets:['opp']});
 assert.deepEqual(h.sent[0].attackers,{});
});
test('omitting a provider-mandatory attacker submits nothing',async()=>{
 const h=harness(['2']);
 await h.api.submitLegalAction({actionType:'DeclareAttackers',action:{type:'DeclareAttackers',playerId:'me',attackers:{}},validAttackers:['bear','spell'],mandatoryAttackers:['bear'],validAttackTargets:['opp']});
 assert.equal(h.sent.length,0);
});
test('cancelling the attack-target chooser submits nothing',async()=>{
 const h=harness(['1',null]);
 await h.api.submitLegalAction({actionType:'DeclareAttackers',action:{type:'DeclareAttackers',playerId:'me',attackers:{}},validAttackers:['bear'],validAttackTargets:['opp']});
 assert.equal(h.sent.length,0);
});
test('required-blocks continuation preserves the exact provider assignments',async()=>{
 const h=harness();
 await h.api.submitLegalAction({actionType:'DeclareBlockers',action:{type:'DeclareBlockers',playerId:'me',blockers:{}},mandatoryBlockerAssignments:{bear:['spell']}});
 assert.deepEqual(h.sent[0].blockers,{bear:['spell']});
});
test('human optional blocks preserve provider maximum block count and exact assignment',async()=>{
 const h=harness(['1','1,2']);h.state.combat={attackers:[{creatureId:'a'},{creatureId:'b'}]};
 await h.api.submitLegalAction({actionType:'DeclareBlockers',action:{type:'DeclareBlockers',playerId:'me',blockers:{}},validBlockers:['bear'],blockerMaxBlockCounts:{bear:2}});
 assert.deepEqual(h.sent[0].blockers,{bear:['a','b']});
});
test('declining optional blockers still preserves mandatory blocks',async()=>{
 const h=harness(['']);
 await h.api.submitLegalAction({actionType:'DeclareBlockers',action:{type:'DeclareBlockers',playerId:'me',blockers:{}},validBlockers:['bear','spell'],mandatoryBlockerAssignments:{bear:['a']}});
 assert.deepEqual(h.sent[0].blockers,{bear:['a']});
});
test('cancelling a blocker target picker submits nothing',async()=>{
 const h=harness(['1',null]);h.state.combat={attackers:[{creatureId:'a'}]};
 await h.api.submitLegalAction({actionType:'DeclareBlockers',action:{type:'DeclareBlockers',playerId:'me',blockers:{}},validBlockers:['bear']});
 assert.equal(h.sent.length,0);
});
test('human command submits player and stack targets in selected-mode order',async()=>{
 const h=harness(['2,1','1','1']);
 await h.api.submitLegalAction(modal([mode(0,[target(['spell'])]),mode(1,[target(['opp'])])]));
 assert.deepEqual(h.sent[0].chosenModes,[1,0]);
 assert.deepEqual(h.sent[0].modeTargetsOrdered,[[{type:'Player',playerId:'opp'}],[{type:'Spell',spellEntityId:'spell'}]]);
 assert.deepEqual(h.sent[0].targets,h.sent[0].modeTargetsOrdered.flat());
});
test('a non-targeted mode retains its empty ordinal group',async()=>{
 const h=harness(['1,2','1']);
 await h.api.submitLegalAction(modal([mode(0),mode(1,[target(['bear'])])]));
 assert.deepEqual(h.sent[0].modeTargetsOrdered,[[],[{type:'Permanent',entityId:'bear'}]]);
});
test('optional empty target requirements need no prompt',async()=>{
 const h=harness(['1,2']);
 await h.api.submitLegalAction(modal([mode(0,[target([],0,0)]),mode(1)]));
 assert.deepEqual(h.sent[0].modeTargetsOrdered,[[],[]]);
});
test('target cancellation submits nothing',async()=>{
 const h=harness(['1,2',null]);
 await h.api.submitLegalAction(modal([mode(0,[target(['bear'])]),mode(1)]));
 assert.equal(h.sent.length,0);
});
test('unavailable mode choices submit nothing',async()=>{
 const h=harness(['2']);const info=modal([mode(0),{...mode(1),available:false}]);
 await h.api.submitLegalAction(info);assert.equal(h.sent.length,0);
});
test('bot submits ordered modal targets instead of an incomplete cast',()=>{
 const h=harness();const info=modal([mode(0,[target(['opp'])]),mode(1,[target(['bear'])])]);
 const a=JSON.parse(JSON.stringify(h.api.completeActionForBot(info,h.state)));
 assert.deepEqual(a.modeTargetsOrdered,[[{type:'Player',playerId:'opp'}],[{type:'Permanent',entityId:'bear'}]]);
});
test('repeatable modes keep two target groups even with one available mode',()=>{
 const h=harness();const info=modal([mode(0,[target(['bear'])])]);info.modalEnumeration.allowRepeat=true;
 const a=JSON.parse(JSON.stringify(h.api.completeActionForBot(info,h.state)));
 assert.deepEqual(a.chosenModes,[0,0]);assert.equal(a.modeTargetsOrdered.length,2);
});
test('bot rejects a mode with too few legal targets',()=>{
 const h=harness();assert.equal(h.api.completeActionForBot(modal([mode(0,[target([])]),mode(1)]),h.state),null);
});
test('distinct-target metadata excludes earlier picks',()=>{
 const h=harness();const info=modal([mode(0,[target(['opp','bear'])]),mode(1,[target(['opp','bear'],1,1,{mustDifferFromEarlier:true})])]);
 const a=JSON.parse(JSON.stringify(h.api.completeActionForBot(info,h.state)));
 assert.deepEqual(a.modeTargetsOrdered,[[{type:'Player',playerId:'opp'}],[{type:'Permanent',entityId:'bear'}]]);
});
for(const amount of [0,2,20])test(`human pays exactly ${amount} life through the numeric chooser`,async()=>{
 const h=harness([String(amount)]);await h.api.submitLegalAction({action:{type:'CastSpell'},additionalCostInfo:{costType:'PayXLife',payXLifeMaxX:20}});
 assert.equal(h.sent[0].additionalCostPayment.payXLifeAmount,amount);
 assert.equal(h.sent[0].xValue,undefined);
});
for(const answer of ['21','-1','2.5','no',''])test(`invalid life choice ${JSON.stringify(answer)} submits nothing`,async()=>{
 const h=harness([answer]);await h.api.submitLegalAction({action:{type:'CastSpell'},additionalCostInfo:{costType:'PayXLife',payXLifeMaxX:20}});
 assert.equal(h.sent.length,0);
});
test('bot uses the advertised paid-life contract with the zero policy',()=>{
 const h=harness();const a=h.api.completeActionForBot({action:{type:'CastSpell'},additionalCostInfo:{costType:'PayXLife',payXLifeMaxX:20}},h.state);
 assert.equal(a.additionalCostPayment.payXLifeAmount,0);
});
test('a replicate offer keeps its server-supplied declaration and count',()=>{
 const h=harness();const a=h.api.completeActionForBot({action:{type:'CastSpell',declaredCostSlot:'replicated',declaredCostTimes:2}},h.state);
 assert.equal(a.declaredCostSlot,'replicated');assert.equal(a.declaredCostTimes,2);
});

test('human repeatable mode captures both target groups with one available mode',async()=>{
 const h=harness(['1,1','1','1']);const info=modal([mode(0,[target(['bear'])])]);info.modalEnumeration.allowRepeat=true;
 await h.api.submitLegalAction(info);assert.deepEqual(h.sent[0].chosenModes,[0,0]);assert.equal(h.sent[0].modeTargetsOrdered.length,2);
});

test('tabletop shows provider-projected toughness after a reduction',()=>{
 const h=harness();h.state.cards.bear.power=1;h.state.cards.bear.toughness=1;
 const card=h.api.providerTableCard(h.state,'bear','BATTLEFIELD');assert.match(card.innerHTML,/argentum-tabletop-stat">1\/1/);
});
for(const x of [0,2])test(`tabletop stack badge preserves X=${x}`,()=>{
 const h=harness();h.state.cards.spell.chosenX=x;h.state.cards.spell.stackText='Provider effect text';
 const card=h.api.providerTableCard(h.state,'spell','STACK');assert.match(card.innerHTML,new RegExp(`argentum-tabletop-x">X=${x}`));assert.equal(card.title,'Provider effect text');
});
test('tabletop does not invent a chosen X when the provider omits it',()=>{
 const h=harness();const card=h.api.providerTableCard(h.state,'spell','STACK');assert.doesNotMatch(card.innerHTML,/argentum-tabletop-x/);
});

test('matching card selections show zones and owners and preserve the selected copy',async()=>{
 const h=harness(['2']);h.state.players[1].name='Opponent';
 h.state.cards.gy={name:'Grizzly Bears',ownerId:'opp',zone:{zoneType:'GRAVEYARD'}};
 h.state.cards.hand={name:'Grizzly Bears',ownerId:'opp',zone:{zoneType:'HAND'}};
 h.state.cards.library={name:'Grizzly Bears',ownerId:'opp',zone:{zoneType:'LIBRARY'}};
 const response=await h.api.decisionResponse({type:'SelectCardsDecision',id:'search',options:['gy','hand','library'],minSelections:0,maxSelections:3});
 assert.deepEqual(JSON.parse(JSON.stringify(response.selectedCards)),['hand']);
});
test('a hidden-zone option uses provider decision card information when the ordinary state omits it',async()=>{
 const h=harness(['1']);
 const response=await h.api.decisionResponse({type:'SelectCardsDecision',id:'search',options:['hidden'],minSelections:0,maxSelections:1,cardInfo:{hidden:{name:'Grizzly Bears'}}});
 assert.equal(response.selectedCards[0],'hidden');
});
test('optional card selections preserve an empty choice',async()=>{
 const h=harness(['']);const response=await h.api.decisionResponse({type:'SelectCardsDecision',id:'search',options:['bear'],minSelections:0,maxSelections:1});
 assert.equal(response.selectedCards.length,0);
});
test('cancelling a card selection returns no choice',async()=>{
 const h=harness([null]);await assert.rejects(()=>h.api.decisionResponse({type:'SelectCardsDecision',id:'search',options:['bear'],minSelections:0,maxSelections:1}),/Action cancelled/);
 assert.equal(h.sent.length,0);
});

test('tabletop displays server-provided counter types and counts without deriving them from chosen X',()=>{
 const h=harness();h.state.cards.bear.chosenX=2;h.state.cards.bear.counters={CHARGE:1,PLUS_ONE_PLUS_ONE:3,LOYALTY:0};
 const card=h.api.providerTableCard(h.state,'bear','BATTLEFIELD');
 assert.match(card.innerHTML,/argentum-tabletop-counter">charge: 1/);
 assert.match(card.innerHTML,/plus one plus one: 3/);assert.doesNotMatch(card.innerHTML,/loyalty: 0|charge: 2/);
});


test('a target chooser cannot submit into a replacement session',async()=>{
 const h=harness(['1']);
 const pending=h.api.submitLegalAction({action:{type:'CastSpell',cardId:'command',playerId:'me'},requiresTargets:true,targetRequirements:[target(['bear'])]});
 h.api.setActive({interactionEpoch:{id:'replacement'},state:h.state,submitAction:action=>h.sent.push(action)});
 await pending;
 assert.equal(h.sent.length,0);
 assert.match(h.elements.argentumStatus.textContent,/game changed during selection/);
});

for(const value of ['0','3','5'])test(`X chooser submits integer ${value} within advertised bounds`,async()=>{
 const h=harness([value]);await h.api.submitLegalAction({action:{type:'CastSpell',cardId:'spell',playerId:'me'},hasXCost:true,minX:0,maxAffordableX:5});
 assert.equal(h.sent[0].xValue,Number(value));
});
for(const value of ['', '1.5', '-1', '6', 'bad', null])test(`X chooser rejects invalid or cancelled value ${value}`,async()=>{
 const h=harness([value]);await h.api.submitLegalAction({action:{type:'CastSpell',cardId:'spell',playerId:'me'},hasXCost:true,minX:0,maxAffordableX:5});
 assert.equal(h.sent.length,0);
});
test('X chooser honors an advertised nonzero minimum',async()=>{
 const h=harness(['1']);await h.api.submitLegalAction({action:{type:'CastSpell',cardId:'spell',playerId:'me'},hasXCost:true,minX:2,maxAffordableX:5});assert.equal(h.sent.length,0);
});

test('an X chooser cannot submit into a replacement session',async()=>{
 const h=harness(['3']);
 const pending=h.api.submitLegalAction({action:{type:'CastSpell',cardId:'spell',playerId:'me'},hasXCost:true,minX:0,maxAffordableX:5});
 h.api.setActive({interactionEpoch:{id:'replacement'},state:h.state,submitAction:action=>h.sent.push(action)});
 await pending;assert.equal(h.sent.length,0);assert.match(h.elements.argentumStatus.textContent,/game changed during selection/);
});

test('Bowmasters target decisions use the native legalTargets map and requirement index',async()=>{
 const h=harness(['1']);const d={type:'ChooseTargetsDecision',id:'bowmasters',targetRequirements:[{index:2,minTargets:1,maxTargets:1,description:'Any target'}],legalTargets:{2:['opp','bear']}};
 const response=h.api.decisionResponseSafeForState(d,'auto',h.state);assert.deepEqual(JSON.parse(JSON.stringify(response)),{type:'TargetsResponse',decisionId:'bowmasters',selectedTargets:{2:['opp']}});
 const human=await h.api.decisionResponse(d,'human');assert.deepEqual(JSON.parse(JSON.stringify(human.selectedTargets)),{2:['opp']});
});
test('automatic target choice refuses an impossible native requirement',()=>{const h=harness();assert.equal(h.api.decisionResponseSafeForState({type:'ChooseTargetsDecision',id:'empty',targetRequirements:[{index:0,minTargets:1}],legalTargets:{0:[]}},'auto',h.state),null);});

test('human fetch activation pays the provider exact source sacrifice',async()=>{
 const h=harness();const info={action:{type:'ActivateAbility',sourceId:'tarn',playerId:'me',abilityId:'fetch',costPayment:{existing:'preserved'}},additionalCostInfo:{costType:'SacrificeSelf',validSacrificeTargets:['tarn'],sacrificeCount:1}};
 await h.api.submitLegalAction(info);assert.deepEqual(h.sent[0].costPayment,{existing:'preserved',sacrificedPermanents:['tarn']});
});
test('AI fetch activation pays source sacrifice while ambiguous sacrifice stays blocked',()=>{
 const h=harness();const info={action:{type:'ActivateAbility',sourceId:'tarn',playerId:'me',abilityId:'fetch'},additionalCostInfo:{costType:'SacrificeSelf',validSacrificeTargets:['tarn'],sacrificeCount:1}};
 assert.deepEqual(JSON.parse(JSON.stringify(h.api.completeActionForBot(info,h.state))).costPayment,{sacrificedPermanents:['tarn']});
 info.additionalCostInfo.validSacrificeTargets=['other'];assert.equal(h.api.completeActionForBot(info,h.state),null);
});

test('AI takes an eligible land from an optional fetch search',()=>{
 const h=harness();h.state.cards.fetch={oracleText:'Search your library for an Island or Mountain card.'};h.state.cards.land={zone:{zoneType:'Library'}};
 const d={type:'SelectCardsDecision',id:'search',context:{sourceId:'fetch'},options:['land'],minSelections:0,maxSelections:1};
 assert.deepEqual(JSON.parse(JSON.stringify(h.api.decisionResponseSafeForState(d,'auto',h.state))).selectedCards,['land']);
});
test('AI optional discard stays empty and empty or zero-cap searches stay legal',()=>{
 const h=harness();const d={type:'SelectCardsDecision',id:'discard',options:['bear'],minSelections:0,maxSelections:1};
 assert.deepEqual(JSON.parse(JSON.stringify(h.api.decisionResponseSafeForState(d,'auto',h.state))).selectedCards,[]);
 d.type='SearchLibraryDecision';d.options=[];assert.deepEqual(JSON.parse(JSON.stringify(h.api.decisionResponseSafeForState(d,'auto',h.state))).selectedCards,[]);
 d.options=['bear'];d.maxSelections=0;assert.deepEqual(JSON.parse(JSON.stringify(h.api.decisionResponseSafeForState(d,'auto',h.state))).selectedCards,[]);
});
