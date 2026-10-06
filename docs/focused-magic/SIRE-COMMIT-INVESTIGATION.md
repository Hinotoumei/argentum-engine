# Sire of Insanity / Commit investigation

The user reported Tamiyo returned to hand by Commit // Memory and surviving Sire's end-step discard. After clarification, the user accepted that Commit's library placement may explain the observation. No card behavior was changed on the basis of this report.

Native regressions cover both active players' end steps, Tamiyo genuinely bounced with Unsummon (discarded), and Tamiyo tucked by Commit (remains second from top while both hands are discarded).

Live isolated checks on deployed revision 55e96003891a4c934bf7f2021b00405bc985ef57: Unsummon returned Tamiyo to hand, then Sire discarded it. In the Commit trace, Tamiyo changed Battlefield -> Library; at the opponent's next turn Forest and the opponent's initial Island were in their respective graveyards, while Tamiyo remained in the library. It was drawn on a later turn, then discarded at that turn's end in the pass-only fixture.

The initial Commit browser assertion required observing a transient zero-hand state before the automatic next draw, and failed because state updates crossed that interval. That assertion is not reported as a passing acceptance test. The recorded zone transitions and graveyard contents corroborate the native regression instead.

Validation: all four SireOfInsanityScenarioTest cases passed; targeted native build successful.
