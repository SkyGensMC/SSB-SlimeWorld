package com.skygensmc.ssb.slimeworld.utils;

import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.skygensmc.ssb.slimeworld.SlimeWorldModule;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorldManager {

    private final SlimeWorldModule module;
    private final Map<SlimeWorld, Long> loadedWorlds = new ConcurrentHashMap<>();

    public WorldManager(SlimeWorldModule module) {
        this.module = module;
        this.run(module.getPlugin());
    }

    public void run(Plugin plugin) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Map.Entry<SlimeWorld, Long> entry : loadedWorlds.entrySet()) {
                World bukkitWorld = Bukkit.getWorld(entry.getKey().getName());
                if (bukkitWorld == null) continue;
                if (!bukkitWorld.getPlayers().isEmpty()) {
                    this.loadedWorlds.replace(entry.getKey(), System.currentTimeMillis());
                    continue;
                }

                boolean expired = System.currentTimeMillis() > (entry.getValue() + (this.module.getSettings().unloadDelay * 1000L));
                if (!expired) continue;

                this.removeWorld(entry.getKey());
                Bukkit.getScheduler().runTask(this.module.getPlugin(), () -> Bukkit.unloadWorld(bukkitWorld, true));
            }
        }, 0, 1);
    }

    public void addWorld(SlimeWorld slimeWorld) {
        this.loadedWorlds.put(slimeWorld, System.currentTimeMillis());
    }

    public void removeWorld(SlimeWorld slimeWorld) {
        this.loadedWorlds.remove(slimeWorld);
    }
}
