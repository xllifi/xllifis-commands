package ru.xllifi.commands.rewards;

import com.mojang.brigadier.context.CommandContext;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.SuffixNode;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.xllifi.commands.config.Config;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static ru.xllifi.commands.Main.*;

public class RewardManager {
    private static void buildGui(ServerPlayerEntity player) {
        SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
        AtomicInteger playtime = new AtomicInteger(player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME)));
        User lpUser = lpApi.getPlayerAdapter(ServerPlayerEntity.class).getUser(player);

        gui.setTitle(Text.literal(CONFIG.rewardConfig.rewardScreenTitle));

        GuiElementBuilder emptyItem = new GuiElementBuilder()
                .setItem(Registries.ITEM.get(Identifier.of(CONFIG.rewardConfig.emptyItem.item_name)))
                .setCount(CONFIG.rewardConfig.emptyItem.count)
                .setName(Text.literal(CONFIG.rewardConfig.emptyItem.item_name))
                .setCustomModelData(List.of(CONFIG.rewardConfig.emptyItem.cmd), List.of(), List.of(), List.of())
                .hideTooltip();

        for (int x = 0; x < 27; x++) gui.setSlot(x, emptyItem);
        for (Config.RewardItem rewardItem: CONFIG.rewardConfig.items) {
            GuiElementBuilder rewardItemBuilder;
            int requiredPlaytime = rewardItem.timeInMinutes*60*20;
            String rewardNode = "suffix.10. %s".formatted(rewardItem.rewardSuffix);
            Timer timer = new Timer();
            if (!lpUser.getNodes(NodeType.SUFFIX).isEmpty() && lpUser.getNodes(NodeType.SUFFIX).stream().allMatch(x -> Objects.equals(x.getKey(), rewardNode))) { // Equpped item
                rewardItemBuilder = new GuiElementBuilder()
                        .setItem(Registries.ITEM.get(Identifier.of(rewardItem.equippedItem.id)))
                        .setCustomModelData(List.of(rewardItem.equippedItem.cmd), List.of(), List.of(), List.of())
                        .hideDefaultTooltip()
                        .setName(Text.literal(rewardItem.equippedItem.item_name))
                        .setCount(rewardItem.equippedItem.count)
                        .glow()
                        .setCallback((index, type, action) -> {
                            ItemStack item = Objects.requireNonNull(gui.getSlot(index)).getItemStack();
                            item.set(DataComponentTypes.ITEM_NAME, Text.literal(rewardItem.chatMessages.equippedClicked));
                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    item.set(DataComponentTypes.ITEM_NAME, Text.literal(rewardItem.equippedItem.item_name));
                                }
                            }, 3000);
                        });
            } else {
                if (playtime.get() >= requiredPlaytime) { // Eligible item
                    rewardItemBuilder = new GuiElementBuilder()
                            .setItem(Registries.ITEM.get(Identifier.of(rewardItem.activeItem.id)))
                            .setCustomModelData(List.of(rewardItem.activeItem.cmd), List.of(), List.of(), List.of())
                            .hideDefaultTooltip()
                            .setName(Text.literal(rewardItem.activeItem.item_name))
                            .setCount(rewardItem.activeItem.count)
                            .setCallback((index, type, action) -> {
//                                ItemStack item = Objects.requireNonNull(gui.getSlot(index)).getItemStack();
                                player.sendMessage(CONFIG.prefix(Text.literal(String.format(rewardItem.chatMessages.activeClicked, formatSeconds(playtime.get() /20)))));
                                player.playSoundToPlayer(SoundEvent.of(Identifier.of(rewardItem.rewardSound)), SoundCategory.AMBIENT, 1, 1);
                                lpApi.getUserManager().modifyUser(player.getUuid(), user -> {
                                    user.data().clear(NodeType.SUFFIX::matches);
                                    user.data().add(Node.builder(rewardNode).build());
                                });

                                LOGGER.info("%s ping: %s".formatted(player.getName().getString(), String.valueOf(player.networkHandler.getLatency())));

                                timer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        buildGui(player);
                                    }
                                }, player.networkHandler.getLatency() == 0 ? 500 : player.networkHandler.getLatency()+150);
                            });
                } else { // Ineligible item
                    rewardItemBuilder = new GuiElementBuilder()
                            .setItem(Registries.ITEM.get(Identifier.of(rewardItem.inactiveItem.id)))
                            .setCustomModelData(List.of(rewardItem.inactiveItem.cmd), List.of(), List.of(), List.of())
                            .hideDefaultTooltip()
                            .setName(Text.literal(rewardItem.inactiveItem.item_name))
                            .setCount(rewardItem.inactiveItem.count)
                            .setCallback((index, type, action) -> {
                                playtime.set(player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME)));
                                if (playtime.get() >= requiredPlaytime) {
                                    buildGui(player);
                                    return;
                                }

                                ItemStack item = Objects.requireNonNull(gui.getSlot(index)).getItemStack();
                                item.set(DataComponentTypes.ITEM_NAME, Text.literal(String.format(rewardItem.chatMessages.inactiveClicked, formatSeconds((requiredPlaytime- playtime.get())/20))));
                                timer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        item.set(DataComponentTypes.ITEM_NAME, Text.literal(rewardItem.inactiveItem.item_name));
                                    }
                                }, 3000);
                            });
                }
            }
            gui.setSlot(rewardItem.itemIndex, rewardItemBuilder);
        }


        gui.open();
    }

    public static int executeCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        buildGui(player);
        return 1;
    }

    private static String formatSeconds(int seconds) {
        int hours = seconds / 3600;
        if (hours != 0) {
            int minutes = (seconds % 3600) / 60;
            return String.format(CONFIG.rewardConfig.hoursFormat + " " + CONFIG.rewardConfig.minutesFormat, hours, minutes);
        } else {
            int minutes = seconds / 60;
            return String.format(CONFIG.rewardConfig.minutesFormat, minutes);
        }
    }
}
