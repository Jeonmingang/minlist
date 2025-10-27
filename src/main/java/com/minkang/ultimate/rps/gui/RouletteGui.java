
package com.minkang.ultimate.rps.gui;

import com.minkang.ultimate.rps.UltimateRpsPlugin;
import com.minkang.ultimate.rps.station.Station;
import com.minkang.ultimate.rps.util.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RouletteGui implements InventoryHolder {

    private final Station station;
    private final Inventory inv;
    private final Player player;
    private final UltimateRpsPlugin plugin;
    private final int betAmount;
    private final ItemStack coinItem;

    private int pointer = 0;
    private int winIndex = 0;
    private int ticks = 0;
    private int delay = 1; // faster start

    public RouletteGui(Player p, Station st, int betAmount, ItemStack coinItem) {
        this.plugin = UltimateRpsPlugin.get();
        this.player = p;
        this.station = st;
        this.betAmount = betAmount;
        this.coinItem = coinItem;
        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("gui.roulette-title","&0보상 룰렛 - {name}").replace("{name}", st.getName()));
        this.inv = Bukkit.createInventory(this, 27, title);
        drawBase();
    }

    public static void openWedgeEditor(Player p, Station st) {
        UltimateRpsPlugin plugin = UltimateRpsPlugin.get();
        Inventory inv = Bukkit.createInventory(p, 27, ChatColor.DARK_GRAY + "웨지 설정 - " + st.getName());
        int[] arr = st.getWedges();
        int total = 0;
        for (int w : arr) total += Math.max(0, w);
        if (total <= 0) total = 1; // avoid div-by-zero

        for (int i=0;i<20;i++) {
            int mult = i+1;
            int w = (i < arr.length ? arr[i] : 0);
            double pr = (w <= 0) ? 0.0 : (100.0 * w / (double) total);
            String ps = String.format(java.util.Locale.US, "%.1f%%", pr);
            Material mat = (w > 0 ? Material.SUNFLOWER : Material.BARRIER);
            String name = ChatColor.AQUA + "x"+mult + (w > 0 ? "" : ChatColor.GRAY + " (비활성)");
            List<String> lore = new ArrayList<>();
            lore.add(ItemUtils.color("&7가중치: &f"+w));
            lore.add(ItemUtils.color("&7확률: &f"+ps));
            lore.add(ItemUtils.color("&8좌클릭: +1  시프트+좌클릭: +5"));
            lore.add(ItemUtils.color("&8우클릭: -1  시프트+우클릭: 비활성(0)"));
            lore.add(ItemUtils.color("&8가운데클릭: 활성/비활성 토글"));
            ItemStack item = ItemUtils.lore(ItemUtils.named(mat, name), lore);
            inv.setItem(i, item);
        }
        p.openInventory(inv);
    }

    private void drawBase() {
        // Fill panes except center row
        ItemStack pane = ItemUtils.named(Material.BLACK_STAINED_GLASS_PANE, ChatColor.DARK_GRAY + " ");
        for (int i=0;i<27;i++) inv.setItem(i, pane);
        // Pointer markers at slots 4 and 22
        ItemStack marker = ItemUtils.named(Material.HOPPER, ChatColor.GOLD + "당첨 지점");
        marker.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.LUCK, 1);
        ItemUtils.hideAllFlags(marker);
        inv.setItem(4, marker);
        inv.setItem(22, marker);
        refreshRow();
    }

    private List<Integer> sequence() {
        List<Integer> seq = new ArrayList<>();
        for (int i=1;i<=20;i++) seq.add(i);
        return seq;
    }

    private void refreshRow() {
        for (int i=0;i<7;i++) {
            int idx = (pointer + i) % 20;
            int mult = idx + 1;
            int w = 0;
            int[] arr = station.getWedges();
            if (arr != null && idx < arr.length) w = Math.max(0, arr[idx]);
            if (w > 0) {
                ItemStack it = ItemUtils.named(Material.SUNFLOWER, ChatColor.AQUA + "x"+mult);
                inv.setItem(10+i, it);
            } else {
                inv.setItem(10+i, ItemUtils.named(Material.BLACK_STAINED_GLASS_PANE, ChatColor.DARK_GRAY + " "));
            }
        }
    }

    public void startSpin() {
        // Decide winIndex via weighted random (skip disabled 0)
        int[] wedges = station.getWedges();
        int total = 0;
        for (int w : wedges) total += Math.max(0, w);
        if (total <= 0) {
            winIndex = new Random().nextInt(20);
        } else {
            int r = new Random().nextInt(total);
            int sum = 0;
            for (int i=0;i<20;i++) {
                int w = Math.max(0, wedges[i]);
                sum += w;
                if (r < sum) { winIndex = i; break; }
            }
        }
        runTick();
    }

    private void runTick() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ticks++;
            pointer = (pointer + 1) % 20;
            refreshRow();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);

            // faster animation then slow down
            int cycles = 20;
            if (ticks > cycles) {
                delay = Math.min(4, delay + (ticks % 3 == 0 ? 1 : 0));
            }
            int centerIndex = (pointer + 3) % 20;
            if (ticks > cycles + 20 && centerIndex == winIndex) {
                finish();
            } else {
                runTickDelay();
            }
        }, delay);
    }

    private void runTickDelay() {
        Bukkit.getScheduler().runTaskLater(plugin, this::runTick, delay);
    }

    private void finish() {
        int multiplier = winIndex + 1;
        int payout = betAmount * multiplier;

        // Close and drop rewards
        player.closeInventory();
        // Broadcast
        String coinName = "코인";
        if (coinItem != null) {
            org.bukkit.inventory.meta.ItemMeta cm = coinItem.getItemMeta();
            if (cm != null && cm.hasDisplayName()) coinName = cm.getDisplayName();
            else coinName = coinItem.getType().name();
        }
        String bc = plugin.getConfig().getString("messages.win-broadcast");
        bc = bc.replace("{player}", player.getName())
                .replace("{name}", station.getName())
                .replace("{bet}", String.valueOf(betAmount))
                .replace("{multiplier}", String.valueOf(multiplier))
                .replace("{win}", String.valueOf(payout))
                .replace("{coin}", coinName);
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', bc));

        // Self message
        String self = plugin.getConfig().getString("messages.win-self");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                self.replace("{multiplier}", String.valueOf(multiplier)).replace("{win}", String.valueOf(payout))));

        // Drop on diamond block
        ItemStack drop = coinItem.clone();
        int remain = payout;
        while (remain > 0) {
            int give = Math.min(64, remain);
            drop.setAmount(give);
            station.getBlockLocation().getWorld().dropItem(station.getBlockLocation().clone().add(0.5,1.2,0.5), drop.clone());
            remain -= give;
        }

        // stats
        UltimateRpsPlugin.get().stats().addWin(player.getUniqueId(), payout, multiplier);
        UltimateRpsPlugin.get().stats().addMachinePayout(station.getName(), payout);
    }

    public Inventory getInventory() { return inv; }
}
