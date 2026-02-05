package me.bixgamer707.hordes.file;

import me.bixgamer707.hordes.Hordes;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;

public class File extends YamlConfiguration {

    private final String fileName;

    private final Hordes plugin;

    private java.io.File file;

    private final java.io.File folder;

    public File(Hordes plugin, String fileName, java.io.File folder) {
        this.folder = folder;
        this.plugin = plugin;
        this.fileName = fileName + (fileName.endsWith(".yml") ? "" : ".yml");
        createFile();

    }

    public File(Hordes plugin, String fileName) {
        this(plugin, fileName, plugin.getDataFolder());
    }

    private void createFile() {
        try {
            file = new java.io.File(this.folder, this.fileName);

            if (file.exists()) {
                load(file);
                save(file);
                return;
            }

            String resourcePath = (this.folder.getName().equalsIgnoreCase("messages"))
                    ? "messages/" + this.fileName
                    : this.fileName;

            InputStream resource = this.plugin.getResource(resourcePath);
            if (resource != null) {
                java.nio.file.Files.copy(resource, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                save(file);
            }

            load(file);
        } catch (InvalidConfigurationException | IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "ERROR: Can't create the file '" + this.fileName + "'.", e);
        }
    }

    public void save() {
        try {
            save(file);
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Save of the file '" + this.fileName + "' failed.", e);
        }
    }

    public void reload() {
        try {
            load(file);
        } catch (IOException | InvalidConfigurationException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Reload of the file '" + this.fileName + "' failed.", e);
        }
    }
}