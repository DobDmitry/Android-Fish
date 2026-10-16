# Storybook fixes — 1.0.24

The runner previously used a perspective path whose painted ground did not match the physics floor. `RunnerScenery.kt` now anchors the inspected grass edge (source y=1176) to the same `JUMP_GROUND_FRACTION` used by landing, obstacles and player placement. The lower soil layer scrolls right-to-left; alternating mirrored tiles avoid a hard texture reset. The distant scenery remains stationary.

Fish no longer interpolate their horizontal scale through zero. `FishTurn` brakes for 0.2 seconds, changes the full-width profile once at rest, and accelerates for 0.2 seconds. Movement follows the displayed profile, including while braking. A short cooldown prevents jitter. Tail mesh amplitude is reduced. Two regression tests cover the profile switch at minimum speed and target changes during a turn.

The procedural seabed crab is replaced with a transparent painted sprite with a subtle walking bob. Final assets are `app/src/main/res/drawable-nodpi/story_runner.png` and `story_crab.png`. Both were made with the built-in ImageGen tool; the crab received a second edit to remove spurious eyes. Alpha is preserved and transparent padding is trimmed at load time.

Physical-device play testing remains pending.

## runner24Prompt

Production background for a SIDE SCROLLING platform running game. Portrait 1024x1536 gouache children's picture book landscape. Warm sunlit woodland seen STRICTLY FROM THE SIDE, blue sky and distant softly painted trees. Bottom 22 percent is solid warm ochre earth with small pebbles, flat straight perfectly HORIZONTAL grassy top edge at exactly 78 percent of image height, from left edge to right edge. That horizontal edge is where a character runs from left to right. Clear continuous walking surface, side elevation view like a theatrical stage. Absolutely NO road receding into the distance, NO winding path, NO perspective road, NO foreground objects above the ground line, NO characters, NO UI, NO text. Upper 78 percent is open air with distant woodland scenery, sparse foliage at extreme sides, quiet pale center. Match the rich painted natural texture, warm colors and fine detail of the reference fish illustration, but create a forest environment only. The grass edge must be clearly visible across the full image at one constant height.

## crab24Prompt

Production game sprite: one charming small pond crab, front view, warm terracotta/coral shell, two claws and eight distinct walking legs, small expressive dark eyes on short stalks. Rich hand-painted gouache children's picture-book illustration matching the attached fish reference (reference for style only). Soft natural painted outlines, warm highlights, delicate speckles, believable anatomy, friendly face. Entire crab centered with 15% transparent padding on all sides, no cropped limbs. Genuine alpha transparent background, no ground, no shadow, no text, no other objects. Landscape image.

## crab24Correction

Edit this crab sprite. Critical anatomy correction: crab must have EXACTLY TWO EYES, both on the existing upper eyestalks. REMOVE the two extra black eye-like circles from the lower face near the mouth, replacing them with ordinary uninterrupted pale cream/coral shell texture. Preserve the two upper eyestalk eyes, smile, entire body, legs and claws, hand-painted gouache style and genuine alpha transparency. No other changes.
