package ru.xllifi.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.apache.logging.log4j.core.jmx.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.xllifi.commands.config.Config;
import ru.xllifi.commands.rewards.RewardManager;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

public class Main implements ModInitializer {
	public static final String MOD_ID = "xllifis-commands";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static Config CONFIG = new Config();
	public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	public static LuckPerms lpApi;

	public static void registerXllifiCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			if (environment.dedicated) {
				// Spawn commands
				dispatcher.register(CommandManager.literal("spawn")
						.executes(SpawnCommand::executeSpawnCommand)
				);
				dispatcher.register(CommandManager.literal("спавн")
						.executes(SpawnCommand::executeSpawnCommand)
				);
				// Grant commands
				dispatcher.register(CommandManager.literal("grant")
						.then(CommandManager.literal("reset")
								.then(CommandManager.argument("targets", EntityArgumentType.players())
										.requires(source -> source.hasPermissionLevel(3))
										.executes(GrantCommand::resetGranted)
								)
						)
						.executes(GrantCommand::executeGrantCommand)
				);
				dispatcher.register(CommandManager.literal("приват")
						.executes(GrantCommand::executeGrantCommand)
				);
				// Config reload commands
				dispatcher.register(CommandManager.literal("xcreload")
						.requires(source -> source.hasPermissionLevel(3))
						.executes(context -> {
							Main.CONFIG = Config.loadOrCreateConfig();
							context.getSource().sendFeedback(() -> CONFIG.prefix(Text.translatable("text.xllifiscommands.reload.success")), false);
							return 1;
						})
				);
				dispatcher.register(CommandManager.literal("rewards")
						.requires(source -> source.hasPermissionLevel(4))
						.executes(RewardManager::executeCommand)
				);
				dispatcher.register(CommandManager.literal("награды")
						.requires(source -> source.hasPermissionLevel(4))
						.executes(RewardManager::executeCommand)
				);
			}
		});
	}

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		ServerLifecycleEvents.SERVER_STARTING.register((s) -> {
			Main.CONFIG = Config.loadOrCreateConfig();
		});
		ServerLifecycleEvents.SERVER_STARTED.register((s) -> {
			lpApi = LuckPermsProvider.get();
		});
		ServerLifecycleEvents.SERVER_STOPPING.register((s) -> {
			List<ServerPlayerEntity> playerEntities = new ArrayList<>(s.getPlayerManager().getPlayerList());
			for (ServerPlayerEntity player : playerEntities) {
				player.networkHandler.disconnect(Text.literal("Server closed"));
			}
		});

		LOGGER.info("xllifi's Commands initialized!");
		registerXllifiCommands();
	}
}