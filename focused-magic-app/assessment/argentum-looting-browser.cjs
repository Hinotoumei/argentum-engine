'use strict';
const fs=require('node:fs');
const assert=require('node:assert/strict');
const {chromium}=require('../../web-client/node_modules/playwright');
const hook=`window.__attachScenario=function(f){
 if(active)active.close();
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
  async function attach(hand,battlefield){
   const r=await fetch('http://127.0.0.1:8082/api/dev/scenarios',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({player1Name:'Control Tester',player2Name:'Opponent',player1:{hand,battlefield:battlefield.map(name=>({name})),library:Array(20).fill('Island')},player2:{hand:[],battlefield:[],library:Array(20).fill('Island')},phase:'PRECOMBAT_MAIN',step:'PRECOMBAT_MAIN',activePlayer:1,priorityPlayer:1,mode:'TWO_PLAYER'})});
   assert.equal(r.status,200,await r.clone().text());await page.evaluate(f=>window.__attachScenario(f),await r.json());
  }
  await attach(['Way of the Pyromancer'],['Mountain','Mountain']);
  await page.locator('#argentumTabletopMyHand button').filter({hasText:'Way of the Pyromancer'}).click();
  await page.locator('#argentumTabletopCardActions button').filter({hasText:/Cast Way of the Pyromancer/}).first().click();
  for(let i=0;i<2;i++){
   await page.locator('#argentumTabletopPass').click();
   await page.waitForTimeout(250);
   if(await page.locator('#argentumTabletopMyBattlefield button').filter({hasText:/^Jace/}).count())break;
  }
  const jace=page.locator('#argentumTabletopMyBattlefield button').filter({hasText:/^Jace/});await jace.waitFor();
  assert.match(await jace.innerText(),/loyalty: 2/);
  await jace.click();
  await page.locator('#argentumTabletopCardActions button').filter({hasText:'Add {R}'}).first().click();
  await page.waitForFunction(()=>Object.values(window.__state()?.cards||{}).some(c=>c.name==='Jace'&&c.counters?.LOYALTY===3));
  await page.locator('#argentumTabletopPass').click();
  await page.waitForFunction(()=>{const s=window.__state();return s?.players.find(p=>p.playerId===s.viewingPlayerId)?.manaPool?.red===1;});
  await jace.click();assert.equal(await page.locator('#argentumTabletopCardActions button').filter({hasText:'Add {R}'}).count(),0);
  console.log('Native tabletop Way created Jace, granted +1, resolved red mana, and enforced loyalty activation limit');
  await attach(['Prismari Command'],['Island','Mountain','Mountain']);
  await page.locator('#argentumTabletopMyHand button').filter({hasText:'Prismari Command'}).click();
  await page.locator('#argentumTabletopCardActions button').filter({hasText:/Cast Prismari Command/}).first().click();
  await dialog.waitFor();
  await dialog.locator('label').filter({hasText:/2 damage/}).locator('input').check();
  await dialog.locator('label').filter({hasText:/Treasure/}).locator('input').check();
  await dialog.getByText('Confirm modes',{exact:true}).click();
  await dialog.waitFor();await dialog.locator('label').filter({hasText:'Opponent'}).locator('input').check();await dialog.locator('button').first().click();
  await dialog.waitFor();await dialog.locator('label').filter({hasText:'Control Tester'}).locator('input').check();await dialog.locator('button').first().click();
  await page.locator('#argentumTabletopPass').click();
  await page.waitForFunction(()=>document.getElementById('argentumTabletopOppLife')?.textContent==='18');
  assert.equal(await page.locator('#argentumTabletopMyBattlefield button').filter({hasText:'Treasure'}).count(),1);
  assert.deepEqual(errors,[]);
  console.log('Native tabletop Prismari Command selected two modes and targets, dealt two damage and created a Treasure');
 }finally{await browser.close();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
