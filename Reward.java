package ru.hideworld.buildroulette;

import org.bukkit.Material;
import java.util.List;

public record Reward(String name, Material material, int chance, List<String> lore, List<String> commands) {}
