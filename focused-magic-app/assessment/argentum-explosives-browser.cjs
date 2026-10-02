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
   await d.accept(message.startsWith('Choose X')?'2':message.includes('exact action JSON')?d.defaultValue():'1');
  });
  await page.route('**/js/provider/argentum.js*',r=>r.fulfill({contentType:'application/javascript',body:fs.readFileSync(path.join(root,'focused-magic-app/js/provider/argentum.js'),'utf8').replace('window.FocusedMagicArgentum={',hook+'\nwindow.FocusedMagicArgentum={')}));
  await page.goto('http://127.0.0.1:8781/');await page.waitForFunction(()=>!!window.__attachScenario);
  const response=await fetch('http://127.0.0.1:8082/api/dev/scenarios',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({player1Name:'Explosives Tester',player2Name:'Opponent',player1:{lifeTotal:20,hand:['Engineered Explosives'],battlefield:[...Array(4).fill({name:'Swamp'}),{name:'Llanowar Elves'}],library:Array(20).fill('Swamp')},player2:{lifeTotal:20,hand:[],graveyard:[],battlefield:[{name:'Llanowar Elves'},{name:'Hill Giant'}],library:Array(20).fill('Island')},phase:'PRECOMBAT_MAIN',step:'PRECOMBAT_MAIN',activePlayer:1,priorityPlayer:1,mode:'TWO_PLAYER'})});
  assert.equal(response.status,200,await response.clone().text());
  const fixture=await response.json();await page.evaluate(f=>window.__attachScenario(f),fixture);
  await page.locator('#argentumTabletopMyHand button').filter({hasText:'Engineered Explosives'}).waitFor();
  await page.locator('#argentumTabletopMyHand button').filter({hasText:'Engineered Explosives'}).click();
  await page.locator('#argentumTabletopCardActions button').filter({hasText:/Cast Engineered Explosives/}).first().click();
  const bomb=page.locator('#argentumTabletopMyBattlefield button').filter({hasText:'Engineered Explosives'});
  await bomb.waitFor();
  const before=await page.evaluate(()=>{
   const s=window.FocusedMagicArgentum.active.state;
   return Object.values(s.cards).find(c=>c.name==='Engineered Explosives');
  });
  assert.equal(before.counters.CHARGE??before.counters.Charge,1);
  await bomb.getByText('charge: 1',{exact:true}).waitFor();
  await bomb.click();
  const action=page.locator('#argentumTabletopCardActions button').filter({hasText:/[Ss]acrifice/});
  await action.first().click();
  await page.waitForFunction(()=>{
   const s=window.FocusedMagicArgentum.active.state;
   return s.zones.filter(z=>String(z.zoneId.zoneType).toUpperCase()==='GRAVEYARD').reduce((n,z)=>n+z.size,0)===3;
  });
  const result=await page.evaluate(()=>{
   const s=window.FocusedMagicArgentum.active.state;
   const names=t=>s.zones.filter(z=>String(z.zoneId.zoneType).toUpperCase()===t).flatMap(z=>z.cardIds.map(id=>s.cards[id].name));
   return {graveyard:names('GRAVEYARD').sort(),battlefield:names('BATTLEFIELD').sort()};
  });
  assert.deepEqual(result.graveyard,['Engineered Explosives','Llanowar Elves','Llanowar Elves']);
  assert.deepEqual(result.battlefield,['Hill Giant','Swamp','Swamp','Swamp','Swamp']);
  assert.deepEqual(errors,[]);
  await page.screenshot({path:path.join(root,'docs/focused-magic/verification-images/explosives-local.png')});
  fs.writeFileSync(path.join(root,'docs/focused-magic/explosives-local-browser-results.json'),JSON.stringify({scope:'Real loopback native server and normal Focused Magic cast/activate buttons; not production deployment or complete deck acceptance',scenario:'X=2 paid with two black mana gives one charge counter; sacrificed Explosives destroys both one-mana Elves, preserving lands and Hill Giant',chargeCounters:1,chargeCounterDisplayed:true,...result,pageErrors:errors},null,2)+'\n');
  console.log('Engineered Explosives real local provider scenario passed.');
 } finally {await browser.close();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});

