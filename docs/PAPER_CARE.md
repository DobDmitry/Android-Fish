# Бумажная комната питомца

Экран ухода продолжает стиль «бумажного двора»: комната с ковриком, Nunito, мягкие панели и собственные иллюстрации.

## Изменения

- Отдельный фон комнаты без интерфейса и персонажей. Рисунок ребёнка и существующая скелетная анимация сохранены.
- Один общий атлас 4×4: десять видов еды и шесть иконок действий/показателей. Картинка декодируется один раз на экран.
- Четыре компактные шкалы с плавным изменением значений и доступными для озвучивания названиями. Уровень перенесён в заголовок.
- Лоток еды содержит названия, количество и выделение любимой еды. В сцену летит соответствующая иллюстрация.
- При мытье появляются полупрозрачные пузырьки с бликами. Усилие мытья учитывает плотность экрана. Спящего питомца сначала нужно разбудить.
- Сон плавно затемняет комнату за 900 мс; появляются луна и мерцающие звёзды. При пробуждении свет возвращается плавно.
- Имя, движения, переселение и отпускание питомца доступны через «Ещё» в заголовке. Подтверждение отпускания сохранено.
- На небольших экранах и при увеличенном шрифте вся страница прокручивается; сцена остаётся в ограниченной по высоте колонке.

Экономика, инвентарь, сохранения, начисление опыта и результаты кормления используют прежние методы GameViewModel.

## Проверка

Сборка `assembleDebug lintDebug` для версии 1.0.17 прошла успешно. Android Lint завершился без ошибок, подпись APK проверена. Подключённого устройства или эмулятора не было, поэтому внешний вид и жесты на устройстве ещё не проверены.

Проверить на телефоне:

1. Открыть уход из двора, погладить питомца и вернуться назад.
2. Покормить обычной и любимой едой; проверить уменьшение количества, сытость и полёт нужной картинки.
3. Проверить пустой инвентарь, сытого и спящего питомца.
4. Включить мытьё, провести по персонажу: пузырьки и чистота меняются; проведение вне персонажа не моет его.
5. Уложить спать и разбудить; проверить, что во сне нельзя кормить и мыть.
6. Открыть игры и наряды. Через «Ещё» проверить имя, движения и переселение; отменить отпускание.
7. Проверить маленький экран, увеличенный системный шрифт и озвучивание показателей.

Компиляция и статическая проверка не заменяют проверку графики и жестов на устройстве.

## Графика и запросы

Изображения созданы встроенным ImageGen и сохранены в `app/src/main/res/drawable-nodpi`. Фон непрозрачный, атлас имеет настоящий alpha-канал. Текст не запечён в изображения.

### bg_care_paper.png

Production background for a children's Android pet care game, living handmade paper picture book style: textured cream paper, cut paper layers, soft watercolor gouache, sage mint and warm coral accents. Square image. Cozy room with warm cream wall, small window with sage curtains high on left, tiny shelf and framed drawing high on right, warm oak plank floor starting at 60% image height. Large pale mint oval rug centered low, center of rug at x50% y83%, for a large animated child drawing character. Small cozy empty bed tucked into far right edge, small wash basin tucked into far left edge, furniture stays in outermost 20% of image and mostly above rug. Spacious unobstructed center from x25% to75%, y25% to90%. Keep room detail restrained and subordinate to the live character. Soft tactile paper textures and contact shadows between paper layers, sunny gentle light. Full bleed game background only. NO characters, animals, text, UI, buttons, icons, speech bubbles, logos or watermark. Optimized for center crop across portrait and nearly square scene viewports. Art style harmonious with hand-drawn crayon characters with white paper cutout borders.

### care_icons.png

Production transparent game icon atlas, square image, exactly 16 icons in a precise regular 4x4 grid of equal square cells. Genuine transparent background, no text or letters, no cell backgrounds or lines. Each icon completely inside its own cell with at least 15% transparent padding on ALL sides, no crossing borders. Consistent handmade cut-paper and watercolor style, visible subtle cream paper grain, soft sage mint, coral, honey yellow, dark plum accents. Clear simple silhouettes readable as small Android game icons. Row1 left to right: red apple with leaf; orange carrot; yellow banana; glass of milk. Row2 left to right: wedge of yellow cheese; chocolate chip cookie; slice of pizza; ice cream cone. Row3 left to right: small slice of cake; small blue fish food icon; yellow washing sponge with three soap bubbles; crescent moon with tiny star. Row4 left to right: warm golden sun; colorful paper pinwheel toy; purple top hat; coral pink heart. Strict uniform 4x4 positioning, precisely centered within each cell. Charming tactile paper illustration, no emoji typography, no photo, no scenery, no watermark.
