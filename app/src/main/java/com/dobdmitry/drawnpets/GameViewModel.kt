package com.dobdmitry.drawnpets

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dobdmitry.drawnpets.anim.CreatureSprite
import com.dobdmitry.drawnpets.anim.ManualRig
import com.dobdmitry.drawnpets.data.Catalog
import com.dobdmitry.drawnpets.data.Creature
import com.dobdmitry.drawnpets.data.DailyQuest
import com.dobdmitry.drawnpets.data.GameState
import com.dobdmitry.drawnpets.data.Habitat
import com.dobdmitry.drawnpets.data.QuestType
import com.dobdmitry.drawnpets.data.StarterArt
import com.dobdmitry.drawnpets.data.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class FeedResult { OK, FAVORITE, FULL, NO_FOOD, SLEEPING }

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val storage = Storage(app)

    var state by mutableStateOf(storage.load())
        private set

    private val sprites = mutableStateMapOf<String, CreatureSprite>()
    private var saveJob: Job? = null

    init {
        applyDecay(System.currentTimeMillis())
        ensureDailyQuests()
        if (!state.starterCreated) createStarters()
        viewModelScope.launch {
            for (c in state.creatures) {
                val sprite = withContext(Dispatchers.Default) {
                    storage.loadImage(c.id)?.let { CreatureSprite.create(it, storage.loadManualRig(c.id)) }
                }
                if (sprite != null) sprites[c.id] = sprite
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(5_000)
                applyDecay(System.currentTimeMillis())
                ensureDailyQuests()
                scheduleSave()
            }
        }
    }

    fun bitmap(id: String): ImageBitmap? = sprites[id]?.image

    /** Картинка со «скелетом» для анимации. */
    fun sprite(id: String): CreatureSprite? = sprites[id]

    /** Сохранить ручную разметку скелета (null — вернуть автоматическую) и пересобрать анимацию. */
    fun setManualRig(id: String, rig: ManualRig?) {
        val bitmap = sprites[id]?.bitmap ?: return
        viewModelScope.launch {
            val sprite = withContext(Dispatchers.Default) {
                storage.saveManualRig(id, rig)
                CreatureSprite.create(bitmap, rig)
            }
            sprites[id] = sprite
        }
    }

    // ---------------------------------------------------------------- сохранение

    private fun update(block: (GameState) -> GameState) {
        state = block(state)
        scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(600)
            val snapshot = state
            withContext(Dispatchers.IO) { storage.save(snapshot) }
        }
    }

    fun saveNow() {
        saveJob?.cancel()
        val snapshot = state
        viewModelScope.launch(Dispatchers.IO) { storage.save(snapshot) }
    }

    // ---------------------------------------------------------------- время

    private fun applyDecay(now: Long) {
        val minutes = ((now - state.lastUpdate) / 60_000f).coerceIn(0f, 24f * 60f)
        if (minutes <= 0f) return
        state = state.copy(
            lastUpdate = now,
            creatures = state.creatures.map { it.decayed(minutes) },
        )
    }

    private fun Creature.decayed(min: Float): Creature {
        val hungerRate = 0.07f
        val sadExtra = if (hunger < 30 || cleanliness < 30) 0.04f else 0f
        if (sleeping) {
            val newEnergy = energy + 1.2f * min
            return copy(
                hunger = (hunger - hungerRate * 0.5f * min).clamp(),
                happiness = (happiness - 0.02f * min).clamp(),
                cleanliness = (cleanliness - 0.02f * min).clamp(),
                energy = newEnergy.clamp(),
                sleeping = newEnergy < 100f,
            )
        }
        return copy(
            hunger = (hunger - hungerRate * min).clamp(),
            happiness = (happiness - (0.05f + sadExtra) * min).clamp(),
            energy = (energy - 0.045f * min).clamp(),
            cleanliness = (cleanliness - 0.035f * min).clamp(),
        )
    }

    private fun Float.clamp() = coerceIn(0f, 100f)

    private fun today(): Long = LocalDate.now().toEpochDay()

    // ---------------------------------------------------------------- персонажи

    private fun createStarters() {
        val kalyaka = Creature(
            id = UUID.randomUUID().toString(), name = "Каляка", habitat = Habitat.YARD,
            createdAt = System.currentTimeMillis(), favoriteFood = "cookie",
        )
        val puzyrik = Creature(
            id = UUID.randomUUID().toString(), name = "Пузырик", habitat = Habitat.POND,
            createdAt = System.currentTimeMillis(), favoriteFood = "fish",
        )
        storage.saveImage(kalyaka.id, StarterArt.kalyaka())
        storage.saveImage(puzyrik.id, StarterArt.puzyrik())
        state = state.copy(creatures = state.creatures + kalyaka + puzyrik, starterCreated = true)
        storage.save(state)
    }

    fun addCreature(bitmap: Bitmap, name: String, habitat: Habitat): String {
        val id = UUID.randomUUID().toString()
        storage.saveImage(id, bitmap)
        sprites[id] = CreatureSprite.create(bitmap)
        val creature = Creature(
            id = id,
            name = name.trim().ifEmpty { Catalog.randomNames.random() },
            habitat = habitat,
            createdAt = System.currentTimeMillis(),
            favoriteFood = Catalog.foods.random().id,
        )
        update { it.copy(creatures = it.creatures + creature) }
        return id
    }

    private fun updateCreature(id: String, block: (Creature) -> Creature) {
        update { s -> s.copy(creatures = s.creatures.map { if (it.id == id) block(it) else it }) }
    }

    private fun progressQuest(type: QuestType, amount: Int = 1) {
        update { s ->
            s.copy(quests = s.quests.map {
                if (it.type == type && !it.claimed) it.copy(progress = min(it.target, it.progress + amount)) else it
            })
        }
    }

    fun feed(id: String, foodId: String): FeedResult {
        val c = state.creature(id) ?: return FeedResult.NO_FOOD
        val food = Catalog.food(foodId) ?: return FeedResult.NO_FOOD
        if (c.sleeping) return FeedResult.SLEEPING
        if ((state.inventory[foodId] ?: 0) <= 0) return FeedResult.NO_FOOD
        if (c.hunger >= 98f) return FeedResult.FULL
        val favorite = c.favoriteFood == foodId
        update { s -> s.copy(inventory = s.inventory + (foodId to (s.inventory[foodId] ?: 1) - 1)) }
        updateCreature(id) {
            it.copy(
                hunger = (it.hunger + food.hunger).clamp(),
                happiness = (it.happiness + food.happiness + if (favorite) 10 else 0).clamp(),
                xp = it.xp + if (favorite) 8 else 4,
            )
        }
        progressQuest(QuestType.FEED)
        return if (favorite) FeedResult.FAVORITE else FeedResult.OK
    }

    fun pet(id: String) {
        updateCreature(id) { it.copy(happiness = (it.happiness + 3f).clamp(), xp = it.xp + 1) }
        progressQuest(QuestType.PET)
    }

    fun wash(id: String, amount: Float) {
        val before = state.creature(id)?.cleanliness ?: return
        updateCreature(id) {
            it.copy(
                cleanliness = (it.cleanliness + amount).clamp(),
                happiness = (it.happiness + amount * 0.15f).clamp(),
            )
        }
        val after = state.creature(id)?.cleanliness ?: return
        if (before < 100f && after >= 100f) {
            updateCreature(id) { it.copy(xp = it.xp + 5) }
            progressQuest(QuestType.WASH)
        }
    }

    fun toggleSleep(id: String) = updateCreature(id) { it.copy(sleeping = !it.sleeping) }

    fun rename(id: String, name: String) {
        if (name.isNotBlank()) updateCreature(id) { it.copy(name = name.trim()) }
    }

    fun moveTo(id: String, habitat: Habitat) = updateCreature(id) { it.copy(habitat = habitat) }

    fun setAccessory(id: String, accessory: String?) = updateCreature(id) { it.copy(accessory = accessory) }

    fun release(id: String) {
        update { s -> s.copy(creatures = s.creatures.filterNot { it.id == id }) }
        sprites.remove(id)
        storage.deleteImage(id)
    }

    /** Персонаж из пруда съел корм для рыбок. */
    fun pondSnack(id: String) {
        updateCreature(id) { it.copy(hunger = (it.hunger + 4f).clamp(), happiness = (it.happiness + 1f).clamp()) }
    }

    fun fishFed() = progressQuest(QuestType.FISH)

    // ---------------------------------------------------------------- магазин

    fun buyFood(foodId: String): Boolean {
        val food = Catalog.food(foodId) ?: return false
        if (state.coins < food.price) return false
        update { s -> s.copy(coins = s.coins - food.price, inventory = s.inventory + (foodId to (s.inventory[foodId] ?: 0) + 1)) }
        return true
    }

    fun buyAccessory(id: String): Boolean {
        val acc = Catalog.accessory(id) ?: return false
        if (state.coins < acc.price || id in state.ownedAccessories) return false
        update { s -> s.copy(coins = s.coins - acc.price, ownedAccessories = s.ownedAccessories + id) }
        return true
    }

    // ---------------------------------------------------------------- игры

    /** Возвращает число заработанных монет. */
    fun finishGame(gameId: String, score: Int, coins: Int, creatureId: String?): Int {
        val reward = max(1, coins)
        update { s ->
            val best = max(s.bestScores[gameId] ?: 0, score)
            s.copy(coins = s.coins + reward, bestScores = s.bestScores + (gameId to best))
        }
        creatureId?.let { id ->
            updateCreature(id) {
                it.copy(
                    happiness = (it.happiness + 15f).clamp(),
                    energy = (it.energy - 8f).clamp(),
                    hunger = (it.hunger - 4f).clamp(),
                    xp = it.xp + 10 + score / 2,
                )
            }
        }
        progressQuest(QuestType.PLAY)
        return reward
    }

    // ---------------------------------------------------------------- NPC

    private fun ensureDailyQuests() {
        val day = today()
        if (state.questDay == day && state.quests.isNotEmpty()) return
        val rnd = Random(day)
        val quests = QuestType.entries.shuffled(rnd).take(3).map { type ->
            val target = when (type) {
                QuestType.FEED -> 3 + rnd.nextInt(3)
                QuestType.PET -> 5 + rnd.nextInt(6)
                QuestType.PLAY -> 2 + rnd.nextInt(2)
                QuestType.WASH -> 1 + rnd.nextInt(2)
                QuestType.FISH -> 5 + rnd.nextInt(6)
            }
            DailyQuest(type, target, reward = 15 + target * 3)
        }
        state = state.copy(quests = quests, questDay = day)
    }

    fun claimQuest(index: Int): Int {
        val q = state.quests.getOrNull(index) ?: return 0
        if (!q.done || q.claimed) return 0
        update { s ->
            s.copy(
                coins = s.coins + q.reward,
                quests = s.quests.mapIndexed { i, it -> if (i == index) it.copy(claimed = true) else it },
            )
        }
        return q.reward
    }

    val giftAvailable: Boolean get() = state.lastGiftDay != today()

    /** Подарок дня от Филина: монетки и случайная вкусняшка. */
    fun claimGift(): Pair<Int, String>? {
        if (!giftAvailable) return null
        val coins = 20 + Random.nextInt(21)
        val food = Catalog.foods.random()
        update { s ->
            s.copy(
                coins = s.coins + coins,
                lastGiftDay = today(),
                inventory = s.inventory + (food.id to (s.inventory[food.id] ?: 0) + 1),
            )
        }
        return coins to food.emoji
    }

    /** Доктор Ёжик лечит всех, кому плохо. Возвращает число вылеченных. */
    fun healAll(): Int {
        val sick = state.creatures.filter { it.mood < 60 || it.hunger < 40 || it.cleanliness < 40 }
        if (sick.isEmpty()) return 0
        val ids = sick.map { it.id }.toSet()
        update { s ->
            s.copy(creatures = s.creatures.map {
                if (it.id in ids) it.copy(
                    hunger = max(it.hunger, 70f),
                    happiness = max(it.happiness, 70f),
                    energy = max(it.energy, 70f),
                    cleanliness = max(it.cleanliness, 70f),
                ) else it
            })
        }
        return sick.size
    }
}
