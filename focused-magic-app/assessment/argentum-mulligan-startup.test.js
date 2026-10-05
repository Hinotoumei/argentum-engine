'use strict';
const assert=require('node:assert/strict');
const {test}=require('node:test');
const fs=require('node:fs');
const vm=require('node:vm');
function harness(){
 const timers=new Map();let next=0;const window={addEventListener(){}};
 const context={window,document:{getElementById(){return null;}},console,setTimeout(fn){timers.set(++next,fn);return next;},clearTimeout(id){timers.delete(id);}};
 vm.createContext(context);
 const source=fs.readFileSync(process.env.ARGENTUM_TEST_SOURCE||require.resolve('../js/provider/argentum.js'),'utf8').replace('  window.FocusedMagicArgentum={',
  '  renderState=()=>{};setStatus=()=>{};log=()=>{};window.Session=ArgentumSession;\n  window.FocusedMagicArgentum={');
 vm.runInContext(source,context);
 const session=new window.Session(),sent=[];
 session.sendRole=(role,msg)=>sent.push({role:role.kind,...JSON.parse(JSON.stringify(msg))});
 session.renderMulligan=()=>{};session.renderBottomCards=()=>{};
 const update=(role,extra={})=>session.onRoleMessage(role,{type:'stateUpdate',interactionEpoch:{id:'current'},state:{viewingPlayerId:role.kind,priorityPlayerId:role.kind,players:[],cards:{},zones:[],...extra},legalActions:[{actionType:'PlayLand',action:{type:'PlayLand',playerId:role.kind,cardId:'land'},description:'Play Island'}]});
 const flush=()=>{const pending=[...timers.values()];timers.clear();pending.forEach(fn=>fn());};
 return {session,sent,update,flush};
}
test('opening resync cannot make the AI act before mulligan prompts arrive',()=>{
 const h=harness();h.update(h.session.ai);h.flush();assert.equal(h.sent.length,0);
});
test('AI keeps automatically but waits while the human opening hand is pending',()=>{
 const h=harness(),s=h.session;
 s.onRoleMessage(s.user,{type:'mulliganDecision'});
 s.onRoleMessage(s.ai,{type:'mulliganDecision'});
 assert(h.sent.some(m=>m.role==='ai'&&m.type==='keepHand'));
 s.onRoleMessage(s.ai,{type:'mulliganComplete'});
 s.onRoleMessage(s.ai,{type:'waitingForOpponentMulligan'});
 h.update(s.ai);h.flush();assert.equal(h.sent.filter(m=>m.type==='submitAction').length,0);
 assert.throws(()=>s.submitAction({type:'PlayLand'}),/opening hands/);
});
test('rapid repeated Mulligan clicks send only one opening-hand request',()=>{
 const h=harness(),s=h.session,prompt={type:'mulliganDecision'};
 s.onRoleMessage(s.user,prompt);
 assert.equal(s.submitOpening(s.user,'mulligan',null,prompt),true);
 assert.equal(s.submitOpening(s.user,'mulligan',null,prompt),false);
 assert.equal(s.submitOpening(s.user,'keepHand',null,prompt),false);
 assert.equal(h.sent.filter(m=>m.type==='mulligan').length,1);
});
test('a stale Mulligan button cannot act on a newly returned opening hand',()=>{
 const h=harness(),s=h.session,old={type:'mulliganDecision'},fresh={type:'mulliganDecision',mulliganCount:1};
 s.onRoleMessage(s.user,old);s.submitOpening(s.user,'mulligan',null,old);
 s.onRoleMessage(s.user,fresh);
 assert.equal(s.submitOpening(s.user,'mulligan',null,old),false);
 assert.equal(s.submitOpening(s.user,'keepHand',null,fresh),true);
});
test('both completion messages request fresh state before the AI plays',()=>{
 const h=harness(),s=h.session;h.update(s.ai);h.flush();
 s.onRoleMessage(s.ai,{type:'mulliganComplete'});
 s.onRoleMessage(s.user,{type:'mulliganComplete'});
 assert.equal(h.sent.filter(m=>m.type==='requestResync').length,2);
 s.autoRole(s.ai);h.flush();assert.equal(h.sent.filter(m=>m.type==='submitAction').length,0);
 h.update(s.ai);h.flush();assert.equal(h.sent.filter(m=>m.type==='submitAction').length,1);
});
test('London bottom-card selection prevents an already scheduled AI action',()=>{
 const h=harness(),s=h.session;s.user.mulliganComplete=true;s.ai.mulliganComplete=true;
 h.update(s.ai);
 s.onRoleMessage(s.user,{type:'chooseBottomCards',cardsToPutOnBottom:1,cardIds:['land']});
 h.flush();assert.equal(h.sent.filter(m=>m.type==='submitAction').length,0);
});
test('identical resyncs cannot submit the same automatic action twice',()=>{
 const h=harness(),s=h.session;s.user.mulliganComplete=true;s.ai.mulliganComplete=true;
 h.update(s.ai);h.flush();h.update(s.ai);h.flush();
 assert.equal(h.sent.filter(m=>m.type==='submitAction').length,1);
 h.update(s.ai,{turnNumber:2});h.flush();
 assert.equal(h.sent.filter(m=>m.type==='submitAction').length,2);
});
test('an unchanged resync is not an authoritative AI action acknowledgement',()=>{
 const h=harness(),s=h.session;s.user.mulliganComplete=true;s.ai.mulliganComplete=true;
 h.update(s.ai);h.flush();h.update(s.ai);
 assert.equal(s.aiMeaningful.length,0);
 h.update(s.ai,{turnNumber:2});assert.equal(s.aiMeaningful.length,1);
});

test('successive Brainstorm choices with unchanged board each receive a response',()=>{
 const h=harness(),s=h.session;s.user.mulliganComplete=true;s.ai.mulliganComplete=true;
 const state={viewingPlayerId:'ai',priorityPlayerId:'ai',players:[],cards:{},zones:[]};
 const choose=id=>s.onRoleMessage(s.ai,{type:'stateUpdate',state,interactionEpoch:{id:'current'},legalActions:[],pendingDecision:{type:'SelectCardsDecision',id,options:['a','b','c'],minSelections:2}});
 choose('return-two');h.flush();choose('return-two');h.flush();
 assert.equal(h.sent.filter(m=>m.action?.type==='SubmitDecision').length,1);
 choose('order-two');h.flush();
 assert.equal(h.sent.filter(m=>m.action?.type==='SubmitDecision').length,2);
});



test('an unpaid human spell keeps both seats connected and requests fresh actions',()=>{
 const h=harness(),s=h.session;let failures=0,closed=0;s.spec.onProviderFail=()=>failures++;s.close=()=>closed++;
 s.onRoleMessage(s.user,{type:'error',code:'INVALID_ACTION',message:'Cannot pay mana cost'});h.flush();
 assert.equal(failures,0);assert.equal(closed,0);assert(h.sent.some(m=>m.type==='requestResync'));
});
test('a genuine provider failure still uses the configured failure handler',()=>{
 const h=harness(),s=h.session;let failures=0,closed=0;s.spec.onProviderFail=()=>failures++;s.close=()=>closed++;
 s.onRoleMessage(s.user,{type:'error',code:'SESSION_NOT_FOUND',message:'Missing session'});h.flush();
 assert.equal(failures,1);assert.equal(closed,1);
});


test('AI skips its rejected command on unchanged state and passes instead',()=>{
 const h=harness(),s=h.session;s.user.mulliganComplete=true;s.ai.mulliganComplete=true;
 const msg={type:'stateUpdate',interactionEpoch:{id:'current'},state:{viewingPlayerId:'ai',priorityPlayerId:'ai',players:[],cards:{},zones:[]},legalActions:[{actionType:'CastSpell',isAffordable:true,action:{type:'CastSpell',playerId:'ai',cardId:'bad'}},{actionType:'PassPriority',action:{type:'PassPriority',playerId:'ai'}}]};
 s.onRoleMessage(s.ai,msg);h.flush();assert.equal(h.sent.filter(m=>m.type==='submitAction').at(-1).action.type,'CastSpell');
 s.onRoleMessage(s.ai,{type:'error',code:'INVALID_ACTION',message:'Not enough targets'});h.flush();s.onRoleMessage(s.ai,msg);h.flush();
 assert.equal(h.sent.filter(m=>m.type==='submitAction').at(-1).action.type,'PassPriority');
});
