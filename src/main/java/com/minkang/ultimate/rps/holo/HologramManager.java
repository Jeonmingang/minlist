
package com.minkang.ultimate.rps.holo;

import com.minkang.ultimate.rps.UltimateRpsPlugin;
import com.minkang.ultimate.rps.station.Station;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HologramManager {

    private final UltimateRpsPlugin plugin;

    public HologramManager(UltimateRpsPlugin plugin) { this.plugin = plugin; }

    public void spawnOrRefresh(Station st) {
        despawn(st);
        java.util.List<String> lines = st.getCustomHologramLines();
        int minMul = 0; int maxMul = 0; int idx = 1;
        for (int w : st.getWedges()) { if (w > 0) { if (minMul==0) minMul = idx; maxMul = idx; } idx++; }
        String coinName = "미설정";
        if (st.getCoinItem() != null) {
            org.bukkit.inventory.meta.ItemMeta cm = st.getCoinItem().getItemMeta();
            if (cm != null && cm.hasDisplayName()) coinName = cm.getDisplayName();
            else coinName = st.getCoinItem().getType().name();
        }

        if (lines == null || lines.isEmpty()) lines = plugin.getConfig().getStringList("hologram.lines");
        List<UUID> ids = new ArrayList<>();
        Location base = st.getBlockLocation().clone().add(0.5, 1.2, 0.5);
        double dy = 0.28;
        for (int i = lines.size()-1; i >= 0; i--) {
            String line = color(lines.get(i).replace("{name}", st.getName()).replace("{coin}", coinName)
                .replace("{최소}", (minMul==0? "1" : String.valueOf(minMul)))
                .replace("{최대}", (maxMul==0? "1" : String.valueOf(maxMul))));
            ArmorStand as = (ArmorStand) base.getWorld().spawnEntity(base.clone().add(0, (lines.size()-1-i)*dy, 0), EntityType.ARMOR_STAND);
            as.setMarker(true);
            as.setGravity(false);
            as.setVisible(false);
            as.setCustomNameVisible(true);
            as.setCustomName(line);
            ids.add(as.getUniqueId());
        }
        st.setHologramIds(ids);
        plugin.stations().save();
    }

    public void despawn(Station st) {
        for (UUID id : st.getHologramIds()) {
            if (id == null) continue;
            if (plugin.getServer().getEntity(id) != null) plugin.getServer().getEntity(id).remove();
        }
        st.setHologramIds(new ArrayList<>());
    }

    public void despawnAll() {
        for (String name : plugin.stations().getNames()) {
            Station st = plugin.stations().getByName(name);
            if (st != null) despawn(st);
        }
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }
}
