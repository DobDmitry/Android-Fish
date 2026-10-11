package com.dobdmitry.drawnpets.data

/** Где живёт персонаж. */
enum class Habitat(val title: String, val emoji: String) {
    YARD("Двор", "🏡"),
    POND("Пруд", "🐟"),
}

data class Creature(
    val id: String,
    val name: String,
    val habitat: Habitat,
    val createdAt: Long,
    /** Сытость 0..100 */
    val hunger: Float = 80f,
    /** Радость 0..100 */
    val happiness: Float = 80f,
    /** Энергия 0..100 */
    val energy: Float = 80f,
    /** Чистота 0..100 */
    val cleanliness: Float = 80f,
    val sleeping: Boolean = false,
    val xp: Int = 0,
    /** Надетые наряды — по одному на каждое место (голова, лицо, шея…). */
    val outfit: List<WornItem> = emptyList(),
    val favoriteFood: String = "apple",
) {
    val level: Int get() = 1 + xp / LEVEL_XP
    val levelProgress: Float get() = (xp % LEVEL_XP) / LEVEL_XP.toFloat()
    val mood: Float get() = (hunger + happiness + energy + cleanliness) / 4f

    val moodEmoji: String
        get() = when {
            sleeping -> "😴"
            mood >= 75 -> "😄"
            mood >= 50 -> "🙂"
            mood >= 30 -> "😕"
            else -> "😢"
        }

    /** Что персонаж «говорит» о своём состоянии. */
    fun wish(): String? = when {
        sleeping -> "Хр-р-р… Zzz…"
        hunger < 30 -> "Я хочу кушать!"
        energy < 25 -> "Я так устал… Уложи меня спать."
        cleanliness < 30 -> "Я весь грязный! Помой меня."
        happiness < 30 -> "Мне скучно… Поиграй со мной!"
        else -> null
    }

    companion object {
        const val LEVEL_XP = 100
    }
}

data class Food(
    val id: String,
    val emoji: String,
    val name: String,
    val price: Int,
    val hunger: Int,
    val happiness: Int,
)

/** Куда надевается наряд. */
enum class Slot(val title: String, val emoji: String) {
    HEAD("Голова", "🎩"),
    FACE("Лицо", "👓"),
    NECK("Шея", "🧣"),
    HAND("В руку", "🎈"),
    BACK("Спина", "🎒"),
}

/** Наряд из каталога. Надеть его может только персонаж уровня [minLevel] и выше. */
data class Accessory(
    val id: String,
    val emoji: String,
    val name: String,
    val price: Int,
    val slot: Slot,
    val minLevel: Int,
)

/**
 * Надетый наряд. [x], [y] — центр в пикселях картинки персонажа, [size] — размер
 * в тех же пикселях, [rot] — поворот в градусах. Если x/y/size не заданы,
 * место выбирается автоматически по «скелету» рисунка.
 */
data class WornItem(
    val id: String,
    val x: Float? = null,
    val y: Float? = null,
    val size: Float? = null,
    val rot: Float = 0f,
) {
    val custom: Boolean get() = x != null && y != null && size != null
}

object Catalog {
    val foods = listOf(
        Food("apple", "🍎", "Яблоко", 5, 15, 2),
        Food("carrot", "🥕", "Морковка", 5, 14, 1),
        Food("banana", "🍌", "Банан", 7, 18, 3),
        Food("milk", "🥛", "Молоко", 8, 16, 4),
        Food("cheese", "🧀", "Сыр", 10, 22, 4),
        Food("cookie", "🍪", "Печенье", 12, 12, 10),
        Food("pizza", "🍕", "Пицца", 18, 35, 8),
        Food("icecream", "🍦", "Мороженое", 15, 10, 16),
        Food("cake", "🍰", "Тортик", 25, 30, 20),
        Food("fish", "🐟", "Рыбка", 14, 28, 6),
    )

    val accessories = listOf(
        // Голова
        Accessory("flower", "🌼", "Цветочек", 25, Slot.HEAD, 1),
        Accessory("blossom", "🌸", "Сакура", 20, Slot.HEAD, 1),
        Accessory("bow", "🎀", "Бантик", 30, Slot.HEAD, 1),
        Accessory("cap", "🧢", "Кепка", 40, Slot.HEAD, 2),
        Accessory("sunhat", "👒", "Шляпка", 45, Slot.HEAD, 2),
        Accessory("tophat", "🎩", "Цилиндр", 60, Slot.HEAD, 3),
        Accessory("helmet", "⛑️", "Каска", 55, Slot.HEAD, 3),
        Accessory("mushroom", "🍄", "Грибная шляпа", 50, Slot.HEAD, 4),
        Accessory("grad", "🎓", "Шапочка отличника", 80, Slot.HEAD, 5),
        Accessory("crown", "👑", "Корона", 150, Slot.HEAD, 7),
        Accessory("star", "🌟", "Звезда", 200, Slot.HEAD, 9),
        // Лицо
        Accessory("specs", "👓", "Очки", 35, Slot.FACE, 1),
        Accessory("glasses", "🕶️", "Тёмные очки", 45, Slot.FACE, 2),
        Accessory("mask", "🎭", "Маска", 65, Slot.FACE, 4),
        // Шея
        Accessory("bell", "🔔", "Колокольчик", 30, Slot.NECK, 1),
        Accessory("scarf", "🧣", "Шарф", 40, Slot.NECK, 2),
        Accessory("beads", "📿", "Бусы", 50, Slot.NECK, 3),
        Accessory("tie", "👔", "Галстук", 55, Slot.NECK, 3),
        Accessory("ribbon", "🎗️", "Ленточка", 60, Slot.NECK, 4),
        Accessory("medal", "🏅", "Медаль", 100, Slot.NECK, 6),
        // В руку
        Accessory("balloon", "🎈", "Шарик", 30, Slot.HAND, 1),
        Accessory("party", "🎉", "Хлопушка", 40, Slot.HAND, 2),
        Accessory("sunflower", "🌻", "Подсолнух", 35, Slot.HAND, 2),
        Accessory("lollipop", "🍭", "Леденец", 45, Slot.HAND, 3),
        Accessory("ball", "⚽", "Мячик", 45, Slot.HAND, 3),
        Accessory("umbrella", "☂️", "Зонтик", 60, Slot.HAND, 4),
        Accessory("guitar", "🎸", "Гитара", 120, Slot.HAND, 6),
        Accessory("crystal", "🔮", "Волшебный шар", 150, Slot.HAND, 7),
        Accessory("trophy", "🏆", "Кубок", 250, Slot.HAND, 10),
        // Спина
        Accessory("backpack", "🎒", "Рюкзак", 45, Slot.BACK, 2),
        Accessory("butterfly", "🦋", "Крылья бабочки", 90, Slot.BACK, 5),
        Accessory("rainbow", "🌈", "Радуга", 180, Slot.BACK, 8),
    )

    fun food(id: String): Food? = foods.firstOrNull { it.id == id }
    fun accessory(id: String?): Accessory? = accessories.firstOrNull { it.id == id }

    val randomNames = listOf(
        "Каляка", "Маляка", "Пушок", "Бубля", "Кузя", "Тяпа", "Шуршик", "Зефирка",
        "Ластик", "Клякса", "Пончик", "Мурзик", "Бусинка", "Хрумка", "Фунтик",
        "Пятнышко", "Карандаш", "Звёздочка", "Чудик", "Ушастик", "Буля", "Тошка",
        "Пиксель", "Плюшка", "Шмяк", "Бублик", "Огонёк", "Капелька",
    )
}

enum class QuestType(val title: String, val emoji: String) {
    FEED("Покорми питомцев", "🍎"),
    PET("Погладь питомцев", "🤗"),
    PLAY("Сыграй в мини-игры", "🎮"),
    WASH("Отмой питомца до блеска", "🧼"),
    FISH("Покорми рыбок в пруду", "🐠"),
}

data class DailyQuest(
    val type: QuestType,
    val target: Int,
    val progress: Int = 0,
    val reward: Int,
    val claimed: Boolean = false,
) {
    val done: Boolean get() = progress >= target
}

data class GameState(
    val coins: Int = 60,
    val creatures: List<Creature> = emptyList(),
    val inventory: Map<String, Int> = mapOf("apple" to 3, "carrot" to 2, "cookie" to 1),
    val ownedAccessories: Set<String> = emptySet(),
    val quests: List<DailyQuest> = emptyList(),
    val questDay: Long = -1,
    val lastGiftDay: Long = -1,
    val lastUpdate: Long = System.currentTimeMillis(),
    val bestScores: Map<String, Int> = emptyMap(),
    val starterCreated: Boolean = false,
) {
    fun creature(id: String): Creature? = creatures.firstOrNull { it.id == id }
}
