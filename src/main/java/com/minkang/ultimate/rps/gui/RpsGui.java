
package com.minkang.ultimate.rps.gui;

import com.minkang.ultimate.rps.UltimateRpsPlugin;
import com.minkang.ultimate.rps.game.GameSession;
import com.minkang.ultimate.rps.game.RpsChoice;
import com.minkang.ultimate.rps.station.Station;
import com.minkang.ultimate.rps.util.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.Collections;

public class RpsGui implements InventoryHolder {

    public static final int SLOT_PLAYER_HEAD = 1; // 2번째칸 (0-indexed)
    public static final int SLOT_SYSTEM_HEAD = 5; // 6번째칸
    public static final int SLOT_ROCK = 19; // 20칸
    public static final int SLOT_PAPER = 21; // 22칸
    public static final int SLOT_SCISSORS = 23; // 24칸
    public static final int SLOT_START = 30; // 31칸
    public static final int SLOT_COIN = 48; // 49칸
    public static final int SLOT_LEFT = 47; // 48칸
    public static final int SLOT_RIGHT = 49; // 50칸

    private final UltimateRpsPlugin plugin;
    private final Station station;
    private final Player player;
    private final Inventory inv;
    private final GameSession session;

    public RpsGui(UltimateRpsPlugin plugin, Player player, Station station) {
        this.plugin = plugin;
        this.player = player;
        this.station = station;
        String title = color(plugin.getConfig().getString("gui.rps-title", "&0가위바위보 - {name}").replace("{name}", station.getName()));
        this.inv = Bukkit.createInventory(this, 54, title);
        this.session = new GameSession(player, station);
        draw();
    }

    public static void open(Player p, Station st) {
        UltimateRpsPlugin plugin = UltimateRpsPlugin.get();
        RpsGui g = new RpsGui(plugin, p, st);
        p.openInventory(g.getInventory());
    }

    private void draw() {
        ItemStack pane = ItemUtils.named(Material.BLACK_STAINED_GLASS_PANE, color(plugin.getConfig().getString("gui.pane-name","&0 ")));
        for (int i=0;i<54;i++) inv.setItem(i, pane);

        // heads
        inv.setItem(SLOT_PLAYER_HEAD, playerHead(player.getName(), color(plugin.getConfig().getString("gui.player-head-name","&b내 선택: &f{choice}").replace("{choice}","-"))));
        ItemStack sys = playerHead("MHF_Question", color(plugin.getConfig().getString("gui.system-head-name","&d시스템 선택: &f{choice}").replace("{choice}","-")));
        sys.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.LUCK, 1);
        ItemUtils.hideAllFlags(sys);
        inv.setItem(SLOT_SYSTEM_HEAD, sys);

        // picks
        inv.setItem(SLOT_ROCK, ItemUtils.lore(ItemUtils.named(Material.STONE, color(plugin.getConfig().getString("gui.pick-rock-name","&f&l바위"))),
                Collections.singletonList(color("&7클릭하여 선택"))));
        inv.setItem(SLOT_PAPER, ItemUtils.lore(ItemUtils.named(Material.PAPER, color(plugin.getConfig().getString("gui.pick-paper-name","&f&l보"))),
                Collections.singletonList(color("&7클릭하여 선택"))));
        inv.setItem(SLOT_SCISSORS, ItemUtils.lore(ItemUtils.named(Material.SHEARS, color(plugin.getConfig().getString("gui.pick-scissors-name","&f&l가위"))),
                Collections.singletonList(color("&7클릭하여 선택"))));

        // start button
        inv.setItem(SLOT_START, ItemUtils.lore(ItemUtils.named(Material.LIME_DYE, color(plugin.getConfig().getString("gui.start-button-name","&a&l시작"))),
                ItemUtils.colorLore(plugin.getConfig().getStringList("gui.start-button-lore"))));

        // coin slot and instructions
        inv.setItem(SLOT_LEFT, ItemUtils.named(Material.OAK_SIGN, color(plugin.getConfig().getString("gui.coin-instruction-left"))));
        inv.setItem(SLOT_RIGHT, ItemUtils.named(Material.OAK_SIGN, color(plugin.getConfig().getString("gui.coin-instruction-right"))));
        inv.setItem(SLOT_COIN, new ItemStack(Material.AIR));
    }

    public Inventory getInventory() { return inv; }

    public GameSession getSession() { return session; }

    public void updatePlayerChoice(RpsChoice choice) {
        session.playerChoice = choice;
        inv.setItem(SLOT_PLAYER_HEAD, playerHead(player.getName(),
                color(plugin.getConfig().getString("gui.player-head-name").replace("{choice}", choice.getKr()))));
    }

    public void updateSystemChoice(RpsChoice choice) {
        session.systemChoice = choice;
        ItemStack sys = playerHead("MHF_Question", color(plugin.getConfig().getString("gui.system-head-name").replace("{choice}", choice.getKr())));
        sys.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.LUCK, 1);
        ItemUtils.hideAllFlags(sys);
        inv.setItem(SLOT_SYSTEM_HEAD, sys);
    }

    private ItemStack playerHead(String owner, String name) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwner(owner);
        meta.setDisplayName(name);
        skull.setItemMeta(meta);
        return skull;
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }
}
