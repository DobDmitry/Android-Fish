# Paper play and expressions — 1.0.22

- Replaced binary 130 ms blinks with a smooth 270 ms close/hold/open envelope. Opening is slower than closing; per-creature timing includes safe handling of negative IDs.
- Eyelids now reveal the original drawing between two closing masks. Fine pencil curves preserve eye width; sleeping and happy eyes have distinct curvature. Lid colour uses the same desaturation filter as a sleeping creature.
- Added shared code-drawn paper artwork for food, hazards, coins, balloons, stars, game icons and memory-card faces. No new raster generation was needed.
- Updated all six games: muted paper backgrounds, grain, cardboard columns, sage bushes, paper cards, headers, score chips and the result sheet. Existing game rules, hit regions, rewards and saved scores remain in place.
- NPCs gently sway around their feet and greet the player with a short nod when their dialog opens.
- Added sparse dust/light in Home, dust and twinkling points on Mars, and surface ripples in Pond. Existing pond bubbles and sleeping breathing remain.

Validation: assembleDebug, lintDebug and testDebugUnitTest. Three new blink regression tests supplement the nine outfit tests. No Android device was connected for runtime visual QA; check blink masks on your own drawings, all six games, and portrait/landscape layouts on device.
