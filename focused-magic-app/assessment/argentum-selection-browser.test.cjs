'use strict';
const assert=require('node:assert/strict');
const fs=require('node:fs');
const {chromium}=require('../../web-client/node_modules/playwright');

async function main(){
 const browser=await chromium.launch({channel:'chrome',headless:true});
 try{
  const page=await browser.newPage();
  await page.setContent('<html><body></body></html>');
  const source=fs.readFileSync(require.resolve('../js/provider/argentum.js'),'utf8').replace('  window.FocusedMagicArgentum={','  window.__decision=decisionResponse; active={state:{cards:{old:{name:"Mountain"},drawn:{name:"Island"},keep:{name:"Shock"}},players:[],zones:[]}};\n  window.FocusedMagicArgentum={');
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
 }finally{await browser.close();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
