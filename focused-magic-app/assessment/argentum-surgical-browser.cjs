const fs=require('fs'),path=require('path'),assert=require('node:assert/strict');
const {chromium}=require('../../web-client/node_modules/playwright');
const root=path.resolve(__dirname,'../..');
const hook=`window.__attachScenario=function(f){
 const session=new ArgentumSession({label:'Local native extraction scenario',presentation:'tabletop'});
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
  const page=await browser.newPage({viewport:{width:1440,height:1050}}),errors=[],prompts=[];
  page.on('pageerror',e=>errors.push(e.message));
  page.on('dialog',async d=>{
   const message=d.message();prompts.push(message);
   const hand=message.match(/^(\d+)\. Grizzly Bears — hand \(Opponent\)/m);
   await d.accept(hand?hand[1]:message.includes('exact action JSON')?d.defaultValue():'1');
  });
  await page.route('**/js/provider/argentum.js*',r=>r.fulfill({contentType:'application/javascript',body:fs.readFileSync(path.join(root,'focused-magic-app/js/provider/argentum.js'),'utf8').replace('window.FocusedMagicArgentum={',hook+'\nwindow.FocusedMagicArgentum={')}));
  await page.goto('http://127.0.0.1:8781/');await page.waitForFunction(()=>!!window.__attachScenario);
  const response=await fetch('http://127.0.0.1:8082/api/dev/scenarios',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({player1Name:'Extraction Tester',player2Name:'Opponent',player1:{lifeTotal:20,hand:['Surgical Extraction'],battlefield:[{name:'Swamp'}],library:Array(20).fill('Swamp')},player2:{lifeTotal:20,hand:['Grizzly Bears'],graveyard:['Grizzly Bears'],battlefield:[],library:['Grizzly Bears',...Array(20).fill('Island')]},phase:'PRECOMBAT_MAIN',step:'PRECOMBAT_MAIN',activePlayer:1,priorityPlayer:1,mode:'TWO_PLAYER'})});
  assert.equal(response.status,200,await response.clone().text());
  const fixture=await response.json();await page.evaluate(f=>window.__attachScenario(f),fixture);
  await page.locator('#argentumTabletopMyHand button').filter({hasText:'Surgical Extraction'}).waitFor();
  await page.locator('#argentumTabletopMyHand button').filter({hasText:'Surgical Extraction'}).click();
  await page.locator('#argentumTabletopCardActions button').filter({hasText:/Cast Surgical Extraction/}).first().click();
  await page.waitForFunction(()=>window.FocusedMagicArgentum.active?.pendingDecision?.type==='SelectCardsDecision');
  await page.locator('#argentumTabletopDecision button').filter({hasText:'Resolve decision'}).click();
  await page.waitForFunction(()=>document.querySelector('#argentumTabletopOppHandCount')?.textContent==='0');
  const result=await page.evaluate(owner=>{
   const s=window.FocusedMagicArgentum.active.state;
   const zone=t=>s.zones.find(z=>(z.zoneId?.ownerId||z.ownerId)===owner&&String(z.zoneId?.zoneType||z.zoneType).toUpperCase()===t);
   const count=t=>{const z=zone(t);return z?.size??z?.cardIds?.length;};
   return {graveyard:count('GRAVEYARD'),exile:count('EXILE'),library:count('LIBRARY')};
  },fixture.player2.playerId);
  assert.deepEqual(result,{graveyard:1,exile:1,library:21});
  const selection=prompts.find(p=>p.includes('Grizzly Bears — hand (Opponent)')&&p.includes('Grizzly Bears — library (Opponent)'));
  assert.ok(selection,'all searched zones must be labeled in the actual selection dialog');
  assert.match(selection,/Grizzly Bears — graveyard \(Opponent\)/);
  assert.equal(await page.locator('#argentumTabletopMyLife').innerText(),'20');
  assert.deepEqual(errors,[]);
  await page.screenshot({path:path.join(root,'docs/focused-magic/verification-images/surgical-local.png')});
  fs.writeFileSync(path.join(root,'docs/focused-magic/surgical-local-browser-results.json'),JSON.stringify({scope:'Real loopback native server and normal Focused Magic cast/choice buttons; not production deployment or complete deck acceptance',scenario:'Surgical Extraction chooses only the matching copy in the opponent hand',selectionLabels:['graveyard (Opponent)','hand (Opponent)','library (Opponent)'],...result,casterLife:20,pageErrors:errors},null,2)+'\n');
  console.log('Surgical Extraction real local provider scenario passed.');
 } finally {await browser.close();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});

