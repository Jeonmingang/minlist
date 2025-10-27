
package com.minkang.ultimate.rps.gui;

import com.minkang.ultimate.rps.UltimateRpsPlugin;
import com.minkang.ultimate.rps.game.RpsChoice;
import com.minkang.ultimate.rps.station.Station;
import com.minkang.ultimate.rps.util.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class GuiListener implements Listener {

    private final UltimateRpsPlugin plugin;
    private final String P;

    public GuiListener(UltimateRpsPlugin plugin) {
        this.plugin = plugin;
        this.P = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix", "&a[ 가위바위보 ]&f "));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Inventory inv = e.getInventory();
        InventoryHolder holder = inv.getHolder();

        // --- RPS GUI ---
        if (holder instanceof RpsGui) {
            RpsGui gui = (RpsGui) holder;
            Player p = (Player) e.getWhoClicked();
            int raw = e.getRawSlot();
            int topSize = inv.getSize();

            // TOP (GUI) region
            if (raw < topSize) {
                e.setCancelled(true);
                int slot = raw;

                // coin slot allows placing exact coin item, max 3 (manual handling)
                if (slot == RpsGui.SLOT_COIN) {
                    // hard block number-key / double-click exploits
                    if (e.getClick() == ClickType.NUMBER_KEY || e.getClick() == ClickType.DOUBLE_CLICK) { e.setCancelled(true); return; }
                    Station st = gui.getSession().station;
                    ItemStack cursor = e.getCursor();
                    ItemStack slotItem = inv.getItem(RpsGui.SLOT_COIN);

                    if (st.getCoinItem() == null) { p.sendMessage(color(plugin.getConfig().getString("messages.coin-not-set"))); return; }

                    // Taking out with empty cursor
                    if (cursor == null || cursor.getType() == Material.AIR) {
                        if (slotItem != null && slotItem.getType() != Material.AIR) {
                            e.getView().setCursor(slotItem);
                            inv.setItem(RpsGui.SLOT_COIN, new ItemStack(Material.AIR));
                        }
                        return;
                    }

                    // Only allow matching coin item
                    if (!ItemUtils.isSimilarIgnoreAmount(cursor, st.getCoinItem())) {
                        p.sendMessage(color(plugin.getConfig().getString("messages.coin-wrong")));
                        return;
                    }

                    int current = (slotItem == null || slotItem.getType() == Material.AIR) ? 0 : slotItem.getAmount();
                    int can = Math.max(0, 3 - current);
                    if (can <= 0) {
                        p.sendMessage(color(plugin.getConfig().getString("messages.coin-too-much")));
                        return;
                    }
                    int move = Math.min(can, cursor.getAmount());
                    if (current == 0) {
                        ItemStack put = cursor.clone(); put.setAmount(move);
                        inv.setItem(RpsGui.SLOT_COIN, put);
                    } else {
                        slotItem.setAmount(current + move);
                        inv.setItem(RpsGui.SLOT_COIN, slotItem);
                    }
                    // reduce cursor
                    int remain = cursor.getAmount() - move;
                    if (remain <= 0) e.getView().setCursor(new ItemStack(Material.AIR));
                    else { ItemStack nc = cursor.clone(); nc.setAmount(remain); e.getView().setCursor(nc); }
                    return;
                }

                // picks and start
                if (slot == RpsGui.SLOT_ROCK) {
                    gui.updatePlayerChoice(RpsChoice.ROCK);
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    return;
                }
                if (slot == RpsGui.SLOT_PAPER) {
                    gui.updatePlayerChoice(RpsChoice.PAPER);
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
                    return;
                }
                if (slot == RpsGui.SLOT_SCISSORS) {
                    gui.updatePlayerChoice(RpsChoice.SCISSORS);
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.4f);
                    return;
                }
                if (slot == RpsGui.SLOT_START) {
                    // validate coin
                    ItemStack bet = inv.getItem(RpsGui.SLOT_COIN);
                    if (bet == null || bet.getType() == Material.AIR) {
                        p.sendMessage(color(plugin.getConfig().getString("messages.need-coin")));
                        return;
                    }
                    if (bet.getAmount() > 3) {
                        p.sendMessage(color(plugin.getConfig().getString("messages.coin-too-much")));
                        return;
                    }
                    // validate choice
                    if (gui.getSession().playerChoice == null) {
                        p.sendMessage(color(plugin.getConfig().getString("messages.need-choose")));
                        return;
                    }
                    // remove coin and start countdown
                    int betAmount = bet.getAmount();
                    ItemStack coinItem = bet.clone(); coinItem.setAmount(1);
                    inv.setItem(RpsGui.SLOT_COIN, new ItemStack(Material.AIR));

                    startCountdown(gui, betAmount, coinItem);
                    return;
                }

                return;
            }

            // BOTTOM (Player inventory) region
            // Allow default behaviour; intercept shift-click to auto-feed coin slot
            if (e.isShiftClick()) {
                // Prevent any non-coin shift-move into GUI

                ItemStack cur = e.getCurrentItem();
                Station st = gui.getSession().station;
                if (cur != null && cur.getType() != Material.AIR) {
                    if (st.getCoinItem() == null) { e.setCancelled(true); return; }
                    if (!ItemUtils.isSimilarIgnoreAmount(cur, st.getCoinItem())) { e.setCancelled(true); return; }
                    ItemStack slot = inv.getItem(RpsGui.SLOT_COIN);
                    int slotAmt = (slot == null || slot.getType() == Material.AIR) ? 0 : slot.getAmount();
                    int can = Math.max(0, 3 - slotAmt);
                    if (can > 0) {
                        int move = Math.min(can, cur.getAmount());
                        if (slotAmt == 0) {
                            ItemStack put = cur.clone(); put.setAmount(move);
                            inv.setItem(RpsGui.SLOT_COIN, put);
                        } else {
                            slot.setAmount(slotAmt + move);
                            inv.setItem(RpsGui.SLOT_COIN, slot);
                        }
                        cur.setAmount(cur.getAmount() - move);
                        e.setCurrentItem(cur.getAmount() <= 0 ? new ItemStack(Material.AIR) : cur);
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            return;
        }


        if (holder instanceof RouletteGui) {
            // Block all interactions in roulette GUI (purely visual/animation)
            e.setCancelled(true);
            return;
        }

        // --- Wedge editor GUI ---
        String title = e.getView().getTitle();
        if (title != null && title.startsWith(ChatColor.DARK_GRAY + "웨지 설정 - ")) {
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player)) return;
            Player p = (Player) e.getWhoClicked();
            String name = ChatColor.stripColor(title).replace("웨지 설정 - ", "");
            com.minkang.ultimate.rps.station.Station st = UltimateRpsPlugin.get().stations().getByName(name);
            if (st == null) return;
            int slot = e.getRawSlot();
            if (slot < 0 || slot >= 20) return;
            int cur = st.getWedges()[slot];
            int newW = cur;

            if (e.getClick().isLeftClick()) {
                // enable/increase
                int delta = e.getClick().isShiftClick() ? 5 : 1;
                newW = (cur <= 0) ? 1 : cur + delta;
            } else if (e.getClick().isRightClick()) {
                if (e.getClick().isShiftClick()) {
                    newW = 0; // disable
                } else {
                    newW = Math.max(1, cur > 0 ? cur - 1 : 0);
                }
            } else if (e.getClick() == ClickType.MIDDLE) {
                newW = (cur <= 0) ? 1 : 0; // toggle
            } else {
                return;
            }

            st.getWedges()[slot] = newW;
            UltimateRpsPlugin.get().stations().save();
            // Reopen to refresh all percentages and icons
            RouletteGui.openWedgeEditor(p, st);
            return;
        }
    }

    private void startCountdown(RpsGui gui, int betAmount, ItemStack coinItem) {
        Player p = (Player) gui.getInventory().getViewers().get(0);
        p.sendMessage(color(plugin.getConfig().getString("messages.countdown")));
        // animate system head: 3..2..1 using CLOCK item
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            gui.getInventory().setItem(RpsGui.SLOT_SYSTEM_HEAD, ItemUtils.named(Material.CLOCK, ChatColor.LIGHT_PURPLE + "3"));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.9f);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                gui.getInventory().setItem(RpsGui.SLOT_SYSTEM_HEAD, ItemUtils.named(Material.CLOCK, ChatColor.LIGHT_PURPLE + "2"));
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.0f);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    gui.getInventory().setItem(RpsGui.SLOT_SYSTEM_HEAD, ItemUtils.named(Material.CLOCK, ChatColor.LIGHT_PURPLE + "1"));
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.1f);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        // decide system choice
                        RpsChoice sys = RpsChoice.values()[new java.util.Random().nextInt(3)];
                        gui.updateSystemChoice(sys);
                        int result = RpsChoice.compare(gui.getSession().playerChoice, sys);
                        if (result == 0) {
                            p.sendMessage(color(plugin.getConfig().getString("messages.draw"))); // draw also consumes bet
                        } else if (result < 0) {
                            p.sendMessage(color(plugin.getConfig().getString("messages.lose")));
                            UltimateRpsPlugin.get().stats().addLoss(p.getUniqueId(), betAmount);
                        } else {
                            // win -> open roulette
                            RouletteGui roll = new RouletteGui(p, gui.getSession().station, betAmount, coinItem);
                            p.openInventory(roll.getInventory());
                            roll.startSpin();
                        }
                    }, 12L);
                }, 12L);
            }, 12L);
        }, 10L);
    }


    @EventHandler
    public void onDrag(org.bukkit.event.inventory.InventoryDragEvent e) {
        Inventory inv = e.getInventory();
        InventoryHolder holder = inv.getHolder();
        if (holder instanceof RpsGui || holder instanceof RouletteGui) {
            int topSize = inv.getSize();
            for (int raw : e.getRawSlots()) {
                if (raw < topSize) {
                    // prohibit any drag into GUI
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof RpsGui) {
            // nothing for now
        }
        String title = e.getView().getTitle();
        if (title != null && title.startsWith(org.bukkit.ChatColor.DARK_GRAY + "웨지 설정 - ")) {
            String name = org.bukkit.ChatColor.stripColor(title).replace("웨지 설정 - ", "");
            com.minkang.ultimate.rps.station.Station st = com.minkang.ultimate.rps.UltimateRpsPlugin.get().stations().getByName(name);
            if (st != null && st.isHologram()) {
                com.minkang.ultimate.rps.UltimateRpsPlugin.get().holograms().spawnOrRefresh(st);
            }
        }
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }
}
