# Changelog

## 6.0.2

Player data and inventories:

- Restore players' inventory, game mode and location synchronously when the server
  stops or reloads. Previously they could be saved with the parkour hotbar.
- `/ip recoverinventory` now actually gives the saved items back, and deletes the
  backup afterwards.
- Elytra parkour writes each player's pre-join state to disk and restores it on the
  next login after a crash.
- Joining block parkour from elytra parkour (or the reverse) restores the first
  mode's state before the second one saves it.

Leaderboards and storage:

- Leaderboards are safe to read and save from different threads, files are written
  atomically, and a corrupt file is backed up instead of being overwritten with an
  empty leaderboard.
- MySQL: `/ip reset` deletes rows, statements are closed, values are parameterized,
  and a lost connection is retried instead of disabling the plugin.
- Runs in Speed, Hourglass, Super Jump and Team Survival are recorded when the
  player leaves, and multiplayer leaderboards are saved after players leave on
  shutdown. Time Trial and Wave Trial keep the best time instead of the latest.
- Recorded times no longer wrap after one hour.
- Legacy IPPlus/IEP data is imported once instead of on every start.

Gameplay:

- Rotated schematic jumps keep ladders, doors, fences and other directional blocks
  facing the right way, and no longer corrupt the cached schematic.
- The `generation.type.special` and `generation.type.schematic` chances were swapped.
- One-time rewards are tracked per mode, so a reward for one mode no longer uses up
  another mode's reward. Elytra leave rewards pay once instead of on every mode switch.
- Duels: the remaining players are handled correctly when the owner leaves, and
  players no longer share an island after someone leaves the lobby.
- Time Trial, Wave Trial and Hourglass progress bars work with any goal or time.
- Elytra: the scoreboard survives mode switches, obstacles are sent once the
  player's client has the chunk, and a failed join no longer leaves the player stuck.

Menus, commands and permissions:

- Menus cancel every click and drag that could move items into or out of them.
- `/ip menu` and `/iep leaderboards` permission checks are no longer inverted.
- `/ip join <player>` respects private sessions.
- `/parkour create`, `/parkour lobbies` and `/parkour invite` work again.
- The elytra parkour button is shown in the play menu.
- `/ip` commands work from RCON, command blocks and `/execute`.
- Off-hand boat placement is only blocked for parkour players.

Other:

- New `update-checker` option in `config.yml`. The checker compares version numbers
  and only logs when Spigot has a newer release (#4).
- Bundled Chinese and Japanese locales are installed for the multiplayer modes, and
  a missing locale falls back to the default language instead of failing.
- Particle types that need extra data no longer throw on every jump.
- Fixes for leaked menu listeners and Team Survival tasks, duplicate styles after
  `/ip reload`, leaderboard holograms not respawning, and Super Jump's air supply
  not being restored.

## 6.0.1 (2026-10-02)

- Add compatibility with Paper 26.3 on Java 25.
- Retain support for Paper 1.21.11, Paper 26.1.2, and Paper 26.2.
- Keep Java 21 bytecode so one jar continues to work across all supported servers.

## 6.0.0 (2026-08-01)

- Merge Infinite Parkour, IPPlus, and Infinite Elytra Parkour into one plugin jar.
- Support Paper 1.21.11, Paper 26.1.2, and Paper 26.2.
- Emit Java 21 bytecode so the same jar can run on Paper 1.21.11 and newer targets.
- Remove the external `vilib`, PaperLib, and VoidGen requirements. The required
  foundation code and native void generator now live inside the plugin.
- Preserve `/ipp` and `/iep` as compatibility commands.
- Migrate missing legacy data into `plugins/IP/plus` and `plugins/IP/elytra`
  without deleting the original folders.
- Give every Bukkit custom event its own `HandlerList`.
- Unregister integrations and PlaceholderAPI expansions during shutdown/reload.

## 5.4.0-paper26

- Initial community port to the Paper 26.1.2 API.
- Replace removed Paper and Adventure API calls.
- Update build tooling for Java 25 class files.

This section is historical. Version 6 supersedes the standalone IPPlus and IEP
builds and no longer requires an independently installed or built `vilib` artifact.
