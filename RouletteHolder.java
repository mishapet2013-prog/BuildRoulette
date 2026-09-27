package ru.hideworld.buildroulette;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class RouletteHolder implements InventoryHolder {
    private Inventory inventory;

    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public @NotNull Inventory getInventory() { return inventory; }
}
