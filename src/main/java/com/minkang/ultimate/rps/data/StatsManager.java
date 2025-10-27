
package com.minkang.ultimate.rps.data;

import com.minkang.ultimate.rps.UltimateRpsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class StatsManager {
    private final UltimateRpsPlugin plugin;
    private final File file;
    private YamlConfiguration yaml;

    public StatsManager(UltimateRpsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        this.yaml = new YamlConfiguration();
    }

    public void load() {
        if (!file.exists()) save();
        try { yaml.load(file); } catch (Exception e) { e.printStackTrace(); }
    }

    public void save() {
        try { yaml.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void addMachinePayout(String station, int coins) {
        String path = "machines."+station;
        yaml.set(path, yaml.getInt(path, 0) + coins);
        save();
    }

    public void addWin(UUID uuid, int coinsWon, int multiplier) {
        yaml.set(uuid+".wins", yaml.getInt(uuid+".wins",0)+1);
        yaml.set(uuid+".coinsWon", yaml.getInt(uuid+".coinsWon",0)+coinsWon);
        int best = yaml.getInt(uuid+".best",1);
        if (multiplier > best) yaml.set(uuid+".best", multiplier);
        save();
    }

    public void addLoss(UUID uuid, int coinsLost) {
        yaml.set(uuid+".losses", yaml.getInt(uuid+".losses",0)+1);
        yaml.set(uuid+".coinsLost", yaml.getInt(uuid+".coinsLost",0)+coinsLost);
        save();
    }

    public void sendTop(CommandSender s) {
        String P = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix", "&a[ 가위바위보 ]&f "));
        java.util.Map<String, Integer> map = new java.util.HashMap<>();
        if (yaml.isConfigurationSection("machines")) {
            for (String key : yaml.getConfigurationSection("machines").getKeys(false)) {
                map.put(key, yaml.getInt("machines."+key, 0));
            }
        }
        java.util.List<java.util.Map.Entry<String,Integer>> list = map.entrySet().stream()
                .sorted((a,b)->b.getValue()-a.getValue()).limit(10).collect(java.util.stream.Collectors.toList());
        s.sendMessage(P + "기계별 총 지급 코인 TOP 10");
        int i=1;
        for (java.util.Map.Entry<String,Integer> e : list) {
            s.sendMessage(ChatColor.GRAY+""+i+". "+ChatColor.AQUA+e.getKey()+ChatColor.WHITE+" - "+e.getValue()+"개");
            i++;
        }
        if (list.isEmpty()) {
            s.sendMessage(ChatColor.GRAY + "데이터가 없습니다.");
        }
    }

    public void sendStats(CommandSender s, String name) {
        String P = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix", "&a[ 가위바위보 ]&f "));
        OfflinePlayer op = Bukkit.getOfflinePlayer(name);
        UUID u = op.getUniqueId();
        int wins = yaml.getInt(u+".wins",0);
        int losses = yaml.getInt(u+".losses",0);
        int won = yaml.getInt(u+".coinsWon",0);
        int lost = yaml.getInt(u+".coinsLost",0);
        int best = yaml.getInt(u+".best",1);
        s.sendMessage(P + ChatColor.AQUA + name + ChatColor.WHITE + "님의 전적");
        s.sendMessage(ChatColor.GRAY + "승: " + wins + "  패: " + losses + "  최고배수: x" + best);
        s.sendMessage(ChatColor.GRAY + "총 획득: " + won + "개  총 손실: " + lost + "개");
    }
}
