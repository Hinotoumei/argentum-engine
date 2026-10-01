const fs=require('fs');
const path=require('path');
const assert=require('node:assert/strict');
const {chromium}=require('../../web-client/node_modules/playwright');
const root=path.resolve(__dirname,'../..');
const output=path.join(root,'docs/focused-magic/verification-images');
const hook=`window.__attachScenario=function(fixture){
 if(active)active.close();
 const session=new ArgentumSession({label:'Local native scenario',presentation:'tabletop'});
 active=session;session.sessionId=fixture.sessionId;session.ai.joinSent=true;
 for(const [role,seat] of [[session.user,fixture.player1],[session.ai,fixture.player2]]){
  role.name=seat.name;role.playerId=seat.playerId;role.token=seat.token;
  role.ws=new WebSocket('ws://127.0.0.1:8082/game');
  role.ws.onopen=()=>{role.connected=true;session.sendRole(role,{type:'connect',playerName:seat.name,token:seat.token});};
  role.ws.onmessage=e=>session.onRoleMessage(role,JSON.parse(e.data));
 }
};`;
async function main(){
 const browser=await chromium.launch({channel:'chrome',headless:true});
 try{
  const page=await browser.newPage({viewport:{width:1440,height:1050}});
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.route('**/js/provider/argentum.js*',route=>route.fulfill({contentType:'application/javascript',body:fs.readFileSync(path.join(root,'focused-magic-app/js/provider/argentum.js'),'utf8').replace('window.FocusedMagicArgentum={',hook+'\nwindow.FocusedMagicArgentum={')}));
  await page.goto('http://127.0.0.1:8781/');
  await page.waitForFunction(()=>!!window.__attachScenario);
  const response=await fetch('http://127.0.0.1:8082/api/dev/scenarios',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({player1Name:'Deluge Tester',player2Name:'Opponent',player1:{lifeTotal:20,hand:['Toxic Deluge','Shock'],battlefield:[{name:'Swamp'},{name:'Swamp'},{name:'Swamp'},{name:'Mountain'},{name:'Hill Giant'}],library:Array(20).fill('Swamp')},player2:{lifeTotal:20,hand:[],battlefield:[{name:'Hill Giant'},{name:'Grizzly Bears'}],library:Array(20).fill('Island')},phase:'PRECOMBAT_MAIN',step:'PRECOMBAT_MAIN',activePlayer:1,priorityPlayer:1,mode:'TWO_PLAYER'})});
  assert.equal(response.status,200,await response.clone().text());
  await page.evaluate(f=>window.__attachScenario(f),await response.json());
  await page.locator('#argentumTabletopMyHand button').filter({hasText:'Toxic Deluge'}).waitFor();
  page.on('dialog',d=>d.accept('2'));
  await page.locator('#argentumTabletopMyHand button').filter({hasText:'Toxic Deluge'}).click();
  await page.locator('#argentumTabletopCardActions button').filter({hasText:/Cast Toxic Deluge/}).first().click();
  await page.locator('#argentumTabletopStack .argentum-tabletop-x').filter({hasText:'X=2'}).waitFor();
  assert.equal(await page.locator('#argentumTabletopMyLife').innerText(),'18');
  fs.mkdirSync(output,{recursive:true});
  await page.screenshot({path:path.join(output,'toxic-deluge-paid-x-local.png')});
  await page.locator('#argentumTabletopPass').click();
  await page.waitForFunction(()=>document.querySelector('#argentumTabletopMyBattlefield')?.textContent.includes('1/1'));
  assert.equal(await page.locator('#argentumTabletopMyBattlefield button').filter({hasText:'Hill Giant'}).locator('.argentum-tabletop-stat').innerText(),'1/1');
  assert.equal(await page.locator('#argentumTabletopOppBattlefield button').filter({hasText:'Hill Giant'}).locator('.argentum-tabletop-stat').innerText(),'1/1');
  assert.equal(await page.locator('#argentumTabletopOppBattlefield button').filter({hasText:'Grizzly Bears'}).count(),0);
  assert.equal(await page.locator('#argentumTabletopOppLife').innerText(),'20');
  assert.equal(await page.locator('#argentumTabletopCardActions button').count(),0);
  await page.screenshot({path:path.join(output,'toxic-deluge-resolved-local.png')});
  fs.writeFileSync(path.join(root,'docs/focused-magic/provider-local-browser-results.json'),JSON.stringify({scenario:'Toxic Deluge via real local server and normal adapter controls',paidLife:2,chosenX:2,casterLife:18,opponentLife:20,bothHillGiants:'1/1',grizzlyBearsDied:true,pageErrors:errors},null,2));
  console.log('Local browser Toxic Deluge scenario passed');
  page.removeAllListeners('dialog');
  const modalResponse=await fetch('http://127.0.0.1:8082/api/dev/scenarios',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({player1Name:'Modal Tester',player2Name:'Opponent',player1:{lifeTotal:20,hand:['Prismari Command'],battlefield:[{name:'Island'},{name:'Mountain'},{name:'Mountain'}],library:Array(20).fill('Island')},player2:{lifeTotal:20,hand:[],battlefield:[],library:Array(20).fill('Island')},phase:'PRECOMBAT_MAIN',step:'PRECOMBAT_MAIN',activePlayer:1,priorityPlayer:1,mode:'TWO_PLAYER'})});
  assert.equal(modalResponse.status,200,await modalResponse.clone().text());
  await page.evaluate(f=>window.__attachScenario(f),await modalResponse.json());
  await page.locator('#argentumTabletopMyHand button').filter({hasText:'Prismari Command'}).waitFor();
  page.on('dialog',async d=>{
   const message=d.message();
   if(message.includes('mode(s)'))return d.accept('1,3');
   const name=message.includes('Treasure')?'Modal Tester':'Opponent';
   const line=message.split('\n').find(x=>/^\d+\./.test(x)&&x.includes(name));
   assert.ok(line,`No advertised target for ${name}`);
   return d.accept(line.match(/^\d+/)[0]);
  });
  await page.locator('#argentumTabletopMyHand button').filter({hasText:'Prismari Command'}).click();
  await page.locator('#argentumTabletopCardActions button').filter({hasText:/Cast Prismari Command/}).first().click();
  await page.locator('#argentumTabletopStack button').filter({hasText:'Prismari Command'}).waitFor();
  await page.locator('#argentumTabletopPass').click();
  await page.waitForFunction(()=>document.querySelector('#argentumTabletopOppLife')?.textContent==='18');
  assert.equal(await page.locator('#argentumTabletopMyBattlefield button').filter({hasText:'Treasure'}).count(),1);
  assert.equal(await page.locator('#argentumTabletopMyLife').innerText(),'20');
  assert.equal(await page.locator('#argentumTabletopCardActions button').count(),0);
  await page.screenshot({path:path.join(output,'prismari-modal-targets-local.png')});
  assert.deepEqual(errors,[]);
  fs.writeFileSync(path.join(root,'docs/focused-magic/provider-local-modal-results.json'),JSON.stringify({scenario:'Prismari Command through normal adapter controls and real local server',chosenModes:[0,2],damageTarget:'Opponent',treasureTarget:'Modal Tester',opponentLife:18,casterLife:20,treasureCount:1,pageErrors:errors},null,2));
  console.log('Local browser Prismari Command two-mode targeting passed');
 }finally{await browser.close();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});


