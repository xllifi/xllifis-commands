package ru.xllifi.commands;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ru.xllifi.commands.Main.CONFIG;

public class SpawnCommand {
    public static int executeSpawnCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (source.getPlayer() != null) {
            ServerWorld serverWorld = source.getServer().getOverworld();
            double spawnX = serverWorld.getSpawnPos().getX();
            double spawnY = serverWorld.getSpawnPos().getY();
            double spawnZ = serverWorld.getSpawnPos().getZ();

            source.getPlayer().teleport(serverWorld, spawnX + 0.5, spawnY, spawnZ + 0.5, new HashSet<>(), serverWorld.getSpawnAngle(), 0, false);
            serverWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL, spawnX + 0.5, spawnY + 1, spawnZ + 0.5,
                    64, .2, .5, .2, 1);
            source.getPlayer().playSoundToPlayer(SoundEvent.of(Identifier.of("minecraft:entity.player.teleport")), SoundCategory.PLAYERS, 1, 1);
            source.sendFeedback(() -> CONFIG.prefix(Text.translatable("text.xllifiscommands.spawn.success")), false);
            return 1;
        } else {
            source.sendFeedback(() -> CONFIG.prefix(Text.translatable("text.xllifiscommands.generic.fail.console")), false);
            return 0;
        }
    };
}
