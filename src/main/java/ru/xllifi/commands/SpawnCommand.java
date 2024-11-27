package ru.xllifi.commands;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static ru.xllifi.commands.Main.CONFIG;

public class SpawnCommand {
    public static int executeSpawnCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (source.getPlayer() != null) {
            ServerWorld serverWorld = source.getServer().getOverworld();
            double spawnX = serverWorld.getSpawnPos().getX();
            double spawnY = serverWorld.getSpawnPos().getY();
            double spawnZ = serverWorld.getSpawnPos().getZ();

            source.getPlayer().teleport(serverWorld, spawnX, spawnY, spawnZ,
                    serverWorld.getSpawnAngle(), 0);
            serverWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL, spawnX, spawnY + 1, spawnZ,
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
