package ru.xllifi.commands.rewards;

import com.mojang.brigadier.context.CommandContext;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.StatFormatter;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.xllifi.commands.config.Config;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static ru.xllifi.commands.Main.*;

public class RewardManager {
    private static void buildGui(ServerPlayerEntity player) {
        SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
        int playtime = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME, StatFormatter.DEFAULT));
        User lpUser = lpApi.getPlayerAdapter(ServerPlayerEntity.class).getUser(player);

        GuiElementBuilder emptyItem = new GuiElementBuilder()
                .setItem(Items.LIGHT_GRAY_STAINED_GLASS_PANE)
                .hideTooltip()
                .setCustomModelData(728);

        for (Config.RewardItem rewardItem: CONFIG.rewardConfig.items) {
            GuiElementBuilder rewardItemBuilder;
            // Items
            int requiredPlaytime = rewardItem.timeInMinutes*60*20;
            String rewardNode = "suffix.10. "+rewardItem.rewardSuffix;
            Timer timer = new Timer();

            if (!lpUser.getCachedData().getPermissionData().checkPermission(rewardNode).asBoolean()) {
                if (playtime >= requiredPlaytime) {
                    rewardItemBuilder = new GuiElementBuilder()
                            .setItem(Registries.ITEM.get(Identifier.of(rewardItem.eligibleItem.id)))
                            .hideDefaultTooltip()
                            .setName(Text.literal(rewardItem.eligibleItem.item_name))
                            .setCount(rewardItem.eligibleItem.count)
                            .setCallback((index, type, action) -> {
                                ItemStack item = Objects.requireNonNull(gui.getSlot(index)).getItemStack();
                                player.sendMessage(CONFIG.prefix(Text.literal(String.format(rewardItem.chatMessages.eligibleGranted, formatSeconds(playtime/20)))));
                                player.playSoundToPlayer(SoundEvent.of(Identifier.of(rewardItem.rewardSound)), SoundCategory.AMBIENT, 1, 1);
                                lpApi.getUserManager().modifyUser(player.getUuid(), user -> {
                                    user.data().clear(NodeType.SUFFIX::matches);
                                    user.data().add(Node.builder(rewardNode).build());
                                });
                                timer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        buildGui(player);
                                    }
                                }, 500);
                            });
                } else {
                    rewardItemBuilder = new GuiElementBuilder()
                            .setItem(Registries.ITEM.get(Identifier.of(rewardItem.ineligibleItem.id)))
                            .hideDefaultTooltip()
                            .setName(Text.literal(rewardItem.ineligibleItem.item_name))
                            .setCount(rewardItem.ineligibleItem.count)
                            .setCallback((index, type, action) -> {
                                ItemStack item = Objects.requireNonNull(gui.getSlot(index)).getItemStack();

                                item.set(DataComponentTypes.ITEM_NAME, Text.literal(String.format(rewardItem.chatMessages.ineligibleClicked, formatSeconds((requiredPlaytime-playtime)/20))));
                                timer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        item.set(DataComponentTypes.ITEM_NAME, Text.literal(rewardItem.ineligibleItem.item_name));
                                    }
                                }, 3000);
                            });
                }
            } else {
                rewardItemBuilder = new GuiElementBuilder()
                        .setItem(Registries.ITEM.get(Identifier.of(rewardItem.equippedItem.id)))
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
            }
            gui.setSlot(rewardItem.itemIndex, rewardItemBuilder);
        }

        gui.setTitle(Text.literal(CONFIG.rewardConfig.rewardScreenTitle));
        for (int x = 0; x < 9; x++) {
            gui.setSlot(x, emptyItem);
            gui.setSlot(x+18, emptyItem);
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
            int minutes = (seconds / 3600) % 60;
            return String.format(CONFIG.rewardConfig.hoursFormat + " " + CONFIG.rewardConfig.minutesFormat, hours, minutes);
        } else {
            int minutes = seconds / 60;
            return String.format(CONFIG.rewardConfig.minutesFormat, minutes);
        }
    }
}
