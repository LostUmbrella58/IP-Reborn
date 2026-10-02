# Infinite Parkour Reborn

Infinite Parkour Reborn combines Infinite Parkour, IPPlus, and Infinite Elytra
Parkour into one plugin and one data directory. The original projects were
created by Efnilite.

## Supported servers

Infinite Parkour Reborn 6.0.1 supports the following server versions:

- Paper 1.21.11 on Java 21
- Paper 26.1.2 on Java 25
- Paper 26.2 on Java 25
- Paper 26.3 on Java 25

Spigot, Folia, older Minecraft releases, and unofficial forks are not supported.

## Installation

1. Stop the server.
2. Remove the old IP, IPPlus, and IEP jars.
3. Put the single Infinite Parkour Reborn jar in `plugins/`.
4. Start the server.

The plugin copies missing files from the old `plugins/IPPlus` and `plugins/IEP`
folders into `plugins/IP/plus` and `plugins/IP/elytra`. The old folders are not
deleted. See [MIGRATION.md](MIGRATION.md) before removing them manually.

Commands from the former plugins remain available through `/ipp` and `/iep`.
The main command is `/witp`, with `/parkour` and `/ip` as aliases.

## Documentation

Installation, migration, commands, permissions, configuration, placeholders,
and developer API documentation are available at
[lostumbrella58.github.io/IP-Reborn](https://lostumbrella58.github.io/IP-Reborn/).

## Building

The default build targets Paper 1.21.11 and emits Java 21 bytecode, retaining
compatibility with that server version while supporting Paper 26.3:

```powershell
.\mvnw.cmd -B clean verify
```

Other supported API baselines can be checked without editing the POM. Use JDK 25
for these builds; the plugin still emits Java 21 bytecode:

```powershell
.\mvnw.cmd -B clean verify -Dpaper.version=26.1.2.build.74-stable
.\mvnw.cmd -B clean verify -Dpaper.version=26.2.build.84-stable
.\mvnw.cmd -B clean verify -Dpaper.version=26.3.build.142-beta
```

The shaded plugin jar is written to `target/IP-6.0.1.jar`.

This project is licensed under GPL-3.0. See [NOTICE.md](NOTICE.md) for upstream
attribution.
