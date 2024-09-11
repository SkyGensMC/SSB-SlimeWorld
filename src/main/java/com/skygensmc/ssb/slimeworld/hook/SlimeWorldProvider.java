package com.skygensmc.ssb.slimeworld.hook;

import com.bgsoftware.superiorskyblock.api.config.SettingsManager;
import com.bgsoftware.superiorskyblock.api.hooks.LazyWorldsProvider;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.world.Dimension;
import com.bgsoftware.superiorskyblock.api.world.WorldInfo;
import com.infernalsuite.aswm.api.AdvancedSlimePaperAPI;
import com.infernalsuite.aswm.api.exceptions.CorruptedWorldException;
import com.infernalsuite.aswm.api.exceptions.NewerFormatException;
import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.properties.SlimeProperties;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import com.skygensmc.ssb.slimeworld.SlimeWorldModule;
import com.skygensmc.ssb.slimeworld.exception.SlimeWorldModuleException;
import com.skygensmc.ssb.slimeworld.utils.Dimensions;
import com.skygensmc.ssb.slimeworld.utils.SlimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SlimeWorldProvider implements LazyWorldsProvider {

    private final Map<UUID, Dimension> islandWorldsToDimensions = new HashMap<>();
    private final SlimeWorldModule module;
    private final AdvancedSlimePaperAPI aspAPI;
    private final SlimeLoader slimeLoader;

    public SlimeWorldProvider(SlimeWorldModule module) {
        this.module = module;
        this.aspAPI = module.slimeAPI();
        this.slimeLoader = module.slimeLoader();
        Bukkit.getScheduler().runTaskLater(module.getPlugin(), () -> {
            Location spawnLocation = this.module.getPlugin().getGrid().getSpawnIsland().getCenter(Dimensions.NORMAL);
            World spawnWorld = spawnLocation.getWorld();
            if (spawnWorld != null) {
                this.islandWorldsToDimensions.computeIfAbsent(spawnWorld.getUID(), u -> Dimension.getByName(spawnWorld.getEnvironment().name()));
            }
        }, 40L);
    }

    @Override
    public void prepareWorld(Island island, Dimension dimension, Runnable finishCallback) {
        if (island.isSpawn()) {
            finishCallback.run();
            return;
        }

        this.asyncGetWorld(island.getUniqueId(), dimension).whenComplete((world, ex) -> {
            if (ex == null) {
                finishCallback.run();
            } else {
                this.module.getLogger().warning(ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    @Override
    public void prepareWorlds() {

    }

    @Override
    public World getIslandsWorld(Island island, Dimension dimension) {
        try {
            return isDimensionEnabled(dimension) ? this.getWorld(island.getUniqueId(), dimension) : null;
        } catch (SlimeWorldModuleException ex) {
            this.module.getLogger().warning("Unable to retrieve island world for Island " + island.getUniqueId());
            return null;
        }
    }

    @Override
    public World getIslandsWorld(Island island, World.Environment environment) {
        return getIslandsWorld(island, Dimension.getByName(environment.name()));
    }

    @Override
    public boolean isIslandsWorld(World world) {
        return SlimeUtils.isIslandWorld(world);
    }

    @Override
    public Location getNextLocation(Location previousLocation, int islandsHeight, int maxIslandSize, UUID islandOwner, UUID islandUUID) {
        // The world should be loaded by now.
        String worldName = SlimeUtils.getWorldName(islandUUID, this.module.getPlugin().getSettings().getWorlds().getDefaultWorldDimension());
        World islandWorld = Bukkit.getWorld(worldName);
        return new Location(islandWorld, 0, islandsHeight, 0);
    }

    @Override
    public void finishIslandCreation(Location islandsLocation, UUID islandOwner, UUID islandUUID) {

    }

    @Override
    public void prepareTeleport(Island island, Location location, Runnable finishCallback) {
        prepareWorld(island, getIslandsWorldDimension(location.getWorld()), finishCallback);
    }

    @Override
    public boolean isNormalEnabled() {
        return isDimensionEnabled(Dimensions.NORMAL);
    }

    @Override
    public boolean isNormalUnlocked() {
        return isDimensionUnlocked(Dimensions.NORMAL);
    }

    @Override
    public boolean isNetherEnabled() {
        return isDimensionEnabled(Dimensions.NETHER);
    }

    @Override
    public boolean isNetherUnlocked() {
        return isDimensionUnlocked(Dimensions.NETHER);
    }

    @Override
    public boolean isEndEnabled() {
        return isDimensionEnabled(Dimensions.THE_END);
    }

    @Override
    public boolean isEndUnlocked() {
        return isDimensionUnlocked(Dimensions.THE_END);
    }

    @Override
    public boolean isDimensionEnabled(Dimension dimension) {
        SettingsManager.Worlds.DimensionConfig dimensionConfig = this.module.getPlugin().getSettings().getWorlds().getDimensionConfig(dimension);
        // If the config is null, it probably means another plugin registered it.
        // Therefore, we register it as enabled.
        return dimensionConfig == null || dimensionConfig.isEnabled();
    }

    @Override
    public boolean isDimensionUnlocked(Dimension dimension) {
        SettingsManager.Worlds.DimensionConfig dimensionConfig = this.module.getPlugin().getSettings().getWorlds().getDimensionConfig(dimension);
        return dimensionConfig != null && dimensionConfig.isUnlocked();
    }

    @Nullable
    @Override
    public WorldInfo getIslandsWorldInfo(Island island, Dimension dimension) {
        return WorldInfo.of(SlimeUtils.getWorldName(island.getUniqueId(), dimension), dimension);
    }

    @Override
    public Dimension getIslandsWorldDimension(World world) {
        Dimension dimension = this.islandWorldsToDimensions.get(world.getUID());
        if (dimension != null)
            return dimension;
        return Dimension.getByName(world.getEnvironment().name());
    }

    @Nullable
    @Override
    public WorldInfo getIslandsWorldInfo(Island island, String worldName) {
        Dimension dimension = SlimeUtils.getDimension(worldName);
        if (dimension == null) {
            return null;
        }

        return WorldInfo.of(worldName, dimension);
    }

    public World getWorld(UUID islandUUID, Dimension dimension) throws SlimeWorldModuleException {
        String worldName = SlimeUtils.getWorldName(islandUUID, dimension);
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            return world;
        }

        SlimePropertyMap properties = new SlimePropertyMap();
        try {
            SlimeWorld slimeWorld;
            if (this.slimeLoader.worldExists(worldName)) {
                slimeWorld = this.aspAPI.readWorld(this.slimeLoader, worldName, false, properties);
            } else {
                properties.setValue(SlimeProperties.DIFFICULTY, this.module.getPlugin().getSettings().getWorlds().getDifficulty().toLowerCase());
                properties.setValue(SlimeProperties.ENVIRONMENT, dimension.getEnvironment().name().toLowerCase());
                slimeWorld = this.aspAPI.createEmptyWorld(worldName, false, properties, this.slimeLoader);
                this.aspAPI.saveWorld(slimeWorld);
            }
            return this.syncLoadWorld(slimeWorld);

        } catch (IOException | UnknownWorldException | CorruptedWorldException | NewerFormatException ex) {
            throw new SlimeWorldModuleException(ex.getMessage(), ex);
        }
    }

    private World syncLoadWorld(SlimeWorld slimeWorld) {
        if (this.module.getPlugin().getServer().isPrimaryThread()) {
            return this.loadWorld(slimeWorld);
        } else {
            CompletableFuture<World> worldFuture = new CompletableFuture<>();
            this.module.getPlugin().getServer().getScheduler().runTask(this.module.getPlugin(), () -> {
                worldFuture.complete(this.loadWorld(slimeWorld));
            });
            return worldFuture.join();
        }
    }

    public CompletableFuture<World> asyncGetWorld(UUID islandUUID, Dimension dimension) {
        CompletableFuture<World> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(this.module.getPlugin(), () -> {
            try {
                World world = this.getWorld(islandUUID, dimension);
                future.complete(world);
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    /**
     * This method will load the Bukkit world from it's SlimeWorld instance.
     * It should be called only from the Main Thread!
     *
     * @param slimeWorld
     * @return bukkitWorld
     */
    public World loadWorld(SlimeWorld slimeWorld) {
        this.aspAPI.loadWorld(slimeWorld, true);
        this.module.getWorldManager().addWorld(slimeWorld);
        return Bukkit.getWorld(slimeWorld.getName());
    }

}
