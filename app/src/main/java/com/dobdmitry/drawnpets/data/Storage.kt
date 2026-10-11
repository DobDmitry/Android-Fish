package com.dobdmitry.drawnpets.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.dobdmitry.drawnpets.anim.LimbKind
import com.dobdmitry.drawnpets.anim.ManualEye
import com.dobdmitry.drawnpets.anim.ManualLimb
import com.dobdmitry.drawnpets.anim.ManualRig
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
        rigFile(id).delete()
    }

    private fun rigFile(id: String) = File(imagesDir, "rig_$id.json")

    /** Ручная разметка рук, ног и глаз (null — автоматическая). */
    fun loadManualRig(id: String): ManualRig? {
        val f = rigFile(id)
        if (!f.exists()) return null
        return try {
            val o = JSONObject(f.readText())
            val limbs = mutableListOf<ManualLimb>()
            o.optJSONArray("limbs")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val l = arr.getJSONObject(i)
                    val kind = runCatching { LimbKind.valueOf(l.getString("kind")) }.getOrNull() ?: continue
                    limbs += ManualLimb(
                        kind, l.getDouble("ax").toFloat(), l.getDouble("ay").toFloat(),
                        l.getDouble("tx").toFloat(), l.getDouble("ty").toFloat(),
                    )
                }
            }
            val eyes = mutableListOf<ManualEye>()
            o.optJSONArray("eyes")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val e = arr.getJSONObject(i)
                    eyes += ManualEye(e.getDouble("cx").toFloat(), e.getDouble("cy").toFloat(), e.getDouble("r").toFloat())
                }
            }
            ManualRig(limbs, eyes)
        } catch (e: Exception) {
            null
        }
    }

    fun saveManualRig(id: String, rig: ManualRig?) {
        val f = rigFile(id)
        if (rig == null) {
            f.delete()
            return
        }
        val o = JSONObject().apply {
            put("limbs", JSONArray().apply {
                rig.limbs.forEach { l ->
                    put(JSONObject().apply {
                        put("kind", l.kind.name)
                        put("ax", l.ax.toDouble()); put("ay", l.ay.toDouble())
                        put("tx", l.tx.toDouble()); put("ty", l.ty.toDouble())
                    })
                }
            })
            put("eyes", JSONArray().apply {
                rig.eyes.forEach { e ->
                    put(JSONObject().apply {
                        put("cx", e.cx.toDouble()); put("cy", e.cy.toDouble()); put("r", e.r.toDouble())
                    })
                }
            })
        }
        f.writeText(o.toString())
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
                    put("outfit", JSONArray().apply {
                        c.outfit.forEach { o ->
                            put(JSONObject().apply {
                                put("id", o.id)
                                o.x?.let { put("x", it.toDouble()) }
                                o.y?.let { put("y", it.toDouble()) }
                                o.size?.let { put("size", it.toDouble()) }
                                put("rot", o.rot.toDouble())
                            })
                        }
                    })
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

    private fun readOutfit(c: JSONObject): List<WornItem> {
        val arr = c.optJSONArray("outfit")
        if (arr == null) {
            // старые сохранения: один наряд в поле accessory
            val old = c.optString("accessory")
            return if (old.isNotEmpty()) listOf(WornItem(old)) else emptyList()
        }
        val out = mutableListOf<WornItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += WornItem(
                id = o.getString("id"),
                x = if (o.has("x")) o.getDouble("x").toFloat() else null,
                y = if (o.has("y")) o.getDouble("y").toFloat() else null,
                size = if (o.has("size")) o.getDouble("size").toFloat() else null,
                rot = o.optDouble("rot", 0.0).toFloat(),
            )
        }
        return out
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
                    outfit = readOutfit(c),
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
