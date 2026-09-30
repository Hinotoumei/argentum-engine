# Consign to Memory

Load `consign-to-memory.json` in the Scenario Builder in self-play mode, or POST it to
`http://localhost:8080/api/dev/scenarios`. Requires a backend built with Consign to Memory.

1. Cast Ornithopter. While it is on the stack, cast Consign without replicate targeting it.
   Ornithopter should go to the graveyard and never enter the battlefield.
2. Restart the board. Cast Grizzly Bears. Consign must not offer the colored creature spell
   as a legal target. Resolve Bears to produce the two Soul Warden triggers.
3. While both triggers are on the stack, cast Consign with Replicate ×1 targeting one trigger.
   Pass priority to resolve the replicate trigger; choose the other trigger for the copy.
   After the copy and original resolve, neither Soul Warden should gain life.
4. Restart as needed to inspect the Replicate ×2 and ×3 menu entries. Each must display
   and charge a different total cost and retain its selected count through targeting.

This is a reproducible manual fixture, not a record that these browser steps have passed.
