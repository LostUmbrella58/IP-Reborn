# Commands

Arguments in `<angle brackets>` are required. Arguments in `[square brackets]` are optional.

## Block parkour

`/parkour`, `/ip`, and `/witp` call the same command.

| Command | Description | Permission |
| --- | --- | --- |
| `/parkour` | Open the main menu; show help when used outside the game | `ip.main` |
| `/parkour help` | Show available commands | — |
| `/parkour join [mode/player]` | Join the default mode, a named mode, or another player's session | `ip.join` |
| `/parkour leave` | Leave the current session | `ip.quit` |
| `/parkour menu` | Open the main menu | `ip.main` |
| `/parkour play` | Open mode selection | `ip.play` |
| `/parkour leaderboard [mode]` | Open leaderboards | `ip.community.leaderboards` |
| `/ip reload` | Reload configuration and locale files | `ip.admin` |
| `/ip reset <everyone/player/uuid>` | Permanently reset block-parkour scores | `ip.admin` |
| `/ip forcejoin <everyone/nearest/player>` | Force players into the default mode | `ip.admin` |
| `/ip forceleave <everyone/player>` | Force players out of parkour | `ip.admin` |
| `/ip recoverinventory <player>` | Restore the saved inventory of an online player who is not in parkour | `ip.admin` |

### Block-parkour schematics

These commands require `ip.admin` and must be run by a player.

| Command | Description |
| --- | --- |
| `/ip schematic wand` | Receive the selection wand |
| `/ip schematic pos1` | Set position 1 at your location |
| `/ip schematic pos2` | Set position 2 at your location |
| `/ip schematic save` | Save the selected area with a generated code |
| `/ip schematic paste <file>` | Paste a loaded schematic at your location |

## Multiplayer

| Command | Description | Permission |
| --- | --- | --- |
| `/ipp create` | Open multiplayer mode selection | `ip.multiplayer` |
| `/ipp lobbies` | View active multiplayer lobbies | `ip.active` |
| `/ipp invite` | Open the invitation menu | `ip.invite` |
| `/ipp reload` | Reload multiplayer configuration | `ip.admin` |
| `/ipp lobbygm pos1` | Set lobby-selection position 1 | `ip.admin` |
| `/ipp lobbygm pos2` | Set lobby-selection position 2 | `ip.admin` |
| `/ipp lobbygm save` | Save the selected lobby area | `ip.admin` |

`/ipp multiplayer` aliases `/ipp create`; `/ipp lobby` aliases `/ipp lobbies`.

## Elytra parkour

| Command | Description | Permission |
| --- | --- | --- |
| `/iep play` | Open the play menu | `iep.play` |
| `/iep leaderboards` | Open elytra leaderboards | `iep.leaderboard` |
| `/iep settings` | Open elytra settings | `iep.setting` |
| `/iep leave` | Leave elytra parkour | `iep.leave` |
| `/iep seed <seed>` | Set a non-negative seed for the current run | `iep.setting.seed` |
| `/iep schematic <x,y,z> <x,y,z>` | Save the selected cuboid as an elytra schematic | Operator |
| `/iep reset <player/uuid> [mode]` | Reset all or one mode's scores | Operator |
