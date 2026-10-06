from inuyasha_engine import CharacterState, Color, GameState, JewelShard, PlayerState


def demo_deck(prefix):
    return [f"{prefix}-CARD-{n}" for n in range(1, 11)]


p1 = PlayerState(
    "Player A",
    [JewelShard("A1"), JewelShard("A2")],
    draw_pile=demo_deck("A"),
)
p2 = PlayerState(
    "Player B",
    [JewelShard("B1"), JewelShard("B2")],
    draw_pile=demo_deck("B"),
)

attacker = CharacterState(
    "Demo Attacker",
    "Player A",
    {Color.RED: 5, Color.BLUE: 1, Color.GREEN: 1, Color.PURPLE: 1, Color.BLACK: 1},
)
defender = CharacterState(
    "Demo Defender",
    "Player B",
    {Color.RED: 4, Color.BLUE: 1, Color.GREEN: 1, Color.PURPLE: 1, Color.BLACK: 1},
)

game = GameState(
    players={"Player A": p1, "Player B": p2},
    turn_active_player_id="Player A",
    characters=[attacker, defender],
)

turn = game.start_turn_and_draw()
print(f"turn #{turn.turn_id}: active={turn.active_player_id}")
print(f"active drew: {turn.active_drawn_cards}")
print(f"opponent drew: {turn.opposing_drawn_cards}")
print("turn/draw windows:")
for event in game.event_log:
    print(f"  - {event.window.value}")

# Clear the demonstration log so attack output is easy to read.
game.event_log.clear()
ctx = game.attack(attacker, defender, Color.RED)
print(f"\nattack #{ctx.attack_id}: {attacker.title} -> {defender.title} on {ctx.color.value}")
print(f"comparison: {ctx.attacker_final_value} vs {ctx.defender_final_value}")
print(f"defeated: {ctx.defender_defeated_by_attack}; shard stolen: {ctx.shard_stolen}")
print("attack windows:")
for event in game.event_log:
    print(f"  - {event.window.value}")
