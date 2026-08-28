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

    // Tracks whether this file has real, unsaved changes.
    // Prevents rewriting (and stripping comments from) files that
    // were only loaded/reloaded but never actually modified.
    private boolean dirty = false;

    public File(Hordes plugin, String fileName, java.io.File folder) {
        this.folder = folder;
        this.plugin = plugin;
        this.fileName = fileName + (fileName.endsWith(".yml") ? "" : ".yml");
        createFile();

    }

    public File(Hordes plugin, String fileName) {
        this(plugin, fileName, plugin.getDataFolder());
    }

    /**
     * Overridden so any code that modifies this configuration (e.g.
     * StatisticsManager) automatically marks the file as needing a save.
     */
    @Override
    public void set(String path, Object value) {
        super.set(path, value);
        dirty = true;
    }

    private void createFile() {
        try {
            file = new java.io.File(this.folder, this.fileName);

            if (file.exists()) {
                // Just load it into memory - do NOT re-save. YamlConfiguration.save()
                // strips all comments, and since nothing changed there's no reason
                // to rewrite the file on every startup/reload.
                load(file);
                dirty = false; // loading may internally call set() on top-level keys; reset
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
            dirty = false;
        } catch (InvalidConfigurationException | IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "ERROR: Can't create the file '" + this.fileName + "'.", e);
        }
    }

    /**
     * Saves this file to disk, but only if it actually has unsaved changes.
     * This avoids needlessly rewriting (and stripping comments from) files
     * that were loaded but never modified.
     */
    public void save() {
        if (!dirty) {
            return;
        }

        try {
            save(file);
            dirty = false;
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Save of the file '" + this.fileName + "' failed.", e);
        }
    }

    /**
     * Forces a save to disk regardless of the dirty flag.
     * Use only when you explicitly need to persist right now
     * (e.g. right after a manual edit through an admin GUI).
     */
    public void forceSave() {
        try {
            save(file);
            dirty = false;
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Save of the file '" + this.fileName + "' failed.", e);
        }
    }

    public void reload() {
        try {
            load(file);
            dirty = false;
        } catch (IOException | InvalidConfigurationException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Reload of the file '" + this.fileName + "' failed.", e);
        }
    }

    public boolean isDirty() {
        return dirty;
    }
}