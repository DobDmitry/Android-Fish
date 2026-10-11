package com.dobdmitry.drawnpets.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Сохраняет состояние игры в JSON, а рисунки персонажей — в PNG. */
class Storage(context: Context) {
    private val stateFile = File(context.filesDir, "state.json")
    private val imagesDir = File(context.filesDir, "creatures").apply { mkdirs() }

    fun imageFile(id: String) = File(imagesDir, "$id.png")

    fun saveImage(id: String, bitmap: Bitmap) {
        imageFile(id).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    fun loadImage(id: String): Bitmap? {
        val f = imageFile(id)
        return if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
    }

    fun deleteImage(id: String) {
        imageFile(id).delete()
    }

    @Synchronized
    fun save(state: GameState) {
        val tmp = File(stateFile.parentFile, "state.json.tmp")
        tmp.writeText(toJson(state).toString())
        tmp.renameTo(stateFile)
    }

    fun load(): GameState {
        if (!stateFile.exists()) return GameState()
        return try {
            fromJson(JSONObject(stateFile.readText()))
        } catch (e: Exception) {
            GameState()
        }
    }

    private fun toJson(s: GameState): JSONObject = JSONObject().apply {
        put("coins", s.coins)
        put("questDay", s.questDay)
        put("lastGiftDay", s.lastGiftDay)
        put("lastUpdate", s.lastUpdate)
        put("starterCreated", s.starterCreated)
        put("creatures", JSONArray().apply {
            s.creatures.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("habitat", c.habitat.name)
                    put("createdAt", c.createdAt)
                    put("hunger", c.hunger.toDouble())
                    put("happiness", c.happiness.toDouble())
                    put("energy", c.energy.toDouble())
                    put("cleanliness", c.cleanliness.toDouble())
                    put("sleeping", c.sleeping)
                    put("xp", c.xp)
                    put("accessory", c.accessory ?: "")
                    put("favoriteFood", c.favoriteFood)
                })
            }
        })
        put("inventory", JSONObject().apply { s.inventory.forEach { (k, v) -> put(k, v) } })
        put("ownedAccessories", JSONArray().apply { s.ownedAccessories.forEach { put(it) } })
        put("bestScores", JSONObject().apply { s.bestScores.forEach { (k, v) -> put(k, v) } })
        put("quests", JSONArray().apply {
            s.quests.forEach { q ->
                put(JSONObject().apply {
                    put("type", q.type.name)
                    put("target", q.target)
                    put("progress", q.progress)
                    put("reward", q.reward)
                    put("claimed", q.claimed)
                })
            }
        })
    }

    private fun fromJson(o: JSONObject): GameState {
        val creatures = mutableListOf<Creature>()
        o.optJSONArray("creatures")?.let { arr ->
            for (i in 0 until arr.length()) {
                val c = arr.getJSONObject(i)
                creatures += Creature(
                    id = c.getString("id"),
                    name = c.optString("name", "Без имени"),
                    habitat = runCatching { Habitat.valueOf(c.optString("habitat")) }.getOrDefault(Habitat.YARD),
                    createdAt = c.optLong("createdAt"),
                    hunger = c.optDouble("hunger", 80.0).toFloat(),
                    happiness = c.optDouble("happiness", 80.0).toFloat(),
                    energy = c.optDouble("energy", 80.0).toFloat(),
                    cleanliness = c.optDouble("cleanliness", 80.0).toFloat(),
                    sleeping = c.optBoolean("sleeping"),
                    xp = c.optInt("xp"),
                    accessory = c.optString("accessory").ifEmpty { null },
                    favoriteFood = c.optString("favoriteFood", "apple"),
                )
            }
        }
        val inventory = mutableMapOf<String, Int>()
        o.optJSONObject("inventory")?.let { inv -> inv.keys().forEach { inventory[it] = inv.getInt(it) } }
        val owned = mutableSetOf<String>()
        o.optJSONArray("ownedAccessories")?.let { arr -> for (i in 0 until arr.length()) owned += arr.getString(i) }
        val best = mutableMapOf<String, Int>()
        o.optJSONObject("bestScores")?.let { b -> b.keys().forEach { best[it] = b.getInt(it) } }
        val quests = mutableListOf<DailyQuest>()
        o.optJSONArray("quests")?.let { arr ->
            for (i in 0 until arr.length()) {
                val q = arr.getJSONObject(i)
                val type = runCatching { QuestType.valueOf(q.getString("type")) }.getOrNull() ?: continue
                quests += DailyQuest(type, q.getInt("target"), q.optInt("progress"), q.getInt("reward"), q.optBoolean("claimed"))
            }
        }
        return GameState(
            coins = o.optInt("coins", 60),
            creatures = creatures,
            inventory = inventory,
            ownedAccessories = owned,
            quests = quests,
            questDay = o.optLong("questDay", -1),
            lastGiftDay = o.optLong("lastGiftDay", -1),
            lastUpdate = o.optLong("lastUpdate", System.currentTimeMillis()),
            bestScores = best,
            starterCreated = o.optBoolean("starterCreated"),
        )
    }
}
