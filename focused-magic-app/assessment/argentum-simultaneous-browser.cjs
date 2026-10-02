// Real local provider acceptance. Requires the newly built native server on 8082 and app on 8781.
const fs=require('fs'),path=require('path'),assert=require('node:assert/strict');
const {chromium}=require('../../web-client/node_modules/playwright');
const root=path.resolve(__dirname,'../..');
const hook=`window.__attachScenario=function(f){
 const session=new ArgentumSession({label:'Local simultaneous-choice scenario',presentation:'tabletop'});
 active=session;session.sessionId=f.sessionId;session.ai.joinSent=true;
 for(const [role,seat] of [[session.user,f.player1],[session.ai,f.player2]]){
  role.name=seat.name;role.playerId=seat.playerId;role.token=seat.token;
  role.ws=new WebSocket('ws://127.0.0.1:8082/game');
  role.ws.onopen=()=>{role.connected=true;session.sendRole(role,{type:'connect',playerName:seat.name,token:seat.token});};
  role.ws.onmessage=e=>session.onRoleMessage(role,JSON.parse(e.data));
 }
};`;
async function main(){
 const browser=await chromium.launch({channel:'chrome',headless:true});
 try {
  const results=[];
  for(const kind of ['geier','edict']){
   const page=await browser.newPage({viewport:{width:1440,height:1050}}),errors=[],prompts=[];
   page.on('pageerror',e=>errors.push(e.message));
   page.on('dialog',async d=>{
    prompts.push(d.message());
    const bear=d.message().match(/^(\d+)\. Grizzly Bears — hand/m);
    await d.accept(bear?bear[1]:d.message().includes('exact action JSON')?d.defaultValue():'1');
   });
   await page.route('**/js/provider/argentum.js*',r=>r.fulfill({contentType:'application/javascript',body:fs.readFileSync(path.join(root,'focused-magic-app/js/provider/argentum.js'),'utf8').replace('window.FocusedMagicArgentum={',hook+'\nwindow.FocusedMagicArgentum={')}));
   await page.goto('http://127.0.0.1:8781/');await page.waitForFunction(()=>!!window.__attachScenario);
   const configuration={player1Name:'Simultaneous Tester',player2Name:'Opponent',
    player1:{lifeTotal:20,hand:kind==='geier'?['Grizzly Bears']:['Sheoldred\'s Edict'],
     battlefield:kind==='geier'?[{name:'Geier Reach Sanitarium'},{name:'Island'},{name:'Island'}]:[{name:'Swamp'},{name:'Swamp'},{name:'Leyline of the Void'}],library:Array(20).fill('Island')},
    player2:{lifeTotal:20,hand:kind==='geier'?['Hill Giant']:[],battlefield:kind==='geier'?[]:[{name:'Darksteel Myr'},{name:'Hill Giant'}],library:Array(20).fill('Island')},
    phase:'PRECOMBAT_MAIN',step:'PRECOMBAT_MAIN',activePlayer:1,priorityPlayer:1,mode:'TWO_PLAYER'};
   const response=await fetch('http://127.0.0.1:8082/api/dev/scenarios',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(configuration)});
   assert.equal(response.status,200,await response.clone().text());
   const fixture=await response.json();await page.evaluate(f=>window.__attachScenario(f),fixture);
   const cardName=kind==='geier'?'Geier Reach Sanitarium':"Sheoldred's Edict";
   const zone=kind==='geier'?'#argentumTabletopMyBattlefield':'#argentumTabletopMyHand';
   const card=page.locator(`${zone} button`).filter({hasText:cardName});await card.waitFor();
   await page.waitForFunction(name=>{
    const session=window.FocusedMagicArgentum.active;
    const id=Object.keys(session?.state?.cards||{}).find(id=>session.state.cards[id].name===name);
    return id&&(session.legalActions||[]).some(a=>JSON.stringify(a).includes(id));
   },cardName);
   await card.click();
   const actions=page.locator('#argentumTabletopCardActions button');
   await actions.filter({hasText:kind==='geier'?/draw|discard/i:/Each opponent sacrifices a nontoken creature/}).first().click();
   if(kind==='geier'){
    await page.waitForFunction(()=>window.FocusedMagicArgentum.active?.pendingDecision?.type==='SelectCardsDecision');
    await page.locator('#argentumTabletopDecision button').filter({hasText:'Resolve decision'}).click();
   }
   await page.waitForFunction(({kind,other})=>{
    const s=window.FocusedMagicArgentum.active?.state;if(!s)return false;
    const count=t=>{const z=s.zones.find(z=>(z.zoneId?.ownerId||z.ownerId)===other&&String(z.zoneId?.zoneType||z.zoneType).toUpperCase()===t);return z?.size??z?.cardIds?.length??0;};
    return kind==='geier'?count('GRAVEYARD')===1:count('EXILE')===1;
   },{kind,other:fixture.player2.playerId});
   const state=await page.evaluate(()=>window.FocusedMagicArgentum.active.state);
   function count(owner,type){const z=state.zones.find(z=>(z.zoneId?.ownerId||z.ownerId)===owner&&String(z.zoneId?.zoneType||z.zoneType).toUpperCase()===type);return z?.size??z?.cardIds?.length??0;}
   if(kind==='geier'){
    assert.equal(count(fixture.player1.playerId,'GRAVEYARD'),1);assert.equal(count(fixture.player2.playerId,'GRAVEYARD'),1);
    assert.equal(count(fixture.player1.playerId,'HAND'),1);assert.equal(count(fixture.player2.playerId,'HAND'),1);
    assert.equal(count(fixture.player1.playerId,'LIBRARY'),19);assert.equal(count(fixture.player2.playerId,'LIBRARY'),19);
    assert.ok(prompts.some(p=>p.includes('Grizzly Bears — hand (Simultaneous Tester)')));
   }else{
    assert.equal(count(fixture.player2.playerId,'BATTLEFIELD'),1);assert.equal(count(fixture.player2.playerId,'EXILE'),1);
    assert.equal(count(fixture.player2.playerId,'GRAVEYARD'),0);assert.equal(count(fixture.player1.playerId,'BATTLEFIELD'),3);
   }
   assert.deepEqual(errors,[]);
   await page.screenshot({path:path.join(root,`docs/focused-magic/verification-images/${kind}-local.png`)});
   results.push({card:cardName,pageErrors:errors,localProviderFlowPassed:true});await page.close();
  }
  fs.writeFileSync(path.join(root,'docs/focused-magic/simultaneous-local-browser-results.json'),JSON.stringify({scope:'Real loopback native engine and normal provider buttons; test fixture/reconnect only. Not production deployment or complete deck acceptance.',results},null,2)+'\n');
  console.log('Both simultaneous-action local browser scenarios passed.');
 } finally {await browser.close();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
