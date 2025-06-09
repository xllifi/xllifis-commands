package ru.xllifi.commands.config;

import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.apache.commons.io.IOUtils;

import java.beans.Transient;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static ru.xllifi.commands.Main.LOGGER;
import static ru.xllifi.commands.Main.gson;

public class Config {
    private transient boolean locked = false;
    public static final Config DEFAULT = new Config();

    // Values to store

    @SerializedName("grant_item")
    public StoredItem[] grantItem = new StoredItem[]{new StoredItem("minecraft:stone", 1, "", 0)};

    @SerializedName("granted_players")
    public ArrayList<UUID> grantedPlayers = new ArrayList<UUID>();

    @SerializedName("message_prefix")
    public String messagePrefix = "§7[Server]§r";

    @SerializedName("_comment")
    public String _comment = "Below are settings of reward system.";

    @SerializedName("reward_config")
    public RewardConfig rewardConfig = new RewardConfig();

    // Atomic locking (?) look into this later

    @Transient
    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    // Types
    public static class StoredItem {
        public String id = "minecraft:stone";
        public int count = 1;
        public String item_name = "";
        public float cmd = 0;
        StoredItem(String id, int count, String item_name, int cmd) {
            this.id = id;
            this.count = count;
            this.item_name = item_name;
            this.cmd = cmd;
        }
    }
    public static class RewardConfig {
        @SerializedName("screen_title")
        public String rewardScreenTitle = "Rewards";
        @SerializedName("hours_format")
        public String hoursFormat = "%dh";
        @SerializedName("minutes_format")
        public String minutesFormat = "%dm";
        @SerializedName("empty_item")
        public StoredItem emptyItem = new StoredItem("minecraft:light_gray_stained_glass_pane", 1, "", 0);
        @SerializedName("items")
        public RewardItem[] items = new RewardItem[]{new RewardItem()};
    }
    public static class RewardItem {
        @SerializedName("item_index")
        public int itemIndex = 9;
        @SerializedName("time_in_minutes")
        public int timeInMinutes = 30;
        @SerializedName("reward_suffix")
        public String rewardSuffix = "30M";
        @SerializedName("reward_suffix_priority")
        public int rewardSuffixPriority = 10;
        @SerializedName("active_item")
        public StoredItem activeItem = new StoredItem("minecraft:iron_ore", 1, "Reward for 30M", 0);
        @SerializedName("inactive_item")
        public StoredItem inactiveItem = new StoredItem("minecraft:stone", 1, "Reward for 30M", 0);
        @SerializedName("equipped_item")
        public StoredItem equippedItem = new StoredItem("minecraft:iron_block", 1, "Reward for 30M (equipped)", 0);
        @SerializedName("chat_messages")
        public RewardItemChatMessages chatMessages = new RewardItemChatMessages();
        @SerializedName("reward_sound")
        public String rewardSound = "minecraft:entity.player.levelup";
        RewardItem() {}
    }
    public static class RewardItemChatMessages {
        @SerializedName("active_clicked")
        public String activeClicked = "Yay! You got a reward because you have played for %s.";
        @SerializedName("inactive_clicked")
        public String inactiveClicked = "You are not eligible for this reward! Play for %s more.";
        @SerializedName("equipped_clicked")
        public String equippedClicked = "This reward is already equipped!";
    }

    // Methods

    public MutableText prefix(MutableText text) {
        if (Objects.equals(messagePrefix, "")) {
            return text;
        } else {
            return Text.empty().append(Text.literal(messagePrefix)).append(Text.literal(" ")).append(text);
        }
    }

    public static Config loadOrCreateConfig() {
        try {
            Config config;
            File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "xllifis-commands.json");

            if (configFile.exists()) {
                String json = IOUtils.toString(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8));

                config = gson.fromJson(json, Config.class);
            } else {
                config = new Config();
            }

            saveConfig(config);
            return config;
        } catch (IOException  exception) {
            LOGGER.error("Something went wrong while reading config!");
            exception.printStackTrace();
            return new Config();
        } catch (Exception e) {
            LOGGER.error("JSON is invalid! Stacktrace:");
            e.getCause();
            return new Config();
        }
    }

    public static void saveConfig(Config config) {
        File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "xllifis-commands.json");
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8));
            writer.write(gson.toJson(config));
            writer.close();
        } catch (Exception e) {
            LOGGER.error("Something went wrong while saving config!");
            e.printStackTrace();
        }
    }
    public boolean validateConfig(Config config) {
        File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "xllifis-commands.json");
        try {
            String json = IOUtils.toString(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8));

            config = gson.fromJson(json, Config.class);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            LOGGER.error("Config is invalid! Stacktrace:");
            e.getCause();
        }
        return true;
    }
}