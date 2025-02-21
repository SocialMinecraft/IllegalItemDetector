package club.somc.illegalItemDetector;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class IllegalItemDetector extends JavaPlugin implements Listener {

    private List<String> illegalItems;
    private String warningMessage;
    private String broadcastMessage;

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();

        // Load configuration
        loadConfiguration();

        // Register events
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("IllegalItemsPlugin has been enabled!");
    }

    private void loadConfiguration() {
        super.onEnable();
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        // Load values
        illegalItems = config.getStringList("illegal-items");
        warningMessage = ChatColor.translateAlternateColorCodes('&', config.getString("warning-message"));
        broadcastMessage = ChatColor.translateAlternateColorCodes('&', config.getString("broadcast-message"));
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());

        if (item != null && isIllegalItem(item.getType())) {
            // Remove the illegal item
            player.getInventory().setItem(event.getNewSlot(), null);

            // Warn the player
            player.sendMessage(warningMessage);

            // Broadcast to all players
            String announcement = broadcastMessage
                    .replace("{player}", player.getName())
                    .replace("{item}", item.getType().toString());
            Bukkit.broadcastMessage(announcement);

            // Log the incident
            getLogger().warning(player.getName() + " attempted to use illegal item: " + item.getType());
        }
    }

    private boolean isIllegalItem(Material material) {
        return illegalItems.contains(material.toString());
    }

    @Override
    public void onDisable() {
        getLogger().info("IllegalItemsPlugin has been disabled!");
    }
}
