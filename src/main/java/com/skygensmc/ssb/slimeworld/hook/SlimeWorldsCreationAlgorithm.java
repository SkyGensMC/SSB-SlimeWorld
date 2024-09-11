package com.skygensmc.ssb.slimeworld.hook;

import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.schematic.Schematic;
import com.bgsoftware.superiorskyblock.api.world.Dimension;
import com.bgsoftware.superiorskyblock.api.world.algorithm.IslandCreationAlgorithm;
import com.bgsoftware.superiorskyblock.api.wrappers.BlockPosition;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.skygensmc.ssb.slimeworld.SlimeWorldModule;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SlimeWorldsCreationAlgorithm implements IslandCreationAlgorithm {

    private final SlimeWorldModule module;
    private final IslandCreationAlgorithm originalAlgorithm;

    public SlimeWorldsCreationAlgorithm(SlimeWorldModule module, IslandCreationAlgorithm originalAlgorithm) {
        this.module = module;
        this.originalAlgorithm = originalAlgorithm;
    }

    @Override
    public CompletableFuture<IslandCreationResult> createIsland(UUID uuid, SuperiorPlayer owner, BlockPosition blockPosition, String name, Schematic schematic) {
        return createIsland(Island.newBuilder().setOwner(owner).setUniqueId(uuid).setName(name).setSchematicName(schematic.getName()), blockPosition);
    }

    @Override
    public CompletableFuture<IslandCreationResult> createIsland(Island.Builder builder, BlockPosition blockPosition) {
        Schematic schematic = builder.getScehmaticName() == null ? null : this.module.getPlugin().getSchematics().getSchematic(builder.getScehmaticName());
        Objects.requireNonNull(schematic, "Cannot create an island from builder with invalid schematic name.");

        Dimension dimension = this.module.getPlugin().getSettings().getWorlds().getDefaultWorldDimension();

        CompletableFuture<IslandCreationResult> result = new CompletableFuture<>();

        this.module.slimeWorldProvider().asyncGetWorld(builder.getUniqueId(), dimension).whenComplete((world, error) -> {
            if (error != null) {
                result.completeExceptionally(error);
                return;
            }

            this.originalAlgorithm.createIsland(builder, blockPosition).whenComplete((res, ex) -> {
                if (ex == null) {
                    result.complete(res);
                } else {
                    this.module.getLogger().warning(ex.getMessage());
                    ex.printStackTrace();
                    result.completeExceptionally(ex);
                }
            });
        });

        return result;
    }

}
