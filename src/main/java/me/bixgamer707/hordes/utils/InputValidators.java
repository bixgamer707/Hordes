package me.bixgamer707.hordes.utils;

import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Common input validators for chat input system
 * Provides reusable validation predicates
 */
public class InputValidators {

    // Arena ID validator (lowercase letters, numbers, underscores)
    private static final Pattern ARENA_ID_PATTERN = Pattern.compile("^[a-z0-9_]+$");
    
    // Display name validator (allow most characters, minimum length)
    private static final Pattern DISPLAY_NAME_PATTERN = Pattern.compile("^.{3,32}$");
    
    // Number validator
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^\\d+$");
    
    // Decimal number validator
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("^\\d+(\\.\\d+)?$");

    /**
     * Validates arena IDs (lowercase letters, numbers, underscores only)
     */
    public static Predicate<String> arenaId() {
        return input -> ARENA_ID_PATTERN.matcher(input).matches();
    }

    /**
     * Validates display names (3-32 characters)
     */
    public static Predicate<String> displayName() {
        return input -> DISPLAY_NAME_PATTERN.matcher(input).matches();
    }

    /**
     * Validates region names (any non-empty string)
     */
    public static Predicate<String> regionName() {
        return input -> !input.isEmpty() && input.length() <= 64;
    }

    /**
     * Validates positive integers
     */
    public static Predicate<String> positiveInteger() {
        return input -> NUMBER_PATTERN.matcher(input).matches() 
            && Integer.parseInt(input) > 0;
    }

    /**
     * Validates integers within a range
     */
    public static Predicate<String> integerRange(int min, int max) {
        return input -> {
            if (!NUMBER_PATTERN.matcher(input).matches()) {
                return false;
            }
            
            int value = Integer.parseInt(input);
            return value >= min && value <= max;
        };
    }

    /**
     * Validates positive decimals
     */
    public static Predicate<String> positiveDecimal() {
        return input -> DECIMAL_PATTERN.matcher(input).matches() 
            && Double.parseDouble(input) > 0;
    }

    /**
     * Validates decimals within a range
     */
    public static Predicate<String> decimalRange(double min, double max) {
        return input -> {
            if (!DECIMAL_PATTERN.matcher(input).matches()) {
                return false;
            }
            
            double value = Double.parseDouble(input);
            return value >= min && value <= max;
        };
    }

    /**
     * Validates non-empty strings
     */
    public static Predicate<String> notEmpty() {
        return input -> !input.isEmpty();
    }

    /**
     * Validates string length
     */
    public static Predicate<String> length(int min, int max) {
        return input -> input.length() >= min && input.length() <= max;
    }

    /**
     * Combines multiple validators (all must pass)
     */
    public static Predicate<String> allOf(Predicate<String>... validators) {
        return input -> {
            for (Predicate<String> validator : validators) {
                if (!validator.test(input)) {
                    return false;
                }
            }
            return true;
        };
    }

    /**
     * Combines multiple validators (any can pass)
     */
    public static Predicate<String> anyOf(Predicate<String>... validators) {
        return input -> {
            for (Predicate<String> validator : validators) {
                if (validator.test(input)) {
                    return true;
                }
            }
            return false;
        };
    }
}