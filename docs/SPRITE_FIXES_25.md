# Sprite fixes — 1.0.25

The phone screenshot exposed an opaque dark matte in the previous crab PNG. Replaced it with a new, simpler painted crab that reads at small sizes. Saved production sprite: `app/src/main/res/drawable-nodpi/story_crab.png`.

Verified the final sprite composited on turquoise, including gaps between the claws and body. Background probes have alpha 0 (one corner is 1/255), versus the old matte reaching 241/255. The final image has no visible dark rectangle. QA composites are previews only; the production PNG keeps its original generated alpha.

The cactus is now cropped from its inspected bounds (655, 738, 235, 270), excluding the neighboring storm cloud. Removed the platform emoji dust emitted by JumpGame at the left-side player position; it resembled stray debris. Other gameplay effects and collision rules are unchanged.

Artwork created with the built-in ImageGen tool. Final prompt:

A clean isolated 2D game sprite of a cute small terracotta orange crab. Hand-painted children's storybook gouache, simple rounded shell, exactly two small dark eyes, two clearly readable claws and short legs, friendly and understated, not photorealistic and not shiny. Designed to read clearly at only 100 pixels wide. Entire crab centered with generous empty margins. TRANSPARENT BACKGROUND: a cutout sticker, only the crab exists, all surrounding space and spaces between legs must be completely empty alpha zero. No lighting effects, no colored backdrop, no gradient, no vignette, no halo, no glow, no cast shadow, no floor, no border, no text. Square canvas.

Validation: assembleDebug, lintDebug, testDebugUnitTest; visual sprite compositing and cactus crop inspection. No physical-device run in this environment.
