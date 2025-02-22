package club.somc.illegalItemDetector;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
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

        if (checkAndRemoveIllegalItem(player, item, "main hand")) {
            player.getInventory().setItem(event.getNewSlot(), null);
            player.updateInventory();
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        boolean illegalItemFound = false;

        // Check offhand item
        ItemStack offhandItem = event.getOffHandItem();
        if (checkAndRemoveIllegalItem(player, offhandItem, "off hand")) {
            illegalItemFound = true;
        }

        // Check mainhand item as well
        ItemStack mainhandItem = event.getMainHandItem();
        if (checkAndRemoveIllegalItem(player, mainhandItem, "main hand")) {
            illegalItemFound = true;
        }

        if (illegalItemFound) {
            // Cancel the event first
            event.setCancelled(true);

            // Then directly remove any illegal items from both hands
            if (offhandItem != null && isIllegalItem(offhandItem.getType())) {
                player.getInventory().setItemInOffHand(null);
            }

            if (mainhandItem != null && isIllegalItem(mainhandItem.getType())) {
                int slot = player.getInventory().getHeldItemSlot();
                player.getInventory().setItem(slot, null);
            }

            player.updateInventory();
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();

        if (checkAndRemoveIllegalItem(player, item, "inventory")) {
            // Cancel the event to prevent the item from being moved/used
            event.setCancelled(true);

            // Directly remove the item from the inventory
            event.setCurrentItem(null);

            // Also check and remove cursor item if it's illegal
            ItemStack cursorItem = event.getCursor();
            if (cursorItem != null && isIllegalItem(cursorItem.getType())) {
                event.setCursor(null);
                notifyIllegalItem(player, cursorItem, "cursor");
            }

            player.updateInventory();
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        ItemStack item = event.getItem().getItemStack();

        if (isIllegalItem(item.getType())) {
            event.setCancelled(true);
            event.getItem().remove();

            // Warn the player
            player.sendMessage(warningMessage);

            // Broadcast to all players
            String announcement = broadcastMessage
                    .replace("{player}", player.getName())
                    .replace("{item}", item.getType().toString());
            Bukkit.broadcastMessage(announcement);

            // Log the incident
            getLogger().warning(player.getName() + " attempted to pick up illegal item: " + item.getType());
        }
    }

    private void notifyIllegalItem(Player player, ItemStack item, String source) {
        // Warn the player
        player.sendMessage(warningMessage);

        // Broadcast to all players
        String announcement = broadcastMessage
                .replace("{player}", player.getName())
                .replace("{item}", item.getType().toString());
        Bukkit.broadcastMessage(announcement);

        // Log the incident
        getLogger().warning(player.getName() + " had illegal item in " + source + ": " + item.getType());
    }

    private boolean checkAndRemoveIllegalItem(Player player, ItemStack item, String source) {
        if (item != null && isIllegalItem(item.getType())) {
            // Warn the player
            player.sendMessage(warningMessage);

            // Broadcast to all players
            String announcement = broadcastMessage
                    .replace("{player}", player.getName())
                    .replace("{item}", item.getType().toString());
            Bukkit.broadcastMessage(announcement);

            // Log the incident
            getLogger().warning(player.getName() + " attempted to use illegal item in " + source + ": " + item.getType());

            return true;
        }

        return false;
    }

    private boolean isIllegalItem(Material material) {
        return illegalItems.contains(material.toString());
    }

    @Override
    public void onDisable() {
        getLogger().info("IllegalItemsPlugin has been disabled!");
    }
}
