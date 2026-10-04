'use strict';
const fs=require('node:fs');
const assert=require('node:assert/strict');
const {chromium}=require('../../web-client/node_modules/playwright');
const hook=`window.__attachScenario=function(f){
 active=new ArgentumSession({label:'Looting acceptance',presentation:'tabletop'});
 active.sessionId=f.sessionId;active.ai.joinSent=true;
 for(const [role,seat] of [[active.user,f.player1],[active.ai,f.player2]]){
  role.playerId=seat.playerId;role.token=seat.token;role.name=seat.name;
  role.ws=new WebSocket('ws://127.0.0.1:8082/game');
  role.ws.onopen=()=>active.sendRole(role,{type:'connect',playerName:seat.name,token:seat.token});
  role.ws.onmessage=e=>active.onRoleMessage(role,JSON.parse(e.data));
 }
};window.__state=()=>active?.state;`;
async function main(){
 const browser=await chromium.launch({channel:'chrome',headless:true});
 try{
  const page=await browser.newPage();const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.route('**/js/provider/argentum.js*',route=>route.fulfill({contentType:'application/javascript',body:fs.readFileSync(require.resolve('../js/provider/argentum.js'),'utf8').replace('  window.FocusedMagicArgentum={',hook+'\n  window.FocusedMagicArgentum={')}));
  await page.goto('http://127.0.0.1:8781/');
  const response=await fetch('http://127.0.0.1:8082/api/dev/scenarios',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({player1Name:'Looting Tester',player2Name:'Opponent',player1:{hand:['Faithless Looting','Shock','Island'],battlefield:[{name:'Mountain'}],library:Array(20).fill('Forest')},player2:{hand:[],battlefield:[],library:Array(20).fill('Island')},phase:'PRECOMBAT_MAIN',step:'PRECOMBAT_MAIN',activePlayer:1,priorityPlayer:1,mode:'TWO_PLAYER'})});
  assert.equal(response.status,200,await response.clone().text());
  await page.evaluate(f=>window.__attachScenario(f),await response.json());
  await page.locator('#argentumTabletopMyHand button').filter({hasText:'Faithless Looting'}).click();
  await page.locator('#argentumTabletopCardActions button').filter({hasText:/Cast Faithless Looting/}).first().click();
  await page.locator('#argentumTabletopPass').click();
  await page.locator('#argentumTabletopDecision button').filter({hasText:'Resolve decision'}).click();
  const dialog=page.locator('dialog');await dialog.waitFor();
  const drawn=dialog.locator('label').filter({hasText:'Forest'});assert.equal(await drawn.count(),2);
  await drawn.nth(0).locator('input').check();await drawn.nth(1).locator('input').check();
  await dialog.locator('button').first().click();
  await page.waitForFunction(()=>{
   const s=window.__state();const gy=s?.zones.find(z=>z.zoneId.ownerId===s.viewingPlayerId&&String(z.zoneId.zoneType).toUpperCase()==='GRAVEYARD');
   return gy?.cardIds.filter(id=>s.cards[id]?.name==='Forest').length===2;
  }).catch(async e=>{console.error(await page.evaluate(()=>({zones:window.__state()?.zones,log:document.getElementById('argentumLog')?.textContent,status:document.getElementById('argentumStatus')?.textContent})));throw e;});
  assert.equal(await page.locator('#argentumTabletopMyHand button').count(),2);
  assert.deepEqual(errors,[]);
  console.log('Native tabletop Faithless Looting drew two, selected newly drawn cards, discarded two, and resumed');
 }finally{await browser.close();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
