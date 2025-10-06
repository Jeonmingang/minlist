package com.minkang.ultimate.voteplus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 자동 공지 기능을 담당하는 매니저.
 * 기본 config.yml 은 유지하고, 별도의 autonotice.yml 로만 관리합니다.
 *
 * 명령:
 *  /자동공지 추가 <번호> <내용>
 *  /자동공지 삭제 <번호>
 *  /자동공지 목록
 *  /자동공지 시간 <초>
 *
 * 권한 체크 없음(요청 사항). 콘솔/플레이어 모두 사용 가능.
 */
public class AutoNoticeManager {
    private final JavaPlugin plugin;
    private File file;
    private YamlConfiguration yaml;
    private int taskId = -1;
    private final AtomicInteger index = new AtomicInteger(0);
    private final Map<Integer, Integer> runningTasks = new LinkedHashMap<>();

    public AutoNoticeManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        try {
            file = new File(plugin.getDataFolder(), "autonotice.yml");
            if (!file.exists()) {
                plugin.getDataFolder().mkdirs();
                yaml = new YamlConfiguration();
                yaml.set("enabled", true);
                yaml.set("interval-seconds", 60); yaml.createSection("intervals");
                Map<String, String> defaults = new LinkedHashMap<>();
                defaults.put("1", "&a[공지]&f 디스코드 참여: &bdiscord.gg/yourcode");
                defaults.put("2", "&e[이벤트]&f 매일 &a/보상 &f확인!");
                yaml.createSection("messages", defaults);
                yaml.save(file);
            }
            yaml = YamlConfiguration.loadConfiguration(file);
            // 보정
            if (!yaml.isInt("interval-seconds")) yaml.set("interval-seconds", 60); yaml.createSection("intervals");
            if (!yaml.isConfigurationSection("messages")) yaml.createSection("messages");
            save();
        } catch (Exception e) {
            plugin.getLogger().warning("[AutoNotice] 로드 중 오류: " + e.getMessage());
        }
    }

    public void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("[AutoNotice] 저장 오류: " + e.getMessage());
        }
    }

    public boolean isEnabled() {
        return yaml.getBoolean("enabled", true);
    }

    public void setEnabled(boolean enabled) {
        yaml.set("enabled", enabled);
        save();
        if (enabled) start(); else stop();
    }

    public int getIntervalSeconds() {
        return Math.max(5, yaml.getInt("interval-seconds", 60));
    }

    public void setIntervalSeconds(int sec) {
        yaml.set("interval-seconds", Math.max(5, sec));
        save();
        // 재시작
        if (isRunning()) {
            stop();
            start();
        }
    }

    public Map<Integer, String> getMessages() {
        Map<Integer, String> map = new TreeMap<>();
        if (yaml.isConfigurationSection("messages")) {
            for (String key : yaml.getConfigurationSection("messages").getKeys(false)) {
                try {
                    int id = Integer.parseInt(key);
                    String msg = yaml.getString("messages." + key, "");
                    if (msg != null && !msg.trim().isEmpty()) {
                        map.put(id, msg);
                    }
                } catch (NumberFormatException ignore) {}
            }
        }
        return map;
    }

    public void addMessage(int id, String content) {
        yaml.set("messages." + id, content);
        save();
    }

    public boolean removeMessage(int id) {
        String path = "messages." + id;
        if (yaml.contains(path)) {
            yaml.set(path, null);
            save();
            return true;
        }
        return false;
    }

    
    public int getDefaultIntervalSeconds() {
        int sec = yaml.getInt("interval-seconds", 60);
        return Math.max(5, sec);
    }
    public void setDefaultIntervalSeconds(int sec) {
        yaml.set("interval-seconds", Math.max(5, sec));
        save();
        if (isRunning()) { stop(); start(); }
    }
    public int getIntervalSeconds(int id) {
        int v = yaml.getInt("intervals." + id, -1);
        if (v <= 0) return getDefaultIntervalSeconds();
        return Math.max(5, v);
    }
    public void setIntervalSeconds(int id, int sec) {
        yaml.set("intervals." + id, Math.max(5, sec));
        save();
        if (isRunning()) { stop(); start(); }
    }

    public boolean isRunning() {
        return taskId != -1;
    }

    public void start() {
        if (!isEnabled()) return;
        if (isRunning()) stop();

        Map<Integer, String> map = new LinkedHashMap<>(getMessages());
        taskId = 0; // marker that we're running
        // For each message, schedule its own task with its own interval
        for (Map.Entry<Integer, String> e : map.entrySet()) {
            final int id = e.getKey();
            final String msg = ChatColor.translateAlternateColorCodes('&', e.getValue());
            final int sec = getIntervalSeconds(id);
            int tid = Bukkit.getScheduler().scheduleSyncRepeatingTask((JavaPlugin) plugin, () -> {
                Bukkit.broadcastMessage(msg);
            }, 20L * 5, 20L * sec); // 5초 지연 후 시작
            runningTasks.put(id, tid);
        }
    }
        index.set(0);
        int period = Math.max(5, getIntervalSeconds());

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!isEnabled()) return;
            if (entries.isEmpty()) return;
            int i = index.getAndUpdate(prev -> (prev + 1) % entries.size());
            String raw = entries.get(i).getValue();
            String colored = ChatColor.translateAlternateColorCodes('&', raw);
            Bukkit.broadcastMessage(colored);
        }, period * 20L, period * 20L);
    }

    public void stop() {
        // cancel per-message tasks
        for (Map.Entry<Integer, Integer> e : new ArrayList<>(runningTasks.entrySet())) {
            Integer tid = e.getValue();
            if (tid != null) {
                try { Bukkit.getScheduler().cancelTask(tid); } catch (Throwable ignore) {}
            }
        }
        runningTasks.clear();
        taskId = -1;
    }
    }

    // 간단한 메시지 헬퍼
    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public void sendList(CommandSender sender) {
        Map<Integer, String> map = getMessages();
        if (map.isEmpty()) {
            sender.sendMessage(color("&7[자동공지] 등록된 메시지가 없습니다."));
            return;
        }
        sender.sendMessage(color("&a[자동공지 목록] &7(기본 간격: " + getDefaultIntervalSeconds() + "초, 상태: " + (isEnabled() ? "&aON" : "&cOFF") + "&7)"));
        for (Map.Entry<Integer, String> e : map.entrySet()) {
            sender.sendMessage(color("&e#" + e.getKey() + " &7(" + getIntervalSeconds(e.getKey()) + "초): &f" + e.getValue()));
        }
    }
}