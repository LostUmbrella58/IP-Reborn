# Migration from IP, IPPlus, and IEP

Version 6 is one plugin. Do not run the old IPPlus or IEP jars beside it.

On first startup, Infinite Parkour Reborn copies files that do not already exist:

| Legacy location | Unified location |
| --- | --- |
| `plugins/IPPlus/` | `plugins/IP/plus/` |
| `plugins/IEP/` | `plugins/IP/elytra/` |

The copy is intentionally non-destructive:

- existing files in the unified location win;
- legacy files and directories remain untouched;
- the migration can be run again safely if startup was interrupted.

Before upgrading, stop the server and back up all three plugin data folders. Once
the unified plugin has started and the modes have been tested, the old jars must
stay removed. Keep the backup until player data, schematics, database settings,
and worlds have been verified.

The main IP data remains in `plugins/IP/`. `/ipp` and `/iep` are retained so
existing staff procedures do not need to change immediately.

To upgrade from Infinite Parkour Reborn 6.0.0 to 6.0.1, stop the server, replace
the plugin jar, and keep the existing `plugins/IP/` directory. Use Java 25 or
newer when running Paper 26.3. The data locations and legacy migration procedure
remain the same.
