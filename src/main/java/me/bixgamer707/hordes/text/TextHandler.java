package me.bixgamer707.hordes.text;

import org.bukkit.entity.Player;

import java.util.Map;

public interface TextHandler {

    TextHandler colorize();

    TextHandler replace(String key, String value);

    String build();

    String build(Player player);

}
