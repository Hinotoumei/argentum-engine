(function(root){
'use strict';
const card=(name,mana_cost,type_line,oracle_text,extra={})=>Object.assign({
  scryfall_id:'',name,set:'local',collector_number:'',cn:0,rarity:'rare',mana_cost,cmc:0,
  type_line,oracle_text,power:null,toughness:null,loyalty:null,colors:[],color_identity:[],
  keywords:[],produced_mana:[],image_uri:'',card_faces:[],released_at:'',layout:'normal'
},extra);
root.FocusedMagicMandatoryLocalDB={
  'bloodstained mire': card('Bloodstained Mire','', 'Land',
    '{T}, Pay 1 life, Sacrifice this land: Search your library for a Swamp or Mountain card, put it onto the battlefield, then shuffle.',
    {produced_mana:[]}),
  'desperate ritual': card('Desperate Ritual','{1}{R}','Instant — Arcane',
    'Add {R}{R}{R}.\nSplice onto Arcane {1}{R}',
    {cmc:2,colors:['R'],color_identity:['R'],rarity:'common'}),
  'exclusive nightclub': card('Exclusive Nightclub','', 'Land',
    'This land enters tapped.\nWhen this land enters from a graveyard, you lose 2 life.\n{T}: Add {U}, {B}, or {R}.\nMayhem',
    {produced_mana:['U','B','R']}),
  'faithless looting': card('Faithless Looting','{R}','Sorcery',
    'Draw two cards, then discard two cards.\nFlashback {2}{R}',
    {cmc:1,colors:['R'],color_identity:['R'],rarity:'uncommon'}),
  'gemstone caverns': card('Gemstone Caverns','', 'Legendary Land',
    "If this card is in your opening hand and you're not the starting player, you may begin the game with Gemstone Caverns on the battlefield with a luck counter on it. If you do, exile a card from your hand.\n{T}: Add {C}. If Gemstone Caverns has a luck counter on it, instead add one mana of any color.",
    {produced_mana:['C']}),
  "jace's machinations": card("Jace's Machinations",'{2}{U}','Instant',
    'Until end of turn, you may activate loyalty abilities of Jace planeswalkers you control on any player’s turn any time you could cast an instant.\nEmpower Jace 8.',
    {cmc:3,colors:['U'],color_identity:['U']}),
  'kasmina, enigma sage': card('Kasmina, Enigma Sage','{1}{G}{U}','Legendary Planeswalker — Kasmina',
    'Each other planeswalker you control has the loyalty abilities of Kasmina.\n+2: Scry 1.\n−X: Create a 0/0 green and blue Fractal creature token. Put X +1/+1 counters on it.\n−8: Search your library for an instant or sorcery card that shares a color with this planeswalker, exile that card, then shuffle. You may cast that card without paying its mana cost.',
    {cmc:3,loyalty:'2',colors:['G','U'],color_identity:['G','U'],rarity:'mythic'}),
  'manamorphose': card('Manamorphose','{1}{R/G}','Instant',
    'Add two mana in any combination of colors.\nDraw a card.',
    {cmc:2,colors:['R','G'],color_identity:['R','G'],rarity:'uncommon'}),
  'monument to endurance': card('Monument to Endurance','{3}','Artifact',
    "Whenever you discard a card, choose one that hasn't been chosen this turn —\n• Draw a card.\n• Create a Treasure token.\n• Each opponent loses 3 life.",
    {cmc:3,color_identity:[]}),
  'reweave': card('Reweave','{5}{U}','Instant — Arcane',
    "Target permanent's controller sacrifices it. If the player does, they reveal cards from the top of their library until they reveal a permanent card that shares a card type with the sacrificed permanent, put that card onto the battlefield, then shuffle.\nSplice onto Arcane {2}{U}{U}",
    {cmc:6,colors:['U'],color_identity:['U']}),
  'starting town': card('Starting Town','', 'Land — Town',
    "This land enters tapped unless it's your first, second, or third turn of the game.\n{T}: Add {C}.\n{T}, Pay 1 life: Add one mana of any color.",
    {produced_mana:['C','W','U','B','R','G']}),
  'way of the pyromancer': card('Way of the Pyromancer','{1}{R}','Legendary Enchantment',
    'When Way of the Pyromancer enters, empower Jace 2.\nPlaneswalkers you control have "[+1]: Add {R}."',
    {cmc:2,colors:['R'],color_identity:['R']})
};
})(typeof globalThis!=='undefined'?globalThis:this);
