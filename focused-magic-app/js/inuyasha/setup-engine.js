(function(root,factory){const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;else root.InuYashaSetupEngine=api;})(typeof globalThis!=='undefined'?globalThis:this,function(){
'use strict';
const title=c=>String(c.name||'').replace(/’/g,"'");
const name=c=>title(c).split(/[,;]/)[0].trim();
const surname=c=>title(c).slice(name(c).length).replace(/^[,;]\s*/,'');
class SetupStep{
 constructor({players,activePlayerId,onWindow=()=>{}}){this.players=players;this.activePlayerId=activePlayerId;this.opponentId=Object.keys(players).find(id=>id!==activePlayerId);if(Object.keys(players).length!==2)throw Error('Set Up requires two players');this.onWindow=onWindow;this.phase='DRAW';this.actor=null;this.counts=Object.fromEntries(Object.keys(players).map(id=>[id,0]));this.once=new Set();this.events=[];this.revealed=[];this.cutOffers=[];}
 window(window,playerId,cardId=null){const event={window,activePlayerId:this.activePlayerId,subjectPlayerId:playerId,cardId};this.events.push(event);this.onWindow(event,this);}
 start(drawComplete){if(!drawComplete||this.phase!=='DRAW')throw Error('Complete Draw before starting Set Up');this.phase='START_EFFECTS';for(const p of [this.activePlayerId,this.opponentId]){this.actor=p;this.window('start_of_setup',p);}this.phase='PLAY';this.actor=this.activePlayerId;}
 requireActor(p){if(this.phase!=='PLAY'||this.actor!==p)throw Error('Wait for your Set Up block');}
 finish(p){this.requireActor(p);if(p===this.activePlayerId){this.actor=this.opponentId;return;}this.phase='END_EFFECTS';for(const actor of [this.activePlayerId,this.opponentId]){this.actor=actor;this.window('end_of_setup',actor);}this.phase='BATTLE_START';this.actor=null;}
 all(){return Object.values(this.players).flatMap(p=>['library','hand','play','discard'].flatMap(z=>p[z]));}
 find(id){const c=this.all().find(c=>c.instanceId===id);if(!c)throw Error('Card is no longer available');return c;}
 controlled(p){return this.players[p].play.filter(c=>c.cardType==='Character'&&c.faceUp&&!c.defeated);}
 attachments(host){return this.all().filter(c=>c.zone==='play'&&c.attachedTo===host.instanceId);}
 hasShroom(p){return this.players[p].play.some(c=>title(c)==="Shippo's Shroom"&&c.attachedTo);}
 unique(c,p){return name(c).toLowerCase()==='shippo'&&this.hasShroom(p)?false:c.unique;}
 move(c,dest,owner=c.owner){for(const p of Object.values(this.players))for(const z of ['library','hand','play','discard']){const i=p[z].indexOf(c);if(i>=0)p[z].splice(i,1);}this.players[owner][dest].push(c);c.owner=owner;c.zone=dest;}
 leave(c,dest='discard'){for(const a of this.attachments(c)){a.attachedTo=null;this.move(a,'discard',a.originalOwner||a.owner);}this.move(c,dest,c.originalOwner||c.owner);c.ready=true;c.faceUp=true;c.defeated=false;}
 attachable(c,host,p,{enemy=false,moving=null}={}){if(host.cardType!=='Character'||!host.faceUp||host.defeated||host.zone!=='play'||(!enemy&&host.owner!==p))throw Error('Choose a legal faceup character');if(this.attachments(host).some(a=>a.cardType==='Item'&&a!==moving))throw Error('That character already has an item');if(c.canAttach&&!c.canAttach(host,p))throw Error('Printed attachment restriction failed');if(c.unique&&this.players[p].play.some(a=>a!==moving&&a.cardType==='Item'&&title(a)===title(c)))throw Error('Unique item already controlled');}
 play(p,id,opts={}){this.requireActor(p);const c=this.find(id),side=this.players[p];if(c.owner!==p||c.zone!=='hand')throw Error('Choose a card in this player’s hand');const kind=opts.cardType||c.cardType,cost=opts.deckCost??c.printedDeckCost,unique=opts.unique??c.unique;
 if(!['Character','Item','Location','Event'].includes(kind))throw Error('Confirm the printed card type');if(!Number.isInteger(cost)||cost<0)throw Error('Confirm the printed deck cost');if(side.library.length<cost)throw Error('Not enough cards to pay the full deck cost');if(['Character','Item'].includes(kind)&&typeof unique!=='boolean')throw Error('Confirm whether the card has Unique');if(c.canPlay&&!c.canPlay(this,p))throw Error('Printed play restriction failed');
 const tiara=title(c)==="Chokyukai's Tiara";if((kind==='Character'||tiara)&&this.counts[p]>=2)throw Error('You already played two Characters this turn');let host=null,old=null,pool=null,results=[],moving=null,moveSource=null;
 if(kind==='Character'&&(unique&&!(name(c).toLowerCase()==='shippo'&&this.hasShroom(p)))){old=this.controlled(p).find(x=>name(x)===name(c));if(old&&surname(old)===surname(c))throw Error('Cannot overlay the same surname');}
 if(kind==='Location'&&side.play.some(x=>x.cardType==='Location'&&x.faceUp&&title(x)===title(c)))throw Error('Only one copy of each Location is allowed');
 if(kind==='Item'){host=this.find(opts.hostId);if(tiara){if(!this.controlled(p).some(x=>name(x).toLowerCase()==='chokyukai'))throw Error('Tiara requires Chokyukai');if(host.owner===p)throw Error('Tiara attaches to an opponent’s Character');}this.attachable({...c,unique},host,p,{enemy:tiara});}
 if(kind==='Event'){
  if(title(c)==='Following the Scent'){if(this.once.has(p+':scent'))throw Error('Following the Scent is once per turn');host=this.find(opts.returnId);if(!this.controlled(p).includes(host))throw Error('Return a Character you control');if(!['library','discard'].includes(opts.searchZone))throw Error('Choose deck or discard');pool=side[opts.searchZone];results=(opts.searchIds||[]).map(i=>this.find(i));if(results.length>3||new Set(results).size!==results.length||results.some(x=>x.cardType!=='Location'||!pool.includes(x)))throw Error('Choose up to three Locations from the chosen zone');if(opts.searchZone==='library'&&results.some(x=>side.library.slice(0,cost).includes(x)))throw Error('Those cards will be discarded as the deck cost; choose other results');}
  else if(title(c)==="Shippo's Shroom"){host=this.find(opts.hostId);if(!this.controlled(p).includes(host)||name(host).toLowerCase()!=='shippo')throw Error('Attach Shroom to your Shippo');}
  else if(title(c)==="Shippo's Item Technique"){host=this.find(opts.hostId);moving=this.find(opts.itemId);moveSource=this.find(moving.attachedTo);if(moving.owner!==p||moving.cardType!=='Item'||moving.zone!=='play'||host===moveSource)throw Error('Move an attached item you control to another Character');this.attachable(moving,host,p,{moving});}
  else throw Error('This Event has no implemented Set Up exception');
 }
 for(const paid of side.library.slice(0,cost))this.move(paid,'discard');c.cardType=kind;c.printedDeckCost=cost;c.unique=unique;c.originalOwner??=p;
 if(kind==='Character'){if(old)this.leave(old);this.move(c,'play');this.counts[p]++;}
 else if(kind==='Location')this.move(c,'play');
 else if(kind==='Item'){this.move(c,'play');c.attachedTo=host.instanceId;if(tiara){this.move(host,'play',p);for(const a of this.attachments(host))this.move(a,'play',p);this.counts[p]++;}}
 else if(title(c)==='Following the Scent'){this.leave(host,'hand');for(const found of results)this.move(found,'hand');this.revealed.push(results.map(x=>x.name));if(opts.searchZone==='library'){const shuffled=opts.shuffle?opts.shuffle(side.library):(()=>{const a=[...side.library];for(let i=a.length-1;i>0;i--){const j=Math.floor(Math.random()*(i+1));[a[i],a[j]]=[a[j],a[i]];}return a;})();side.library=shuffled||side.library;this.cutOffers.push({from:p,to:Object.keys(this.players).find(id=>id!==p)});}this.once.add(p+':scent');this.move(c,'discard');}
 else if(title(c)==="Shippo's Shroom"){this.move(c,'play');c.attachedTo=host.instanceId;}
 else {moving.attachedTo=host.instanceId;this.move(c,'discard');}
 if(kind==='Character'&&c.whenPlayed)c.whenPlayed(this,p,c);if(kind!=='Event')this.window('when_'+kind.toLowerCase()+'_played',p,c.instanceId);if(kind!=='Character'&&kind!=='Event'&&c.whenPlayed)c.whenPlayed(this,p,c);return c;
 }
 removeAttachment(id,{byEffect=true}={}){const c=this.find(id);if(!c.attachedTo)throw Error('Card is not attached');if(byEffect&&title(c)==="Shippo's Shroom")throw Error('Effects cannot discard Shroom from Shippo');const host=this.find(c.attachedTo);c.attachedTo=null;this.move(c,'discard',c.originalOwner||c.owner);if(title(c)==="Chokyukai's Tiara"){this.move(host,'play',host.originalOwner||host.owner);for(const a of this.attachments(host))this.move(a,'play',host.owner);}}
}
return {VERSION:'0.3.0',SetupStep};
});
