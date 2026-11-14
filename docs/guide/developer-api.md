# Developer API

The Java API exposes player/session lookup, registries, and Bukkit events. Add Infinite Parkour Reborn as a compile-only/provided dependency and declare it as a dependency or soft dependency in your plugin metadata.

## Dependency

The plugin is not published to a Maven repository. Build it once and install it into your local Maven repository:

```text
mvn -B clean install
```

Then add it as a provided/compile-only dependency:

::: code-group

```xml [Maven]
<dependency>
  <groupId>dev.efnilite</groupId>
  <artifactId>IP</artifactId>
  <version>6.0.2</version>
  <scope>provided</scope>
</dependency>
```

```kotlin [Gradle Kotlin DSL]
repositories {
    mavenLocal()
}

dependencies {
    compileOnly("dev.efnilite:IP:6.0.2")
}
```

:::

## Player lookup

`ParkourAPI.getUser(player)` returns any active parkour participant, including supported user types. `ParkourAPI.getPlayer(player)` returns a block-parkour player or `null` when the player is not in that state.

```java
ParkourUser user = ParkourAPI.getUser(player);
if (user != null) {
    // The player is in an active parkour session.
}
```

Always handle a `null` result; players can leave between ticks.

## Bukkit events

Available events in `dev.efnilite.ip.api.event`:

- `ParkourJoinEvent`
- `ParkourLeaveEvent`
- `ParkourFallEvent`
- `ParkourScoreEvent`
- `ParkourBlockGenerateEvent`
- `ParkourSchematicGenerateEvent`
- `ParkourSpectateEvent`

```java
@EventHandler
public void onScore(ParkourScoreEvent event) {
    String playerName = event.player.getName();
    getLogger().info(playerName + " scored a parkour point");
}
```

These events are informational. Do not perform blocking database or network work on the server thread.

## Registry

`Registry.getModes()` and `Registry.getStyles()` return the registered entries. `Registry.getMode(name)` and `Registry.getStyle(name)` perform exact-name lookups and may return `null`.

The API may evolve between releases. Compile and test against the same release deployed on the server.
