# Changelog

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
