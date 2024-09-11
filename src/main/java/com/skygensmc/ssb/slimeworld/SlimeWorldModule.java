package com.skygensmc.ssb.slimeworld;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblock;
import com.bgsoftware.superiorskyblock.api.commands.SuperiorCommand;
import com.bgsoftware.superiorskyblock.api.modules.ModuleLoadTime;
import com.bgsoftware.superiorskyblock.api.modules.PluginModule;
import com.bgsoftware.superiorskyblock.api.world.algorithm.IslandCreationAlgorithm;
import com.infernalsuite.aswm.api.AdvancedSlimePaperAPI;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.skygensmc.ssb.slimeworld.config.SettingsManager;
import com.skygensmc.ssb.slimeworld.exception.SlimeWorldModuleException;
import com.skygensmc.ssb.slimeworld.hook.SlimeWorldProvider;
import com.skygensmc.ssb.slimeworld.hook.SlimeWorldsCreationAlgorithm;
import com.skygensmc.ssb.slimeworld.listener.IslandsListener;
import com.skygensmc.ssb.slimeworld.utils.SlimeUtils;
import com.skygensmc.ssb.slimeworld.utils.WorldManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Listener;

import javax.annotation.Nullable;
import java.io.IOException;

public class SlimeWorldModule extends PluginModule {

    private static SlimeWorldModule instance;

    @Getter
    private SuperiorSkyblock plugin;
    private SettingsManager settingsManager;
    private AdvancedSlimePaperAPI aspAPI;
    private SlimeLoader slimeLoader;
    private SlimeWorldProvider slimeWorldProvider;
    @Getter
    private WorldManager worldManager;

    public SlimeWorldModule() {
        super(SlimeWorldModule.class.getSimpleName(), "grayr0ot");
        instance = this;
    }

    @Override
    public void onEnable(SuperiorSkyblock plugin) {
        this.plugin = plugin;
        this.settingsManager = new SettingsManager(this);
        this.aspAPI = AdvancedSlimePaperAPI.instance();
        this.worldManager = new WorldManager(this);

        try {
            this.slimeLoader = SlimeUtils.getLoader(this.settingsManager.dataSource);
            loadWorldsProvider();
            loadCreationAlgorithm();
        } catch (SlimeWorldModuleException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void onReload(SuperiorSkyblock plugin) {

    }

    @Override
    public void onDisable(SuperiorSkyblock plugin) {
        for (World world : Bukkit.getWorlds()) {
            if (!SlimeUtils.isIslandWorld(world)) continue;
            long start = System.currentTimeMillis();
            SlimeWorld slimeWorld = this.aspAPI.getLoadedWorld(world.getName());
            try {
                this.aspAPI.saveWorld(slimeWorld);
                getLogger().info("Unloaded island world " + world.getName() + " in " + (System.currentTimeMillis() - start) + "ms");
            } catch (IOException ex) {
                getLogger().warning("Unable to unload island world " + world.getName());
                ex.printStackTrace();
            }
        }
    }

    @Override
    public Listener[] getModuleListeners(SuperiorSkyblock plugin) {
        return new Listener[]{new IslandsListener(this)};
    }

    @Nullable
    @Override
    public SuperiorCommand[] getSuperiorCommands(SuperiorSkyblock plugin) {
        return null;
    }

    @Nullable
    @Override
    public SuperiorCommand[] getSuperiorAdminCommands(SuperiorSkyblock plugin) {
        return null;
    }

    @Override
    public ModuleLoadTime getLoadTime() {
        return ModuleLoadTime.BEFORE_WORLD_CREATION;
    }

    public SettingsManager getSettings() {
        return settingsManager;
    }

    private void loadWorldsProvider() {
        this.slimeWorldProvider = new SlimeWorldProvider(this);
        plugin.getProviders().setWorldsProvider(this.slimeWorldProvider);
    }

    private void loadCreationAlgorithm() {
        IslandCreationAlgorithm islandCreationAlgorithm = plugin.getGrid().getIslandCreationAlgorithm();
        plugin.getGrid().setIslandCreationAlgorithm(new SlimeWorldsCreationAlgorithm(this, islandCreationAlgorithm));
    }

    public SlimeWorldProvider slimeWorldProvider() {
        return this.slimeWorldProvider;
    }

    public AdvancedSlimePaperAPI slimeAPI() {
        return this.aspAPI;
    }

    public SlimeLoader slimeLoader() {
        return this.slimeLoader;
    }

    public static SlimeWorldModule instance() {
        return instance;
    }

}
