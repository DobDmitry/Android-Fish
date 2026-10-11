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
    val accessory: String? = null,
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

data class Accessory(
    val id: String,
    val emoji: String,
    val name: String,
    val price: Int,
)

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
        Accessory("bow", "🎀", "Бантик", 30),
        Accessory("cap", "🧢", "Кепка", 40),
        Accessory("tophat", "🎩", "Цилиндр", 60),
        Accessory("flower", "🌼", "Цветочек", 25),
        Accessory("party", "🥳", "Праздник", 50),
        Accessory("glasses", "🕶️", "Очки", 45),
        Accessory("crown", "👑", "Корона", 120),
        Accessory("helmet", "⛑️", "Каска", 55),
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
