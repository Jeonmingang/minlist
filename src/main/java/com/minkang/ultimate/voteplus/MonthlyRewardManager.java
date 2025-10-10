
package com.minkang.ultimate.voteplus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class MonthlyRewardManager {
    private final JavaPlugin plugin;
    private final ZoneId zoneId;
    private final File file;
    private YamlConfiguration data;

    public MonthlyRewardManager(JavaPlugin plugin) {
        this.plugin = plugin;
        String tz = plugin.getConfig().getString("monthly-reward.timezone", "Asia/Seoul");
        ZoneId zid;
        try { zid = ZoneId.of(tz); } catch (Exception e) { zid = ZoneId.of("Asia/Seoul"); }
        this.zoneId = zid;
        this.file = new File(plugin.getDataFolder(), "votes.yml");
        load();
        // schedule checker
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkMonthly, 20L * 10, 20L * 60 * 5);
    }

    private void load() {
        try {
            if (!file.exists()) {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            }
        } catch (IOException ignored) {}
        data = YamlConfiguration.loadConfiguration(file);
        if (!data.isString("lastProcessed")) {
            YearMonth prev = YearMonth.now(zoneId).minusMonths(1);
            data.set("lastProcessed", prev.toString());
            // optional: reset cumulative stats and totals after month changes
            if (plugin.getConfig().getBoolean("monthly-reward.reset-cumulative", true)) {
                // clear plugin-wide totals used for broadcasts
                data.set("total", 0);
                data.set("bySite", null);
                data.set("byPlayer", null);
                // clear alltime (누적) per-player counts
                data.set("alltime", null);
                save();
                try {
                    if (plugin instanceof UltimateVotePlus) {
                        ((UltimateVotePlus) plugin).reloadStatsFromDisk();
                    }
                } catch (Throwable ignored) {}
            }
            save();
        }
    }

    private void save() {
        try { data.save(file); } catch (IOException ignored) {}
    }

    public void recordVote(String playerName) {
        if (!plugin.getConfig().getBoolean("monthly-reward.enabled", true)) return;
        String p = playerName.toLowerCase(Locale.ROOT);
        String ym = YearMonth.from(ZonedDateTime.ofInstant(Instant.now(), zoneId)).toString();
        String base = "monthly." + ym + "." + p;
        data.set(base, data.getInt(base, 0) + 1);
        data.set("lastVoteTime." + ym + "." + p, System.currentTimeMillis());
        // also track alltime (누적)
        data.set("alltime." + p, data.getInt("alltime." + p, 0) + 1);
        save();
    }

    private void checkMonthly() {
        purgeOldMonths(plugin.getConfig().getInt("monthly-reward.keep-months", 12));
        if (!plugin.getConfig().getBoolean("monthly-reward.enabled", true)) return;
        YearMonth now = YearMonth.now(zoneId);
        YearMonth prev = now.minusMonths(1);
        String lastStr = data.getString("lastProcessed", prev.toString());
        YearMonth last = YearMonth.parse(lastStr);
        if (last.isBefore(prev)) {
            rewardTopFor(prev);
            data.set("lastProcessed", prev.toString());
            // optional: reset cumulative stats and totals after month changes
            if (plugin.getConfig().getBoolean("monthly-reward.reset-cumulative", true)) {
                // clear plugin-wide totals used for broadcasts
                data.set("total", 0);
                data.set("bySite", null);
                data.set("byPlayer", null);
                // clear alltime (누적) per-player counts
                data.set("alltime", null);
                save();
                try {
                    if (plugin instanceof UltimateVotePlus) {
                        ((UltimateVotePlus) plugin).reloadStatsFromDisk();
                    }
                } catch (Throwable ignored) {}
            }
            save();
        }
    }

    public void rewardTopFor(YearMonth ym) {
        String base = "monthly." + ym.toString();
        if (data.getConfigurationSection(base) == null) return;
        Map<String,Integer> map = new HashMap<>();
        for (String k : data.getConfigurationSection(base).getKeys(false)) {
            map.put(k, data.getInt(base + "." + k, 0));
        }
        if (map.isEmpty()) return;

        String tie = plugin.getConfig().getString("monthly-reward.tie-breaker", "earliest");
        String winner = null; int best = -1; long bestTime = Long.MAX_VALUE;
        for (Map.Entry<String,Integer> e : map.entrySet()) {
            String p = e.getKey(); int c = e.getValue();
            long t = data.getLong("lastVoteTime." + ym + "." + p, 0L);
            if (c > best) { best = c; winner = p; bestTime = (t==0L?Long.MAX_VALUE:t); }
            else if (c == best) {
                if ("earliest".equalsIgnoreCase(tie)) {
                    if ((t==0L?Long.MAX_VALUE:t) < bestTime) { winner = p; bestTime = t; }
                } else if ("latest".equalsIgnoreCase(tie)) {
                    if ((t==0L?0L:t) > bestTime) { winner = p; bestTime = t; }
                } else if ("lexicographical".equalsIgnoreCase(tie)) {
                    if (winner == null || p.compareTo(winner) < 0) winner = p;
                }
            }
        }
        if (winner == null) return;

        String cmd = plugin.getConfig().getString("monthly-reward.command", "캐시 지급 {player} 10000")
                .replace("{player}", winner)
                .replace("{count}", String.valueOf(best))
                .replace("{month}", ym.toString());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        String msg = plugin.getConfig().getString("monthly-reward.broadcast",
                "&e[추천] 지난달 1위 {player} ({count}회) — 10,000 캐시 지급!")
                .replace("{player}", winner)
                .replace("{count}", String.valueOf(best))
                .replace("{month}", ym.toString());
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }
}
