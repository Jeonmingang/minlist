package com.minkang.ultimate.voteplus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class AutoNoticeManager {
    private final JavaPlugin plugin;
    private File file;
    private YamlConfiguration yaml;
    private final Map<Integer, Integer> runningTasks = new LinkedHashMap<>(); // id -> taskId

    public AutoNoticeManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** autonotice.yml 로드/초기화 */
    public void load() {
        try {
            file = new File(plugin.getDataFolder(), "autonotice.yml");
            if (!file.exists()) {
                plugin.getDataFolder().mkdirs();
                yaml = new YamlConfiguration();
                yaml.set("enabled", true);
                yaml.set("interval-seconds", 60);            // 기본 간격
                Map<String, String> defaults = new LinkedHashMap<>();
                defaults.put("1", "&a[공지]&f 디스코드 참여: &bdiscord.gg/yourcode");
                defaults.put("2", "&e[이벤트]&f 매일 &a/보상 &f확인!");
                yaml.createSection("messages", defaults);     // 번호 -> 메시지
                yaml.createSection("intervals");              // 번호 -> 개별 간격(초)
                yaml.save(file);
            }
            yaml = YamlConfiguration.loadConfiguration(file);
            // 보정
            if (!yaml.isSet("enabled")) yaml.set("enabled", true);
            if (!yaml.isInt("interval-seconds")) yaml.set("interval-seconds", 60);
            if (!yaml.isConfigurationSection("messages")) yaml.createSection("messages");
            if (!yaml.isConfigurationSection("intervals")) yaml.createSection("intervals");
            save();
        } catch (Exception e) {
            plugin.getLogger().warning("[AutoNotice] 로드 오류: " + e.getMessage());
        }
    }

    public void save() {
        try { yaml.save(file); } catch (IOException e) {
            plugin.getLogger().warning("[AutoNotice] 저장 오류: " + e.getMessage());
        }
    }

    // 상태/간격
    public boolean isEnabled() {
        return yaml.getBoolean("enabled", true);
    }
    public void setEnabled(boolean v) {
        yaml.set("enabled", v);
        save();
        if (v) start(); else stop();
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

    // 메시지 CRUD
    public Map<Integer, String> getMessages() {
        Map<Integer,String> map = new TreeMap<>();
        for (String k : yaml.getConfigurationSection("messages").getKeys(false)) {
            try {
                int id = Integer.parseInt(k);
                String msg = yaml.getString("messages." + k, "");
                if (msg != null && !msg.isEmpty()) map.put(id, msg);
            } catch (NumberFormatException ignore) {}
        }
        return map;
    }
    public void addMessage(int id, String content) {
        yaml.set("messages." + id, content);
        save();
        if (isRunning()) { stop(); start(); }
    }
    public boolean removeMessage(int id) {
        String path = "messages." + id;
        if (yaml.contains(path)) {
            yaml.set(path, null);
            // interval도 같이 정리
            yaml.set("intervals." + id, null);
            save();
            if (isRunning()) { stop(); start(); }
            return true;
        }
        return false;
    }

    public boolean isRunning() {
        return !runningTasks.isEmpty();
    }

    public void start() {
        if (!isEnabled()) return;
        stop(); // 초기화
        Map<Integer, String> map = getMessages();
        for (Map.Entry<Integer, String> e : map.entrySet()) {
            final int id = e.getKey();
            final String msg = ChatColor.translateAlternateColorCodes('&', e.getValue());
            final int sec = getIntervalSeconds(id);
            int tid = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                Bukkit.broadcastMessage(msg);
            }, 20L * 5, 20L * sec); // 5초 후 시작
            runningTasks.put(id, tid);
        }
        plugin.getLogger().info("[AutoNotice] started " + runningTasks.size() + " tasks.");
    }

    public void stop() {
        for (Integer tid : runningTasks.values()) {
            try { Bukkit.getScheduler().cancelTask(tid); } catch (Throwable ignore) {}
        }
        runningTasks.clear();
    }

    // UI
    private static String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }

    public void sendList(CommandSender sender) {
        Map<Integer, String> map = getMessages();
        sender.sendMessage(color("&a[자동공지 목록] &7(기본 간격: " + getDefaultIntervalSeconds() + "초, 상태: " + (isEnabled() ? "&aON" : "&cOFF") + "&7)"));
        if (map.isEmpty()) {
            sender.sendMessage(color("&7등록된 메시지가 없습니다."));
            return;
        }
        for (Map.Entry<Integer, String> e : map.entrySet()) {
            int id = e.getKey();
            sender.sendMessage(color("&e#" + id + " &7(" + getIntervalSeconds(id) + "초): &f" + e.getValue()));
        }
    }
}
