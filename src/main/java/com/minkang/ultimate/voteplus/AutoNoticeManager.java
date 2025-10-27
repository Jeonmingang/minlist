package com.minkang.ultimate.voteplus;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public class AutoNoticeManager {
    private final JavaPlugin plugin;
    private File file;
    private YamlConfiguration yaml;

    private int taskId = -1;
    private String lastId = null;

    public AutoNoticeManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        try {
            file = new File(plugin.getDataFolder(), "autonotice.yml");
            if (!file.exists()) {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            }
            yaml = YamlConfiguration.loadConfiguration(file);
            if (!yaml.isConfigurationSection("autonotice.messages")) {
                yaml.createSection("autonotice.messages");
                save();
            }
        } catch (IOException e) {
            plugin.getLogger().severe("[AutoNotice] 파일 로드 오류: " + e.getMessage());
        }
    }

    public void save() {
        try {
            if (yaml != null) yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("[AutoNotice] 파일 저장 오류: " + e.getMessage());
        }
    }

    public List<String> getIds() {
        Set<String> keys = yaml.getConfigurationSection("autonotice.messages").getKeys(false);
        List<String> list = new ArrayList<>(keys);
        list.sort(Comparator.comparingInt(Integer::parseInt));
        return list;
    }

    public String getText(String id) {
        return yaml.getString("autonotice.messages." + id + ".text", null);
    }

    public int getSeconds(String id) {
        return yaml.getInt("autonotice.messages." + id + ".seconds", 0);
    }

    public void setText(String id, String text) {
        yaml.set("autonotice.messages." + id + ".text", text);
        save();
    }

    public void setSeconds(String id, int seconds) {
        yaml.set("autonotice.messages." + id + ".seconds", Math.max(0, seconds));
        save();
    }

    public void remove(String id) {
        yaml.set("autonotice.messages." + id, null);
        save();
    }

    public String color(String s) {
        if (s == null) return "";
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
    }

    private List<String> validIdsWithTime() {
        List<String> ids = getIds();
        List<String> out = new ArrayList<>();
        for (String id : ids) {
            if (getSeconds(id) > 0 && getText(id) != null && !getText(id).isEmpty()) out.add(id);
        }
        return out;
    }

    private String nextId() {
        List<String> ids = validIdsWithTime();
        if (ids.isEmpty()) return null;
        if (lastId == null) return ids.get(0);
        int idx = ids.indexOf(lastId);
        if (idx == -1 || idx + 1 >= ids.size()) return ids.get(0);
        return ids.get(idx + 1);
    }

    public void start() {
        stop();
        scheduleNext(0L);
        plugin.getLogger().info("[AutoNotice] started with " + validIdsWithTime().size() + " message(s).");
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private void scheduleNext(long delayTicks) {
        stop();
        taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            String id = nextId();
            if (id == null) {
                // nothing to send; check again in 10s
                scheduleNext(200L);
                return;
            }
            lastId = id;
            String text = getText(id);
            int sec = getSeconds(id);

            // Broadcast (supports multi-part with '|')
            String colored = color(text == null ? "" : text);
            String[] parts = colored.split(Pattern.quote("|"), -1);
            if (parts.length == 0) {
                Bukkit.broadcastMessage(colored);
            } else if (parts.length == 1) {
                Bukkit.broadcastMessage(parts[0].trim());
            } else {
                if (!parts[0].trim().isEmpty()) Bukkit.broadcastMessage(parts[0].trim());
                if (!parts[1].trim().isEmpty()) Bukkit.broadcastMessage(parts[1].trim());
            }

            // Schedule using this message's seconds
            long nextTicks = Math.max(1, sec) * 20L;
            scheduleNext(nextTicks);
        }, delayTicks);
    }
}
