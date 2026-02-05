package me.bixgamer707.hordes.file;

import me.bixgamer707.hordes.Hordes;

import java.util.HashMap;
import java.util.Map;

public class FileManager{

    private final Hordes plugin;
    private java.io.File[] files;
    private java.io.File[] messages;
    private final Map<String, File> filesMap = new HashMap<>(7);

    public FileManager(Hordes plugin) {
        this.plugin = plugin;
    }

    public void loadFiles() {
        java.io.File messagesFolder = new java.io.File(plugin.getDataFolder(), "messages");
        if (!messagesFolder.exists()) {
            messagesFolder.mkdirs();
        }

        filesMap.put("config.yml", new File(plugin, "config"));
        filesMap.put("mobs.yml", new File(plugin, "mobs"));
        filesMap.put("arenas.yml", new File(plugin, "arenas"));
        filesMap.put("statistics.yml", new File(plugin, "statistics"));
        filesMap.put("guis.yml", new File(plugin, "guis"));

        filesMap.put("en_us.yml", new File(plugin, "en_us.yml", messagesFolder));
        filesMap.put("es_es.yml", new File(plugin, "es_es.yml", messagesFolder));

        files = plugin.getDataFolder().listFiles();
        if (files != null) {
            for (java.io.File file : files) {
                if (file.getName().endsWith(".yml")) {
                    filesMap.put(file.getName(), new File(plugin, file.getName()));
                }
            }
        }

        messages = messagesFolder.listFiles();
        if (messages != null) {
            for (java.io.File file : messages) {
                if (file.getName().endsWith(".yml")) {
                    filesMap.put(file.getName(), new File(plugin, file.getName(), messagesFolder));
                }
            }
        }
    }

    public void reload() {
        filesMap.forEach((name, file) -> file.reload());
    }

    public File getConfig(){
        return getFile("config.yml");
    }

    public File getMobs(){
        return getFile("mobs.yml");
    }

    public File getArenas(){
        return getFile("arenas.yml");
    }

    public File getMessages(){
        return getFile(
                getConfig().contains("Settings.language") ? getConfig().getString("Settings.language") : "en_us.yml"
        );
    }

    public File getGuis(){
        return getFile("guis.yml");
    }

    public void createFile(String id) {
        filesMap.put(id, new File(plugin, id));
    }

    public File getFile(String name) {
        if(filesMap.get(name) == null){
            createFile(name);
        }

        return filesMap.get(name);
    }

    public void saveFiles() {
        filesMap.forEach((key, value) -> value.save());
        filesMap.clear();
    }


    public File getStatistics() {
        return getFile("statistics.yml");
    }
}
