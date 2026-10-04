'use strict';
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const root=path.resolve(__dirname,'../..');
const context={window:{addEventListener(){}},document:{},console,setTimeout,clearTimeout};vm.createContext(context);
vm.runInContext(fs.readFileSync(path.join(root,'focused-magic-app/js/provider/argentum.js'),'utf8'),context);
const api=context.window.FocusedMagicArgentum;
const normalize=s=>s.toLowerCase().replace(/[^a-z0-9]/g,'');
function files(dir){return fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?(e.name==='build'?[]:files(path.join(dir,e.name))):[path.join(dir,e.name)]);}
const kotlin=files(path.join(root,'mtg-sets')).filter(f=>f.endsWith('.kt'));
const definitions=new Map();
for(const f of kotlin.filter(f=>f.includes(`${path.sep}main${path.sep}`))){
 const source=fs.readFileSync(f,'utf8');
 for(const m of source.matchAll(/\b(?:card|basicLand)\("([^"]+)"\)/g))definitions.set(normalize(m[1]),path.relative(root,f));
}
const tests=kotlin.filter(f=>f.includes(`${path.sep}test${path.sep}`));
const profiles=['october','mandatory','legacy'].flatMap(key=>[
 [key,api.FIXTURES[key].user],[key+'-ai',api.FIXTURES[key].ai]
]);
const decks=Object.fromEntries(profiles.map(([key,source])=>{
 const deck=api.parseDecklist(source);
 return [key,{mainCount:deck.mainCount,sideboardCount:deck.sideboardCount,cards:deck.uniqueNames.map(name=>{
  const canonical=api.providerCardName(name),norm=normalize(canonical);
  const scenarioFiles=tests.filter(f=>normalize(path.basename(f,'.kt')).startsWith(norm+'scenario')||normalize(path.basename(f,'.kt'))===norm+'test').map(f=>path.relative(root,f));
  return {name,canonical,definition:definitions.get(norm)||null,scenarioFiles,gameplayStatus:'not established by this source audit'};
 })}];
}));
const report={scope:'Source and test inventory only; existence is not gameplay verification',decks};
fs.writeFileSync(path.join(root,'docs/focused-magic/deck-source-audit.json'),JSON.stringify(report,null,2)+'\n');
for(const [key,deck] of Object.entries(decks))console.log(key+': '+deck.cards.length+' identities; missing definitions: '+deck.cards.filter(c=>!c.definition).map(c=>c.name).join(', '));
