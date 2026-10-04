'use strict';
const assert=require('node:assert/strict');
const fs=require('node:fs');
const {chromium}=require('../../web-client/node_modules/playwright');

async function main(){
 const browser=await chromium.launch({channel:'chrome',headless:true});
 try{
  const page=await browser.newPage();
  await page.setContent('<html><body></body></html>');
  const source=fs.readFileSync(require.resolve('../js/provider/argentum.js'),'utf8').replace('  window.FocusedMagicArgentum={','  window.__decision=decisionResponse; window.__card=providerTableCard; active={state:{cards:{old:{name:"Mountain"},drawn:{name:"Island"},keep:{name:"Shock"}},players:[],zones:[]}};\n  window.FocusedMagicArgentum={');
  await page.addScriptTag({content:source});
  await page.evaluate(()=>{window.__response=null;window.__decision({type:'SelectCardsDecision',id:'loot',prompt:'Discard two cards',options:['old','drawn','keep'],minSelections:2,maxSelections:2}).then(r=>window.__response=r);});
  const dialog=page.locator('dialog');
  await dialog.waitFor();
  assert.equal(await dialog.locator('button').first().isDisabled(),true);
  await dialog.locator('input').nth(0).check();
  assert.equal(await dialog.locator('button').first().isDisabled(),true);
  await dialog.locator('input').nth(1).check();
  await dialog.locator('button').first().click();
  await page.waitForFunction(()=>window.__response!==null);
  assert.deepEqual(await page.evaluate(()=>window.__response),{type:'CardsSelectedResponse',decisionId:'loot',selectedCards:['old','drawn']});
  await page.evaluate(()=>{window.__cancelled=false;window.__decision({type:'SelectCardsDecision',id:'cancel',options:['keep'],minSelections:0,maxSelections:1}).catch(e=>window.__cancelled=e.message==='Action cancelled.');});
  await dialog.waitFor();
  await dialog.getByText('Cancel',{exact:true}).click();
  await page.waitForFunction(()=>window.__cancelled===true);
  console.log('Native browser discard selection and cancellation passed');
  const fixture=await page.evaluate(()=>{const a=window.FocusedMagicArgentum;return a.parseDecklist(a.FIXTURES.october.user);});
  assert.equal(fixture.mainCount,60);assert.equal(fixture.uniqueNames.length,30);assert.equal(fixture.main['Commit // Memory'],1);
  const printings=await fetch('http://127.0.0.1:8082/api/cards/Way%20of%20the%20Pyromancer/printings');
  assert.equal(printings.status,200);const rows=await printings.json();assert.ok(rows.some(r=>r.imageUri));
  await page.route('**/api/cards/**/printings',route=>route.fulfill({contentType:'application/json',body:JSON.stringify(rows)}));
  await page.evaluate(()=>{const state={cards:{way:{name:'Way of the Pyromancer',counters:{LOYALTY:3},chosenX:0,power:2,toughness:4}}};document.body.appendChild(window.__card(state,'way','BATTLEFIELD'));});
  await page.locator('button[data-card-id="way"] img').waitFor();
  const rendered=page.locator('button[data-card-id="way"]');
  assert.equal(await rendered.locator('.argentum-tabletop-counter').innerText(),'loyalty: 3');
  assert.equal(await rendered.locator('.argentum-tabletop-stat').innerText(),'2/4');
  assert.equal(await rendered.locator('.argentum-tabletop-x').innerText(),'X=0');
  console.log('Uploaded deck counts and actual provider printing-art fallback preserve counters, stats and X');
 }finally{await browser.close();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
