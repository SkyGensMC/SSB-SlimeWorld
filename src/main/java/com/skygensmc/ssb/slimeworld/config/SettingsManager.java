package com.skygensmc.ssb.slimeworld.config;


import com.bgsoftware.common.config.CommentedConfiguration;
import com.skygensmc.ssb.slimeworld.SlimeWorldModule;
import com.skygensmc.ssb.slimeworld.data.DataSourceParams;

import java.io.File;

public class SettingsManager {

    public final com.skygensmc.ssb.slimeworld.data.DataSourceParams dataSource;
    public final int unloadDelay;
    public final boolean teleportToIsland;

    public SettingsManager(SlimeWorldModule module) {
        File file = new File(module.getModuleFolder(), "config.yml");

        if (!file.exists())
            module.saveResource("config.yml");

        CommentedConfiguration config = CommentedConfiguration.loadConfiguration(file);
        convertData(config);

        try {
            config.syncWithConfig(file, module.getResource("config.yml"));
        } catch (Exception error) {
            error.printStackTrace();
        }

        this.dataSource = DataSourceParams.parse(config.getConfigurationSection("data-source"));
        this.unloadDelay = config.getInt("unload-delay");
        this.teleportToIsland = config.getBoolean("teleport-to-island", true);
    }

    private static void convertData(CommentedConfiguration config) {
        if (config.isString("data-source")) {
            String dataSourceType = config.getString("data-source");
            config.set("data-source", null);
            config.set("data-source.type", dataSourceType);
            config.set("data-source.file.path", "slime_worlds");
        }
    }

}