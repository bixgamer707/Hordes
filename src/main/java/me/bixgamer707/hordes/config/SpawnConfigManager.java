package me.bixgamer707.hordes.config;

import me.bixgamer707.hordes.Hordes;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages dynamic spawn point configuration
 * Allows in-game modification of spawn points in arenas.yml
 */
public class SpawnConfigManager {

    private final Hordes plugin;

    public SpawnConfigManager(Hordes plugin) {
        this.plugin = plugin;
    }

    /**
     * Sets a spawn point for an arena
     * Modifies arenas.yml directly
     * 
     * @param arenaId Arena ID
     * @param spawnType Type: "lobby", "arena", or "exit"
     * @param location Location to set
     * @return true if successful
     */
    public boolean setSpawn(String arenaId, String spawnType, Location location) {
        try {
            // Get the actual file from FileManager
            me.bixgamer707.hordes.file.File arenasConfig = plugin.getFileManager().getArenas();
            File arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
            
            if (!arenasFile.exists()) {
                return false;
            }
            
            // Read file
            List<String> lines = readFile(arenasFile);
            
            // Find and update spawn
            boolean found = modifySpawn(lines, arenaId, spawnType, location);
            
            if (found) {
                // Write back to file
                writeFile(arenasFile, lines);
                
                // Reload the File object
                arenasConfig.reload();
                
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            plugin.logError("Failed to set spawn: " + e.getMessage());
            if (plugin.getFileManager().getFile("config.yml").getBoolean("debug-mode", false)) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Reads file into list of lines
     */
    private List<String> readFile(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        
        return lines;
    }

    /**
     * Writes lines back to file
     */
    private void writeFile(File file, List<String> lines) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (int i = 0; i < lines.size(); i++) {
                writer.write(lines.get(i));
                if (i < lines.size() - 1) {
                    writer.newLine();
                }
            }
        }
    }

    /**
     * Modifies spawn configuration in lines
     */
    private boolean modifySpawn(List<String> lines, String arenaId, String spawnType, Location location) {
        // Find arena section
        int arenaLineIndex = -1;
        int indentLevel = 0;
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();
            
            if (trimmed.equals(arenaId + ":")) {
                arenaLineIndex = i;
                indentLevel = getIndentLevel(line);
                break;
            }
        }
        
        if (arenaLineIndex == -1) {
            return false;
        }
        
        // Find or create spawn section
        String spawnKey = spawnType + "-spawn:";
        int spawnLineIndex = findSpawnSection(lines, arenaLineIndex, spawnKey, indentLevel);
        
        if (spawnLineIndex == -1) {
            // Add new spawn section
            spawnLineIndex = addSpawnSection(lines, arenaLineIndex, spawnKey, location, indentLevel);
        } else {
            // Update existing spawn section
            updateSpawnSection(lines, spawnLineIndex, location, indentLevel);
        }
        
        return true;
    }

    /**
     * Finds spawn section in arena
     */
    private int findSpawnSection(List<String> lines, int startIndex, String spawnKey, int arenaIndent) {
        for (int i = startIndex + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            int indent = getIndentLevel(line);
            
            // If we're back to arena level or less, spawn doesn't exist
            if (indent <= arenaIndent && !line.trim().isEmpty()) {
                return -1;
            }
            
            if (line.trim().equals(spawnKey)) {
                return i;
            }
        }
        
        return -1;
    }

    /**
     * Adds new spawn section
     */
    private int addSpawnSection(List<String> lines, int arenaLineIndex, String spawnKey, Location location, int arenaIndent) {
        int indent = arenaIndent + 2;
        String indentStr = getIndentString(indent);
        
        // Find where to insert (after arena line, before next arena or end)
        int insertIndex = arenaLineIndex + 1;
        
        // Skip existing content
        for (int i = arenaLineIndex + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineIndent = getIndentLevel(line);
            
            if (lineIndent <= arenaIndent && !line.trim().isEmpty()) {
                break;
            }
            insertIndex = i + 1;
        }
        
        // Insert spawn section
        lines.add(insertIndex, indentStr + spawnKey);
        lines.add(insertIndex + 1, indentStr + "  world: " + location.getWorld().getName());
        lines.add(insertIndex + 2, indentStr + "  x: " + location.getBlockX());
        lines.add(insertIndex + 3, indentStr + "  y: " + location.getBlockY());
        lines.add(insertIndex + 4, indentStr + "  z: " + location.getBlockZ());
        lines.add(insertIndex + 5, indentStr + "  yaw: " + String.format("%.1f", location.getYaw()));
        lines.add(insertIndex + 6, indentStr + "  pitch: " + String.format("%.1f", location.getPitch()));
        
        return insertIndex;
    }

    /**
     * Updates existing spawn section
     */
    private void updateSpawnSection(List<String> lines, int spawnLineIndex, Location location, int arenaIndent) {
        int indent = arenaIndent + 4; // spawn properties are 2 levels deeper
        String indentStr = getIndentString(indent);
        
        // Remove old spawn data
        int i = spawnLineIndex + 1;
        while (i < lines.size()) {
            String line = lines.get(i);
            int lineIndent = getIndentLevel(line);
            
            if (lineIndent <= arenaIndent + 2 && !line.trim().isEmpty()) {
                break;
            }
            
            lines.remove(i);
        }
        
        // Add new spawn data
        lines.add(spawnLineIndex + 1, indentStr + "world: " + location.getWorld().getName());
        lines.add(spawnLineIndex + 2, indentStr + "x: " + location.getBlockX());
        lines.add(spawnLineIndex + 3, indentStr + "y: " + location.getBlockY());
        lines.add(spawnLineIndex + 4, indentStr + "z: " + location.getBlockZ());
        lines.add(spawnLineIndex + 5, indentStr + "yaw: " + String.format("%.1f", location.getYaw()));
        lines.add(spawnLineIndex + 6, indentStr + "pitch: " + String.format("%.1f", location.getPitch()));
    }

    /**
     * Gets indent level of a line
     */
    private int getIndentLevel(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    /**
     * Generates indent string
     */
    private String getIndentString(int spaces) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < spaces; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
