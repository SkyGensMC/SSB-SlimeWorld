package com.skygensmc.ssb.slimeworld.utils;

import com.bgsoftware.superiorskyblock.api.world.Dimension;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.loaders.api.APILoader;
import com.infernalsuite.aswm.loaders.file.FileLoader;
import com.infernalsuite.aswm.loaders.mongo.MongoLoader;
import com.infernalsuite.aswm.loaders.mysql.MysqlLoader;
import com.skygensmc.ssb.slimeworld.SlimeWorldModule;
import com.skygensmc.ssb.slimeworld.data.DataSourceParams;
import com.skygensmc.ssb.slimeworld.exception.SlimeWorldModuleException;
import org.bukkit.World;

import javax.annotation.Nullable;
import java.io.File;
import java.sql.SQLException;
import java.util.UUID;

public class SlimeUtils {

    private static final String ISLAND_WORLD_PREFIX = "island_";

    public static SlimeLoader getLoader(DataSourceParams dataSourceParams) throws SlimeWorldModuleException {
        switch (dataSourceParams) {
            case DataSourceParams.MongoDB mongoDB -> {
                return new MongoLoader(mongoDB.database, mongoDB.collection, mongoDB.username, mongoDB.password, mongoDB.auth, mongoDB.host, mongoDB.port, mongoDB.url);
            }
            case DataSourceParams.File file -> {
                return new FileLoader(new File(file.path));
            }
            case DataSourceParams.MySQL mySQL -> {
                try {
                    return new MysqlLoader(mySQL.url, mySQL.host, mySQL.port, mySQL.database, mySQL.useSSL, mySQL.username, mySQL.password);
                } catch (SQLException ex) {
                    SlimeWorldModule.instance().getLogger().warning("Unable to create SQLLoader: " + ex.getMessage());
                    throw new IllegalStateException(ex);
                }
            }
            case DataSourceParams.API api -> {
                return new APILoader(api.uri, api.username, api.token, api.ignoreSSLCertificate);
            }
            default -> throw new SlimeWorldModuleException("SlimeLoader not implemented for " + dataSourceParams);
        }
    }

    public static String getWorldName(UUID islandUUID, Dimension dimension) {
        return ISLAND_WORLD_PREFIX + islandUUID + "_" + dimension.getName().toLowerCase();
    }

    @Nullable
    public static Dimension getDimension(String worldName) {
        String[] nameSections = worldName.split("_");

        if (nameSections.length < 3)
            return null;

        StringBuilder environmentName = new StringBuilder();
        for (int i = 2; i < nameSections.length; ++i) {
            environmentName.append("_").append(nameSections[i]);
        }

        return Dimension.getByName(environmentName.substring(1).toUpperCase());
    }

    public static boolean isIslandWorld(SlimeWorld slimeWorld) {
        return isIslandWorld(slimeWorld.getName());
    }

    public static boolean isIslandWorld(World world) {
        return isIslandWorld(world.getName());
    }

    public static boolean isIslandWorld(String worldName) {
        String[] nameSections = worldName.split("_");

        if (nameSections.length < 3)
            return false;

        StringBuilder environmentName = new StringBuilder();
        for (int i = 2; i < nameSections.length; ++i) {
            environmentName.append("_").append(nameSections[i]);
        }

        try {
            UUID.fromString(nameSections[1]);
            World.Environment.valueOf(environmentName.substring(1).toUpperCase());
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

}
