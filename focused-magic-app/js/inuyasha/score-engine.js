(function(root,factory){
  const api=factory();
  if(typeof module==='object'&&module.exports){module.exports=api;}else{root.InuYashaScoreEngine=api;}
})(typeof globalThis!=='undefined'?globalThis:this,function(){
  'use strict';

  class RuleError extends Error {
    constructor(message){super(message);this.name='RuleError';}
  }

  const Color=Object.freeze({RED:'red',BLUE:'blue',GREEN:'green',PURPLE:'purple',BLACK:'black',WHITE:'white'});
  const TimingWindow=Object.freeze({
    CHARACTER_EXPENDED:'when_character_expended',
    WHEN_ATTACKING:'when_attacking',
    WHEN_ATTACKED:'when_attacked',
    COMPARING_COLOR_VALUES:'when_comparing_color_values',
    CHARACTER_DEFEATED:'when_character_defeated',
    JEWEL_SHARD_STOLEN:'when_jewel_shard_stolen',
    ATTACK_END:'when_attack_ends'
  });

  class AttachmentState {
    constructor(title,{faceUp=true,expended=false}={}){this.title=title;this.faceUp=faceUp;this.expended=expended;}
    defeatWithHost(){this.faceUp=false;this.expended=true;}
  }

  class CharacterState {
    constructor({title,controllerId,colorValues,ready=true,faceUp=true,inPlay=true,defeated=false,attachments=[],temporaryColorModifiers={},constantColorModifiers={}}){
      this.title=title;this.controllerId=controllerId;this.colorValues={...colorValues};this.ready=ready;this.faceUp=faceUp;this.inPlay=inPlay;this.defeated=defeated;
      this.attachments=[...attachments];this.temporaryColorModifiers={...temporaryColorModifiers};this.constantColorModifiers={...constantColorModifiers};
    }
    get expended(){return !this.ready;}
    finalColorValue(color){
      if(!(color in this.colorValues))throw new RuleError(`${this.title} has no implemented ${color} value`);
      const value=Number(this.colorValues[color])+Number(this.temporaryColorModifiers[color]||0)+Number(this.constantColorModifiers[color]||0);
      if(!Number.isFinite(value))throw new RuleError('Color values must be finite numbers');
      return Math.max(0,value);
    }
    expend(){if(!this.ready)throw new RuleError(`${this.title} is already expended`);this.ready=false;}
    defeat(){this.defeated=true;this.faceUp=false;this.ready=false;for(const attachment of this.attachments)attachment.defeatWithHost();}
  }

  class JewelShard { constructor(opaqueId){this.opaqueId=opaqueId;} }
  class PlayerState { constructor(playerId,jewelShards=[]){this.playerId=playerId;this.jewelShards=[...jewelShards];} }

  class GameState {
    constructor({players,turnActivePlayerId,characters=[]}){
      this.players={...players};this.turnActivePlayerId=turnActivePlayerId;this.characters=[...characters];this.eventLog=[];this._windowHandlers={};this._nextAttackId=1;
    }
    player(playerId){const p=this.players[playerId];if(!p)throw new RuleError(`Unknown player: ${playerId}`);return p;}
    registerWindowHandler(window,handler){(this._windowHandlers[window]||(this._windowHandlers[window]=[])).push(handler);}
    _openWindow(ctx,window,detail=null){
      const event={window,attackId:ctx.attackId,activePlayerId:ctx.attackingPlayerId,attackerTitle:ctx.attacker.title,defenderTitle:ctx.defender?.title||ctx.defendingPlayerId,color:ctx.color,detail};
      this.eventLog.push(event);for(const handler of [...(this._windowHandlers[window]||[])])handler(this,ctx,event);
    }
    _validateBasicAttack(attacker,defender,color){
      if(!this.characters.includes(attacker)||!this.characters.includes(defender))throw new RuleError("Both attacker and defender must be in the game state's play area");
      if(!attacker.inPlay||!defender.inPlay)throw new RuleError('Both attacker and defender must be in play when the attack is declared');
      if(attacker.controllerId===defender.controllerId)throw new RuleError('v0.1 only permits attacks against an opposing character');
      if(attacker.defeated||!attacker.faceUp)throw new RuleError('A defeated/facedown character cannot declare this v0.1 attack');
      if(defender.defeated||!defender.faceUp)throw new RuleError('A defeated/facedown character cannot be the v0.1 defender');
      if(!attacker.ready)throw new RuleError('The attacking character must be ready before it is expended to attack');
      if(!Object.values(Color).includes(color)||!(color in attacker.colorValues)||!(color in defender.colorValues))throw new RuleError('Choose a color printed on both characters');
    }
    _stealOneOpaqueShard(thiefId,victimId){
      const thief=this.player(thiefId),victim=this.player(victimId);if(!victim.jewelShards.length)return false;
      thief.jewelShards.push(victim.jewelShards.pop());return true;
    }
    attack(attacker,defender,color){
      this._validateBasicAttack(attacker,defender,color);
      const ctx={attackId:this._nextAttackId++,turnActivePlayerId:this.turnActivePlayerId,attackingPlayerId:attacker.controllerId,attacker,defender,color,attackerFinalValue:null,defenderFinalValue:null,defenderDefeatedByAttack:false,shardStolen:false,ended:false};
      attacker.expend();this._openWindow(ctx,TimingWindow.CHARACTER_EXPENDED);
      this._openWindow(ctx,TimingWindow.WHEN_ATTACKING);this._openWindow(ctx,TimingWindow.WHEN_ATTACKED);
      ctx.attackerFinalValue=attacker.finalColorValue(color);ctx.defenderFinalValue=defender.finalColorValue(color);
      this._openWindow(ctx,TimingWindow.COMPARING_COLOR_VALUES,`${ctx.attackerFinalValue} vs ${ctx.defenderFinalValue}`);
      if(ctx.attackerFinalValue>=ctx.defenderFinalValue){
        defender.defeat();ctx.defenderDefeatedByAttack=true;this._openWindow(ctx,TimingWindow.CHARACTER_DEFEATED);
        if(this._stealOneOpaqueShard(attacker.controllerId,defender.controllerId)){ctx.shardStolen=true;this._openWindow(ctx,TimingWindow.JEWEL_SHARD_STOLEN);}
      }
      this._openWindow(ctx,TimingWindow.ATTACK_END);ctx.ended=true;return ctx;
    }
    attackPlayer(attacker,defendingPlayerId,color){
      this.player(defendingPlayerId);this.player(attacker.controllerId);
      if(!this.characters.includes(attacker)||!attacker.inPlay||!attacker.faceUp||attacker.defeated||!attacker.ready)throw new RuleError('Choose a ready character you control in play');
      if(attacker.controllerId===defendingPlayerId)throw new RuleError('You cannot attack yourself');
      if(this.characters.some(c=>c.controllerId===defendingPlayerId&&c.inPlay&&c.faceUp&&!c.defeated))throw new RuleError('A direct attack requires the opponent to control no characters; a color mismatch does not permit it');
      if(!Object.values(Color).includes(color)||!Object.hasOwn(attacker.colorValues,color))throw new RuleError('Choose a color printed on the attacking character');
      const ctx={attackId:this._nextAttackId++,turnActivePlayerId:this.turnActivePlayerId,attackingPlayerId:attacker.controllerId,defendingPlayerId,attacker,defender:null,color,attackerFinalValue:null,defenderFinalValue:null,defenderDefeatedByAttack:false,shardStolen:false,ended:false};
      attacker.expend();this._openWindow(ctx,TimingWindow.CHARACTER_EXPENDED);this._openWindow(ctx,TimingWindow.WHEN_ATTACKING);
      if(this._stealOneOpaqueShard(attacker.controllerId,defendingPlayerId)){ctx.shardStolen=true;this._openWindow(ctx,TimingWindow.JEWEL_SHARD_STOLEN);}
      this._openWindow(ctx,TimingWindow.ATTACK_END);ctx.ended=true;return ctx;
    }
  }

  return {RuleError,Color,TimingWindow,AttachmentState,CharacterState,JewelShard,PlayerState,GameState};
});
