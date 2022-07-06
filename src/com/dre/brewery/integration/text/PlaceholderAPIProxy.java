package com.dre.brewery.integration.text;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;

public class PlaceholderAPIProxy {

    public static List<String> parseList(Player player, List<String> strings) {
        return PlaceholderAPI.setPlaceholders(player, strings);
    }

    public static List<String> parseList(OfflinePlayer player, List<String> strings) {
        return PlaceholderAPI.setPlaceholders(player, strings);
    }

    public static String parse(Player player, String string) {
        return PlaceholderAPI.setPlaceholders(player, string);
    }

    public static String parse(OfflinePlayer player, String string) {
        return PlaceholderAPI.setPlaceholders(player, string);
    }

    public static List<String> parseList(List<String> strings) {
        return PlaceholderAPI.setPlaceholders(null, strings);
    }

    public static String parse(String string) {
        return PlaceholderAPI.setPlaceholders(null, string);
    }
}
