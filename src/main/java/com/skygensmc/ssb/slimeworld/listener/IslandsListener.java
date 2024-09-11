package com.skygensmc.ssb.slimeworld.listener;

import com.bgsoftware.superiorskyblock.api.events.IslandDisbandEvent;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.world.Dimension;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.infernalsuite.aswm.api.AdvancedSlimePaperAPI;
import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.skygensmc.ssb.slimeworld.SlimeWorldModule;
import com.skygensmc.ssb.slimeworld.utils.Dimensions;
import com.skygensmc.ssb.slimeworld.utils.SlimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class IslandsListener implements Listener {

    private final SlimeWorldModule module;
    private final AdvancedSlimePaperAPI aspAPI;
    private final SlimeLoader slimeLoader;

    public IslandsListener(SlimeWorldModule module) {
        this.module = module;
        this.aspAPI = module.slimeAPI();
        this.slimeLoader = module.slimeLoader();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandDisband(IslandDisbandEvent event) {
        List<Dimension> enabledDimensions = new LinkedList<>();
        for (Dimension dimension : Dimension.values()) {
            if (!isWorldGenerated(event.getIsland(), dimension)) continue;

            enabledDimensions.add(dimension);
        }

        deleteWorldsForIsland(event.getIsland(), enabledDimensions);
    }

    private void deleteWorldsForIsland(Island island, List<Dimension> dimensions) {
        Bukkit.getScheduler().runTaskLater(module.getPlugin(), () -> {
            for (Dimension dimension : dimensions) {
                String worldName = SlimeUtils.getWorldName(island.getUniqueId(), dimension);
                SlimeWorld slimeWorld = this.aspAPI.getLoadedWorld(worldName);
                this.module.getWorldManager().removeWorld(slimeWorld);
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    Bukkit.unloadWorld(world, false);
                }
                try {
                    this.slimeLoader.deleteWorld(worldName);
                } catch (UnknownWorldException | IOException ex) {
                    this.module.getLogger().warning("Unable to delete world " + dimension.getName() + " for island " + island.getUniqueId() + ": " + ex.getMessage());
                }
            }
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        SuperiorPlayer superiorPlayer = this.module.getPlugin().getPlayers().getSuperiorPlayer(event.getPlayer());
        Island island = superiorPlayer.getIsland();
        if (island == null) return;

        for (Dimension dimension : Dimension.values()) {
            if (!isWorldGenerated(island, dimension)) continue;

            this.module.slimeWorldProvider().asyncGetWorld(island.getUniqueId(), dimension).whenComplete((world, ex) -> {
                if (ex != null) {
                    this.module.getLogger().warning("Unable to load island for player " + superiorPlayer.getUniqueId() + ": " + ex.getMessage());
                    return;
                }

                if (dimension != Dimensions.NORMAL) return;
                if (!this.module.getSettings().teleportToIsland) return;

                superiorPlayer.teleport(island);
            });
        }
    }

    private static boolean isWorldGenerated(Island island, Dimension dimension) {
        return island.wasSchematicGenerated(dimension) && island.isDimensionEnabled(dimension);
    }

}
