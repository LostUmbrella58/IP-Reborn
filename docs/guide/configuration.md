# Configuration

Stop the server and back up the affected file before a large edit. `/ip reload` reloads the main configuration set; `/ipp reload` reloads multiplayer configuration. Restarting the server is the safest way to apply changes across every subsystem.

## File map

| File | Purpose |
| --- | --- |
| `plugins/IP/config.yml` | Core behavior, world, storage, permissions, styles, particles, and defaults |
| `plugins/IP/generation.yml` | Jump distances, chances, and block generation |
| `plugins/IP/rewards-v2.yml` | Block-parkour and multiplayer rewards |
| `plugins/IP/schematics/schematics.yml` | Schematic difficulty registration |
| `plugins/IP/locales/<language>.yml` | Core menus and messages |
| `plugins/IP/plus/config.yml` | Multiplayer modes and incremental styles |
| `plugins/IP/plus/locales/<language>.yml` | Multiplayer menus and messages |
| `plugins/IP/elytra/config.yml` | Elytra modes, settings, storage, and styles |
| `plugins/IP/elytra/rewards.yml` | Elytra rewards |
| `plugins/IP/elytra/locales/<language>.yml` | Elytra menus and messages |

## Core configuration

Important sections in `plugins/IP/config.yml`:

- `update-checker`: check Spigot for a newer release on startup and log a notice only when one exists. Set it to `false` to skip the check.
- `joining`: allow players to enter parkour. Disable it on a leaderboard-only server.
- `bungeecord`: automatic join, return server, and non-proxy return location.
- `sql`: enable MySQL and configure host, port, credentials, database, and table prefix.
- `world`: parkour world name, removal on reload, and fallback world.
- `options`: block lead choices, time formatting, and inventory handling.
- `permissions`: enable permission checks and optional per-style permissions.
- `focus-mode`: restrict commands during parkour, with a whitelist.
- `styles.list`: named random block palettes.
- `scoring`: point and interval-reward behavior.
- `particles`: generation particle shape/type plus sound type, pitch, and volume.
- `default-values`: menu visibility and default player settings.

::: warning Inventory safety
Keep `options.inventory-saving` enabled when `options.inventory-handling` is enabled. If a crash interrupts restoration, use `/ip recoverinventory <player>` while that player is online.
:::

## Multiplayer configuration

`plugins/IP/plus/config.yml` contains:

- `send_back_after_multiplayer`: restore the previous location/data after a match, or move players into regular single-player parkour.
- `gamemodes.<name>.enabled`: show or hide each mode.
- Mode-specific values such as `time`, `goal`, `max`, and `island_distance`.
- `styles.incremental`: ordered material palettes used one block after another.

Mode keys are `hourglass`, `lobby`, `practice`, `speed`, `super_jump`, `time_trial`, `wave_trial`, `duels`, and `team_survival`.

## Elytra configuration

`plugins/IP/elytra/config.yml` contains:

- `join-on-join`, `permissions`, and `time-format`.
- `proxy` return-server settings.
- `mysql` connection settings.
- `mode-settings` for Close, Min Speed, Obstacle, Speed Demon, and Time Trial.
- `settings` entries for style, radius, time, seed, locale, fall, information, and metric units.
- `styles.random` and `styles.incremental` material palettes.

The configured radius must be 3–6, world time 0–24000, and fixed seeds 0–1,000,000. The default seed `-1` selects a random seed.

## Custom locales

Copy an existing locale file, rename it to the language key you want to use, and translate values without changing their YAML paths. Preserve MiniMessage tags and replacement tokens. Custom elytra style names belong under `styles.names.<key>` in the corresponding elytra locale.
