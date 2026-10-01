const fs=require('fs'),path=require('path'),assert=require('node:assert/strict');
const {chromium}=require('../../web-client/node_modules/playwright');
const root=path.resolve(__dirname,'../..');
const hook=`window.__attachScenario=function(f){
 const session=new ArgentumSession({label:'Local Gathan Raiders scenario',presentation:'tabletop'});
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
 try{
  const page=await browser.newPage({viewport:{width:1440,height:1050}}),errors=[];
  page.on('pageerror',e=>errors.push(e.message));
  await page.route('**/js/provider/argentum.js*',r=>r.fulfill({contentType:'application/javascript',body:fs.readFileSync(path.join(root,'focused-magic-app/js/provider/argentum.js'),'utf8').replace('window.FocusedMagicArgentum={',hook+'\nwindow.FocusedMagicArgentum={')}));
  await page.goto('http://127.0.0.1:8781/');await page.waitForFunction(()=>!!window.__attachScenario);
  const response=await fetch('http://127.0.0.1:8082/api/dev/scenarios',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({player1Name:'Morph Tester',player2Name:'Opponent',player1:{lifeTotal:20,hand:['Gathan Raiders','Mountain'],battlefield:[{name:'Swamp'},{name:'Swamp'},{name:'Swamp'}],library:Array(20).fill('Swamp')},player2:{lifeTotal:20,hand:[],battlefield:[],library:Array(20).fill('Island')},phase:'PRECOMBAT_MAIN',step:'PRECOMBAT_MAIN',activePlayer:1,priorityPlayer:1,mode:'TWO_PLAYER'})});
  assert.equal(response.status,200,await response.clone().text());
  await page.evaluate(f=>window.__attachScenario(f),await response.json());
  await page.locator('#argentumTabletopMyHand button').filter({hasText:'Gathan Raiders'}).waitFor();
  await page.locator('#argentumTabletopMyHand button').filter({hasText:'Gathan Raiders'}).click();
  await page.locator('#argentumTabletopCardActions button').filter({hasText:'Cast Gathan Raiders face-down'}).click();
  const faceDown=page.locator('#argentumTabletopMyBattlefield button').filter({has:page.locator('.argentum-tabletop-stat',{hasText:'2/2'})});
  await faceDown.waitFor();
  assert.equal(await page.locator('#argentumTabletopMyHand button').filter({hasText:'Mountain'}).count(),1);
  assert.equal(await page.locator('#argentumTabletopMyLife').innerText(),'20');
  await faceDown.click();
  await page.locator('#argentumTabletopCardActions button').filter({hasText:/Turn.*face.*up/i}).click();
  await page.locator('#argentumTabletopDecision button').filter({hasText:'Resolve decision'}).waitFor();
  page.on('dialog',d=>d.accept('1'));
  await page.locator('#argentumTabletopDecision button').filter({hasText:'Resolve decision'}).click();
  const revealed=page.locator('#argentumTabletopMyBattlefield button').filter({hasText:'Gathan Raiders'});
  await revealed.locator('.argentum-tabletop-stat').filter({hasText:'5/5'}).waitFor();
  assert.equal(await page.locator('#argentumTabletopMyHand button').count(),0);
  assert.equal(await page.locator('#argentumTabletopMyGyCount').innerText(),'1');
  assert.equal(await page.locator('#argentumTabletopMyLife').innerText(),'20');
  assert.deepEqual(errors,[]);
  await page.screenshot({path:path.join(root,'docs/focused-magic/verification-images/gathan-morph-local.png')});
  fs.writeFileSync(path.join(root,'docs/focused-magic/gathan-local-browser-results.json'),JSON.stringify({scenario:'Native Gathan Raiders face-down casting and discard-to-morph through normal adapter controls',faceDownStats:'2/2',faceUpStats:'5/5',discardedCards:1,handCards:0,life:20,pageErrors:errors},null,2)+'\n');
  console.log('Local browser Gathan Raiders morph scenario passed');
 }finally{await browser.close();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
