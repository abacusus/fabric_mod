package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.block.Blocks;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Random;
import java.util.Set;

public class PrisonMod implements ModInitializer {
    @Override
    public void onInitialize() {
        // Listen for player death caused by another player
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killed) -> {
            if (world.isClient) return;

            if (entity instanceof ServerPlayerEntity killer && killed instanceof ServerPlayerEntity victim) {
                BlockPos deathPos = victim.getBlockPos();

                // Pick random location 300–500 blocks away
                Random rand = new Random();
                int distance = 300 + rand.nextInt(201);
                double angle = rand.nextDouble() * 2 * Math.PI;
                int dx = (int) (Math.cos(angle) * distance);
                int dz = (int) (Math.sin(angle) * distance);
                BlockPos prisonPos = deathPos.add(dx, 64, dz); // y=64 baseline

                // Build prison
                buildPrison(world, prisonPos);

                // Teleport victim
                victim.teleport(
    (ServerWorld) world,
    prisonPos.getX() + 0.5,  // Adding 0.5 centers player in block
    prisonPos.getY() + 1,
    prisonPos.getZ() + 0.5,
    Set.of(),  // Empty set of PositionFlags
    victim.getYaw(),
    victim.getPitch(),
    false  // Whether to disable teleportation restrictions
);

                // Update scoreboard
                // Update scoreboard
Scoreboard scoreboard = world.getScoreboard();
ScoreboardObjective obj = scoreboard.getNullableObjective("prisonedPlayers");
if (obj == null) {
    obj = scoreboard.addObjective(
        "prisonedPlayers",                   // String name
        ScoreboardCriterion.DUMMY,           // ScoreboardCriterion criterion
        Text.literal("Prisoned Players"),    // Text displayName
        ScoreboardCriterion.RenderType.INTEGER, // RenderType renderType
        false,                               // boolean displayAutoUpdate
        null                                 // NumberFormat numberFormat (nullable)
    );
}

                scoreboard.getOrCreateScore(victim, obj).setScore(1);

                // Broadcast
world.getServer().getPlayerManager().broadcast(Text.literal(
    victim.getName().getString() + " is trapped at " + prisonPos.getX() + " " + prisonPos.getY() + " " + prisonPos.getZ()
), false);
            }
        });
    }

    private void buildPrison(net.minecraft.server.world.ServerWorld world, BlockPos pos) {
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 4; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos bp = pos.add(x, y, z);
                    boolean edge = (x == -2 || x == 2 || y == 0 || y == 4 || z == -2 || z == 2);
                    if (edge) {
                        world.setBlockState(bp, Blocks.OBSIDIAN.getDefaultState());
                    } else {
                        world.setBlockState(bp, Blocks.AIR.getDefaultState());
                    }
                }
            }
        }
    }
}
