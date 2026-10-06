from __future__ import annotations
from dataclasses import dataclass, field
from enum import Enum
from typing import Callable, Optional
import random

class CardKind(str, Enum):
    CHARACTER = 'Character'
    ITEM = 'Item'
    LOCATION = 'Location'
    EVENT = 'Event'

@dataclass(frozen=True)
class SetupCard:
    card_id: str
    title: str
    kind: CardKind
    deck_cost: int = 0
    unique: bool = False
    character_name: str = ''
    surname: str = ''
    colors: dict = field(default_factory=dict)
    can_attach: Optional[Callable] = None
    can_play: Optional[Callable] = None
    when_played: Optional[Callable] = None

@dataclass
class LocationState:
    card_id: str
    title: str
    controller_id: str
    owner_id: str
    face_up: bool = True

@dataclass
class SetupContext:
    turn_id: int
    active_player_id: str
    opposing_player_id: str
    phase: str = 'START_EFFECTS'
    acting_player_id: Optional[str] = None
    characters_played: dict = field(default_factory=dict)
    once_used: set = field(default_factory=set)
    revealed_searches: list = field(default_factory=list)
    cut_offers: list = field(default_factory=list)

class SetupRulesMixin:
    """ARD Step 2 and the four explicit CRD Setup exceptions. No Battle actions."""
    def _error(self, message):
        from .core import RuleError
        raise RuleError(message)

    def _definition(self, card_id):
        card = self.setup_cards.get(card_id)
        if not isinstance(card, SetupCard):
            self._error('No verified SetupCard definition for ' + str(card_id))
        if not isinstance(card.deck_cost, int) or card.deck_cost < 0:
            self._error('Invalid deck cost')
        return card

    def _character(self, card_id):
        for character in self.characters:
            if character.card_id == card_id:
                return character
        self._error('Unknown character')

    def _controlled(self, player_id):
        return [c for c in self.characters if c.controller_id == player_id and c.face_up and c.in_play]

    def start_setup(self, draw_context):
        from .core import TimingWindow
        if not draw_context.draw_step_complete or draw_context.active_player_id != self.turn_active_player_id:
            self._error('A completed matching Draw Step is required')
        if self.setup_context and draw_context.turn_id <= self.setup_context.turn_id:
            self._error('Set Up already started for this turn')
        opponent = self._opposing_player_id()
        if opponent != draw_context.opposing_player_id:
            self._error('Draw Step players do not match')
        ctx = SetupContext(draw_context.turn_id, self.turn_active_player_id, opponent,
                           characters_played={self.turn_active_player_id: 0, opponent: 0})
        self.setup_context = ctx
        for player_id in (ctx.active_player_id, ctx.opposing_player_id):
            ctx.acting_player_id = player_id
            self._open_turn_window(ctx, TimingWindow.SETUP_START, subject_player_id=player_id)
        ctx.phase = 'PLAY'
        ctx.acting_player_id = ctx.active_player_id
        return ctx

    def finish_setup_actions(self, player_id):
        from .core import TimingWindow
        ctx = self._setup_actor(player_id)
        if player_id == ctx.active_player_id:
            ctx.acting_player_id = ctx.opposing_player_id
            return ctx
        ctx.phase = 'END_EFFECTS'
        for actor in (ctx.active_player_id, ctx.opposing_player_id):
            ctx.acting_player_id = actor
            self._open_turn_window(ctx, TimingWindow.SETUP_END, subject_player_id=actor)
        ctx.phase = 'BATTLE_START'
        ctx.acting_player_id = None
        return ctx

    def _setup_actor(self, player_id):
        ctx = self.setup_context
        if not ctx or ctx.phase != 'PLAY' or ctx.acting_player_id != player_id:
            self._error('It is not this player\'s Set Up action block')
        return ctx

    def _validate_hand_cost(self, player_id, card):
        player = self.player(player_id)
        if card.card_id not in player.hand:
            self._error('Card must be in the acting player\'s hand')
        if len(player.draw_pile) < card.deck_cost:
            self._error('Cannot pay the full deck cost; deck-exhaustion consequence outside this slice')
        if card.can_play and not card.can_play(self, player_id):
            self._error('Card-specific play restriction failed')

    def _pay_and_remove(self, player_id, card):
        player = self.player(player_id)
        player.discard_pile.extend(player.draw_pile[:card.deck_cost])
        del player.draw_pile[:card.deck_cost]
        player.hand.remove(card.card_id)

    def _is_unique_character(self, card, player_id):
        # CRD Shippo's Shroom removes Unique from its controller's Shippos in all zones,
        # and stays active even while its attached Shippo is defeated.
        if card.character_name.casefold() == 'shippo':
            for host in self.characters:
                if host.controller_id == player_id and any(a.title.replace('’', "'") == "Shippo's Shroom" for a in host.attachments):
                    return False
        return card.unique

    def _validate_attachment(self, card, host, player_id, *, opponent_allowed=False, moving=None):
        if not host.in_play or not host.face_up or (host.controller_id != player_id and not opponent_allowed):
            self._error('Cannot attach to this character')
        if any(a.is_item and a is not moving for a in host.attachments):
            self._error('Character already has an item')
        if card.can_attach and not card.can_attach(self, host, player_id):
            self._error('Character cannot legally attach this item')
        if card.unique:
            for character in self._controlled(player_id):
                if any(a.title == card.title and a is not moving for a in character.attachments):
                    self._error('Unique item already controlled; items cannot overlay')

    def _discard_character(self, character):
        self.player(character.owner_id or character.controller_id).discard_pile.append(character.card_id)
        for attachment in character.attachments:
            self.player(attachment.owner_id or character.owner_id or character.controller_id).discard_pile.append(attachment.card_id)
        character.attachments.clear()
        character.in_play = False

    def play_setup_card(self, player_id, card_id, *, host_id=None, return_character_id=None,
                        search_ids=(), search_zone='deck', item_id=None, shuffle=None):
        from .core import CharacterState, AttachmentState, TimingWindow
        ctx = self._setup_actor(player_id)
        card = self._definition(card_id)
        self._validate_hand_cost(player_id, card)
        extra_character = card.title == "Chokyukai's Tiara"
        if card.kind == CardKind.CHARACTER or extra_character:
            if ctx.characters_played[player_id] >= 2:
                self._error('Two Characters per turn limit reached')
        overlay = None
        host = None
        if card.kind == CardKind.CHARACTER:
            if not card.character_name:
                self._error('Character name required independently of surname')
            if self._is_unique_character(card, player_id):
                matches = [c for c in self._controlled(player_id) if self._definition(c.card_id).character_name == card.character_name]
                if len(matches) > 1:
                    self._error('Resolve existing uniqueness violation first')
                if matches:
                    overlay = matches[0]
                    old = self._definition(overlay.card_id)
                    if old.surname == card.surname:
                        self._error('Cannot overlay the same surname')
        elif card.kind == CardKind.LOCATION:
            if any(l.controller_id == player_id and l.face_up and l.title == card.title for l in self.locations):
                self._error('Only one copy of each location; locations cannot overlay')
        elif card.kind == CardKind.ITEM:
            host = self._character(host_id)
            if extra_character:
                if not any(self._definition(c.card_id).character_name.casefold() == 'chokyukai' for c in self._controlled(player_id)):
                    self._error('Tiara requires control of Chokyukai')
                opponent = ctx.opposing_player_id if player_id == ctx.active_player_id else ctx.active_player_id
                if host.controller_id != opponent:
                    self._error('Tiara attaches to an opponent\'s character')
            self._validate_attachment(card, host, player_id, opponent_allowed=extra_character)
        elif card.kind == CardKind.EVENT:
            return self._play_setup_event(player_id, card, host_id, return_character_id, search_ids, search_zone, item_id, shuffle)
        else:
            self._error('Only verified Set Up card types are supported')
        self._pay_and_remove(player_id, card)
        if overlay:
            self._discard_character(overlay)
        if card.kind == CardKind.CHARACTER:
            self.characters.append(CharacterState(card.title, player_id, dict(card.colors), card_id=card_id, owner_id=player_id))
            ctx.characters_played[player_id] += 1
            window = TimingWindow.CHARACTER_PLAYED
        elif card.kind == CardKind.ITEM:
            host.attachments.append(AttachmentState(card.title, card_id=card_id, owner_id=player_id))
            if extra_character:
                host.controller_id = player_id
                ctx.characters_played[player_id] += 1
            window = TimingWindow.ITEM_PLAYED
        else:
            self.locations.append(LocationState(card_id, card.title, player_id, player_id))
            window = TimingWindow.LOCATION_PLAYED
        # Character's own when-played effects precede its response window; item/location
        # response windows precede their own when-played effects (ARD play procedures).
        if card.kind == CardKind.CHARACTER and card.when_played:
            card.when_played(self, player_id, card_id)
        self._open_turn_window(ctx, window, subject_player_id=player_id, detail=card_id)
        if card.kind != CardKind.CHARACTER and card.when_played:
            card.when_played(self, player_id, card_id)
        return card_id

    def _play_setup_event(self, player_id, card, host_id, return_id, search_ids, search_zone, item_id, shuffle):
        from .core import AttachmentState
        ctx = self.setup_context
        if card.title == 'Following the Scent':
            key = (player_id, card.title)
            if key in ctx.once_used:
                self._error('Following the Scent is limited to once per turn')
            host = self._character(return_id)
            if host not in self._controlled(player_id):
                self._error('Return a character you control')
            if search_zone not in ('deck', 'discard'):
                self._error('Search deck or discard, not both')
            pool = self.player(player_id).draw_pile if search_zone == 'deck' else self.player(player_id).discard_pile
            ids = list(search_ids)
            if len(ids) > 3 or len(ids) != len(set(ids)):
                self._error('Choose up to three distinct locations')
            prospective = list(pool) + (self.player(player_id).draw_pile[:card.deck_cost] if search_zone == 'discard' else [])
            if any(i not in prospective or self._definition(i).kind != CardKind.LOCATION for i in ids):
                self._error('Search selections must be locations in the chosen zone')
            # Costs discard top cards first: cannot promise a card removed as deck cost.
            if search_zone == 'deck' and any(i in pool[:card.deck_cost] for i in ids):
                self._error('Choose search results after deck cost; selected card would leave deck')
            self._pay_and_remove(player_id, card)
            host.in_play = False
            self.player(host.owner_id or player_id).hand.append(host.card_id)
            for a in host.attachments:
                self.player(a.owner_id or player_id).discard_pile.append(a.card_id)
            host.attachments.clear()
            for i in ids:
                pool.remove(i)
                self.player(player_id).hand.append(i)
            ctx.once_used.add(key)
            ctx.revealed_searches.append((player_id, ids))
            if search_zone == 'deck':
                (shuffle or random.shuffle)(pool)
                ctx.cut_offers.append((player_id, self._opposing_player_id() if player_id == self.turn_active_player_id else self.turn_active_player_id))
            self.player(player_id).discard_pile.append(card.card_id)
        elif card.title.replace('’', "'") == "Shippo's Shroom":
            host = self._character(host_id)
            if host not in self._controlled(player_id) or self._definition(host.card_id).character_name.casefold() != 'shippo':
                self._error('Shroom attaches to your Shippo')
            self._pay_and_remove(player_id, card)
            host.attachments.append(AttachmentState(card.title, card_id=card.card_id, owner_id=player_id, is_item=False))
        elif card.title == "Shippo's Item Technique":
            target = self._character(host_id)
            found = [(c, a) for c in self._controlled(player_id) for a in c.attachments if a.card_id == item_id and a.is_item]
            if len(found) != 1:
                self._error('Choose an attached item you control')
            source, attachment = found[0]
            if source is target:
                self._error('Choose another character')
            self._validate_attachment(self._definition(item_id), target, player_id, moving=attachment)
            self._pay_and_remove(player_id, card)
            source.attachments.remove(attachment)
            target.attachments.append(attachment)
            self.player(player_id).discard_pile.append(card.card_id)
        else:
            self._error('Event lacks a verified CRD Set Up exception')
        return card.card_id

    def remove_setup_attachment(self, host_id, attachment_id, *, by_effect=False):
        host = self._character(host_id)
        attachment = next((a for a in host.attachments if a.card_id == attachment_id), None)
        if attachment is None:
            self._error('Unknown attachment')
        if by_effect and attachment.title.replace('’', "'") == "Shippo's Shroom":
            self._error("Effects cannot discard Shippo's Shroom from Shippo")
        host.attachments.remove(attachment)
        self.player(attachment.owner_id or host.owner_id or host.controller_id).discard_pile.append(attachment.card_id)
        if attachment.title == "Chokyukai's Tiara":
            host.controller_id = host.owner_id
