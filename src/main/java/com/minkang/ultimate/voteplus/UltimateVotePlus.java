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

import org.bukkit.event.inventory.InventoryDragEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.BaseComponent;

import java.time.*;
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
    private MonthlyRewardManager monthly;
    private AutoNoticeManager autoNoticeManager;

    private Inventory gui;
    private int taskId = -1;

    private File queueFile, statsFile;
    private org.bukkit.configuration.file.YamlConfiguration queue, stats;

    private static final String GUI_TITLE = ChatColor.GREEN + "추천 보상 설정 (마인리스트/마인페이지)";
    private static final String PREVIEW_TITLE = ChatColor.YELLOW + "추천 보상 미리보기";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        // Ensure autonotice.yml exists on first run
        if (!new java.io.File(getDataFolder(), "autonotice.yml").exists()) {
            try { getDataFolder().mkdirs(); saveResource("autonotice.yml", false); } catch (Exception ignored) {}
        }
        // Auto notice wiring
        this.autoNoticeManager = new AutoNoticeManager(this);
        this.autoNoticeManager.load();
        this.autoNoticeManager.start();
        getLogger().info("[AutoNotice] enabled=" + this.autoNoticeManager.isEnabled() + ", messages=" + this.autoNoticeManager.getMessages().size());
        setupFiles();
        getServer().getPluginManager().registerEvents(this, this);
        // Register /자동공지 command
        if (getCommand("자동공지") != null) {
            AutoNoticeCommand anc = new AutoNoticeCommand(this.autoNoticeManager);
            getCommand("자동공지").setExecutor(anc);
            getCommand("자동공지").setTabCompleter(anc);
        }
        startAnnounceTask();
        hookVotifier();
        monthly = new MonthlyRewardManager(this);
        log("&aUltimateVotePlus v1.3.2 enabled.");
    }

    @Override
    public void onDisable() {
        if (autoNoticeManager != null) { autoNoticeManager.stop(); autoNoticeManager.save(); }
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        saveYaml(stats, statsFile);
        saveYaml(queue, queueFile);
    }

    private void setupFiles() {
        queueFile = new File(getDataFolder(), "queued.yml");
        statsFile = new File(getDataFolder(), "votes.yml");
        try {
            if (!queueFile.exists()) { queueFile.getParentFile().mkdirs(); queueFile.createNewFile(); }
            if (!statsFile.exists()) { statsFile.getParentFile().mkdirs(); statsFile.createNewFile(); }
        } catch (IOException ignored) {}
        queue = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(queueFile);
        stats = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(statsFile);
        if (!stats.isSet("total")) stats.set("total", 0);
        if (!stats.isConfigurationSection("bySite")) stats.createSection("bySite");
        if (!stats.isConfigurationSection("byPlayer")) stats.createSection("byPlayer");
        saveYaml(stats, statsFile);
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
            broadcastWithPreviewButton(out);
        }, 20L, seconds * 20L);
    }

    private void hookVotifier() {
        PluginManager pm = getServer().getPluginManager();
        try {
            final Class<? extends Event> voteEventClass =
                    (Class<? extends Event>) Class.forName("com.vexsoftware.votifier.model.VotifierEvent");

            EventExecutor exec = (listener, event) -> {
                if (!voteEventClass.isInstance(event)) return;
                try {
                    Method getVote = voteEventClass.getMethod("getVote");
                    Object vote = getVote.invoke(event);
                    if (vote == null) return;

                    Method getUsername = vote.getClass().getMethod("getUsername");
                    Method getServiceName = vote.getClass().getMethod("getServiceName");

                    String playerName = String.valueOf(getUsername.invoke(vote));
                    String service = String.valueOf(getServiceName.invoke(vote));
                    if (service == null || "null".equalsIgnoreCase(service)) service = "";
                    handleVote(playerName, service);
                } catch (Exception ex) {
                    getLogger().severe("VotifierEvent 처리 오류: " + ex.getMessage());
                }
            };
            Listener dummy = new Listener() {};
            pm.registerEvent(voteEventClass, dummy, EventPriority.NORMAL, exec, this);
            log("&aVotifierEvent listener hooked.");
        } catch (ClassNotFoundException e) {
            log("&eNuVotifier가 감지되지 않았습니다. 투표 수신 불가.");
        }
    }

    private void handleVote(String playerName, String serviceRaw) {
        if (playerName == null || playerName.isEmpty()) return;
        String service = (serviceRaw == null ? "" : serviceRaw.toLowerCase(Locale.ROOT));

        ServiceType type = ServiceType.fromServiceName(service);
        List<ItemStack> rewards = getConfiguredRewards(type);

        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null && getConfig().getBoolean("reward.allow-fuzzy-online-lookup", true)) {
            target = Bukkit.getPlayer(playerName);
        }

        if (target != null && target.isOnline()) {
            for (ItemStack it : rewards) if (it != null) target.getInventory().addItem(it.clone());
            target.sendMessage(color("&a[추천] 보상이 지급되었습니다! &7(" + type.display + ")"));
            target.playSound(target.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
            log("&a보상 지급: " + target.getName() + " / site=" + type + " / items=" + rewards.size());
        } else {
            if (getConfig().getBoolean("reward.queue-offline", true)) {
                List<String> list = queue.getStringList("queue." + playerName.toLowerCase(Locale.ROOT));
                if (list == null) list = new ArrayList<>();
                list.addAll(ItemSerializer.serializeList(rewards.toArray(new ItemStack[0])));
                queue.set("queue." + playerName.toLowerCase(Locale.ROOT), list);
                saveYaml(queue, queueFile);
                log("&e오프라인 보상 보류: " + playerName + " / site=" + type + " / items=" + rewards.size());
            } else {
                log("&7오프라인이어서 지급되지 않았습니다(보류 비활성). player=" + playerName);
            }
        }

        incrementStats(playerName, type);
        if (monthly != null) monthly.recordVote(playerName);
        
        // Per-vote cash reward via console command
        if (getConfig().getBoolean("vote-reward.command-enabled", true)) {
            String cmd = getConfig().getString("vote-reward.command", "캐시 지급 {player} 50")
                    .replace("{player}", playerName)
                    .replace("{site}", type.display);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            // Direct message to the voter about the cash reward
            if (target != null) {
                String defMsg = "&a[추천]&f 추천 보상으로 &e{amount} 캐시&f가 지급되었습니다!.";
                String pm = getConfig().getString("vote-reward.player-message", defMsg);
                // Try to extract {amount} from the configured console command as a convenience
                String amount = "50";
                try {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(cmd);
                    String last = null;
                    while (m.find()) last = m.group(1);
                    if (last != null) amount = last;
                } catch (Exception ignored) {}
                pm = pm.replace("{amount}", amount).replace("{site}", type.display);
                target.sendMessage(color(pm));
            }

        }
        // countdown message to player
        if (target != null && getConfig().getBoolean("reset-countdown.enabled", true)) {
            String left = getTimeLeftToMonthReset();
            target.sendMessage(color(getConfig().getString("reset-countdown.message", "&7[마인리스트 초기화까지 남은시간] &f{time}")
                    .replace("{time}", left)));
        }
        maybeBroadcastReward(playerName, type);
    }

    private void maybeBroadcastReward(String playerName, ServiceType type) {
        if (!getConfig().getBoolean("broadcast-on-reward.enabled", true)) return;
        int total = stats.getInt("total", 0);
        int ml = stats.getInt("bySite.minelist", 0);
        int mp = stats.getInt("bySite.minepage", 0);
        String siteName = type.display;
        String fmt = getConfig().getString("broadcast-on-reward.message",
                "&a[추천]&f {player} 님이 &e{site}&f 추천 완료! &7[ 누적 추천수 {count_total} ]");
        String out = fmt.replace("{player}", playerName)
                        .replace("{site}", siteName)
                        .replace("{count_total}", String.valueOf(total))
                        .replace("{count_minelist}", String.valueOf(ml))
                        .replace("{count_minepage}", String.valueOf(mp));
        broadcastWithPreviewButton(out);
    }

    private void incrementStats(String playerName, ServiceType type) {
        int total = stats.getInt("total", 0) + 1;
        stats.set("total", total);
        String key = (type == ServiceType.MINEPAGE ? "minepage" : "minelist"); // UNKNOWN은 minelist로 합산
        stats.set("bySite." + key, stats.getInt("bySite." + key, 0) + 1);
        stats.set("byPlayer." + playerName.toLowerCase(Locale.ROOT), stats.getInt("byPlayer." + playerName.toLowerCase(Locale.ROOT), 0) + 1);
        
        // Update last vote date (yyyy-MM-dd) and monthly counter (yyyyMM)
        try {
            java.time.ZoneId zone = java.time.ZoneId.systemDefault();
            java.time.LocalDate today = java.time.LocalDate.now(zone);
            String todayStr = today.toString(); // yyyy-MM-dd
            String ym = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
            String playerKey = playerName.toLowerCase(java.util.Locale.ROOT);
            stats.set("lastVote." + playerKey, todayStr);
            String monthlyKey = "monthly." + ym + "." + playerKey;
            stats.set(monthlyKey, stats.getInt(monthlyKey, 0) + 1);
        } catch (Throwable t) { /* ignore time errors */ }

        saveYaml(stats, statsFile);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        String key = e.getPlayer().getName().toLowerCase(Locale.ROOT);
        List<String> data = queue.getStringList("queue." + key);
        if (data != null && !data.isEmpty()) {
            List<ItemStack> items = ItemSerializer.deserializeList(data);
            Bukkit.getScheduler().runTaskLater(this, () -> {
                for (ItemStack it : items) if (it != null) e.getPlayer().getInventory().addItem(it);
                e.getPlayer().sendMessage(color("&a[추천] 오프라인 중 보류된 보상을 지급했습니다."));
                queue.set("queue." + key, null);
                saveYaml(queue, queueFile);
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
        List<String> data;
        switch (type) {
            case MINEPAGE: data = cfg.getStringList("rewards.data.minepage"); break;
            case MINELIST:
            case UNKNOWN:
            default: data = cfg.getStringList("rewards.data.minelist"); break;
        }
        return ItemSerializer.deserializeList(data);
    }

    private String color(String s){ return ChatColor.translateAlternateColorCodes('&', s); }

    private void broadcastWithPreviewButton(String legacyMessage) {
        String msg = color(legacyMessage);
        BaseComponent[] base = TextComponent.fromLegacyText(msg);
        TextComponent button = new TextComponent(ChatColor.GRAY + " [ " + ChatColor.YELLOW + "보상 보기 클릭" + ChatColor.GRAY + " ]");
        button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/마인리스트 보상 보기 클릭"));
        button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.YELLOW + "클릭하여 보상 미리보기").create()));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.spigot().sendMessage(new ComponentBuilder().append(base).append(" ").append(button).create());
        }
        // 콘솔 출력도 남김
        Bukkit.getConsoleSender().sendMessage(ChatColor.stripColor(msg) + " [보상 보기 클릭: /마인리스트 보상 보기 클릭]");
    }


    // ---------- GUI ----------
    private void openGui(Player p) {
        gui = Bukkit.createInventory(p, 54, GUI_TITLE);
        decorate();
        FileConfiguration cfg = getConfig();
        List<ItemStack> ml = ItemSerializer.deserializeList(cfg.getStringList("rewards.data.minelist"));
        List<Integer> mlSlots = cfg.getIntegerList("rewards.slots.minelist");
        for (int i = 0; i < ml.size() && i < mlSlots.size(); i++) gui.setItem(mlSlots.get(i), ml.get(i));
        List<ItemStack> mp = ItemSerializer.deserializeList(cfg.getStringList("rewards.data.minepage"));
        List<Integer> mpSlots = cfg.getIntegerList("rewards.slots.minepage");
        for (int i = 0; i < mp.size() && i < mpSlots.size(); i++) gui.setItem(mpSlots.get(i), mp.get(i));
        p.openInventory(gui);
    }

    private void openPreview(Player p) {
        Inventory inv = Bukkit.createInventory(p, 54, PREVIEW_TITLE);
        // 장식과 안내
        inv.setItem(4, makeItem(Material.EMERALD_BLOCK, "&a마인리스트 보상", "&7보기 전용"));
        inv.setItem(22, makeItem(Material.LAPIS_BLOCK, "&b마인페이지 보상", "&7보기 전용"));
        ItemStack pane = makeItem(Material.GRAY_STAINED_GLASS_PANE, "&7", "");
        for (int i = 0; i < 54; i++) if (inv.getItem(i) == null) inv.setItem(i, pane);
        FileConfiguration cfg = getConfig();
        List<ItemStack> ml = ItemSerializer.deserializeList(cfg.getStringList("rewards.data.minelist"));
        List<Integer> mlSlots = cfg.getIntegerList("rewards.slots.minelist");
        for (int i = 0; i < ml.size() && i < mlSlots.size(); i++) inv.setItem(mlSlots.get(i), ml.get(i).clone());
        List<ItemStack> mp = ItemSerializer.deserializeList(cfg.getStringList("rewards.data.minepage"));
        List<Integer> mpSlots = cfg.getIntegerList("rewards.slots.minepage");
        for (int i = 0; i < mp.size() && i < mpSlots.size(); i++) inv.setItem(mpSlots.get(i), mp.get(i).clone());
        p.openInventory(inv);
    }

    private void decorate() {
        gui.setItem(4, makeItem(Material.EMERALD_BLOCK, "&a마인리스트 보상 영역", "&7좌측 녹색 슬롯에 넣으세요"));
        gui.setItem(22, makeItem(Material.LAPIS_BLOCK, "&b마인페이지 보상 영역", "&7우측 파란 슬롯에 넣으세요"));
        gui.setItem(49, makeItem(Material.ANVIL, "&e저장", "&7설정 저장"));
        gui.setItem(45, makeItem(Material.REDSTONE_BLOCK, "&c마인리스트 보상 초기화"));
        gui.setItem(53, makeItem(Material.REDSTONE, "&c마인페이지 보상 초기화"));
        ItemStack pane = makeItem(Material.GRAY_STAINED_GLASS_PANE, "&7", "");
        for (int i = 0; i < 54; i++) if (gui.getItem(i) == null) gui.setItem(i, pane);
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
        // 수동 저장만 허용
    }

    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreviewClick(org.bukkit.event.inventory.InventoryClickEvent e) {
        if (e.getView() == null || e.getView().getTitle() == null) return;
        if (!e.getView().getTitle().equals(PREVIEW_TITLE)) return;
        e.setCancelled(true); // 보기 전용
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreviewDrag(InventoryDragEvent e) {
        if (e.getView() == null || e.getView().getTitle() == null) return;
        if (!e.getView().getTitle().equals(PREVIEW_TITLE)) return;
        e.setCancelled(true); // 보기 전용
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

    private void saveYaml(org.bukkit.configuration.file.YamlConfiguration y, File f) {
        try { y.save(f); } catch (IOException ignored) {}
    }

    private void log(String msg) {
        if (getConfig().getBoolean("debug.log", true)) {
            getLogger().info(ChatColor.stripColor(color(msg)));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(command.getName().equals("마인리스트") || command.getName().equals("추천"))) return false;

        if (args.length == 0) {

            String minelist = getConfig().getString("links.minelist", "https://minelist.kr/");
            String minepage = getConfig().getString("links.minepage", "https://mine.page/");
            sender.sendMessage(color("&a[추천 링크]&f 마인리스트: &e" + minelist));
            // (moved below clickable)
            if (sender instanceof org.bukkit.entity.Player) {
                String key = ((org.bukkit.entity.Player)sender).getName().toLowerCase(java.util.Locale.ROOT);
                String todayStr;
                int monthlyCount;
                try {
                    java.time.ZoneId zone = java.time.ZoneId.systemDefault();
                    java.time.LocalDate today = java.time.LocalDate.now(zone);
                    todayStr = today.toString();
                    String ym = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
                    monthlyCount = stats.getInt("monthly." + ym + "." + key, 0);
                } catch (Throwable t) {
                    todayStr = "";
                    monthlyCount = 0;
                }
                boolean votedToday = todayStr.equals(stats.getString("lastVote." + key, ""));
                sender.sendMessage(color("&f오늘 추천 여부: " + (votedToday ? "&a예" : "&c아니오") + " &7| &f이번달 누적: &e" + monthlyCount + "회"));
            } else {
                // Console: don't print the clickable button; keep minimal info
            }


            
            if (sender instanceof org.bukkit.entity.Player) {
    org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
    net.md_5.bungee.api.chat.TextComponent prefix = new net.md_5.bungee.api.chat.TextComponent(org.bukkit.ChatColor.GREEN + "추천 " + org.bukkit.ChatColor.WHITE + "하시고 " + org.bukkit.ChatColor.AQUA + "보상 " + org.bukkit.ChatColor.WHITE + "받아 가세요 ");
    net.md_5.bungee.api.chat.TextComponent button = new net.md_5.bungee.api.chat.TextComponent(org.bukkit.ChatColor.GRAY + "[ " + org.bukkit.ChatColor.YELLOW + "보상 보기 클릭" + org.bukkit.ChatColor.GRAY + " ]");
    button.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/마인리스트 보상"));
    button.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.ComponentBuilder(org.bukkit.ChatColor.GRAY + "클릭하여 보상 미리보기").create()));
    p.spigot().sendMessage(new net.md_5.bungee.api.chat.ComponentBuilder().append(prefix).append(button).create());
}
    
            return true;
        } else if ("보상".equalsIgnoreCase(args[0]) || "보상 보기 클릭".equalsIgnoreCase(args[0]) || "보상미리보기".equalsIgnoreCase(args[0])) {
            if (!(sender instanceof org.bukkit.entity.Player)) { sender.sendMessage(color("&c게임 내에서만 사용 가능합니다.")); return true; }
            org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
            openPreview(p);
            return true;
        } else if ("랭킹".equalsIgnoreCase(args[0]) || "순위".equalsIgnoreCase(args[0])) {
            org.bukkit.configuration.ConfigurationSection sec = stats.getConfigurationSection("byPlayer");
            if (sec == null || sec.getKeys(false).isEmpty()) {
                sender.sendMessage(color("&e[추천 랭킹]&f 데이터가 아직 없습니다."));
                return true;
            }
            java.util.List<java.util.Map.Entry<String, Integer>> entries = new java.util.ArrayList<>();
            for (String k : sec.getKeys(false)) {
                int c = stats.getInt("byPlayer." + k, 0);
                entries.add(new java.util.AbstractMap.SimpleEntry<>(k, c));
            }
            entries.sort((a,b) -> Integer.compare(b.getValue(), a.getValue()));
            sender.sendMessage(color("&e[추천 랭킹]&7 상위 10명"));
            sender.sendMessage(color("&7[마인리스트 초기화까지 남은시간] &f" + getTimeLeftToMonthReset()));
            int n = 1;
            for (java.util.Map.Entry<String, Integer> e : entries) {
                String name = e.getKey();
                int cnt = e.getValue();
                sender.sendMessage(color("&6#" + n + " &f" + name + " &7- &e" + cnt + "회"));
                if (++n > 10) break;
            }
            return true;
        } else if ("설정".equalsIgnoreCase(args[0])) {
            if (!(sender instanceof org.bukkit.entity.Player)) { sender.sendMessage(color("&c게임 내에서만 사용 가능합니다.")); return true; }
            org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
            if (!p.hasPermission("uvp.admin")) { p.sendMessage(color("&c권한이 없습니다. (uvp.admin)")); return true; }
            openGui(p); return true;
        } else if ("리로드".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("uvp.admin")) { sender.sendMessage(color("&c권한이 없습니다. (uvp.admin)")); return true; }
            reloadConfig();
            if (taskId != -1) { org.bukkit.Bukkit.getScheduler().cancelTask(taskId); taskId = -1; }
            startAnnounceTask();
            sender.sendMessage(color("&a설정을 리로드하고 알림 태스크를 재시작했습니다."));
            return true;
        } else if ("테스트".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("uvp.admin")) { sender.sendMessage(color("&c권한이 없습니다. (uvp.admin)")); return true; }
            if (args.length < 2) { sender.sendMessage(color("&c사용법: /마인리스트 테스트 <닉네임> [사이트키:minelist|minepage]")); return true; }
            String name = args[1];
            String site = (args.length >= 3 ? args[2] : "minelist");
            handleVote(name, site);
            sender.sendMessage(color("&a테스트 투표 처리: &f" + name + " &7(" + site + ")"));
            return true;
        }
        return false;
    }

    private String getTimeLeftToMonthReset() {
        String tz = getConfig().getString("monthly-reward.timezone", "Asia/Seoul");
        ZoneId zone;
        try { zone = ZoneId.of(tz); } catch (Exception e) { zone = ZoneId.of("Asia/Seoul"); }
        ZonedDateTime now = ZonedDateTime.now(zone);
        YearMonth ym = YearMonth.from(now);
        ZonedDateTime next = ym.plusMonths(1).atDay(1).atStartOfDay(zone);
        long secs = Duration.between(now, next).getSeconds();
        if (secs < 0) secs = 0;
        return formatDuration(secs);
    }

    private String formatDuration(long seconds) {
        long s = seconds;
        long days = s / 86400; s %= 86400;
        long hours = s / 3600; s %= 3600;
        long minutes = s / 60; long sec = s % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("일 ");
        if (hours > 0 || days > 0) sb.append(hours).append("시간 ");
        sb.append(minutes).append("분 ").append(sec).append("초");
        return sb.toString().trim();
    }

    public void reloadStatsFromDisk() {
        try {
            this.stats = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(this.statsFile);
        } catch (Exception ignored) {}
    }

}