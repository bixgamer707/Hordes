package me.bixgamer707.hordes.gui.admin;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.arena.ArenaState;
import me.bixgamer707.hordes.gui.BaseGUI;
import me.bixgamer707.hordes.text.Text;
import org.bukkit.entity.Player;

/**
 * Main admin panel GUI
 * 100% configurable from guis.yml
 */
public class AdminMainGUI extends BaseGUI {

    public AdminMainGUI(Hordes plugin, Player player) {
        super(plugin, player, "admin-main");
    }

    @Override
    protected void buildDynamic() {
        // Update server stats in info item
        updateServerStats();
    }

    /**
     * Updates server statistics
     */
    private void updateServerStats() {
        int totalArenas = plugin.getArenaManager().getArenas().size();
        int activeArenas = 0;

        for (Arena arena : plugin.getArenaManager().getArenas().values()) {
            if (arena.getState() != ArenaState.WAITING && arena.getState() != ArenaState.DISABLED) {
                activeArenas++;
            }
        }

        int onlinePlayers = plugin.getServer().getOnlinePlayers().size();

        // Update placeholders in configured items
        updateItemLore(4, new String[]{
                String.valueOf(totalArenas),
                String.valueOf(activeArenas),
                String.valueOf(onlinePlayers)
        });
    }

    @Override
    protected void handleCustomAction(int slot, String actionType, String actionValue, String itemId) {
        switch (actionType) {
            case "open-arena-manager":
                new ArenaListGUI(plugin, player).open();
                break;

            case "reload-plugin":
                handleReload();
                break;
        }
    }

    /**
     * Handles plugin reload
     */
    private void handleReload() {
        try {
            plugin.reload();
            player.sendMessage(Text.createTextWithLang("admin.reload-success").build());
            playSound("success");
            refresh();
        } catch (Exception e) {
            player.sendMessage(Text.createTextWithLang("admin.reload-failed").build());
            playSound("error");
        }
    }
}