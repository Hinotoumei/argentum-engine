'use strict';
const assert=require('node:assert/strict');
const {test}=require('node:test');
const fs=require('node:fs');
const vm=require('node:vm');
function harness(){
 const timers=new Map();let next=0;const window={addEventListener(){}};
 const context={window,document:{},console,setTimeout(fn){timers.set(++next,fn);return next;},clearTimeout(id){timers.delete(id);}};
 vm.createContext(context);
 const source=fs.readFileSync(process.env.ARGENTUM_TEST_SOURCE||require.resolve('../js/provider/argentum.js'),'utf8').replace('  window.FocusedMagicArgentum={',
  '  renderState=()=>{};setStatus=()=>{};log=()=>{};window.Session=ArgentumSession;\n  window.FocusedMagicArgentum={');
 vm.runInContext(source,context);
 const session=new window.Session(),sent=[];
 session.sendRole=(role,msg)=>sent.push({role:role.kind,...JSON.parse(JSON.stringify(msg))});
 session.renderMulligan=()=>{};session.renderBottomCards=()=>{};
 const update=role=>session.onRoleMessage(role,{type:'stateUpdate',interactionEpoch:{id:'current'},state:{viewingPlayerId:role.kind,priorityPlayerId:role.kind,players:[],cards:{},zones:[]},legalActions:[{actionType:'PlayLand',action:{type:'PlayLand',playerId:role.kind,cardId:'land'},description:'Play Island'}]});
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
 assert.equal(h.sent[0].type,'keepHand');
 s.onRoleMessage(s.ai,{type:'mulliganComplete'});
 s.onRoleMessage(s.ai,{type:'waitingForOpponentMulligan'});
 h.update(s.ai);h.flush();assert.equal(h.sent.filter(m=>m.type==='submitAction').length,0);
 assert.throws(()=>s.submitAction({type:'PlayLand'}),/opening hands/);
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
