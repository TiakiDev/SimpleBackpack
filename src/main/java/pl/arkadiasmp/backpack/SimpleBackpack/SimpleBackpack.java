package pl.arkadiasmp.simplebackpack;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class SimpleBackpack extends JavaPlugin implements Listener {

    private HashMap<UUID, Inventory> playerBackpacks = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info(ChatColor.AQUA + "Plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("SimpleBackpack plugin has been disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (command.getName().equalsIgnoreCase("backpack")) {
                // Read settings from config
                int size = getConfig().getInt("backpack.size");
                String backpackName = ChatColor.translateAlternateColorCodes('&', getConfig().getString("backpack.name"));
                String messageOnOpen = ChatColor.translateAlternateColorCodes('&', getConfig().getString("backpack.message_on_open"));

                // Read sound settings from config
                String soundName = getConfig().getString("backpack.sound_on_open.sound");
                float volume = (float) getConfig().getDouble("backpack.sound_on_open.volume");
                float pitch = (float) getConfig().getDouble("backpack.sound_on_open.pitch");

                // Validate backpack size (must be a multiple of 9)
                if (size % 9 != 0 || size <= 0 || size > 54) {
                    player.sendMessage("The backpack size must be a multiple of 9 and between 9 and 54 slots.");
                    return true;
                }

                // Load or create a new backpack
                Inventory backpack = loadBackpack(player, size, backpackName);

                // Open backpack for the player
                player.openInventory(backpack);
                player.sendMessage(messageOnOpen);

                // Play sound
                try {
                    Sound sound = Sound.valueOf(soundName.toUpperCase());
                    player.playSound(player.getLocation(), sound, volume, pitch);
                } catch (IllegalArgumentException e) {
                    player.sendMessage("The sound '" + soundName + "' is invalid!");
                }

                return true;
            }
        }

        return false;
    }

    // Save the backpack when the player closes it
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();
        if (playerBackpacks.containsKey(player.getUniqueId()) && playerBackpacks.get(player.getUniqueId()).equals(inventory)) {
            saveBackpack(player, inventory);
        }
    }

    // Method to save the player's backpack
    private void saveBackpack(Player player, Inventory inventory) {
        // Create the playerdata folder if it doesn't exist
        File playerDataFolder = new File(getDataFolder(), "playerdata");
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }

        // Save player data in the playerdata folder
        File backpackFile = new File(playerDataFolder, player.getUniqueId() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(backpackFile);

        // Save items
        config.set("backpack.contents", inventory.getContents());

        try {
            config.save(backpackFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to load the player's backpack
    private Inventory loadBackpack(Player player, int size, String backpackName) {
        // Load data from the playerdata folder
        File playerDataFolder = new File(getDataFolder(), "playerdata");
        File backpackFile = new File(playerDataFolder, player.getUniqueId() + ".yml");

        Inventory backpack = Bukkit.createInventory(player, size, backpackName);

        if (backpackFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(backpackFile);
            List<ItemStack> contents = (List<ItemStack>) config.get("backpack.contents");
            if (contents != null) {
                backpack.setContents(contents.toArray(new ItemStack[0]));
            }
        }

        playerBackpacks.put(player.getUniqueId(), backpack);
        return backpack;
    }
}
