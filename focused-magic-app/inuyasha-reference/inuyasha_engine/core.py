from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum
from typing import Callable, Dict, List, Optional


class RuleError(ValueError):
    """Raised when an attempted game action is illegal under implemented rules."""


class Color(str, Enum):
    RED = "red"
    BLUE = "blue"
    GREEN = "green"
    PURPLE = "purple"
    BLACK = "black"


class TimingWindow(str, Enum):
    CHARACTER_EXPENDED = "when_character_expended"
    WHEN_ATTACKING = "when_attacking"
    WHEN_ATTACKED = "when_attacked"
    COMPARING_COLOR_VALUES = "when_comparing_color_values"
    CHARACTER_DEFEATED = "when_character_defeated"
    JEWEL_SHARD_STOLEN = "when_jewel_shard_stolen"
    ATTACK_END = "when_attack_ends"


@dataclass
class AttachmentState:
    title: str
    face_up: bool = True
    expended: bool = False

    def defeat_with_host(self) -> None:
        # ARD Attack Timing Sequence: defeated character is turned facedown,
        # expended "with all its attached cards."
        self.face_up = False
        self.expended = True


@dataclass
class CharacterState:
    title: str
    controller_id: str
    color_values: Dict[Color, int]
    ready: bool = True
    face_up: bool = True
    in_play: bool = True
    defeated: bool = False
    attachments: List[AttachmentState] = field(default_factory=list)
    temporary_color_modifiers: Dict[Color, int] = field(default_factory=dict)
    constant_color_modifiers: Dict[Color, int] = field(default_factory=dict)

    @property
    def expended(self) -> bool:
        return not self.ready

    def final_color_value(self, color: Color) -> int:
        """Return the current final value for one color.

        This intentionally keeps printed/base values separate from modifiers so
        later constant/floating/temporary effects can plug into the same path.
        """
        if color not in self.color_values:
            raise RuleError(f"{self.title} has no implemented {color.value} value")
        return (
            self.color_values[color]
            + self.temporary_color_modifiers.get(color, 0)
            + self.constant_color_modifiers.get(color, 0)
        )

    def expend(self) -> None:
        if not self.ready:
            raise RuleError(f"{self.title} is already expended")
        self.ready = False

    def defeat(self) -> None:
        self.defeated = True
        self.face_up = False
        self.ready = False
        for attachment in self.attachments:
            attachment.defeat_with_host()


@dataclass
class JewelShard:
    """Opaque shard-pile object.

    The ARD says players may know shard-pile counts but may not look at their
    facedown shard cards. v0.1 therefore moves opaque shard objects without
    exposing an identity to the attack resolver.
    """

    opaque_id: str


@dataclass
class PlayerState:
    player_id: str
    jewel_shards: List[JewelShard] = field(default_factory=list)


@dataclass
class WindowEvent:
    window: TimingWindow
    attack_id: int
    active_player_id: str
    attacker_title: str
    defender_title: str
    color: Color
    detail: Optional[str] = None


@dataclass
class AttackContext:
    attack_id: int
    turn_active_player_id: str
    attacking_player_id: str
    attacker: CharacterState
    defender: CharacterState
    color: Color
    attacker_final_value: Optional[int] = None
    defender_final_value: Optional[int] = None
    defender_defeated_by_attack: bool = False
    shard_stolen: bool = False
    ended: bool = False


WindowHandler = Callable[["GameState", AttackContext, WindowEvent], None]


@dataclass
class GameState:
    players: Dict[str, PlayerState]
    turn_active_player_id: str
    characters: List[CharacterState] = field(default_factory=list)
    event_log: List[WindowEvent] = field(default_factory=list)
    _window_handlers: Dict[TimingWindow, List[WindowHandler]] = field(default_factory=dict)
    _next_attack_id: int = 1

    def player(self, player_id: str) -> PlayerState:
        try:
            return self.players[player_id]
        except KeyError as exc:
            raise RuleError(f"Unknown player: {player_id}") from exc

    def register_window_handler(self, window: TimingWindow, handler: WindowHandler) -> None:
        """Register a hook for later triggered/response-effect implementation."""
        self._window_handlers.setdefault(window, []).append(handler)

    def _open_window(self, ctx: AttackContext, window: TimingWindow, detail: Optional[str] = None) -> None:
        event = WindowEvent(
            window=window,
            attack_id=ctx.attack_id,
            active_player_id=ctx.attacking_player_id,
            attacker_title=ctx.attacker.title,
            defender_title=ctx.defender.title,
            color=ctx.color,
            detail=detail,
        )
        self.event_log.append(event)
        for handler in list(self._window_handlers.get(window, [])):
            handler(self, ctx, event)

    def _validate_basic_attack(self, attacker: CharacterState, defender: CharacterState, color: Color) -> None:
        # This is deliberately only the minimum legality required by the first
        # ARD attack-timing slice. Target restrictions, traits, "cannot attack",
        # missing defenders, etc. are later-rule work and are not guessed here.
        if attacker not in self.characters or defender not in self.characters:
            raise RuleError("Both attacker and defender must be in the game state's play area")
        if not attacker.in_play or not defender.in_play:
            raise RuleError("Both attacker and defender must be in play when the attack is declared")
        if attacker.controller_id == defender.controller_id:
            raise RuleError("v0.1 only permits attacks against an opposing character")
        if attacker.defeated or not attacker.face_up:
            raise RuleError("A defeated/facedown character cannot declare this v0.1 attack")
        if defender.defeated or not defender.face_up:
            raise RuleError("A defeated/facedown character cannot be the v0.1 defender")
        if not attacker.ready:
            raise RuleError("The attacking character must be ready before it is expended to attack")
        if color not in attacker.color_values or color not in defender.color_values:
            raise RuleError("Both test characters need an implemented value for the chosen attack color")

    def _steal_one_opaque_shard(self, thief_id: str, victim_id: str) -> bool:
        thief = self.player(thief_id)
        victim = self.player(victim_id)
        if not victim.jewel_shards:
            return False
        # The attack section says to steal a shard but does not specify a visible
        # choice procedure. Because shards are opaque here, moving one element
        # does not reveal card identity. Later setup/shard rules may refine how
        # this facedown object is selected without changing attack semantics.
        shard = victim.jewel_shards.pop()
        thief.jewel_shards.append(shard)
        return True

    def attack(self, attacker: CharacterState, defender: CharacterState, color: Color) -> AttackContext:
        """Resolve the ARD v4.0 Attack Timing Sequence implemented by v0.1.

        Implemented sequence:
        1) declare attacker, defender, color
        2) expend attacker; open expend window
        3) attack begins; open when-attacking / when-attacked windows
        4) compare final values; open comparison window
        5) if attacker >= defender, defeat defender + attachments; open defeat window
        6) if defeated by attack, steal one opponent shard; open shard-stolen window
        7) attack ends; open attack-end window

        During this attack, the attacking player is the active player for attack
        windows; turn_active_player_id is retained separately and never mutated.
        """
        self._validate_basic_attack(attacker, defender, color)

        ctx = AttackContext(
            attack_id=self._next_attack_id,
            turn_active_player_id=self.turn_active_player_id,
            attacking_player_id=attacker.controller_id,
            attacker=attacker,
            defender=defender,
            color=color,
        )
        self._next_attack_id += 1

        # Declare attack -> expend attacker.
        attacker.expend()
        self._open_window(ctx, TimingWindow.CHARACTER_EXPENDED)

        # Attack begins.
        self._open_window(ctx, TimingWindow.WHEN_ATTACKING)
        self._open_window(ctx, TimingWindow.WHEN_ATTACKED)

        # Resolve attack. The ARD contains one internally inconsistent phrase
        # ("defending character's attacking color value") after previously saying
        # "defending character's final color value." v0.1 uses the chosen color's
        # final value for both characters and records the wording issue in README.
        ctx.attacker_final_value = attacker.final_color_value(color)
        ctx.defender_final_value = defender.final_color_value(color)
        self._open_window(
            ctx,
            TimingWindow.COMPARING_COLOR_VALUES,
            detail=f"{ctx.attacker_final_value} vs {ctx.defender_final_value}",
        )

        if ctx.attacker_final_value >= ctx.defender_final_value:
            defender.defeat()
            ctx.defender_defeated_by_attack = True
            self._open_window(ctx, TimingWindow.CHARACTER_DEFEATED)

            if self._steal_one_opaque_shard(attacker.controller_id, defender.controller_id):
                ctx.shard_stolen = True
                self._open_window(ctx, TimingWindow.JEWEL_SHARD_STOLEN)

        self._open_window(ctx, TimingWindow.ATTACK_END)
        ctx.ended = True
        return ctx
