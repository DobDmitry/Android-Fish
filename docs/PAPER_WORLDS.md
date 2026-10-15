# Бумажные NPC, Дом, Пруд и Марс

Версия 1.0.21 продолжает стиль бумажного двора. Новый общий атлас содержит Енота Тимофея, Доктора Ёжика, Мурку, Кваку и Винтика; существующий Филин сохранён. Все портреты в диалогах, магазине и меню игр используют ту же графику, что и сцены. Лёгкое дыхание, отражение по направлению и зоны нажатия рассчитаны по пропорциям вырезок.

Три новых панорамы имеют открытые игровые зоны: деревянный пол Дома, водная толща Пруда и терракотовая площадка Марса. Общие бумажные вкладки теперь выделяют текущую локацию. Шапка и нижние кнопки отделены от сцены во всех локациях. Для пруда поверхность воды и точка Кваки перенесены на 30% высоты по новому рисунку; кормление, плавание и сбор монет сохранены. Рыбы получили приглушённую палитру и бумажные крапинки, краб заменён собственным рисунком. На Марсе сохранена низкая гравитация.

## Файлы

`app/src/main/res/drawable-nodpi/paper_npcs.png`, `bg_home_paper.png`, `bg_pond_paper.png`, `bg_mars_paper.png`. Старые фоны сохранены. Графика создана встроенным ImageGen по существующему двору и Филину как стилевым референсам. Альфа-канал атласа проверен: за силуэтами прозрачность нулевая. Границы спрайтов заданы по фактическим силуэтам, включая хвосты, цветок и антенну.

## Проверка

Сборка `assembleDebug lintDebug testDebugUnitTest`; ручная проверка исходных изображений и прозрачности. На телефоне проверить переходы между четырьмя вкладками, панорамирование, касания каждого NPC, портреты в диалогах, кормление рыб, монеты и открытие ухода, отражение персонажей и прыжки на Марсе. Проверка игровых жестов и внешнего вида на устройстве в этой сессии не выполнена.

## Точные запросы ImageGen

### npcPrompt

Create a production game sprite atlas, transparent background, landscape 3 columns x 2 equal rows, five separate full-body characters, sixth cell empty transparent. Reference image is STYLE ONLY: match its tactile layered torn colored paper, softly shaded cutout edges, warm children's picture book faces. Every character fully isolated within its cell with generous transparent margin, feet visible, no overlap, no ground/shadow outside character, no labels, no UI. Row1 left: friendly gray raccoon shopkeeper wearing sage green apron with pocket, striped tail, holding small kraft parcel. Row1 middle: friendly hedgehog doctor, brown layered paper spines, cream face, ivory coat with little mint medical bag, warm caring expression. Row1 right: playful ginger cat with cream muzzle, coral neck bow, holding small colorful pinwheel, cheerful smile. Row2 left: friendly green frog pond host with cream belly, small straw sunhat and lotus flower, webbed feet. Row2 middle: friendly handmade exploration robot made of cream and muted teal cardboard panels, copper joints, glass round eyes, short antenna, small backpack, terracotta boots. Row2 right completely empty. All standing frontal or slight three-quarter, same apparent full-body height. Beautiful richly textured paper collage, not flat vector or plastic 3D. Do not include owl.

### npcCutoutPrompt

Edit this game sprite sheet. Preserve the exact five character designs, faces, paper textures and clothing. Remove ALL backgrounds, colored haze, glows, drop shadows and ground shadows completely to true alpha=0 transparency outside the character silhouettes. Characters must be clean isolated cutouts. Re-layout to strict 3 columns by 2 equal rows on landscape canvas: raccoon top left, hedgehog top middle, cat top right; frog bottom left, robot bottom middle; bottom right empty transparent. Shrink each character slightly to fit entirely inside its cell with 7 percent cell margin all around; do not crop ears pinwheel feet tail antenna or flower. Keep full character silhouettes. No checkerboard pixels, no white or black background, no glow.

### homePrompt

Production game background panorama, landscape 3:2, same tactile layered torn paper children's picture book aesthetic as reference garden (style reference only). A warm cozy playroom inside a woodland cottage: cream plaster-paper walls, curved wooden window with sage curtains showing sunny garden, shelves of books and handmade toys on the far left, low reading sofa on far right, paper garlands and small houseplants around edges. Upper 55 percent holds walls and furniture. Bottom 45 percent is broad uncluttered honey-colored wooden floor with a subtle round pale woven rug, an open stage for moving characters. Camera level straight-on with mild floor perspective, no people no animals no faces no letters no UI or interface. Keep central walking area clear across panorama. Muted cream, sage, honey and terracotta accents, beautiful fibrous paper and handmade collage shadowing, cozy daylight. No harsh black outlines.

### pondPrompt

Production mobile game panoramic background 3:2 landscape. Match reference's richly textured torn-paper collage, warm handmade children's picture book world, but depict a POND CUTAWAY, side-view cross section. Strict layout: top 24 percent above-water landscape, a thin grassy far bank with reeds and paper trees at edges, pale sky; perfectly horizontal water surface at exactly 24 percent of image height. Bottom 76 percent shows clear mint turquoise underwater space with layered translucent paper texture, bright enough to see moving fish added by game. Sandy bottom starts at 84 percent height. Sparse paper pebbles, shells, water plants only along bottom corners; leave large center of water entirely open. At x87percent y24percent a small floating lily pad with its upper surface at waterline for a frog sprite to stand on. No animals, fish, people or characters anywhere, no text, no UI. No big foreground plants obstructing center. Horizontal panorama with continuous water area. Gentle rays beneath surface, cream sandy bottom, sage reeds.

### marsPrompt

Production game panorama landscape 3:2. Match the reference garden's tactile torn-paper collage and richly layered handmade children's storybook illustration, but depict a cozy friendly Mars exploration site. Dusty terracotta, peach, rust and warm cream ground; distant rounded layered paper hills at horizon around 50 percent image height. Upper half pale dusky lavender sky with a few little cut-paper stars, small distant planet, no Earth landscape. Small cream and sage cardboard exploration habitat dome at far left; compact telescope, antenna and stacked supply crates at far right. Bottom 45 percent is broad unobstructed gently rolling terracotta walking ground, only sparse little rocks at extreme edges, shallow round crater near lower corner. Warm welcoming playful space adventure, not harsh realistic desert. No characters, no robots, no people, no animals, no text, no logos, no UI. Keep central play area empty for moving player drawings and robot NPC added by game. Natural paper fibers and subtle layered shadows, no thick black outlines.

