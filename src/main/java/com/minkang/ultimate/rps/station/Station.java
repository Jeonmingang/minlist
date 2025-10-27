
package com.minkang.ultimate.rps.station;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Station {
    private final String name;
    private Location blockLocation;
    private ItemStack coinItem; // amount=1
    private int[] wedges; // size 20, index 0 -> x1
    private boolean hologram;
    private List<UUID> hologramIds = new ArrayList<>();
    private List<String> customHologramLines = new ArrayList<>();

    public Station(String name, Location blockLocation, int[] wedges) {
        this.name = name;
        this.blockLocation = blockLocation;
        this.wedges = wedges;
        this.hologram = false;
    }

    public String getName() { return name; }
    public Location getBlockLocation() { return blockLocation; }
    public void setBlockLocation(Location loc) { this.blockLocation = loc; }

    public ItemStack getCoinItem() { return coinItem; }
    public void setCoinItem(ItemStack coinItem) { this.coinItem = coinItem; }

    public int[] getWedges() { return wedges; }
    public void setWedges(int[] wedges) { this.wedges = wedges; }

    public boolean isHologram() { return hologram; }
    public void setHologram(boolean hologram) { this.hologram = hologram; }

    public List<UUID> getHologramIds() { return hologramIds; }
    public void setHologramIds(List<UUID> ids) { this.hologramIds = ids; }

    public List<String> getCustomHologramLines() { return customHologramLines; }
    public void setCustomHologramLines(List<String> lines) { this.customHologramLines = lines; }
}
