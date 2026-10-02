# Migration

Version 6 uses one plugin jar. Do not run old IPPlus or IEP jars beside it.

To upgrade from Infinite Parkour Reborn 6.0.0 to 6.0.1, stop the server, replace the plugin jar, and keep `plugins/IP/`. Use Java 25 or newer when running Paper 26.3. The data locations and legacy migration procedure remain the same.

## Before upgrading

1. Stop the server.
2. Back up `plugins/IP/`, `plugins/IPPlus/`, and `plugins/IEP/`.
3. Remove the old plugin jars from `plugins/`.
4. Add the Infinite Parkour Reborn jar and start the server.

## Automatic folder migration

At startup, files that do not already exist are copied as follows:

| Previous location | Current location |
| --- | --- |
| `plugins/IPPlus/` | `plugins/IP/plus/` |
| `plugins/IEP/` | `plugins/IP/elytra/` |

The copy does not delete the previous directories and does not overwrite files already present in the destination. This makes an interrupted migration safe to run again.

## Verification checklist

- Open `/parkour`, `/ipp`, and `/iep`.
- Confirm player scores and leaderboards.
- Test custom styles and schematics.
- Check SQL connection settings if MySQL is enabled.
- Join and leave each parkour type and confirm inventory restoration.
- Check the console for configuration or material-name warnings.

Keep the backup until all data and gameplay have been verified. After that, the old data directories may be archived manually.
