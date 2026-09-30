# Local supporting checks, September 30

These checks exercise the imported frontend on localhost. They are not deployment verification or user-live A–H acceptance.

- JavaScript syntax: provider, core deck, and InuYasha tabletop passed.
- InuYasha JavaScript parity suite: 11/11 passed.
- Actual browser click path: played Rin, Young Follower and Ginkotsu, Band of Seven; selected attacker and defender; opened Resolve Attack.
- Blank, negative, fractional, exponent, and unsafe-integer attack values disabled confirmation. Negative defender value also disabled it. Zero and valid integer attack values enabled it.
- With explicit test values 7 versus 2, confirmation expended the attacker and defeated/expended the defender.
- Active Core deck and mandatory assessment fixtures now use the printed Gray Ogre name, as approved by the user. The original packet remains preserved unchanged.

The manual 7-versus-2 check proves only numeric input handling. It does not verify printed character colors or attack legality. The user's later Rin/color and no-matching-color reports remain unresolved. Official rules govern character targeting and direct attacks; the frontend must enforce printed shared colors and must not infer permission to attack directly merely from a lack of shared colors.

The recovered Magic backend batch subsequently passed the full regression build excluding accepted legacy mtgish-only tests; the remaining packet cards still need implementation and behavioral verification. Remaining live gates include the exact required-card API check, mandatory Constructed admission/start, existing-service deployment, and user-live InuYasha A–H.
