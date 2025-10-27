
package com.minkang.ultimate.rps.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemUtils {

    public static ItemStack named(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        im.setDisplayName(name);
        it.setItemMeta(im);
        return it;
    }

    public static ItemStack lore(ItemStack it, List<String> lore) {
        ItemMeta im = it.getItemMeta();
        im.setLore(lore);
        it.setItemMeta(im);
        return it;
    }

    public static List<String> colorLore(List<String> list) {
        List<String> out = new ArrayList<>();
        for (String s : list) out.add(color(s));
        return out;
    }

    public static boolean isSimilarIgnoreAmount(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getType() != b.getType()) return false;
        ItemMeta am = a.getItemMeta();
        ItemMeta bm = b.getItemMeta();
        if ((am == null) != (bm == null)) return false;
        if (am == null) return true;
        String an = am.hasDisplayName() ? am.getDisplayName() : null;
        String bn = bm.hasDisplayName() ? bm.getDisplayName() : null;
        if (an != null || bn != null) {
            if (an == null || bn == null) return false;
            if (!an.equals(bn)) return false;
        }
        List<String> al = am.hasLore() ? am.getLore() : null;
        List<String> bl = bm.hasLore() ? bm.getLore() : null;
        if (al != null || bl != null) {
            if (al == null || bl == null) return false;
            if (!al.equals(bl)) return false;
        }
        return true;
    }

    public static void hideAllFlags(ItemStack it) {
        ItemMeta m = it.getItemMeta();
        for (ItemFlag f : ItemFlag.values()) m.addItemFlags(f);
        it.setItemMeta(m);
    }

    public static String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }
}
