package me.bixgamer707.hordes.gui.player;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.gui.BaseGUI;
import me.bixgamer707.hordes.statistics.PlayerStatistics;
import me.bixgamer707.hordes.text.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Leaderboard GUI - Shows top players
 * 100% configurable from guis.yml
 */
public class LeaderboardGUI extends BaseGUI {

    private String currentCategory;
    private static final int ENTRIES_PER_PAGE = 28;
    private static final int[] ENTRY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public LeaderboardGUI(Hordes plugin, Player player) {
        this(plugin, player, "completions");
    }

    public LeaderboardGUI(Hordes plugin, Player player, String category) {
        super(plugin, player, "leaderboard");
        this.currentCategory = category;
    }

    @Override
    protected void buildDynamic() {
        // Load leaderboard data based on category
        List<PlayerStatistics> topPlayers = getTopPlayers(currentCategory, ENTRIES_PER_PAGE);

        // Display entries
        for (int i = 0; i < topPlayers.size() && i < ENTRY_SLOTS.length; i++) {
            PlayerStatistics stats = topPlayers.get(i);
            int slot = ENTRY_SLOTS[i];
            int position = i + 1;

            ItemStack entry = createEntryItem(stats, position);
            inventory.setItem(slot, entry);
        }

        // Update your position
        updateYourPosition();
    }

    private List<PlayerStatistics> getTopPlayers(String category, int limit) {
        if (!plugin.getStatisticsManager().isEnabled()) {
            return new ArrayList<>();
        }

        switch (category) {
            case "completions":
                return plugin.getStatisticsManager().getTopByCompletions(limit);
            case "kills":
                return plugin.getStatisticsManager().getTopByKills(limit);
            case "speed":
                return plugin.getStatisticsManager().getTopBySpeed(limit);
            case "winrate":
                // Not implemented yet
                return new ArrayList<>();
            default:
                return new ArrayList<>();
        }
    }

    private ItemStack createEntryItem(PlayerStatistics stats, int position) {
        Material material = getMaterialForPosition(position);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String medal = getMedal(position);
            String name = medal + " &7#" + position + " &f" + stats.getPlayerName();
            meta.setDisplayName(Text.createText(name).build(player));

            List<String> lore = new ArrayList<>();
            String statValue = getStatValue(stats, currentCategory);
            lore.add(Text.createText("&7" + getCategoryName() + ": &e" + statValue).build(player));

            meta.setLore(lore);

            // Set skull owner if player head
            if (material == Material.PLAYER_HEAD && meta instanceof SkullMeta) {
                ((SkullMeta) meta).setOwner(stats.getPlayerName());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    private Material getMaterialForPosition(int position) {
        if (position == 1) return Material.GOLD_BLOCK;
        if (position == 2) return Material.IRON_BLOCK;
        if (position == 3) return Material.COPPER_BLOCK;
        return Material.PLAYER_HEAD;
    }

    private String getMedal(int position) {
        switch (position) {
            case 1: return "§6🥇";
            case 2: return "§7🥈";
            case 3: return "§c🥉";
            default: return "§7 ";
        }
    }

    private String getStatValue(PlayerStatistics stats, String category) {
        switch (category) {
            case "completions":
                return String.valueOf(stats.getTotalCompletions());
            case "kills":
                return String.valueOf(stats.getTotalKills());
            case "speed":
                return formatTime(stats.getFastestCompletion());
            case "winrate":
                return String.format("%.1f%%", stats.getWinRate());
            default:
                return "N/A";
        }
    }

    private String getCategoryName() {
        return Text.getMessages().getString("Messages.leaderboard." + currentCategory, currentCategory);
    }

    private String formatTime(long seconds) {
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return minutes + "m " + secs + "s";
    }

    private void updateYourPosition() {
        String path = "guis." + guiId + ".items.your-position";

        // If this item isn't configured for this GUI, there's nothing to update
        if (!guiConfig.contains(path)) {
            return;
        }

        int slot = guiConfig.getInt(path + ".slot", 49);

        String rankDisplay = "N/A";
        String valueDisplay = "N/A";

        if (plugin.getStatisticsManager() != null && plugin.getStatisticsManager().isEnabled()) {
            int rank = plugin.getStatisticsManager().getRank(player.getUniqueId(), currentCategory);
            PlayerStatistics myStats = plugin.getStatisticsManager()
                    .getStatistics(player.getUniqueId(), player.getName());

            if (rank > 0) {
                rankDisplay = String.valueOf(rank);
            }
            if (myStats != null) {
                valueDisplay = getStatValue(myStats, currentCategory);
            }
        }

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();

        if (meta instanceof SkullMeta) {
            ((SkullMeta) meta).setOwner(player.getName());
        }

        if (meta != null) {
            String name = guiConfig.getString(path + ".name", "&e&lYour Position");
            meta.setDisplayName(Text.createText(name).build(player));

            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList(path + ".lore")) {
                line = line.replace("{your_rank}", rankDisplay)
                        .replace("{stat_name}", getCategoryName())
                        .replace("{your_value}", valueDisplay);
                lore.add(Text.createText(line).build(player));
            }
            meta.setLore(lore);

            if (guiConfig.getBoolean(path + ".glow", false)) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
    }

    @Override
    protected void handleCustomAction(int slot, String actionType, String actionValue, String itemId) {
        if (actionType.equals("category")) {
            currentCategory = actionValue;
            refresh();
        }
    }
}