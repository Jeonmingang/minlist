
package com.minkang.ultimate.rps.game;

import com.minkang.ultimate.rps.station.Station;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GameSession {
    public final Player player;
    public final Station station;
    public RpsChoice playerChoice = null;
    public RpsChoice systemChoice = null;
    public ItemStack betItem = null; // item (amount used as bet count)
    public int betAmount = 0;

    public GameSession(Player player, Station station) {
        this.player = player;
        this.station = station;
    }
}
