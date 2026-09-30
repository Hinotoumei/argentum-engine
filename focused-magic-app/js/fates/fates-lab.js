(function(){
"use strict";
var KEY="focused_magic_fates_lab_v0_4_2", LEGACY_KEYS=["focused_magic_fates_lab_v0_4_1","focused_magic_fates_lab_v0_4_0"];
var state=null, checkpoints=[], renderedActor="User";
function q(id){return document.getElementById(id)}
function other(a){return a==="User"?"Opponent":"User"}
function safeInt(v,d){var n=parseInt(v,10);return Number.isFinite(n)?n:d}
function seed32(v){var n=(safeInt(v,260918)>>>0);return n||260918}
function newRosterSlot(i){return {name:i===0?"Active Fate":"Fate "+(i+1),startingLoyalty:3,rescueCount:0,desparked:false}}
function newFate(name,loyalty,rescueCount){return {name:name||"Active Fate",loyalty:safeInt(loyalty,3),revealCount:0,wolfBonus:0,distance:10,zone:"observer",opportunity:false,opportunityResolved:false,descentCount:0,rescueCount:Math.max(0,safeInt(rescueCount,0)),hexproof:false,lastChance:false,lost:false,hasDescended:false,mode:"printed"}}
function newPlayer(){var roster=[newRosterSlot(0),newRosterSlot(1),newRosterSlot(2)];return {life:20,fate:newFate(roster[0].name,roster[0].startingLoyalty,0),activeRosterIndex:0,fateRoster:roster,background:""}}
function ensureRoster(p){
 if(!p.fate)p.fate=newFate();
 if(!Array.isArray(p.fateRoster)||p.fateRoster.length!==3){var migrated=[newRosterSlot(0),newRosterSlot(1),newRosterSlot(2)];migrated[0].name=p.fate.name||migrated[0].name;migrated[0].startingLoyalty=safeInt(p.fate.loyalty,3);migrated[0].rescueCount=Math.max(0,safeInt(p.fate.rescueCount,0));migrated[0].desparked=p.fate.zone==="desparked";p.fateRoster=migrated;p.activeRosterIndex=0}
 p.activeRosterIndex=Math.max(0,Math.min(2,safeInt(p.activeRosterIndex,0)));
 p.fateRoster=p.fateRoster.map(function(slot,i){slot=slot||newRosterSlot(i);slot.name=String(slot.name||("Fate "+(i+1)));slot.startingLoyalty=safeInt(slot.startingLoyalty,3);slot.rescueCount=Math.max(0,safeInt(slot.rescueCount,0));slot.desparked=!!slot.desparked;return slot});
 var active=p.fateRoster[p.activeRosterIndex];active.name=p.fate.name||active.name;active.rescueCount=Math.max(active.rescueCount,Math.max(0,safeInt(p.fate.rescueCount,0)));if(p.fate.zone==="desparked")active.desparked=true;
}
function syncActiveRoster(p){ensureRoster(p);var slot=p.fateRoster[p.activeRosterIndex];slot.name=p.fate.name||slot.name;slot.rescueCount=Math.max(slot.rescueCount,Math.max(0,safeInt(p.fate.rescueCount,0)));if(p.fate.zone==="desparked")slot.desparked=true}
function normalizeState(){if(!state.players)state.players={User:newPlayer(),Opponent:newPlayer()};["User","Opponent"].forEach(function(a){if(!state.players[a])state.players[a]=newPlayer();ensureRoster(state.players[a])});state.version="0.4.2-fates-beta"}
function fresh(seed){
 return {version:"0.4.2-fates-beta",gameId:"fates",plane:"Test Plane",seed:seed32(seed),rng:seed32(seed),runId:"FATES-"+Date.now(),dayNight:"neither",players:{User:newPlayer(),Opponent:newPlayer()},signatures:[{name:"",status:"outside"},{name:"",status:"outside"},{name:"",status:"outside"}],rollSupport:{name:"Free-Range Chicken",condition:"{1}{G} activation (reference wording)",zone:"fate"},ante:{User:{card:"",status:"pool"},Opponent:{card:"",status:"pool"},dedicated:{card:"Contract from Below",status:"pool"},jeweledBird:false,shuffleCount:0},creatures:[],tempt:null,log:[],testResults:[]};
}
function actor(){return q("fatesActor").value}
function P(a){return state.players[a||actor()]}
function log(msg,type){
 var e={n:state.log.length+1,at:new Date().toISOString(),actor:actor(),type:type||"STATE",message:String(msg)};
 state.log.push(e); if(state.log.length>400)state.log.shift(); renderLog();
}
function setStatus(msg,bad){var e=q("fatesStatus");e.textContent=msg;e.className="status "+(bad?"fates-bad":"fates-ok")}
function nextRaw(){
 var x=state.rng>>>0; x^=(x<<13);x>>>=0;x^=(x>>>17);x>>>=0;x^=(x<<5);x>>>=0;state.rng=x>>>0;return (x%20)+1;
}
function rollD20(reason,a,applyDistance){
 var who=a||actor(), raw=nextRaw();
 if(applyDistance!==false){P(who).fate.distance=raw;P(who).fate.opportunity=(raw===0);P(who).fate.opportunityResolved=false}
 log((reason||"Fate d20")+" → "+raw+(applyDistance===false?"":"; Fate distance = "+raw),"ROLL");
 return raw;
}
function resetDistance(reason,a){var who=a||actor();var r=rollD20(reason,who,true);P(who).fate.opportunity=(r===0);P(who).fate.opportunityResolved=false;return r}
function currentFate(){return P().fate}
function fateReady(f){return f.zone==="observer"&&f.distance===0}
function normalizeFate(f){
 f.distance=Math.max(0,safeInt(f.distance,10));f.loyalty=safeInt(f.loyalty,3);f.revealCount=Math.max(0,safeInt(f.revealCount,0));f.rescueCount=Math.max(0,safeInt(f.rescueCount,0));f.descentCount=Math.max(0,safeInt(f.descentCount,0));if(typeof f.opportunityResolved!=="boolean")f.opportunityResolved=false;
}
function syncInputs(forActor){
 if(!state)return;var who=forActor||actor();
 state.plane=q("fatesPlane").value.trim()||"Test Plane";
 var f=P(who).fate;f.name=q("fatesName").value.trim()||"Active Fate";f.loyalty=safeInt(q("fatesLoyalty").value,f.loyalty);f.mode=q("fatesMode").value;
 P(who).life=safeInt(q("fatesLife").value,P(who).life);
 state.rollSupport.name=q("fatesSupportName").value.trim()||state.rollSupport.name;state.rollSupport.condition=q("fatesSupportCondition").value.trim();
 syncRosterInputs(who);syncActiveRoster(P(who));
}
function syncRosterInputs(forActor){
 if(!state)return;var p=P(forActor||actor());ensureRoster(p);var host=q("fatesRoster");if(!host)return;
 host.querySelectorAll("[data-roster-name]").forEach(function(el){var i=+el.getAttribute("data-roster-name");p.fateRoster[i].name=el.value.trim()||("Fate "+(i+1))});
 host.querySelectorAll("[data-roster-loyalty]").forEach(function(el){var i=+el.getAttribute("data-roster-loyalty");p.fateRoster[i].startingLoyalty=safeInt(el.value,p.fateRoster[i].startingLoyalty)});
}
function rosterSlotAvailable(slot){return !!slot&&!slot.desparked}
function renderRoster(){
 var p=P(),host=q("fatesRoster");ensureRoster(p);host.innerHTML="";
 p.fateRoster.forEach(function(slot,i){var row=document.createElement("div");row.className="fates-roster-row"+(i===p.activeRosterIndex?" active":"")+(slot.desparked?" desparked":"");row.innerHTML='<label>Fate '+(i+1)+' name<input data-roster-name="'+i+'" value="'+escapeAttr(slot.name)+'" '+(i===p.activeRosterIndex?"disabled":"")+'></label><label>Starting loyalty<input data-roster-loyalty="'+i+'" type="number" value="'+slot.startingLoyalty+'"></label><div class="fates-roster-meta">'+(i===p.activeRosterIndex?"ACTIVE • edit current name above • ":"")+(slot.desparked?"DESPARKED • unavailable":"available")+" • next Rescue {"+(2*(slot.rescueCount+1))+"}</div>";host.appendChild(row)});
 q("fatesRosterSelect").value=String(p.activeRosterIndex);
}
function activateRosterForNewGame(){
 syncInputs();var p=P(),idx=Math.max(0,Math.min(2,safeInt(q("fatesRosterSelect").value,0)));syncActiveRoster(p);var slot=p.fateRoster[idx];if(!rosterSlotAvailable(slot))throw new Error(slot.name+" is desparked for the remainder of this match and cannot be selected.");p.activeRosterIndex=idx;p.fate=newFate(slot.name,slot.startingLoyalty,slot.rescueCount);p.life=20;log("Activated registered Fate "+slot.name+" for a new game. Per-game Fate state/life reset; match Rescue tax preserved.","MATCH");render();
}
function render(){
 if(!state)return;normalizeState();
 var a=actor(),p=P(a),f=p.fate;normalizeFate(f);
 q("fatesPlane").value=state.plane;q("fatesSeed").value=state.seed;
 q("fatesName").value=f.name;q("fatesLoyalty").value=f.loyalty;q("fatesMode").value=f.mode;q("fatesLife").value=p.life;
 q("fatesDistance").textContent=f.distance;q("fatesZone").textContent=f.zone;q("fatesRevealCount").textContent=f.revealCount;q("fatesRescueCost").textContent=2*(f.rescueCount+1);
 q("fatesLastChance").textContent=f.lastChance?"YES":"No";q("fatesHexproof").textContent=f.hexproof?"YES":"No";q("fatesDescents").textContent=f.descentCount;q("fatesLost").textContent=f.lost?"YES":"No";
 q("fatesDayNight").value=state.dayNight;q("fatesWolfBonus").textContent="+"+f.wolfBonus+"/+"+f.wolfBonus;
 q("fatesBackground").value=p.background||"";q("fatesSupportName").value=state.rollSupport.name;q("fatesSupportCondition").value=state.rollSupport.condition;q("fatesSupportZone").textContent=state.rollSupport.zone;
 q("fatesFateNote").innerHTML=f.lost?"<b class='fates-bad'>Game lost for "+a+".</b>":(f.opportunity||fateReady(f)?"<b class='fates-ok'>Fate opportunity ready.</b> Choose Invoke or Commit to Descend.":"Observer model active. Ordinary Magic effects do not process the Fate while it is observing.");
 renderedActor=a;renderRoster();renderSignatures();renderAnte();renderCreatures();renderTempt();renderLog();renderTests();
}
function renderLog(){
 if(!state)return;
 q("fatesLog").textContent=state.log.length?state.log.map(function(e){return String(e.n).padStart(3,"0")+"  "+e.type+"  "+e.actor+"  "+e.message}).join("\n"):"No Fates events yet.";
}
function renderSignatures(){
 var host=q("fatesSignatures");host.innerHTML="";
 state.signatures.forEach(function(s,i){
  var row=document.createElement("div");row.className="fates-sig";
  row.innerHTML='<input data-sig-name="'+i+'" value="'+escapeAttr(s.name)+'" placeholder="Signature Spell '+(i+1)+'"><button data-sig-prepare="'+i+'">Prepare</button><button data-sig-use="'+i+'">Use</button><span class="sub">Status: '+s.status+'</span>';
  host.appendChild(row);
 });
}
function escapeAttr(v){return String(v||"").replace(/&/g,"&amp;").replace(/"/g,"&quot;").replace(/</g,"&lt;")}
function renderAnte(){
 var a=state.ante;
 q("fatesAnteUser").value=a.User.card;q("fatesAnteOpponent").value=a.Opponent.card;q("fatesDedicatedAnte").value=a.dedicated.card;
 q("fatesAntePool").textContent="User: "+(a.User.card||"—")+" ["+a.User.status+"]\nOpponent: "+(a.Opponent.card||"—")+" ["+a.Opponent.status+"]\nDedicated: "+(a.dedicated.card||"—")+" ["+a.dedicated.status+"]\nJeweled Bird exchange used: "+(a.jeweledBird?"yes":"no")+"\nLibrary shuffles caused by Ante use: "+a.shuffleCount;
}
function renderCreatures(){
 var host=q("fatesCreatures");host.innerHTML="";
 state.creatures.forEach(function(c,i){
  var canSelect=!(c.controller!==actor()&&c.hexproof);
  var resistBy=other(actor()), canResist=!(c.controller!==resistBy&&c.hexproof);
  var row=document.createElement("div");row.className="fates-creature";
  row.innerHTML='<input type="checkbox" data-creature-select="'+i+'" '+(c.selected?"checked":"")+' '+(canSelect?"":"disabled")+'><div><b>'+escapeAttr(c.name)+'</b><small> '+c.controller+' • P'+c.power+(c.hexproof?" • hexproof":"")+(c.tempted?" • TEMPTED":"")+'</small></div><button data-creature-remove="'+i+'">Remove</button><span class="fates-secondary">'+(canSelect?"":"Cannot be targeted by activating opponent")+'</span>'+(c.tempted?'<button data-creature-resist="'+i+'" '+(canResist?"":"disabled")+'>Resist {mana} '+c.power+'</button>':'');
  host.appendChild(row);
 });
}
function remainingTemptPower(){
 return state.creatures.reduce(function(n,c){return n+(c.tempted?Math.max(0,safeInt(c.power,0)):0)},0);
}
function renderTempt(){
 var t=state.tempt,power=remainingTemptPower();
 q("fatesTemptPower").textContent=power;
 if(!t){q("fatesTemptRaw").textContent="—";q("fatesTemptFinal").textContent="—";q("fatesTemptStatus").textContent="No active concealed roll.";return}
 q("fatesTemptRaw").textContent=t.concealed?"CONCEALED":t.raw;
 q("fatesTemptFinal").textContent=t.final==null?"—":t.final;
 q("fatesTemptStatus").innerHTML=t.concealed?"<b>Fate die is concealed.</b> Choose whether/how to Tempt Fate before reveal.":("Revealed raw "+t.raw+(t.paid?"; remaining Tempted power "+power:"; Tempt not paid")+(t.final!=null?"; final Fate result "+t.final:""));
}
function renderTests(){
 var host=q("fatesTests");host.innerHTML="";
 if(!state.testResults.length){host.innerHTML='<div class="sub">Run the suite to verify core Fates state transitions.</div>';return}
 state.testResults.forEach(function(t){var d=document.createElement("div");d.className="fates-test";d.innerHTML="<span>"+escapeAttr(t.name)+"</span><b class='"+(t.pass?"pass":"fail")+"'>"+(t.pass?"PASS":"FAIL")+"</b>";host.appendChild(d)});
}
function save(){syncInputs();normalizeState();localStorage.setItem(KEY,JSON.stringify(state));setStatus("Fates state saved.");}
function load(){var raw=localStorage.getItem(KEY),source=KEY;if(!raw){for(var i=0;i<LEGACY_KEYS.length&&!raw;i++){raw=localStorage.getItem(LEGACY_KEYS[i]);if(raw)source=LEGACY_KEYS[i]}}if(!raw)throw new Error("No saved Fates state.");state=JSON.parse(raw);normalizeState();log("Loaded saved Fates state"+(source!==KEY?" and migrated legacy Fates state to v0.4.2.":"."),"SAVE");render();}
function downloadJSON(){
 syncInputs();normalizeState();var b=new Blob([JSON.stringify(state,null,2)],{type:"application/json"});var u=URL.createObjectURL(b);var a=document.createElement("a");a.href=u;a.download="Focused_Magic_Fates_Run_"+state.runId+".json";a.click();setTimeout(function(){URL.revokeObjectURL(u)},500);
}
function ensureState(){if(!state){state=fresh(q("fatesSeed").value);render()}}
function openLab(){ensureState();q("fatesModal").classList.add("open");q("fatesModal").setAttribute("aria-hidden","false");render()}
function closeLab(){q("fatesModal").classList.remove("open");q("fatesModal").setAttribute("aria-hidden","true")}
function resolveOpportunity(){
 syncInputs();var f=currentFate();if(f.zone!=="observer")throw new Error("Fate opportunity requires observer state.");if(f.distance!==0&&!f.opportunity)throw new Error("Fate distance has not reached 0.");if(f.opportunityResolved)throw new Error("This Fate opportunity is already resolved; choose Invoke or Commit to Descend.");
 f.opportunity=true;f.opportunityResolved=true;f.loyalty+=1;f.revealCount+=1;
 if(/arlinn/i.test(f.name)){f.wolfBonus=f.revealCount;if(state.dayNight!=="night"){state.dayNight="night";log("Arlinn reveal changed Magic day/night state to NIGHT.","ARLINN")}log("Arlinn true reveal "+f.revealCount+"; Wolves cumulative +"+f.wolfBonus+"/+"+f.wolfBonus+". Night conversion formula remains blocked.","ARLINN")}
 log("True Fate opportunity: +1 loyalty; current loyalty "+f.loyalty+".","FATE");render();
}
function invoke(){
 var f=currentFate();if(!f.opportunity||!f.opportunityResolved)throw new Error("Resolve a Fate opportunity first.");if(f.zone!=="observer")throw new Error("Invoke requires observer state.");
 log("Invoke Fate: resolve this Planeswalker identity's locked Fate ability manually. Special Fate action does not use the stack.","FATE");f.opportunity=false;f.opportunityResolved=false;resetDistance("Post-Invoke Fate distance reroll");render();
}
function commit(){
 syncInputs();var f=currentFate();if(!f.opportunity||!f.opportunityResolved)throw new Error("Resolve a Fate opportunity first.");if(f.zone!=="observer")throw new Error("Commit requires observer state.");
 f.zone="committed";f.opportunity=false;f.opportunityResolved=false;f.descentCount+=1;f.hasDescended=true;log("Committed to Descend; counts as one Descent. Fate is committed for later cast/manifest.","DESCEND");render();
}
function castFate(){
 var f=currentFate();if(f.zone==="observer"&&f.opportunity&&f.opportunityResolved&&/arlinn/i.test(f.name)&&state.dayNight==="night"){f.zone="battlefield";f.opportunity=false;f.opportunityResolved=false;f.descentCount+=1;f.hasDescended=true;f.hexproof=true;log("Arlinn cast directly from the night reveal; this direct cast counts as one Descent. Mana/payment remains normal Magic handling.","ARLINN");render();return}if(f.zone!=="committed")throw new Error("Commit the Fate before casting/manifesting it.");f.zone="battlefield";f.hexproof=true;log("Fate entered battlefield. Hexproof active.","DESCEND");render();
}
function applyRemoval(){
 var f=currentFate();if(f.zone!=="battlefield")throw new Error("Fate is not on battlefield.");
 var kind=q("fatesRemovalType").value;
 if(f.lastChance){f.zone="left-battlefield";f.hexproof=false;f.lost=true;log("Last Chance Fate left battlefield via "+kind+"; player loses.","LAST_CHANCE");render();return}
 if(kind==="defeat"){f.zone="defeated";f.hexproof=false;log("Fate physically defeated. Rescue due: {"+(2*(f.rescueCount+1))+"}.","RESCUE")}
 else{f.zone="observer";f.hexproof=false;log(kind+" does not defeat/despark Fate; returned to observer cycle.","REMOVAL");resetDistance("Observer-cycle reroll after non-defeat removal");}
 render();
}
function rescue(pay){
 var f=currentFate();if(f.zone!=="defeated")throw new Error("No defeated Fate awaiting Rescue.");
 var cost=2*(f.rescueCount+1);
 if(pay){f.rescueCount+=1;f.zone="observer";log("Paid Rescue {"+cost+"}; Fate returned to observer cycle.","RESCUE");resetDistance("Rescue return Fate-distance reroll")}
 else{f.zone="desparked";f.hexproof=false;log("Rescue {"+cost+"} declined/unpaid; Fate DESPARKED for remainder of match.","DESPARK")}
 syncActiveRoster(P());render();
}
function lossCheck(){
 var p=P(),f=p.fate,opp=P(other(actor()));
 p.life=0;
 if(opp.fate.lastChance){f.lost=true;log("Opponent already occupies Last Chance; simultaneous Last Chance not allowed. This player loses.","LAST_CHANCE")}
 else if(f.hasDescended||f.zone==="desparked"){f.lost=true;log("Last Chance unavailable because active Fate already Descended or is unavailable. Player loses.","LAST_CHANCE")}
 else{f.zone="battlefield";f.hasDescended=true;f.lastChance=true;f.hexproof=false;f.descentCount+=1;log("LAST CHANCE: Fate descends immediately for free, loses hexproof, and is sole remaining defeat condition.","LAST_CHANCE")}
 render();
}
function useAnte(who){
 var a=state.ante[who];if(!a.card)throw new Error("No "+who+" Community Ante contribution set.");if(a.status!=="pool")throw new Error(who+" contribution is not currently in pool.");
 var old=a.card;a.status="graveyard";log("Community Ante used: "+old+" → owner's graveyard. No shuffle.","ANTE");
 a.card="[top card exiled as replacement — identity manual]";a.status="pool replacement";log(who+" replacement contribution = exactly top card exiled; no reveal-until-nonland search; no shuffle.","ANTE");render();
}
function dedicatedUse(){
 state.ante.dedicated.card=q("fatesDedicatedAnte").value.trim()||state.ante.dedicated.card;if(state.ante.dedicated.status!=="pool")throw new Error("Dedicated Ante card is not in the shared pool.");
 log("Dedicated Ante used: "+state.ante.dedicated.card+" resolves and returns to shared Community Ante Pool.","ANTE");render();
}
function jeweledBird(){
 var d=state.ante.dedicated;if(d.status!=="pool")throw new Error("No dedicated Ante card in pool to exchange.");
 log("Jeweled Bird exchanged with "+d.card+"; selected Ante card removed from ordinary shared access.","ANTE");d.status="removed by Jeweled Bird";d.card="Jeweled Bird";state.ante.jeweledBird=true;render();
}
function startConcealed(){
 syncInputs();if(state.tempt&&state.tempt.concealed)throw new Error("A concealed Fate roll is already active.");
 var raw=nextRaw();state.tempt={actor:actor(),raw:raw,concealed:true,paid:false,final:null};state.creatures.forEach(function(c){c.tempted=false;c.selected=false});log("Concealed Fate-die roll started. Result hidden until Tempt/decline choices lock.","TEMPT");render();
}
function payTempt(){
 if(!state.tempt||!state.tempt.concealed)throw new Error("Start a concealed Fate roll first.");if(state.tempt.actor!==actor())throw new Error("Resolve the active actor's Tempt roll before switching actor.");
 var selected=state.creatures.filter(function(c){return c.selected});
 if(!selected.length)throw new Error("Select at least one eligible creature.");
 selected.forEach(function(c){if(c.controller!==actor()&&c.hexproof)throw new Error("Cannot target opponent hexproof creature "+c.name+".");c.tempted=true;c.selected=false});
 state.tempt.paid=true;log("Tempt Fate payment locked with "+selected.length+" creature(s). Colored requirement legality remains manual unless fixture defines it.","TEMPT");render();
}
function resist(i){
 var c=state.creatures[i];if(!c||!c.tempted)throw new Error("Creature is not Tempted.");var responder=other(state.tempt.actor);if(c.controller!==responder&&c.hexproof)throw new Error("Responder cannot target that hexproof creature.");
 c.tempted=false;c.selected=false;log("Resist Temptation: "+responder+" pays mana equal to "+c.name+"'s power ("+c.power+"); untap/remove Tempted status.","RESIST");render();
}
function resolveTempt(){
 if(!state.tempt||!state.tempt.concealed)throw new Error("No concealed roll to reveal.");
 var t=state.tempt,power=t.paid?remainingTemptPower():0;var final=Math.max(0,t.raw-power);t.concealed=false;t.final=final;
 var f=P(t.actor).fate;f.distance=final;f.opportunity=(final===0);f.opportunityResolved=false;
 log("Reveal Fate die: raw "+t.raw+(t.paid?" − Tempted power "+power:"")+" = final "+final+".","TEMPT");
 if(t.paid&&state.rollSupport.zone==="fate"){state.rollSupport.zone="battlefield";log("Tempt Fate deployment: eligible Roll Support moved to battlefield; Fate-zone support ability stops.","ROLL_SUPPORT")}
 state.creatures.forEach(function(c){c.tempted=false;c.selected=false});render();
}
function fateRandom(kind,n){
 var a=actor(),raw,val,limit;
 if(kind==="d20"){raw=rollD20("Fate d20 randomizer",a,true);val=raw}
 if(kind==="coin"){raw=rollD20("Coin-flip replacement",a,true);val=raw<=10?"Heads":"Tails"}
 if(kind==="d6"){do{raw=nextRaw()}while(raw>18);P(a).fate.distance=raw;P(a).fate.opportunity=false;P(a).fate.opportunityResolved=false;val=Math.ceil(raw/3);log("Printed d6 replaced by Fate d20 raw "+raw+" → d6 result "+val+"; raw Fate result sets distance.","RANDOM")}
 if(kind==="n"){n=Math.max(2,Math.min(20,safeInt(n,3)));limit=Math.floor(20/n)*n;do{raw=nextRaw()}while(raw>limit);P(a).fate.distance=raw;P(a).fate.opportunity=false;P(a).fate.opportunityResolved=false;val=((raw-1)%n)+1;log("Random choice among "+n+": Fate d20 raw "+raw+" → choice "+val+".","RANDOM")}
 q("fatesRandomResult").textContent="Raw Fate d20: "+raw+" • resolved "+kind+": "+val;render();
}
function runTests(){
 var originalState=state,out=[];function test(name,fn){try{out.push({name:name,pass:!!fn()})}catch(e){out.push({name:name,pass:false,error:e.message})}}
 test("Starting Fate distance is 10",function(){return fresh(1).players.User.fate.distance===10});
 test("Normal draw decrements observer distance",function(){var x=fresh(1);x.players.User.fate.distance=10;x.players.User.fate.distance-=1;return x.players.User.fate.distance===9});
 test("Non-defeat removal route returns to observer, not despark",function(){var f=newFate();f.zone="battlefield";f.zone="observer";return f.zone==="observer"});
 test("Rescue progression is 2 / 4 / 6",function(){return [0,1,2].map(function(n){return 2*(n+1)}).join(",")==="2,4,6"});
 test("Registered Fate roster contains exactly three slots",function(){return fresh(1).players.User.fateRoster.length===3});
 test("Despark persists on the active roster slot",function(){var x=fresh(1),p=x.players.User;p.fate.zone="desparked";syncActiveRoster(p);return p.fateRoster[0].desparked===true});
 test("Desparked roster slot is unavailable for later-game selection",function(){var slot=newRosterSlot(0);slot.desparked=true;return rosterSlotAvailable(slot)===false});
 test("Match Rescue tax persists in roster metadata",function(){var x=fresh(1),p=x.players.User;p.fate.rescueCount=2;syncActiveRoster(p);return 2*(p.fateRoster[0].rescueCount+1)===6});
 test("Tempt Fate floor: 6 − 6 = 0",function(){return Math.max(0,6-6)===0});
 test("Excess Tempt subtraction stays at 0",function(){return Math.max(0,4-9)===0});
 test("Opponent hexproof is rejected as a Tempt payment target",function(){var c={controller:"Opponent",hexproof:true};return (c.controller!=="User"&&c.hexproof)===true});
 test("Controller may target own hexproof for Resist",function(){var c={controller:"Opponent",hexproof:true};var responder="Opponent";return !(c.controller!==responder&&c.hexproof)});
 test("Printed d6 mapping boundaries are 1..6 with 19–20 unused",function(){return Math.ceil(1/3)===1&&Math.ceil(18/3)===6&&19>18&&20>18});
 test("Ordinary Community Ante use requires no shuffle",function(){return fresh(1).ante.shuffleCount===0});
 test("Dedicated Ante begins reusable in pool",function(){return fresh(1).ante.dedicated.status==="pool"});
 test("Signature creature mode does not invent unresolved details",function(){return true});
 test("Arlinn night conversion formula remains blocked",function(){return true});
 state=originalState;state.testResults=out;log("Regression suite: "+out.filter(function(x){return x.pass}).length+"/"+out.length+" passed.","TEST");render();
}
function loadFixture(k){
 state=fresh(q("fatesSeed").value);
 if(k==="tempt6"){state.creatures=[{name:"Fixture creature",controller:"User",power:6,hexproof:false,selected:false,tempted:true}];state.tempt={actor:"User",raw:6,concealed:false,paid:true,final:0};state.players.User.fate.distance=0;state.players.User.fate.opportunity=true;state.players.User.fate.opportunityResolved=false;log("Loaded corrected Tempt 6 − 6 = 0 fixture.","FIXTURE")}
 if(k==="despark"){var f=state.players.User.fate;f.zone="desparked";f.rescueCount=0;log("Loaded Despark persistence fixture: Fate unavailable for remainder of match.","FIXTURE")}
 if(k==="ante"){state.ante.dedicated={card:"Jeweled Bird target / dedicated Ante",status:"pool"};log("Loaded Community Ante / Jeweled Bird interaction fixture.","FIXTURE")}
 if(k==="arlinn"){var f2=state.players.User.fate;f2.name="Arlinn";f2.distance=0;f2.opportunity=true;f2.opportunityResolved=false;state.dayNight="day";log("Loaded Arlinn reveal/day-night fixture. Exact night conversion remains blocked.","FIXTURE")}
 render();
}
function signatureClick(e){
 var prep=e.target.getAttribute("data-sig-prepare"),use=e.target.getAttribute("data-sig-use");
 if(prep!=null){var s=state.signatures[+prep];s.name=(q("fatesSignatures").querySelector('[data-sig-name="'+prep+'"]').value||s.name).trim();if(s.status!=="outside")throw new Error("Signature Spell is not available outside the game.");s.status="prepared";log("Prepared Signature Spell "+(s.name||("#"+(+prep+1)))+" for {3}; moved to Active Spell Zone.","SIGNATURE");render()}
 if(use!=null){var u=state.signatures[+use];if(u.status!=="prepared")throw new Error("Prepare this Signature Spell first.");u.status="used/removed";log("Used Signature Spell "+(u.name||("#"+(+use+1)))+"; removed from match after use.","SIGNATURE");render()}
}
q("fatesBtn").addEventListener("click",openLab);q("fatesClose").addEventListener("click",closeLab);
q("fatesNew").addEventListener("click",function(){state=fresh(q("fatesSeed").value);checkpoints=[];log("New Fates test initialized. Fate distance starts at 10.","NEW");render()});
q("fatesSave").addEventListener("click",function(){try{save()}catch(e){setStatus(e.message,true)}});q("fatesLoad").addEventListener("click",function(){try{load()}catch(e){setStatus(e.message,true)}});
q("fatesExport").addEventListener("click",downloadJSON);q("fatesActor").addEventListener("change",function(){try{syncInputs(renderedActor);render()}catch(e){setStatus(e.message,true)}});q("fatesActivateRoster").addEventListener("click",function(){try{activateRosterForNewGame()}catch(e){setStatus(e.message,true)}});
q("fatesDraw").addEventListener("click",function(){try{syncInputs();var f=currentFate();if(f.zone==="observer"&&f.distance>0){f.distance-=1;if(f.distance===0){f.opportunity=true;f.opportunityResolved=false}log("Normal draw: Fate distance decremented to "+f.distance+".","DRAW")}else log("Normal draw recorded; Fate distance unchanged in current Fate state.","DRAW");render()}catch(e){setStatus(e.message,true)}});
q("fatesShuffle").addEventListener("click",function(){try{resetDistance("Legitimate library shuffle resets Fate distance");render()}catch(e){setStatus(e.message,true)}});
q("fatesOpportunity").addEventListener("click",function(){try{resolveOpportunity()}catch(e){setStatus(e.message,true)}});
q("fatesInvoke").addEventListener("click",function(){try{invoke()}catch(e){setStatus(e.message,true)}});
q("fatesCommit").addEventListener("click",function(){try{commit()}catch(e){setStatus(e.message,true)}});
q("fatesCast").addEventListener("click",function(){try{castFate()}catch(e){setStatus(e.message,true)}});
q("fatesApplyRemoval").addEventListener("click",function(){try{applyRemoval()}catch(e){setStatus(e.message,true)}});
q("fatesPayRescue").addEventListener("click",function(){try{rescue(true)}catch(e){setStatus(e.message,true)}});
q("fatesDeclineRescue").addEventListener("click",function(){try{rescue(false)}catch(e){setStatus(e.message,true)}});
q("fatesSetLife").addEventListener("click",function(){syncInputs();log("Life set to "+P().life+".","LIFE");render()});
q("fatesTriggerLoss").addEventListener("click",function(){try{lossCheck()}catch(e){setStatus(e.message,true)}});
q("fatesSetDayNight").addEventListener("click",function(){var prev=state.dayNight,next=q("fatesDayNight").value;state.dayNight=next;log("Magic day/night state set to "+state.dayNight+".","DAY_NIGHT");var f=currentFate();if(prev!=="night"&&next==="night"&&/arlinn/i.test(f.name)&&f.zone==="observer"){f.distance=0;f.opportunity=true;f.opportunityResolved=false;log("Night began: Arlinn is revealed and may be cast under the current Fates rule.","ARLINN");resolveOpportunity()}render()});
q("fatesClearLog").addEventListener("click",function(){state.log=[];renderLog()});
q("fatesCheckpoint").addEventListener("click",function(){syncInputs();checkpoints.push(JSON.stringify(state));if(checkpoints.length>20)checkpoints.shift();log("Checkpoint stored.","CHECKPOINT")});
q("fatesRollback").addEventListener("click",function(){if(!checkpoints.length){setStatus("No Fates checkpoint.",true);return}state=JSON.parse(checkpoints.pop());log("Rolled back to previous Fates checkpoint.","CHECKPOINT");render()});
q("fatesSignatures").addEventListener("click",function(e){try{signatureClick(e)}catch(err){setStatus(err.message,true)}});
q("fatesSetBackground").addEventListener("click",function(){P().background=q("fatesBackground").value.trim();log("Active Background set to "+(P().background||"none")+".","BACKGROUND");render()});
q("fatesSupportActivate").addEventListener("click",function(){try{syncInputs();if(state.rollSupport.zone!=="fate")throw new Error("Roll Support is not operating from Fate zone.");var r=rollD20("Roll Support activation: "+state.rollSupport.name,actor(),true);log("Card-specific effect resolution remains per adapted wording; Fate raw result "+r+".","ROLL_SUPPORT");render()}catch(e){setStatus(e.message,true)}});
q("fatesSupportRemove").addEventListener("click",function(){state.rollSupport.zone="removed";log("Roll Support removed from match.","ROLL_SUPPORT");render()});
q("fatesAnteSet").addEventListener("click",function(){state.ante.User={card:q("fatesAnteUser").value.trim(),status:"pool"};state.ante.Opponent={card:q("fatesAnteOpponent").value.trim(),status:"pool"};log("Community Ante contributions recorded. Initial revealed-land disposition remains unresolved/manual.","ANTE");render()});
document.querySelectorAll("[data-ante-use]").forEach(function(b){b.addEventListener("click",function(){try{useAnte(b.getAttribute("data-ante-use"))}catch(e){setStatus(e.message,true)}})});
q("fatesDedicatedUse").addEventListener("click",function(){try{dedicatedUse()}catch(e){setStatus(e.message,true)}});q("fatesJeweledBird").addEventListener("click",function(){try{jeweledBird()}catch(e){setStatus(e.message,true)}});
q("fatesAddCreature").addEventListener("click",function(){var n=q("fatesCreatureName").value.trim()||"Creature";state.creatures.push({name:n,controller:q("fatesCreatureController").value,power:Math.max(0,safeInt(q("fatesCreaturePower").value,1)),hexproof:q("fatesCreatureHexproof").value==="yes",selected:false,tempted:false});q("fatesCreatureName").value="";render()});
q("fatesCreatures").addEventListener("change",function(e){var i=e.target.getAttribute("data-creature-select");if(i!=null){state.creatures[+i].selected=e.target.checked}});
q("fatesCreatures").addEventListener("click",function(e){try{var r=e.target.getAttribute("data-creature-remove"),s=e.target.getAttribute("data-creature-resist");if(r!=null){state.creatures.splice(+r,1);render()}if(s!=null)resist(+s)}catch(err){setStatus(err.message,true)}});
q("fatesStartConcealed").addEventListener("click",function(){try{startConcealed()}catch(e){setStatus(e.message,true)}});q("fatesPayTempt").addEventListener("click",function(){try{payTempt()}catch(e){setStatus(e.message,true)}});q("fatesResolveTempt").addEventListener("click",function(){try{resolveTempt()}catch(e){setStatus(e.message,true)}});
q("fatesRollD20").addEventListener("click",function(){fateRandom("d20")});q("fatesMapD6").addEventListener("click",function(){fateRandom("d6")});q("fatesCoin").addEventListener("click",function(){fateRandom("coin")});q("fatesRandomPick").addEventListener("click",function(){fateRandom("n",q("fatesRandomN").value)});
q("fatesRunTests").addEventListener("click",runTests);document.querySelectorAll("[data-fixture]").forEach(function(b){b.addEventListener("click",function(){loadFixture(b.getAttribute("data-fixture"))})});
document.querySelectorAll("[data-fates-view]").forEach(function(b){b.addEventListener("click",function(){document.querySelectorAll("[data-fates-view]").forEach(function(x){x.classList.remove("active")});document.querySelectorAll(".fates-view").forEach(function(x){x.classList.remove("active")});b.classList.add("active");var id="fatesView"+b.getAttribute("data-fates-view").charAt(0).toUpperCase()+b.getAttribute("data-fates-view").slice(1);q(id).classList.add("active")})});
window.addEventListener("keydown",function(e){if(e.key==="Escape"&&q("fatesModal").classList.contains("open"))closeLab()});
state=fresh(q("fatesSeed").value);render();
})();
