<div align="center">

# ⚔️ Hordes

**Advanced dungeon and arena system for Paper/Spigot servers**
**Sistema avanzado de mazmorras y arenas para servidores Paper/Spigot**

[![Version](https://img.shields.io/badge/version-2.0.0-blueviolet)](#)
[![Minecraft](https://img.shields.io/badge/minecraft-1.20.1-brightgreen)](#)
[![Java](https://img.shields.io/badge/java-17%2B-orange)](#)
[![API](https://img.shields.io/badge/API-Paper-blue)](#)
[![License](https://img.shields.io/badge/license-All%20Rights%20Reserved-lightgrey)](#)

**[English](#-english)** · **[Español](#-español)**

</div>

---

## 🇬🇧 English

### Table of Contents
- [About](#about)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Commands](#commands)
- [Permissions](#permissions)
- [Configuration](#configuration)
- [PlaceholderAPI](#placeholderapi)
- [Integrations](#integrations)
- [Building from source](#building-from-source)
- [Roadmap / Known limitations](#roadmap--known-limitations)
- [Support](#support)

### About

**Hordes** is a fully configurable dungeon/arena plugin for Minecraft servers running Paper (or Spigot-based forks). Create wave-based arenas with custom mobs, flexible death handling, progressive rewards, cooldowns, statistics and leaderboards — all manageable through an in-game GUI, no server restarts required.

### Features

- 🌊 **Wave-based arenas** — fully configurable number of waves, spawn delay, mobs per spawn cycle and spawn locations per wave.
- 🎮 **Two play modes** — *Survival Mode* (keep inventory/gamemode, dungeon-style) or *Arena Mode* (clean inventory, kit-based, MobArena-style).
- 💀 **4 death handling modes** — `KICK`, `SPECTATE`, `REJOIN` (with cooldown), `RESPAWN`.
- 🎒 **4 item drop modes** — `ALL_PLAYERS`, `ARENA_PLAYERS`, `OWNER_ONLY`, `TELEPORT_WITH_PLAYER`.
- ⏭️ **Wave progression control** — `AUTOMATIC` (auto-advance) or `MANUAL` (button/sign trigger).
- 🎁 **Reward system** — money (Vault), items, and console commands, with `COMPLETION_ONLY`, `PROGRESSIVE`, or `BOTH` reward types.
- 👹 **Mob system** — vanilla mobs with health/damage multipliers and custom colored names, plus optional **MythicMobs** integration.
- ⏱️ **Cooldown system** — per-arena, global (all arenas), and temporary cooldowns for rejoin mechanics.
- 📊 **Statistics tracking** — kills, deaths, completions, attempts, playtime, win rate, K/D ratio, fastest completion, highest wave reached.
- 🏆 **Leaderboards** — in-game GUI ranking players by any tracked statistic.
- 🩸 **Boss bar** — live wave progress and mobs-remaining indicator.
- 🔊 **Configurable sound effects** for wave start/complete, victory, defeat, and player death.
- 🧩 **PlaceholderAPI expansion** — dozens of placeholders for arenas, waves, players and statistics.
- 🌍 **WorldGuard integration** — optional auto-join when a player enters an arena's region.
- 🖱️ **Full in-game admin GUI** — create, edit and configure arenas, waves, rewards, death/item handling and spawn points without touching a single config file.
- 💬 **Chat input system** — type values directly in chat when configuring through the GUI (with timeout and cancel keyword support).
- 🌐 **Multi-language messages** — English and Spanish included out of the box, fully external and editable.
- 📝 **Heavily commented YAML configs** — every option in `config.yml`, `arenas.yml` and `mobs.yml` is documented inline.

### Requirements

| Requirement | Version |
|---|---|
| Server software | Paper (or fork) |
| Minecraft version | 1.20.1 |
| Java | 17+ |

**Optional soft-dependencies** (the plugin works without them, with reduced functionality):

| Plugin | Enables |
|---|---|
| [Vault](https://www.spigotmc.org/resources/vault.34315/) | Money rewards |
| [WorldGuard](https://enginehub.org/worldguard) | Auto-join by region |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | `%hordes_*%` placeholders |
| [MythicMobs](https://www.mythicmobs.net/) | Custom mob spawning in waves |

### Installation

1. Download `Hordes-v1.0-shaded.jar` (includes the shaded VaultAPI dependency).
2. Drop it into your server's `/plugins` folder.
3. (Optional) Install Vault, WorldGuard, PlaceholderAPI and/or MythicMobs beforehand if you want their integrations.
4. Start/restart the server. Hordes will generate its default files under `/plugins/Hordes/`.
5. Configure your arenas in `arenas.yml` and their waves/mobs in `mobs.yml` — or use `/hordesadmin create <arena>` to build one entirely through the in-game GUI.
6. Set the lobby, arena and exit locations with `/hordesadmin setspawn <arena> <type>` (or the GUI).

### Commands

| Command | Aliases | Description |
|---|---|---|
| `/hordes join <arena>` | `/hd join`, `/horde join` | Join an arena |
| `/hordes leave` | | Leave the current arena |
| `/hordes list` | | List all available arenas |
| `/hordes info <arena>` | | View arena information |
| `/hordes stats` | | View your statistics |

| Admin command | Aliases | Description |
|---|---|---|
| `/hordesadmin reload` | `/hda reload` | Reload all configurations |
| `/hordesadmin create <arena>` | | Create a new arena |
| `/hordesadmin delete <arena>` | | Delete an arena |
| `/hordesadmin setspawn <arena> <type>` | | Set a spawn point (lobby / arena / exit) |
| `/hordesadmin forcestart <arena>` | | Force-start an arena |
| `/hordesadmin forcestop <arena>` | | Force-stop an arena |
| `/hordesadmin tp <arena>` | | Teleport to an arena |
| `/hordesadmin debug` | | Show debug information |

### Permissions

| Permission | Default | Description |
|---|---|---|
| `hordes.use` | `true` | Base permission for player commands |
| `hordes.join` | `true` | Allows joining arenas |
| `hordes.join.*` | `true` | Allows joining **all** arenas |
| `hordes.join.<arena>` | — | Allows joining a **specific** arena (grant this and revoke `hordes.join.*` to restrict access) |
| `hordes.leave` / `hordes.list` / `hordes.info` / `hordes.stats` | `true` | Individual player sub-permissions |
| `hordes.vip` | `false` | VIP group: bundles `hordes.use` + cooldown bypass |
| `hordes.cooldown.bypass` | `false` | Bypasses arena cooldowns |
| `hordes.admin` | `op` | Full admin access (bundles all `hordes.admin.*` nodes) |
| `hordes.admin.reload` / `.create` / `.delete` / `.setspawn` / `.forcestart` / `.forcestop` / `.tp` / `.debug` | `op` | Individual admin sub-permissions |

### Configuration

Hordes ships with 5 main configuration files, all generated under `/plugins/Hordes/`:

```
plugins/Hordes/
├── config.yml        # General settings, feature toggles, defaults
├── arenas.yml         # Arena definitions (locations, waves, rewards, death/item handling...)
├── mobs.yml            # Mob composition per wave, per arena
├── guis.yml              # In-game GUI layout and text customization
└── messages/
    ├── en_us.yml            # English messages
    └── es_es.yml            # Spanish messages
```

Minimal arena example (`arenas.yml`):

```yaml
arenas:
  beginner_arena:
    enabled: true
    display-name: "&a&lBeginner Arena"
    min-players: 1
    max-players: 4
    lobby-spawn: { world: world, x: 0, y: 64, z: 0 }
    arena-spawn: { world: world, x: 10, y: 64, z: 10 }
    exit-location: { world: world, x: 0, y: 64, z: 0 }
    waves: 3
    wave-delay: 10
    wave-progression: AUTOMATIC
    countdown-time: 10
    auto-start: true
    cooldown: 300
    survival-mode:
      enabled: false
      clear-inventory: true
      force-gamemode: true
    death-handling:
      action: KICK
    rewards:
      enabled: true
      type: COMPLETION_ONLY
      money: 100
      items: ["DIAMOND 3", "GOLDEN_APPLE 5"]
```

And its matching waves (`mobs.yml`):

```yaml
beginner_arena:
  wave-1:
    spawn-delay: 20
    mobs-per-spawn: 2
    mobs:
      - type: VANILLA
        id: ZOMBIE
        amount: 5
        health-multiplier: 1.0
      - type: VANILLA
        id: SKELETON
        amount: 3
```

See the comments inside each generated file for the full list of options.

### PlaceholderAPI

Once PlaceholderAPI is installed, Hordes registers the `%hordes_*%` expansion. Some of the available placeholders:

| Placeholder | Description |
|---|---|
| `%hordes_in_arena%` | Whether the player is currently in an arena |
| `%hordes_arena%` / `%hordes_arena_name%` | Current arena ID / display name |
| `%hordes_arena_state%` | Current arena state |
| `%hordes_wave%` / `%hordes_total_waves%` / `%hordes_wave_progress%` | Wave progress |
| `%hordes_mobs_alive%` / `%hordes_mobs_total%` | Mobs remaining in the current wave |
| `%hordes_players%` / `%hordes_players_alive%` | Players in the arena |
| `%hordes_total_kills%` / `%hordes_total_deaths%` / `%hordes_total_completions%` | Lifetime statistics |
| `%hordes_win_rate%` / `%hordes_kd_ratio%` | Computed statistics |
| `%hordes_highest_wave%` / `%hordes_fastest_time%` / `%hordes_playtime%` | Records |
| `%hordes_arena_<id>_state%` / `_players%` / `_wave%` / `_totalwaves%` | Info for a **specific** arena, by ID |

### Integrations

- **Vault** — money rewards via any Vault-compatible economy plugin.
- **WorldGuard** — optional auto-join when a player walks into a configured region.
- **MythicMobs** — spawn custom MythicMobs creations inside waves alongside (or instead of) vanilla mobs.
- **PlaceholderAPI** — see above.

All four are soft-dependencies: the plugin loads and works fine without them, simply disabling the related feature.

### Building from source

```bash
git clone https://github.com/bixgamer707/Hordes.git
cd Hordes
mvn clean package
```

The shaded jar will be produced at `target/Hordes-v1.0-shaded.jar`. Building requires network access to the PaperMC, ExtendedClip, EngineHub and Lumine Maven repositories declared in `pom.xml`.

### Roadmap / Known limitations

- `wave-progression: MIXED` and the per-wave `progression: MANUAL` override documented in `mobs.yml` are parsed but not yet applied — currently `MIXED` behaves like `AUTOMATIC`.
- `statistics.storage-type` only supports `YAML` at the moment; `SQLITE` and `MYSQL` are reserved for a future release.
- Leaderboard GUI does not yet show the viewing player's own rank position.

### Support

Report bugs or request features via [GitHub Issues](https://github.com/bixgamer707/hordes/issues).

---

## 🇪🇸 Español

### Tabla de contenidos
- [Acerca de](#acerca-de)
- [Características](#características)
- [Requisitos](#requisitos)
- [Instalación](#instalación)
- [Comandos](#comandos)
- [Permisos](#permisos)
- [Configuración](#configuración)
- [PlaceholderAPI](#placeholderapi-1)
- [Integraciones](#integraciones)
- [Compilar desde el código fuente](#compilar-desde-el-código-fuente)
- [Roadmap / Limitaciones conocidas](#roadmap--limitaciones-conocidas)
- [Soporte](#soporte)

### Acerca de

**Hordes** es un plugin de mazmorras/arenas totalmente configurable para servidores de Minecraft con Paper (o forks basados en Spigot). Permite crear arenas por oleadas con mobs personalizados, manejo de muerte flexible, recompensas progresivas, cooldowns, estadísticas y tablas de clasificación — todo administrable desde un GUI en el propio juego, sin reiniciar el servidor.

### Características

- 🌊 **Arenas por oleadas** — número de waves totalmente configurable, con retraso de spawn, mobs por ciclo y ubicaciones de spawn específicas por wave.
- 🎮 **Dos modos de juego** — *Modo Survival* (conserva inventario/modo de juego, estilo mazmorra) o *Modo Arena* (inventario limpio, basado en kits, estilo MobArena).
- 💀 **4 modos de manejo de muerte** — `KICK`, `SPECTATE`, `REJOIN` (con cooldown), `RESPAWN`.
- 🎒 **4 modos de caída de items** — `ALL_PLAYERS`, `ARENA_PLAYERS`, `OWNER_ONLY`, `TELEPORT_WITH_PLAYER`.
- ⏭️ **Control de progresión de oleadas** — `AUTOMATIC` (avanza solo) o `MANUAL` (requiere botón/cartel).
- 🎁 **Sistema de recompensas** — dinero (Vault), items y comandos de consola, con tipos `COMPLETION_ONLY`, `PROGRESSIVE` o `BOTH`.
- 👹 **Sistema de mobs** — mobs vanilla con multiplicadores de vida/daño y nombres personalizados con color, más integración opcional con **MythicMobs**.
- ⏱️ **Sistema de cooldowns** — por arena, global (todas las arenas) y temporales para mecánicas de reingreso.
- 📊 **Seguimiento de estadísticas** — kills, muertes, completados, intentos, tiempo jugado, % de victorias, ratio K/D, tiempo más rápido, wave más alta alcanzada.
- 🏆 **Tablas de clasificación** — GUI en el juego que rankea a los jugadores por cualquier estadística registrada.
- 🩸 **Boss bar** — indicador en vivo del progreso de la wave y mobs restantes.
- 🔊 **Efectos de sonido configurables** para inicio/fin de wave, victoria, derrota y muerte de jugador.
- 🧩 **Expansión de PlaceholderAPI** — decenas de placeholders para arenas, waves, jugadores y estadísticas.
- 🌍 **Integración con WorldGuard** — auto-unión opcional cuando un jugador entra en la región de una arena.
- 🖱️ **GUI de administración completo en el juego** — crea, edita y configura arenas, waves, recompensas, manejo de muerte/items y puntos de spawn sin tocar un solo archivo de configuración.
- 💬 **Sistema de entrada por chat** — escribe valores directamente en el chat al configurar desde el GUI (con timeout y palabra clave para cancelar).
- 🌐 **Mensajes multilenguaje** — inglés y español incluidos de fábrica, totalmente externos y editables.
- 📝 **Archivos YAML muy bien comentados** — cada opción de `config.yml`, `arenas.yml` y `mobs.yml` está documentada directamente en el archivo.

### Requisitos

| Requisito | Versión |
|---|---|
| Software del servidor | Paper (o fork) |
| Versión de Minecraft | 1.20.1 |
| Java | 17+ |

**Dependencias opcionales** (el plugin funciona sin ellas, con funcionalidad reducida):

| Plugin | Habilita |
|---|---|
| [Vault](https://www.spigotmc.org/resources/vault.34315/) | Recompensas de dinero |
| [WorldGuard](https://enginehub.org/worldguard) | Auto-unión por región |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | Placeholders `%hordes_*%` |
| [MythicMobs](https://www.mythicmobs.net/) | Spawn de mobs personalizados en las waves |

### Instalación

1. Descarga `Hordes-v1.0-shaded.jar` (incluye la dependencia de VaultAPI embebida).
2. Colócalo en la carpeta `/plugins` de tu servidor.
3. (Opcional) Instala Vault, WorldGuard, PlaceholderAPI y/o MythicMobs previamente si quieres usar sus integraciones.
4. Inicia/reinicia el servidor. Hordes generará sus archivos por defecto en `/plugins/Hordes/`.
5. Configura tus arenas en `arenas.yml` y sus waves/mobs en `mobs.yml` — o usa `/hordesadmin create <arena>` para crear una completamente desde el GUI en el juego.
6. Define las ubicaciones de lobby, arena y salida con `/hordesadmin setspawn <arena> <tipo>` (o desde el GUI).

### Comandos

| Comando | Alias | Descripción |
|---|---|---|
| `/hordes join <arena>` | `/hd join`, `/horde join` | Unirse a una arena |
| `/hordes leave` | | Salir de la arena actual |
| `/hordes list` | | Listar todas las arenas disponibles |
| `/hordes info <arena>` | | Ver información de una arena |
| `/hordes stats` | | Ver tus estadísticas |

| Comando de admin | Alias | Descripción |
|---|---|---|
| `/hordesadmin reload` | `/hda reload` | Recargar todas las configuraciones |
| `/hordesadmin create <arena>` | | Crear una nueva arena |
| `/hordesadmin delete <arena>` | | Eliminar una arena |
| `/hordesadmin setspawn <arena> <tipo>` | | Establecer un punto de spawn (lobby / arena / salida) |
| `/hordesadmin forcestart <arena>` | | Forzar inicio de una arena |
| `/hordesadmin forcestop <arena>` | | Forzar fin de una arena |
| `/hordesadmin tp <arena>` | | Teletransportarse a una arena |
| `/hordesadmin debug` | | Mostrar información de depuración |

### Permisos

| Permiso | Por defecto | Descripción |
|---|---|---|
| `hordes.use` | `true` | Permiso base para comandos de jugador |
| `hordes.join` | `true` | Permite unirse a arenas |
| `hordes.join.*` | `true` | Permite unirse a **todas** las arenas |
| `hordes.join.<arena>` | — | Permite unirse a una arena **específica** (concede este y revoca `hordes.join.*` para restringir el acceso) |
| `hordes.leave` / `hordes.list` / `hordes.info` / `hordes.stats` | `true` | Sub-permisos individuales de jugador |
| `hordes.vip` | `false` | Grupo VIP: agrupa `hordes.use` + bypass de cooldown |
| `hordes.cooldown.bypass` | `false` | Ignora los cooldowns de arena |
| `hordes.admin` | `op` | Acceso total de administrador (agrupa todos los nodos `hordes.admin.*`) |
| `hordes.admin.reload` / `.create` / `.delete` / `.setspawn` / `.forcestart` / `.forcestop` / `.tp` / `.debug` | `op` | Sub-permisos individuales de administrador |

### Configuración

Hordes trae 5 archivos de configuración principales, generados en `/plugins/Hordes/`:

```
plugins/Hordes/
├── config.yml        # Ajustes generales, funciones activables, valores por defecto
├── arenas.yml         # Definición de arenas (ubicaciones, waves, recompensas, manejo de muerte/items...)
├── mobs.yml            # Composición de mobs por wave, por arena
├── guis.yml              # Diseño y textos personalizables del GUI en el juego
└── messages/
    ├── en_us.yml            # Mensajes en inglés
    └── es_es.yml            # Mensajes en español
```

Ejemplo mínimo de arena (`arenas.yml`):

```yaml
arenas:
  beginner_arena:
    enabled: true
    display-name: "&a&lArena Principiante"
    min-players: 1
    max-players: 4
    lobby-spawn: { world: world, x: 0, y: 64, z: 0 }
    arena-spawn: { world: world, x: 10, y: 64, z: 10 }
    exit-location: { world: world, x: 0, y: 64, z: 0 }
    waves: 3
    wave-delay: 10
    wave-progression: AUTOMATIC
    countdown-time: 10
    auto-start: true
    cooldown: 300
    survival-mode:
      enabled: false
      clear-inventory: true
      force-gamemode: true
    death-handling:
      action: KICK
    rewards:
      enabled: true
      type: COMPLETION_ONLY
      money: 100
      items: ["DIAMOND 3", "GOLDEN_APPLE 5"]
```

Y sus waves correspondientes (`mobs.yml`):

```yaml
beginner_arena:
  wave-1:
    spawn-delay: 20
    mobs-per-spawn: 2
    mobs:
      - type: VANILLA
        id: ZOMBIE
        amount: 5
        health-multiplier: 1.0
      - type: VANILLA
        id: SKELETON
        amount: 3
```

Consulta los comentarios dentro de cada archivo generado para ver la lista completa de opciones.

### PlaceholderAPI

Una vez instalado PlaceholderAPI, Hordes registra la expansión `%hordes_*%`. Algunos de los placeholders disponibles:

| Placeholder | Descripción |
|---|---|
| `%hordes_in_arena%` | Si el jugador está actualmente en una arena |
| `%hordes_arena%` / `%hordes_arena_name%` | ID / nombre visible de la arena actual |
| `%hordes_arena_state%` | Estado actual de la arena |
| `%hordes_wave%` / `%hordes_total_waves%` / `%hordes_wave_progress%` | Progreso de waves |
| `%hordes_mobs_alive%` / `%hordes_mobs_total%` | Mobs restantes en la wave actual |
| `%hordes_players%` / `%hordes_players_alive%` | Jugadores en la arena |
| `%hordes_total_kills%` / `%hordes_total_deaths%` / `%hordes_total_completions%` | Estadísticas históricas |
| `%hordes_win_rate%` / `%hordes_kd_ratio%` | Estadísticas calculadas |
| `%hordes_highest_wave%` / `%hordes_fastest_time%` / `%hordes_playtime%` | Récords |
| `%hordes_arena_<id>_state%` / `_players%` / `_wave%` / `_totalwaves%` | Información de una arena **específica**, por ID |

### Integraciones

- **Vault** — recompensas de dinero mediante cualquier plugin de economía compatible con Vault.
- **WorldGuard** — auto-unión opcional cuando un jugador entra en una región configurada.
- **MythicMobs** — genera criaturas personalizadas de MythicMobs dentro de las waves, junto a (o en vez de) mobs vanilla.
- **PlaceholderAPI** — ver arriba.

Las cuatro son dependencias opcionales: el plugin carga y funciona correctamente sin ellas, simplemente desactivando la función relacionada.

### Compilar desde el código fuente

```bash
git clone https://github.com/bixgamer707/Hordes.git
cd Hordes
mvn clean package
```

El jar final se genera en `target/Hordes-v1.0-shaded.jar`. Compilarlo requiere acceso de red a los repositorios Maven de PaperMC, ExtendedClip, EngineHub y Lumine declarados en `pom.xml`.

### Roadmap / Limitaciones conocidas

- `wave-progression: MIXED` y el override por-wave `progression: MANUAL` documentado en `mobs.yml` se leen pero todavía no se aplican — actualmente `MIXED` se comporta igual que `AUTOMATIC`.
- `statistics.storage-type` solo soporta `YAML` por ahora; `SQLITE` y `MYSQL` están reservados para una futura versión.
- El GUI de tabla de clasificación todavía no muestra la posición propia del jugador que lo consulta.

### Soporte

Reporta errores o solicita funciones a través de [GitHub Issues](https://github.com/bixgamer707/hordes/issues).

---

<div align="center">

Made with ❤️ by **bixgamer707**

</div>
