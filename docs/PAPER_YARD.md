# Бумажный двор

Первый игровой экран в стиле согласованного концепта «оживший альбом».

## Что изменено

- Отдельный панорамный фон `bg_yard_paper.png`, без запечённых персонажей и интерфейса.
- Новые вкладки локаций, кнопка «Оживить рисунок», нижняя навигация и карточка выбранного героя.
- Собственные иллюстрированные иконки и монеты вместо эмодзи на основных элементах двора.
- Филин из бумажных слоёв с лёгкой анимацией дыхания. Остальные NPC сохраняют прежнюю отрисовку.
- Текст интерфейса остаётся текстом Compose. Используется Nunito с кириллицей, лицензия в `Nunito-OFL.txt`.
- Все иконки используют один декодированный атлас 3×3 через `PaperArtwork`; область иконки выбирается при отрисовке.
- Высота верхней и нижней панелей измеряется с учётом системных отступов и размера текста. Основная сцена размещается между панелями.
- Увеличены герои, их начальные позиции находятся в видимой части двора. Рисунки, скелетная анимация, уход, сохранения и экономика остаются существующими.
- Передний план слегка покачивается. Монеты и стрелки прокрутки используют новое оформление.

Остальные локации и внутренние экраны пока сохраняют прежний стиль. Выбранный герой открывает карточку поверх нижней части сцены; её можно закрыть отдельной кнопкой.

## Проверка на устройстве

1. Открыть двор на экранах 360 и 412 dp, проверить обычный и увеличенный системный шрифт.
2. Провести пальцем в обе стороны: дом, лавка и NPC доступны, стрелки исчезают у границ.
3. Нажать на героя, закрыть карточку, открыть уход и вернуться.
4. Нажать на Филина, Енота и Ёжика; проверить прежние диалоги.
5. Собрать монету и проверить счётчик, в том числе после прокрутки.
6. Открыть создание рисунка, игры, лавку, альбом, настройки и все локации.
7. Проверить детские рисунки разной формы и героев с нарядами.
8. Проверить TalkBack для вкладок, настроек и кнопок навигации.

Сборка: JDK 17, Android SDK 35, `./gradlew assembleDebug lintDebug`.
Для установки поверх существующей версии используйте тот же ключ `app/debug.keystore` и актуальный номер сборки через `GITHUB_RUN_NUMBER`.

## Результат проверки 14 октября 2026

- `assembleDebug lintDebug`: успешно, итоговая версия 1.0.16 (versionCode 16).
- Android Lint: 0 ошибок, 9 предупреждений. Они относятся к зависимостям, фиксированной ориентации, значку приложения, прежним API и сохранённому старому фону `bg_yard`; в новых компонентах интерфейса замечаний нет.
- `apksigner verify`: подпись APK корректна, SHA-256 сертификата совпадает с `app/debug.keystore`.
- SHA-256 сертификата: `242fe202c2c5233e87d1831306c0a8bdb31bdaf0c460b40fe69fa5d48e17ee5b`.
- Идентификатор приложения: `com.dobdmitry.drawnpets`, минимальный Android 8.0.
- Подключённого телефона или эмулятора не было: внешний вид, плавность и взаимодействия на устройстве пока не проверены. Список ручной проверки приведён выше.
- Исходная ревизия: `0b47a081d4dbe470d624f12f2c80c60cee132b72`.

На данной Windows-машине Java потребовала короткий путь для временных сокетов (`jdk.net.unixdomain.tmpdir`). Это локальная настройка сборки, в приложение она не включена.

## Графика

Новые изображения созданы встроенным инструментом ImageGen. Фон использует согласованный `yard-concept.png` только как референс стиля. Иконки и Филин имеют настоящий alpha-канал. Исходные PNG сохранены в ресурсах проекта; текст и интерактивные элементы рисуются отдельно.

### Запрос для фона

Create a production game background asset inspired by the attached approved UI concept, image is STYLE REFERENCE ONLY. Landscape aspect ratio 3:2, high resolution. A continuous horizontally scrolling children's picture-book courtyard made from textured cut paper and gouache, cream, sage green, mint, terracotta, warm sunlight. NO UI, NO text, NO icons, NO characters, NO animals, NO stickers, NO speech bubbles. Top 35 percent: pale blue sky, paper clouds, distant rounded hills; middle 35 percent: small cozy cottage on far left around x=15%, wooden fence and garden, tiny rustic empty market stall at far right x=85%, a distant blue pond centrally, layered handmade foliage at sides. Entire bottom 35 percent: open calm pale sage grassy play area, sparse tiny daisies along outer edges only, no obstacles or furniture in play area. All structures small enough to fit complete within background. Composition suitable for a portrait viewport cropping any third of this wide panorama; focal playable space across all bottom portion, unobstructed. Charming tactile watercolor paper fibers, soft shadows between paper layers, same artisanal illustrative style as reference. Background only, no interface. Full bleed, no borders.

### Запрос для иконок

Production game UI icon atlas, square image, transparent background. EXACTLY nine isolated icons in a perfectly regular 3 by 3 grid of equal square cells. Every icon centered within its cell, size 65% of cell, generous fully transparent padding, no touching between cells. No text, no letters, no borders, no shadows beyond cell. Consistent handcrafted cut-paper watercolor children's picture book style, cream paper texture, sage mint green, coral terracotta, plum outlines. Row 1 left: cozy cottage with tree (yard). Row 1 middle: cozy small house (home). Row 1 right: blue pond with green lily pad (pond). Row 2 left: coral round Mars planet with small flag (Mars). Row 2 middle: cream camera with green pencil diagonally beside it (create drawing). Row 2 right: colorful paper pinwheel toy (games). Row 3 left: small wooden market stall with red white striped canopy (shop). Row 3 middle: closed purple scrapbook album with a little drawing on cover (album). Row 3 right: single golden coin with embossed star (currency). Perfectly aligned equal 3x3 grid on genuine transparent background. Rich tactile paper texture, legible small silhouettes, professionally illustrated, not emoji. All nine must fit fully into their cells.

### Запрос для Филина

Single full body friendly grandfather owl character sprite for a children's living paper picture book game. Genuine transparent background. Front three-quarter view, both eyes OPEN and kind, circular dark plum eyeglasses, cream belly, warm brown textured cut paper and gouache feather wings, tiny orange feet fully visible. One wing gently lifted in greeting, centered, isolated character only with ample transparent padding. Handmade watercolor paper texture, soft dimensional paper layers, charming simple readable silhouette. No scenery, no stump, no text, no speech bubble, no interface, no ground shadow. Match artisanal cream sage coral paper collage children's world, not realistic, not 3D plastic. Portrait or square asset.
