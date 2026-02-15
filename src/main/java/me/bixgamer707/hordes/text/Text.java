package me.bixgamer707.hordes.text;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.file.File;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clase mejorada para manejo de texto con soporte para:
 * - Colores hexadecimales (1.16+)
 * - PlaceholderAPI
 * - Mensajes multilenguaje
 * - Optimizaciones de rendimiento
 */
public class Text implements TextHandler {

    private final String text;
    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");

    /**
     * Constructor principal
     * @param text Texto a procesar (acepta null)
     */
    public Text(String text) {
        this.text = text == null ? "" : text;
    }

    /**
     * Crea un TextHandler con el texto ya colorizado
     * @param text Texto a procesar
     * @return TextHandler con colores aplicados
     */
    public static TextHandler createText(String text) {
        return new Text(text).colorize();
    }

    /**
     * Crea un TextHandler desde el archivo de mensajes principal
     * @param path Ruta en el archivo de mensajes
     * @return TextHandler con el mensaje colorizado
     */
    public static TextHandler createTextWithLang(String path) {
        return new Text(getMessages().getString("Messages." + path)).colorize();
    }

    /**
     * Crea un TextHandler desde un archivo personalizado
     * @param path Ruta en el archivo
     * @param file Archivo de configuración
     * @return TextHandler con el mensaje colorizado
     */
    public static TextHandler createTextWithLang(String path, File file) {
        return new Text(file.getString(path)).colorize();
    }

    /**
     * Envía un mensaje del archivo de idioma al jugador
     * @param path Ruta del mensaje
     * @param player Jugador que recibirá el mensaje
     */
    public static void createTextWithLang(String path, Player player) {
        createTextsWithLang(path, player, player);
    }

    /**
     * Envía un mensaje del archivo de idioma a un jugador con placeholders de otro
     * @param path Ruta del mensaje
     * @param player Jugador que recibirá el mensaje
     * @param target Jugador del cual se tomarán los placeholders
     */
    public static void createTextsWithLang(String path, Player player, Player target) {
        createTextWithLang(getMessages().getStringList("Messages." + path), player, target);
    }

    /**
     * Envía múltiples mensajes a un jugador
     * @param messages Lista de mensajes a enviar
     * @param player Jugador que recibirá los mensajes
     * @param target Jugador del cual se tomarán los placeholders
     */
    public static void createTextWithLang(List<String> messages, Player player, Player target) {
        if (messages == null || messages.isEmpty()) return;

        String[] lines = messages.stream()
                .map(s -> createText(s).build(target))
                .toArray(String[]::new);

        player.sendMessage(lines);
    }

    /**
     * Envía múltiples mensajes a un CommandSender (jugador o consola)
     * @param messages Lista de mensajes a enviar
     * @param sender CommandSender que recibirá los mensajes
     * @param target Jugador del cual se tomarán los placeholders
     */
    public static void createTextWithLang(List<String> messages, CommandSender sender, Player target) {
        if (messages == null || messages.isEmpty()) return;

        String[] lines = messages.stream()
                .map(s -> createText(s).build(target))
                .toArray(String[]::new);

        sender.sendMessage(lines);
    }

    /**
     * Reemplaza una clave por un valor en el texto
     * Usa replace() literal en vez de replaceAll() para evitar interpretación regex
     * @param key Clave a buscar
     * @param replace Valor de reemplazo
     * @return Nuevo TextHandler con el reemplazo aplicado
     */
    @Override
    public TextHandler replace(String key, String replace) {
        return createText(text.replace(
                key == null ? "" : key,
                replace == null ? "" : replace
        ));
    }

    /**
     * Aplica colorización al texto incluyendo:
     * - Códigos de color tradicionales (&)
     * - Colores hexadecimales (#RRGGBB) en 1.16+
     * - Prefijo del plugin
     * @return TextHandler con colores aplicados
     */
    @Override
    public TextHandler colorize() {
        String result = text;

        // Procesar colores hexadecimales si la versión lo soporta
        if (Bukkit.getVersion().contains("1.16")) {
            Matcher matcher = HEX_PATTERN.matcher(result);
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                String hex = matcher.group();
                String replacement;

                try {
                    replacement = ChatColor.of(hex).toString();
                } catch (IllegalArgumentException e) {
                    // Si el formato hex es inválido, mantener el original
                    replacement = hex;
                }

                // Usar quoteReplacement para evitar interpretación de caracteres especiales
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }

            matcher.appendTail(sb);
            result = sb.toString();
        }

        // Obtener prefijo de forma segura
        String prefix = "";
        try {
            String cfgPrefix = getConfig().getString("settings.prefix");
            prefix = cfgPrefix == null ? "" : cfgPrefix;
        } catch (Exception ignored) {
            // Si falla la lectura del config, usar prefijo vacío
        }

        // Aplicar códigos de color tradicionales y reemplazar prefijo
        result = ChatColor.translateAlternateColorCodes('&', result.replace("%prefix%", prefix));

        return new Text(result);
    }

    /**
     * Construye el texto final sin placeholders
     * @return Texto procesado
     */
    @Override
    public String build() {
        return text;
    }

    /**
     * Construye el texto final con placeholders de PlaceholderAPI
     * @param player Jugador del cual tomar los placeholders
     * @return Texto procesado con placeholders reemplazados
     */
    @Override
    public String build(Player player) {
        if (player != null && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                return PlaceholderAPI.setPlaceholders(player, text);
            } catch (Throwable ignored) {
                // Si PlaceholderAPI falla, devolver texto sin placeholders
            }
        }

        return build();
    }

    /**
     * Sends a message to a player with placeholders
     *
     * @param player Player to send message to
     * @param path Message path in messages file
     * @param replacements Placeholder replacements for {0}, {1}, etc.
     */
    public static void sendMessage(Player player, String path, Object... replacements) {
        String message = getMessage(path, replacements);
        player.sendMessage(createText(message).build(player));
    }

    /**
     * Sends a message to a CommandSender (player or console)
     *
     * @param sender CommandSender to send message to
     * @param path Message path in messages file
     * @param replacements Placeholder replacements
     */
    public static void sendMessage(CommandSender sender, String path, Object... replacements) {
        String message = getMessage(path, replacements);

        if (sender instanceof Player) {
            sender.sendMessage(Text.createText(message).build((Player) sender));
        } else {
            sender.sendMessage(Text.createText(message).build());
        }
    }

    /**
     * Gets a formatted message without sending
     *
     * @param path Message path in messages file
     * @param replacements Placeholder replacements
     * @return Formatted and colorized message
     */
    public static String getMessage(String path, Object... replacements) {
        String message = Text.getMessages().getString("Messages." + path, path);

        // Replace {0}, {1}, {2}...
        for (int i = 0; i < replacements.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(replacements[i]));
        }

        return Text.createText(message).build();
    }

    /**
     * Broadcasts a message to multiple players
     *
     * @param players Array of players
     * @param path Message path
     * @param replacements Placeholder replacements
     */
    public static void broadcast(Iterable<Player> players, String path, Object... replacements) {
        String message = getMessage(path, replacements);

        for (Player player : players) {
            player.sendMessage(Text.createText(message).build(player));
        }
    }

    /**
     * Obtiene el archivo de mensajes del plugin
     * @return Archivo de mensajes
     */
    public static File getMessages() {
        return Hordes.getInstance().getFileManager().getMessages();
    }

    /**
     * Obtiene el archivo de configuración del plugin
     * @return Archivo de configuración
     */
    public File getConfig() {
        return Hordes.getInstance().getFileManager().getConfig();
    }
}