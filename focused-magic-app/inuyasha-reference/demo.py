from inuyasha_engine import CharacterState, Color, GameState, JewelShard, PlayerState

p1 = PlayerState("Player A", [JewelShard("A1"), JewelShard("A2")])
p2 = PlayerState("Player B", [JewelShard("B1"), JewelShard("B2")])

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
    turn_active_player_id="Player B",
    characters=[attacker, defender],
)

ctx = game.attack(attacker, defender, Color.RED)

print(f"attack #{ctx.attack_id}: {attacker.title} -> {defender.title} on {ctx.color.value}")
print(f"comparison: {ctx.attacker_final_value} vs {ctx.defender_final_value}")
print(f"defeated: {ctx.defender_defeated_by_attack}; shard stolen: {ctx.shard_stolen}")
print("windows:")
for e in game.event_log:
    print(f"  - {e.window.value}")
