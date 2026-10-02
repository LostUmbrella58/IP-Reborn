# Installation

## Requirements

Use one of these combinations:

| Paper version | Required Java |
| --- | --- |
| 1.21.11 | Java 21+ |
| 26.1.2 | Java 25+ |
| 26.2 | Java 25+ |
| 26.3 | Java 25+ |

Paper 26.3 support was added in Infinite Parkour Reborn 6.0.1.

The plugin does not require `vilib`, PaperLib, VoidGen, or a separate void-world generator.

## New installation

1. Stop the server.
2. Download the jar from [GitHub Releases](https://github.com/LostUmbrella58/IP-Reborn/releases) or the [Spigot resource page](https://www.spigotmc.org/resources/infinite-parkour-reborn.136046/).
3. Place the jar in the server's `plugins/` directory.
4. Start the server and wait for the plugin to generate its files.
5. Run `/parkour`, `/ipp`, and `/iep` in game to verify each menu.
6. Stop the server before editing YAML files, then start it again.

## First-start files

The main files are generated below `plugins/IP/`:

```text
plugins/IP/
├─ config.yml
├─ generation.yml
├─ rewards-v2.yml
├─ locales/
├─ schematics/
├─ plus/
│  ├─ config.yml
│  └─ locales/
└─ elytra/
   ├─ config.yml
   ├─ rewards.yml
   ├─ locales/
   └─ schematics/
```

## Optional plugins

PlaceholderAPI, Vault, Multiverse-Core, HolographicDisplays, floodgate, and Chunky are detected when installed. They are optional; see [Integrations](./integrations).

::: tip
Keep a copy of the generated defaults before making large changes. YAML indentation matters, and material, particle, and sound names must exist in the Paper version you run.
:::
