package me.bixgamer707.hordes.wave;

/**
 * Estados del ciclo de vida de una wave
 */
public enum WaveState {

    /**
     * Wave pendiente, no iniciada
     */
    PENDING,

    /**
     * Spawneando mobs gradualmente
     */
    SPAWNING,

    /**
     * Todos los mobs spawneados, esperando que mueran
     */
    ACTIVE,

    /**
     * Wave completada exitosamente
     */
    COMPLETED,

    /**
     * Wave cancelada (arena terminó prematuramente)
     */
    CANCELLED
}