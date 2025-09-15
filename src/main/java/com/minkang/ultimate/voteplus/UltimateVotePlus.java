package com.minkang.ultimate.voteplus;

import com.minkang.ultimate.voteplus.util.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

public class UltimateVotePlus extends JavaPlugin implements Listener {

    private Inventory gui;
    private int taskId = -1;
    private File queueFile;
    private org.bukkit.configuration.file.YamlConfiguration queue;

    private static final String GUI_TITLE = ChatColor.GREEN + "추천 보상 설정 (마인리스트/마인페이지)";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        setupQueue();
        getServer().getPluginManager().registerEvents(this, this);
        startAnnounceTask();
        hookVotifier();
        getLogger().info("UltimateVotePlus v1.1.0 enabled.");
    }

    @Override
    public void onDisable() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private void setupQueue() {
        queueFile = new File(getDataFolder(), "queued.yml");
        if (!queueFile.exists()) {
            try { queueFile.getParentFile().mkdirs(); queueFile.createNewFile(); } catch (IOException ignored) {}
        }
        queue = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(queueFile);
    }

    private void startAnnounceTask() {
        FileConfiguration cfg = getConfig();
        if (!cfg.getBoolean("announce.enabled", true)) return;
        int seconds = Math.max(5, cfg.getInt("announce.interval-seconds", 30));
        String msg = color(cfg.getString("announce.message",
                "&a[알림]&f 마인리스트/마인페이지 추천 부탁드립니다! &e{minelist}&f / &b{minepage}"));
        String minelist = cfg.getString("links.minelist", "https://minelist.kr/");
        String minepage = cfg.getString("links.minepage", "https://mine.page/");

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            String out = msg.replace("{minelist}", minelist).replace("{minepage}", minepage);
            Bukkit.broadcastMessage(color(out));
        }, 20L, seconds * 20L);
    }

    private void hookVotifier() {
        PluginManager pm = getServer().getPluginManager();
        try {
            final Class<? extends Event> voteEventClass =
                    (Class<? extends Event>) Class.forName("com.vexsoftware.votifier.model.VotifierEvent");

            EventExecutor exec = new EventExecutor() {
                @Override
                public void execute(Listener listener, Event event) {
                    if (!voteEventClass.isInstance(event)) return;
                    try {
                        Method getVote = voteEventClass.getMethod("getVote");
                        Object vote = getVote.invoke(event);
                        if (vote == null) return;

                        Method getUsername = vote.getClass().getMethod("getUsername");
                        Method getServiceName = vote.getClass().getMethod("getServiceName");

                        String playerName = String.valueOf(getUsername.invoke(vote));
                        String service = String.valueOf(getServiceName.invoke(vote));
                        handleVote(playerName, service);
                    } catch (Exception ex) {
                        getLogger().severe("VotifierEvent 처리 오류: " + ex.getMessage());
                    }
                }
            };
            Listener dummy = new Listener() {};
            pm.registerEvent(voteEventClass, dummy, EventPriority.NORMAL, exec, this);
            getLogger().info("VotifierEvent listener hooked.");
        } catch (ClassNotFoundException e) {
            getLogger().warning("NuVotifier가 감지되지 않았습니다. 투표 수신 불가.");
        }
    }

    private void handleVote(String playerName, String serviceRaw) {
        if (playerName == null || playerName.isEmpty()) return;
        String service = (serviceRaw == null ? "" : serviceRaw.toLowerCase(Locale.ROOT));
        ServiceType type = ServiceType.fromServiceName(service);

        List<ItemStack> rewards = getConfiguredRewards(type);
        if (rewards.isEmpty()) return;

        Player p = Bukkit.getPlayerExact(playerName);
        if (p != null && p.isOnline()) {
            for (ItemStack it : rewards) {
                if (it != null) p.getInventory().addItem(it.clone());
            }
            p.sendMessage(color("&a[추천] 보상이 지급되었습니다! &7(" + type.display + ")"));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
        } else {
            if (getConfig().getBoolean("queue-offline", true)) {
                List<String> list = queue.getStringList("queue." + playerName.toLowerCase(Locale.ROOT));
                if (list == null) list = new ArrayList<>();
                list.addAll(ItemSerializer.serializeList(rewards.toArray(new ItemStack[0])));
                queue.set("queue." + playerName.toLowerCase(Locale.ROOT), list);
                try { queue.save(queueFile); } catch (IOException ignored) {}
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        String key = e.getPlayer().getName().toLowerCase(Locale.ROOT);
        List<String> data = queue.getStringList("queue." + key);
        if (data != null && !data.isEmpty()) {
            List<ItemStack> items = ItemSerializer.deserializeList(data);
            Bukkit.getScheduler().runTaskLater(this, () -> {
                for (ItemStack it : items) {
                    if (it != null) e.getPlayer().getInventory().addItem(it);
                }
                e.getPlayer().sendMessage(color("&a[추천] 오프라인 중 보류된 보상을 지급했습니다."));
                queue.set("queue." + key, null);
                try { queue.save(queueFile); } catch (IOException ignored) {}
            }, 20L);
        }
    }

    private enum ServiceType {
        MINELIST("마인리스트"),
        MINEPAGE("마인페이지"),
        UNKNOWN("기타");

        final String display;
        ServiceType(String d){ this.display = d; }

        static ServiceType fromServiceName(String service){
            if (service == null) return UNKNOWN;
            String s = service.toLowerCase(Locale.ROOT);
            if (s.contains("minelist")) return MINELIST;
            if (s.contains("mine.page") || s.contains("minepage")) return MINEPAGE;
            return UNKNOWN;
        }
    }

    private List<ItemStack> getConfiguredRewards(ServiceType type) {
        FileConfiguration cfg = getConfig();
        List<String> data = (type == ServiceType.MINEPAGE)
                ? cfg.getStringList("rewards.data.minepage")
                : cfg.getStringList("rewards.data.minelist");
        return com.minkang.ultimate.voteplus.util.ItemSerializer.deserializeList(data);
    }

    private String color(String s){ return ChatColor.translateAlternateColorCodes('&', s); }

    // ---------- GUI ----------
    private void openGui(Player p) {
        gui = Bukkit.createInventory(p, 54, GUI_TITLE);
        decorate();
        FileConfiguration cfg = getConfig();
        // load items
        List<ItemStack> ml = ItemSerializer.deserializeList(cfg.getStringList("rewards.data.minelist"));
        List<Integer> mlSlots = cfg.getIntegerList("rewards.slots.minelist");
        for (int i = 0; i < ml.size() && i < mlSlots.size(); i++) gui.setItem(mlSlots.get(i), ml.get(i));
        List<ItemStack> mp = ItemSerializer.deserializeList(cfg.getStringList("rewards.data.minepage"));
        List<Integer> mpSlots = cfg.getIntegerList("rewards.slots.minepage");
        for (int i = 0; i < mp.size() && i < mpSlots.size(); i++) gui.setItem(mpSlots.get(i), mp.get(i));
        p.openInventory(gui);
    }

    private void decorate() {
        // label items
        gui.setItem(4, makeItem(Material.EMERALD_BLOCK, "&a마인리스트 보상 영역", "&7좌측 녹색 슬롯에 넣으세요"));
        gui.setItem(22, makeItem(Material.LAPIS_BLOCK, "&b마인페이지 보상 영역", "&7우측 파란 슬롯에 넣으세요"));
        gui.setItem(49, makeItem(Material.ANVIL, "&e저장", "&7설정 저장"));
        gui.setItem(45, makeItem(Material.REDSTONE_BLOCK, "&c마인리스트 보상 초기화"));
        gui.setItem(53, makeItem(Material.REDSTONE, "&c마인페이지 보상 초기화"));
        // background panes
        ItemStack pane = makeItem(Material.GRAY_STAINED_GLASS_PANE, "&7", "");
        for (int i = 0; i < 54; i++) if (gui.getItem(i) == null) gui.setItem(i, pane);
        // clear editable
        FileConfiguration cfg = getConfig();
        for (int s : cfg.getIntegerList("rewards.slots.minelist")) gui.setItem(s, null);
        for (int s : cfg.getIntegerList("rewards.slots.minepage")) gui.setItem(s, null);
    }

    private ItemStack makeItem(Material mat, String name, String... lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            List<String> l = new ArrayList<>();
            for (String s : lore) l.add(color(s));
            meta.setLore(l);
            it.setItemMeta(meta);
        }
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView() == null || e.getView().getTitle() == null) return;
        if (!e.getView().getTitle().equals(GUI_TITLE)) return;
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        if (!p.hasPermission("uvp.admin")) { e.setCancelled(true); return; }
        int slot = e.getRawSlot();
        if (slot >= 54) return;

        if (slot == 4 || slot == 22 || slot == 49 || slot == 45 || slot == 53) {
            e.setCancelled(true);
            if (slot == 49) {
                saveGui();
                p.playSound(p.getLocation(), Sound.UI_TOAST_IN, 1f, 1.6f);
                p.sendMessage(color("&a보상을 저장했습니다."));
                p.closeInventory();
            } else if (slot == 45) {
                for (int s : getConfig().getIntegerList("rewards.slots.minelist")) gui.setItem(s, null);
                p.sendMessage(color("&c마인리스트 보상을 초기화했습니다."));
            } else if (slot == 53) {
                for (int s : getConfig().getIntegerList("rewards.slots.minepage")) gui.setItem(s, null);
                p.sendMessage(color("&c마인페이지 보상을 초기화했습니다."));
            }
            return;
        }

        List<Integer> editable = new ArrayList<>();
        editable.addAll(getConfig().getIntegerList("rewards.slots.minelist"));
        editable.addAll(getConfig().getIntegerList("rewards.slots.minepage"));
        if (!editable.contains(slot)) e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getView() == null || e.getView().getTitle() == null) return;
        if (!e.getView().getTitle().equals(GUI_TITLE)) return;
        // no auto-save
    }

    private void saveGui() {
        FileConfiguration cfg = getConfig();
        List<Integer> mlSlots = cfg.getIntegerList("rewards.slots.minelist");
        List<Integer> mpSlots = cfg.getIntegerList("rewards.slots.minepage");
        List<ItemStack> ml = new ArrayList<>();
        for (int s : mlSlots) { ItemStack it = gui.getItem(s); if (it != null && it.getType()!=Material.AIR) ml.add(it.clone()); }
        List<ItemStack> mp = new ArrayList<>();
        for (int s : mpSlots) { ItemStack it = gui.getItem(s); if (it != null && it.getType()!=Material.AIR) mp.add(it.clone()); }
        cfg.set("rewards.data.minelist", ItemSerializer.serializeList(ml.toArray(new ItemStack[0])));
        cfg.set("rewards.data.minepage", ItemSerializer.serializeList(mp.toArray(new ItemStack[0])));
        saveConfig();
    }

    // Commands
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equals("마인리스트")) return false;
        if (args.length == 0) {
            String minelist = getConfig().getString("links.minelist", "https://minelist.kr/");
            String minepage = getConfig().getString("links.minepage", "https://mine.page/");
            sender.sendMessage(color("&a[추천 링크]&f 마인리스트: &e" + minelist));
            sender.sendMessage(color("&a[추천 링크]&f 마인페이지: &b" + minepage));
            return true;
        }
        if ("설정".equalsIgnoreCase(args[0])) {
            if (!(sender instanceof Player)) { sender.sendMessage(color("&c게임 내에서만 사용 가능합니다.")); return true; }
            Player p = (Player) sender;
            if (!p.hasPermission("uvp.admin")) { p.sendMessage(color("&c권한이 없습니다. (uvp.admin)")); return true; }
            openGui(p); return true;
        } else if ("리로드".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("uvp.admin")) { sender.sendMessage(color("&c권한이 없습니다. (uvp.admin)")); return true; }
            reloadConfig();
            if (taskId != -1) { Bukkit.getScheduler().cancelTask(taskId); taskId = -1; }
            startAnnounceTask();
            sender.sendMessage(color("&a설정을 리로드하고 알림 태스크를 재시작했습니다."));
            return true;
        }
        return false;
    }
}
