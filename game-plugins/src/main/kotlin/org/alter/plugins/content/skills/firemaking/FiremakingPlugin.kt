package org.alter.plugins.content.skills.firemaking

import org.alter.api.Skills
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.DynamicObject
import org.alter.game.model.entity.GroundItem
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.rscm.RSCM.getRSCM

class FiremakingPlugin(r: PluginRepository, world: World, server: Server) : KotlinPlugin(r, world, server) {

    private companion object {
        const val FIRE_OBJECT = 26185
        const val LIGHT_ANIM = 733
    }

    private enum class FireLog(val item: String, val level: Int, val xp: Double, val lifespanTicks: Int) {
        LOGS("item.logs", 1, 40.0, 75),
        OAK("item.oak_logs", 15, 60.0, 85),
        WILLOW("item.willow_logs", 30, 90.0, 100),
        TEAK("item.teak_logs", 35, 105.0, 110),
        MAPLE("item.maple_logs", 45, 135.0, 130),
        YEW("item.yew_logs", 60, 202.5, 180),
        MAGIC("item.magic_logs", 75, 303.8, 250),
        REDWOOD("item.redwood_logs", 90, 350.0, 400),
    }

    init {
        FireLog.values().forEach { log ->
            onItemOnItem(item1 = log.item, item2 = "item.tinderbox") {
                val p = player
                p.queue { lightLog(this, p, log) }
            }
        }
    }

    private suspend fun lightLog(task: QueueTask, player: Player, log: FireLog) {
        val level = player.getSkills().getCurrentLevel(Skills.FIREMAKING)
        if (level < log.level) {
            player.message("You need a Firemaking level of ${log.level} to light these logs.")
            return
        }
        val logId = getRSCM(log.item)
        if (!player.inventory.contains(logId)) return

        player.lock()
        try {
            player.animate(LIGHT_ANIM)
            task.wait(3)
            if (!player.inventory.contains(logId)) return

            player.inventory.remove(logId, 1)
            player.addXp(Skills.FIREMAKING, log.xp)
            player.message("The logs catch fire.")

            val tile = player.tile
            val fire = DynamicObject(id = FIRE_OBJECT, type = 10, rot = 0, tile = tile)
            world.spawn(fire)

            world.queue {
                wait(log.lifespanTicks)
                if (world.isSpawned(fire)) {
                    world.remove(fire)
                    world.spawn(GroundItem(getRSCM("item.ashes"), 1, tile))
                }
            }
        } finally {
            player.unlock()
        }
    }
}
