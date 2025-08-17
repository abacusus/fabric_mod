package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.block.Blocks;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map; // 
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class PrisonMod implements ModInitializer {

    // Store prison locations for victims
    private final Map<UUID, BlockPos> pendingPrisons = new HashMap<>();

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
                buildPrison((ServerWorld) world, prisonPos);

                // Save victim’s UUID + prison position for respawn
                pendingPrisons.put(victim.getUuid(), prisonPos);

                // Messages
                killer.sendMessage(Text.literal("You trapped " + victim.getName().getString()), false);
                victim.sendMessage(Text.literal("You will be imprisoned after respawn..."), false);

                // ✅ scoreboard update INSIDE same lambda where victim exists
                Scoreboard scoreboard = world.getScoreboard();
                ScoreboardObjective obj = scoreboard.getNullableObjective("prisonedPlayers");
                if (obj == null) {
                    obj = scoreboard.addObjective(
                            "prisonedPlayers",
                            ScoreboardCriterion.DUMMY,
                            Text.literal("Prisoned Players"),
                            ScoreboardCriterion.RenderType.INTEGER,
                            false,
                            null
                    );
                }
                scoreboard.getOrCreateScore(victim, obj).setScore(1);

                // Broadcast
                world.getServer().getPlayerManager().broadcast(
                        Text.literal(victim.getName().getString() + " is trapped at " +
                                prisonPos.getX() + " " +
                                prisonPos.getY() + " " +
                                prisonPos.getZ()),
                        false
                );
            }
        });

        // After respawn (⚠ must be registered OUTSIDE combat event)
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            BlockPos prisonPos = pendingPrisons.remove(newPlayer.getUuid());
            if (prisonPos != null) {
                newPlayer.teleport(
    (ServerWorld) newPlayer.getWorld(),
    prisonPos.getX() + 0.5,
    prisonPos.getY() + 1,
    prisonPos.getZ() + 0.5,
    Set.of(),                
    newPlayer.getYaw(),
    newPlayer.getPitch(),
    false                    
);

                newPlayer.sendMessage(Text.literal("You have been imprisoned!"), false);
            }
        });
    }

    private void buildPrison(ServerWorld world, BlockPos pos) {
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
