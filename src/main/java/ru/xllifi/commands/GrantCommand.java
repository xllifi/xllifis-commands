package ru.xllifi.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.StringUtils;
import ru.xllifi.commands.config.Config;

import java.util.List;
import java.util.Objects;

import static ru.xllifi.commands.Main.CONFIG;
import static ru.xllifi.commands.Main.LOGGER;

public class GrantCommand {
    public static int executeGrantCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (CONFIG.isLocked()) {
            source.sendFeedback(() -> CONFIG.prefix(Text.translatable("text.xllifiscommands.grant.fail.locked")), false);
            return 0;
        }

        if (!source.isExecutedByPlayer()) {
            source.sendFeedback(() -> CONFIG.prefix(Text.translatable("text.xllifiscommands.generic.fail.console")), false);
            return 0;
        }
        if (CONFIG.grantedPlayers.contains(Objects.requireNonNull(source.getPlayer()).getUuid())) {
            source.sendFeedback(() -> CONFIG.prefix(Text.translatable("text.xllifiscommands.grant.fail.already_granted")), false);
            return 0;
        }
        /*
         2 'for' loops are needed because I don't want players to only receive some items because
         that creates confusion if player should be marked as granted or not.
        */
        for (Config.StoredItem storedItem : CONFIG.grantItem) {
            if (!Registries.ITEM.containsId(Identifier.of(storedItem.id))) {
                LOGGER.error("Error occurred when granting: No such item {}", storedItem.id);
                source.sendFeedback(() -> CONFIG.prefix(Text.translatable("text.xllifiscommands.grant.fail.bad_item")), false);
                return 0;
            }
        }
        for (Config.StoredItem storedItem : CONFIG.grantItem) {
            ItemStack itemStack = new ItemStack(Registries.ITEM.get(Identifier.of(storedItem.id)));
            itemStack.setCount(storedItem.count);
            if (StringUtils.isNotBlank(storedItem.item_name)) {
                itemStack.set(DataComponentTypes.ITEM_NAME, Text.literal(storedItem.item_name));
            }
            source.getPlayer().giveItemStack(itemStack);
        }
        CONFIG.setLocked(true);
        LOGGER.info("Old list: " + CONFIG.grantedPlayers.toString());
        CONFIG.grantedPlayers.add(source.getPlayer().getUuid());
        LOGGER.info("New list: " + CONFIG.grantedPlayers.toString());
        Config.saveConfig(CONFIG);
        CONFIG.setLocked(false);
        source.sendFeedback(() -> CONFIG.prefix(Text.translatable("text.xllifiscommands.grant.success")), false);
        return 1;
    }
    public static int resetGranted(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        if (CONFIG.isLocked()) {
            source.sendFeedback(() -> CONFIG.prefix(Text.translatable("text.xllifiscommands.grant.fail.locked")), false);
            return 0;
        }
        List<String> playerNames = new java.util.ArrayList<>(List.of());

        for(PlayerEntity player : EntityArgumentType.getPlayers(context, "targets")) {
            playerNames.add(Objects.requireNonNull(player.getDisplayName()).getString());
            CONFIG.grantedPlayers.remove(player.getUuid());
        }
        CONFIG.setLocked(true);
        Config.saveConfig(CONFIG);
        CONFIG.setLocked(false);
        source.sendFeedback(() -> CONFIG.prefix(Text.translatable("text.xllifiscommands.grant.reset.success", playerNames.toString())), true);
        return 1;
    }
}
