package ru.hideworld.buildroulette;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class RouletteListener implements Listener {
    private final BuildRoulettePlugin plugin;
    private final Random random = new Random();

    public RouletteListener(BuildRoulettePlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Structure structure = findStructure(event.getBlockPlaced());
        if (structure == null) return;
        structure.remove();
        Player player = event.getPlayer();
        player.sendMessage(color(plugin.getConfig().getString("messages.activated")));
        Bukkit.getScheduler().runTaskLater(plugin, () -> startRoulette(player), 2L);
    }

    // Ищет фигуру из 3 нижних блоков и 2 блоков над центральным.
    private Structure findStructure(Block placed) {
        for (int dy = -2; dy <= 0; dy++) {
            Block center = placed.getRelative(0, dy, 0);
            Structure alongX = new Structure(center, 1, 0);
            if (alongX.complete()) return alongX;
            Structure alongZ = new Structure(center, 0, 1);
            if (alongZ.complete()) return alongZ;
        }
        return null;
    }

    private void startRoulette(Player player) {
        if (!player.isOnline()) return;
        List<Reward> rewards = loadRewards();
        if (rewards.isEmpty()) {
            player.sendMessage(color("&cВ config.yml не настроены награды."));
            return;
        }
        RouletteHolder holder = new RouletteHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27, color(plugin.getConfig().getString("roulette-title")));
        holder.setInventory(inventory);
        fill(inventory);
        player.openInventory(inventory);
        int duration = plugin.getConfig().getInt("roulette-duration-ticks", 100);

        new BukkitRunnable() {
            int elapsed = 0;
            @Override public void run() {
                if (!player.isOnline() || !(player.getOpenInventory().getTopInventory().getHolder() instanceof RouletteHolder)) { cancel(); return; }
                Reward shown = randomReward(rewards);
                inventory.setItem(13, item(shown, false));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.7f, 1.4f);
                elapsed += 5;
                if (elapsed >= duration) {
                    Reward win = randomReward(rewards);
                    inventory.setItem(13, item(win, true));
                    give(player, win);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                    player.sendMessage(color(plugin.getConfig().getString("messages.win").replace("%reward%", win.name())));
                    cancel();
                    Bukkit.getScheduler().runTaskLater(plugin, player::closeInventory, 60L);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private List<Reward> loadRewards() {
        List<Reward> list = new ArrayList<>();
        var section = plugin.getConfig().getConfigurationSection("rewards");
        if (section == null) return list;
        for (String id : section.getKeys(false)) {
            String p = "rewards." + id + ".";
            Material material = Material.matchMaterial(plugin.getConfig().getString(p + "material", "CHEST"));
            list.add(new Reward(color(plugin.getConfig().getString(p + "name", "&fНаграда")), material == null ? Material.CHEST : material, Math.max(1, plugin.getConfig().getInt(p + "chance", 1)), plugin.getConfig().getStringList(p + "lore"), plugin.getConfig().getStringList(p + "commands")));
        }
        return list;
    }

    private Reward randomReward(List<Reward> rewards) {
        int total = rewards.stream().mapToInt(Reward::chance).sum();
        int value = random.nextInt(total) + 1;
        for (Reward reward : rewards) { value -= reward.chance(); if (value <= 0) return reward; }
        return rewards.getFirst();
    }

    private void give(Player player, Reward reward) {
        for (String command : reward.commands()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()).replace("%uuid%", player.getUniqueId().toString()));
    }

    private void fill(Inventory inventory) {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta(); meta.setDisplayName(" "); pane.setItemMeta(meta);
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, pane);
        inventory.setItem(4, pane); inventory.setItem(22, pane);
    }

    private ItemStack item(Reward reward, boolean winner) {
        ItemStack result = new ItemStack(reward.material());
        ItemMeta meta = result.getItemMeta();
        meta.setDisplayName(winner ? color("&6&lПОБЕДА! &r") + reward.name() : reward.name());
        List<String> lore = new ArrayList<>();
        if (winner) lore.add(color("&eЭтот приз выдан вам.")); else lore.add(color("&8Возможная награда"));
        lore.add("");
        for (String line : reward.lore()) lore.add(color(line));
        meta.setLore(lore); meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); result.setItemMeta(meta);
        return result;
    }

    @EventHandler public void onClick(InventoryClickEvent event) { if (event.getView().getTopInventory().getHolder() instanceof RouletteHolder) event.setCancelled(true); }
    @EventHandler public void onDrag(InventoryDragEvent event) { if (event.getView().getTopInventory().getHolder() instanceof RouletteHolder) event.setCancelled(true); }
    private String color(String text) { return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text); }

    private static final class Structure {
        private final Block center; private final int dx; private final int dz;
        private Structure(Block center, int dx, int dz) { this.center = center; this.dx = dx; this.dz = dz; }
        private List<Block> blocks() { return List.of(center.getRelative(-dx, 0, -dz), center, center.getRelative(dx, 0, dz), center.getRelative(0, 1, 0), center.getRelative(0, 2, 0)); }
        private boolean complete() { return blocks().stream().allMatch(block -> !block.getType().isAir()); }
        private void remove() { for (Block block : blocks()) block.setType(Material.AIR, false); }
    }
}
