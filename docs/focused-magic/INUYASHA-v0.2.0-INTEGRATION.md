# InuYasha v0.2.0 integration

Source: user-supplied INUYASHA_SCORE_RULES_ENGINE_v0.2.0.zip. Ported turn start/draw sequence and tests into the preserved Python reference and JavaScript engine. Existing custom combat fixes are retained rather than replacing the engine wholesale. Both exact draws are prevalidated before mutations, and browser duplicate draw prevention keeps turn-active identity until manual finish turn.

Verified: 22 Python cases, 18 JavaScript cases, real Chrome initial draw, duplicate rejection and P2 draw (inuyasha-v020-browser.json). Full setup, Set Up, Recovery and card text are outside this slice. Existing manual draw/play/ready controls remain explicit diagnostics. Public deployment requires replacement ZIP upload.
