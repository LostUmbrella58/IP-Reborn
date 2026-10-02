# Troubleshooting

## The plugin does not load

1. Confirm the server is Paper 1.21.11, 26.1.2, 26.2, or 26.3.
2. Confirm Java 21+ for Paper 1.21.11 or Java 25+ for Paper 26.
3. Remove old IPPlus and IEP jars; only one Infinite Parkour Reborn jar should be installed.
4. Read the first exception in the console, not only the final “disabled” message.

## A config change has no effect

- Validate YAML indentation and quoting.
- Check that materials, particles, and sounds exist in your Paper version.
- Use `/ip reload` for core files or `/ipp reload` for multiplayer files.
- Restart the server for world, mode registration, storage, or elytra changes.
- Compare the file with a newly generated default after making a backup.

## Players cannot open a menu or join

Check `permissions.enabled` in `plugins/IP/config.yml`, the matching `default-values` menu entry, and the player's permission nodes. Elytra uses its own `permissions` switch in `plugins/IP/elytra/config.yml`.

Also confirm `joining: true` in the core config when players should be able to start block parkour.

## Inventory was not restored

Keep the affected player online and run:

```text
/ip recoverinventory <player>
```

The command requires `ip.admin`. Do not repeatedly join and leave before recovery, because newer saved state may replace the data you need. Preserve `plugins/IP/inventories/` while investigating.

## A migrated file is missing

The migration only copies a file when the destination does not already exist. Compare:

- `plugins/IPPlus/` with `plugins/IP/plus/`
- `plugins/IEP/` with `plugins/IP/elytra/`

Stop the server before manually copying anything and keep both backups.

## Reporting a bug

Open a [GitHub issue](https://github.com/LostUmbrella58/IP-Reborn/issues) or join the [Discord server](https://discord.gg/WxfBtuAsv6). Include:

- exact Paper and Java versions;
- plugin version;
- startup log and complete exception;
- relevant configuration with passwords removed;
- steps that reproduce the problem.

Do not report bugs in a review, because reviews do not provide enough space to diagnose them.
