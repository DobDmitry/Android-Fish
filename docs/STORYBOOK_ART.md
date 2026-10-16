# Storybook artwork — 1.0.23

Three painted fish species replace procedural pond fish. A reusable bitmap mesh bends the tail and body; facing interpolates with elapsed frame time. Seven fish keep their existing movement and feeding behavior. Player drawings are unchanged.

Six games use individually composed gouache environments. Food, hazards, rewards, baskets, bushes, balloons and flight pillars use transparent painted sprites. Memory cards use the same artwork and still include the player's drawings. Backgrounds center-crop without stretching; sprite padding is trimmed once when resources load. The decoded game atlas is shared across rounds.

Production resources: `story_fish.png`, `story_scenes.png`, `story_items.png` in `app/src/main/res/drawable-nodpi`. The generated item rows have manually inspected boundaries at 0/272/515/735/1024 pixels. Artwork generated using the built-in ImageGen tool from the approved fish and orchard references; no external stock assets.

Validation: assembleDebug, lintDebug, testDebugUnitTest. Runtime play and performance on a physical Android device still require a device check. Suggested smoke pass: pond turn/feed; Catch basket/items; Jump landing and obstacle edges; Memory matching/reset; Balloon normal/star/storm; Fly passage gaps/ground; Hide bush shuffle/reveal.

# Exact production prompts

## fishProductionPrompt

Edit this approved fish sheet into a production sprite atlas. Preserve the three illustrated fish designs, gouache detail, colors and right-facing profiles. Rearrange into THREE EQUAL HORIZONTAL ROWS on a 1536x1024 canvas: gold carp centered in top third, slender teal fish centered in middle third, coral round goldfish centered in bottom third. Each complete fish including tail fits entirely within its row with at least 25 pixels transparent margin, occupies about 750 pixels width, no overlaps. Remove all cream background, use genuine alpha transparency. No labels or shadows. This atlas will be cropped into three equal rows for animation.

## backgroundProductionPrompt

Use the approved image as a STYLE REFERENCE only. Generate a production environment atlas for six portrait mobile mini games. Landscape canvas 1536x1024 arranged EXACTLY 3 equal columns and 2 equal rows, six rectangular panels with NO gutters, borders or text. Each panel is 512x512, composition designed for center cropping to narrow portrait. Match the reference's beautiful painted gouache children's picture-book orchard, organic detailed foliage, warm sunlight, fine paper grain. NO characters, NO UI, NO floating collectable objects. Top-left: orchard, apple branches confined extreme top corners, distant cottage in bottom right, central quiet blue sky, walking ground horizontal at 90% height. Top-middle: forest running path, open central cream-blue air, distant trees at sides, horizontal grassy ground at 82% height. Top-right: warm wooden tabletop with cream parchment sheet, tiny flower decorations at extreme corners, empty central area for memory cards. Bottom-left: airy flower meadow with lots of open pale sky for balloons, flower border only at bottom 12%. Bottom-middle: open high blue sky and soft clouds with distant hills at bottom 10%, flight obstacles will be added separately. Bottom-right: woodland clearing with unobstructed softly textured grass from 30% to bottom, shrubs and tall trees only at extreme sides, empty center for movable hide-and-seek bushes. Consistent gorgeous illustrated storybook style, no thick vector outlines, no emojis, no labels, no cats, no baskets.

## itemProductionPrompt

Create a production sprite atlas matching the richly painted gouache picture-book reference. EXACT 5 columns and 4 rows on 1536x1024 canvas. Genuine transparent background. Each object centered entirely inside its equal cell with generous 18% empty margins, separate objects never touch. NO text, no grid lines. Order left to right: row1 red apple, pear, carrot, cookie, banana; row2 strawberry, cheese wedge, milk bottle, cake slice, gray rock; row3 bee, golden five-point star, gold coin, wicker catching basket seen from front, dense rounded leafy bush; row4 red balloon with knot but no string, dark storm cloud, cactus, red mushroom, tall ivy covered stone pillar. Warm painterly detailed textures and soft organic edges, highly readable game assets. Single object per cell.

