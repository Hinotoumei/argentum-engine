'use strict';
const assert=require('node:assert/strict');
const {test}=require('node:test');
const fs=require('node:fs');
const vm=require('node:vm');
function harness(answers=[]){
  const elements=Object.fromEntries(['argentumModal','argentumStatus','argentumLog'].map(id=>[id,{textContent:'',classList:{toggle(){}}}]));
  const prompts=[],sent=[];
  const window={addEventListener(){},prompt(question){prompts.push(question);return answers.shift()??null;}};
  const context={window,document:{getElementById:id=>elements[id]||null,createElement:()=>({dataset:{},classList:{add(){}},innerHTML:''})},prompt:window.prompt,console,setTimeout,clearTimeout};
  vm.createContext(context);
  const source=fs.readFileSync(process.env.ARGENTUM_TEST_SOURCE||require.resolve('../js/provider/argentum.js'),'utf8').replace('  window.FocusedMagicArgentum={','  window.__test={decisionResponse,providerTableCard,submitLegalAction,completeActionForBot,setActive(value){active=value;}};\n  window.FocusedMagicArgentum={');
  vm.runInContext(source,context);
  const state={viewingPlayerId:'me',players:[{playerId:'me',life:20},{playerId:'opp',life:20}],cards:{bear:{name:'Bear',controllerId:'opp'},spell:{name:'Spell',controllerId:'opp'}},zones:[{zoneId:{zoneType:'BATTLEFIELD'},cardIds:['bear']},{zoneId:{zoneType:'STACK'},cardIds:['spell']}]};
  window.__test.setActive({interactionEpoch:{id:'epoch'},state,submitAction:action=>sent.push(JSON.parse(JSON.stringify(action)))});
  return {api:window.__test,state,prompts,sent,elements};
}
const target=(ids,min=1,max=1,extra={})=>({description:'Choose target',validTargets:ids,minTargets:min,maxTargets:max,...extra});
const modal=modes=>({action:{type:'CastSpell',cardId:'command',playerId:'me'},modalEnumeration:{minChooseCount:2,chooseCount:2,allowRepeat:false,modes,unavailableIndices:[]},requiresTargets:true});
const mode=(index,reqs=[])=>({index,description:`Mode ${index}`,available:true,targetRequirements:reqs});
test('human command submits player and stack targets in selected-mode order',async()=>{
 const h=harness(['2,1','1','1']);
 await h.api.submitLegalAction(modal([mode(0,[target(['spell'])]),mode(1,[target(['opp'])])]));
 assert.deepEqual(h.sent[0].chosenModes,[1,0]);
 assert.deepEqual(h.sent[0].modeTargetsOrdered,[[{type:'Player',playerId:'opp'}],[{type:'Spell',spellEntityId:'spell'}]]);
 assert.deepEqual(h.sent[0].targets,h.sent[0].modeTargetsOrdered.flat());
 assert.equal(h.prompts.length,3);
});
test('a non-targeted mode retains its empty ordinal group',async()=>{
 const h=harness(['1,2','1']);
 await h.api.submitLegalAction(modal([mode(0),mode(1,[target(['bear'])])]));
 assert.deepEqual(h.sent[0].modeTargetsOrdered,[[],[{type:'Permanent',entityId:'bear'}]]);
});
test('optional empty target requirements need no prompt',async()=>{
 const h=harness(['1,2']);
 await h.api.submitLegalAction(modal([mode(0,[target([],0,0)]),mode(1)]));
 assert.deepEqual(h.sent[0].modeTargetsOrdered,[[],[]]);assert.equal(h.prompts.length,1);
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
 assert.equal(h.sent[0].additionalCostPayment.payXLifeAmount,amount);assert.equal(h.prompts.length,1);
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

test('matching card selections show zones and owners and preserve the selected copy',()=>{
 const h=harness(['2']);h.state.players[1].name='Opponent';
 h.state.cards.gy={name:'Grizzly Bears',ownerId:'opp',zone:{zoneType:'GRAVEYARD'}};
 h.state.cards.hand={name:'Grizzly Bears',ownerId:'opp',zone:{zoneType:'HAND'}};
 h.state.cards.library={name:'Grizzly Bears',ownerId:'opp',zone:{zoneType:'LIBRARY'}};
 const response=h.api.decisionResponse({type:'SelectCardsDecision',id:'search',options:['gy','hand','library'],minSelections:0,maxSelections:3});
 assert.deepEqual(JSON.parse(JSON.stringify(response.selectedCards)),['hand']);
 for(const zone of ['graveyard','hand','library'])assert.match(h.prompts[0],new RegExp(`Grizzly Bears — ${zone} \\(Opponent\\)`));
});
test('a hidden-zone option uses provider decision card information when the ordinary state omits it',()=>{
 const h=harness(['1']);
 const response=h.api.decisionResponse({type:'SelectCardsDecision',id:'search',options:['hidden'],minSelections:0,maxSelections:1,cardInfo:{hidden:{name:'Grizzly Bears'}}});
 assert.match(h.prompts[0],/Grizzly Bears \[hidden\]/);assert.equal(response.selectedCards[0],'hidden');
});
test('optional card selections preserve an empty choice',()=>{
 const h=harness(['']);const response=h.api.decisionResponse({type:'SelectCardsDecision',id:'search',options:['bear'],minSelections:0,maxSelections:1});
 assert.equal(response.selectedCards.length,0);
});
test('cancelling a card selection returns no choice',()=>{
 const h=harness([null]);assert.throws(()=>h.api.decisionResponse({type:'SelectCardsDecision',id:'search',options:['bear'],minSelections:0,maxSelections:1}),/Action cancelled/);
 assert.equal(h.sent.length,0);
});

test('tabletop displays server-provided counter types and counts without deriving them from chosen X',()=>{
 const h=harness();h.state.cards.bear.chosenX=2;h.state.cards.bear.counters={CHARGE:1,PLUS_ONE_PLUS_ONE:3,LOYALTY:0};
 const card=h.api.providerTableCard(h.state,'bear','BATTLEFIELD');
 assert.match(card.innerHTML,/argentum-tabletop-counter">charge: 1/);
 assert.match(card.innerHTML,/plus one plus one: 3/);assert.doesNotMatch(card.innerHTML,/loyalty: 0|charge: 2/);
});
