package com.minkang.ultimate.voteplus.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ItemSerializer {

    public static String itemToBase64(ItemStack item) throws IOException {
        if (item == null) return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BukkitObjectOutputStream oos = new BukkitObjectOutputStream(out);
        oos.writeObject(item);
        oos.close();
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    public static ItemStack itemFromBase64(String data) throws IOException, ClassNotFoundException {
        if (data == null) return null;
        byte[] bytes = Base64.getDecoder().decode(data);
        BukkitObjectInputStream ois = new BukkitObjectInputStream(new ByteArrayInputStream(bytes));
        try {
            Object obj = ois.readObject();
            return (ItemStack) obj;
        } finally {
            ois.close();
        }
    }

    public static List<String> serializeList(ItemStack[] items) {
        List<String> out = new ArrayList<>();
        if (items == null) return out;
        for (ItemStack it : items) {
            try {
                if (it != null) {
                    String b64 = itemToBase64(it);
                    if (b64 != null) out.add(b64);
                }
            } catch (IOException ignored) {}
        }
        return out;
    }

    public static List<ItemStack> deserializeList(List<String> data) {
        List<ItemStack> out = new ArrayList<>();
        if (data == null) return out;
        for (String s : data) {
            try {
                ItemStack it = itemFromBase64(s);
                if (it != null) out.add(it);
            } catch (Exception ignored) {}
        }
        return out;
    }
}
