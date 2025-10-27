
package com.minkang.ultimate.rps.station;

import com.minkang.ultimate.rps.UltimateRpsPlugin;
import com.minkang.ultimate.rps.gui.RpsGui;
import com.minkang.ultimate.rps.util.SerializeUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class StationManager implements Listener {
    private final UltimateRpsPlugin plugin;
    private final Map<String, Station> byName = new LinkedHashMap<>(); // keep order
    private final Map<String, String> byBlockKey = new HashMap<>(); // blockKey -> name
    private File file;
    private YamlConfiguration yaml;

    public StationManager(UltimateRpsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stations.yml");
        this.yaml = new YamlConfiguration();
    }

    public void load() {
        byName.clear();
        byBlockKey.clear();
        if (!file.exists()) {
            save();
            return;
        }
        try { yaml.load(file); } catch (Exception e) { e.printStackTrace(); }
        if (!yaml.isConfigurationSection("stations")) return;
        for (String name : yaml.getConfigurationSection("stations").getKeys(false)) {
            String path = "stations."+name+".";
            String world = yaml.getString(path+"world");
            double x = yaml.getDouble(path+"x");
            double y = yaml.getDouble(path+"y");
            double z = yaml.getDouble(path+"z");
            World w = Bukkit.getWorld(world);
            if (w == null) continue;
            Location loc = new Location(w, x, y, z);
            List<Integer> wedgeList = yaml.getIntegerList(path+"wedges");
            int[] wedges = new int[20];
            for (int i=0;i<20;i++) wedges[i] = (i < wedgeList.size() ? wedgeList.get(i) : 1);
            Station st = new Station(name, loc, wedges);
            String coin64 = yaml.getString(path+"coin");
            if (coin64 != null && !coin64.isEmpty()) {
                try { ItemStack coin = SerializeUtils.itemFromBase64(coin64);
                      if (coin != null) { coin.setAmount(1); st.setCoinItem(coin); } } catch (Exception ex) { ex.printStackTrace(); }
            }
            st.setHologram(yaml.getBoolean(path+"hologram", false));
            List<String> ids = yaml.getStringList(path+"holo-ids");
            List<UUID> uids = new ArrayList<>();
            for (String s : ids) { try { uids.add(UUID.fromString(s)); } catch (Exception ignored) {} }
            st.setHologramIds(uids);
            // custom holo lines
            java.util.List<String> hLines = yaml.getStringList(path+"holo-lines");
            if (hLines != null && !hLines.isEmpty()) st.setCustomHologramLines(hLines);

            byName.put(name, st);
            byBlockKey.put(blockKey(loc), name);
        }
    }

    public void save() {
        yaml = new YamlConfiguration();
        for (Station st : byName.values()) {
            String path = "stations."+st.getName()+".";
            yaml.set(path+"world", st.getBlockLocation().getWorld().getName());
            yaml.set(path+"x", st.getBlockLocation().getX());
            yaml.set(path+"y", st.getBlockLocation().getY());
            yaml.set(path+"z", st.getBlockLocation().getZ());
            // wedges
            List<Integer> wedges = new ArrayList<>();
            int[] w = st.getWedges();
            for (int i=0;i<20;i++) wedges.add(i < w.length ? w[i] : 1);
            yaml.set(path+"wedges", wedges);
            // coin
            if (st.getCoinItem() != null) {
                try { yaml.set(path+"coin", SerializeUtils.itemToBase64(st.getCoinItem())); } catch (Exception e) { e.printStackTrace(); }
            }
            yaml.set(path+"hologram", st.isHologram());
            List<String> ids = new ArrayList<>();
            for (UUID id : st.getHologramIds()) ids.add(id.toString());
            yaml.set(path+"holo-ids", ids);
            if (st.getCustomHologramLines() != null && !st.getCustomHologramLines().isEmpty()) yaml.set(path+"holo-lines", st.getCustomHologramLines());
        }
        try { yaml.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public Station createStation(String name, Location block) {
        int[] def = getDefaultWedges();
        Station st = new Station(name, block.clone().getBlock().getLocation().add(0,0,0), def);
        byName.put(name, st);
        byBlockKey.put(blockKey(block), name);
        save();
        return st;
    }

    public void removeStation(String name) {
        Station st = byName.remove(name);
        if (st != null) {
            byBlockKey.remove(blockKey(st.getBlockLocation()));
            save();
        }
    }

    public Station getByName(String name) { return byName.get(name); }
    public Set<String> getNames() { return byName.keySet(); }

    public Station getByBlock(Block block) {
        String key = blockKey(block.getLocation());
        String name = byBlockKey.get(key);
        if (name == null) return null;
        return byName.get(name);
    }

    private String blockKey(Location loc) {
        Location b = loc.getBlock().getLocation();
        return b.getWorld().getName()+":"+b.getBlockX()+","+b.getBlockY()+","+b.getBlockZ();
    }

    private int[] getDefaultWedges() {
        List<Integer> list = plugin.getConfig().getIntegerList("default-wedges");
        int[] arr = new int[20];
        for (int i=0;i<20;i++) arr[i] = i < list.size() ? list.get(i) : 1;
        return arr;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;
        Station st = getByBlock(e.getClickedBlock());
        if (st == null) return;
        if (e.getClickedBlock().getType() != Material.DIAMOND_BLOCK) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        if (st.getCoinItem() == null) {
            p.sendMessage(color(plugin.getConfig().getString("messages.coin-not-set")));
            return;
        }
        RpsGui.open(p, st);
    }

    private String color(String s) { return org.bukkit.ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }
}
