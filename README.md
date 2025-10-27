# UltimateVotePlus 2.0.5

This patch addresses a startup failure:

```
org.bukkit.plugin.InvalidPluginException: java.lang.IllegalArgumentException: Plugin already initialized!
Caused by: java.lang.IllegalStateException: Initial initialization
  at org.bukkit.plugin.java.PluginClassLoader.initialize(PluginClassLoader.java:211)
  ...
  at com.example.rps.RpsPlugin.<init>(RpsPlugin.java:11)
```

**Cause (typical):** Another `JavaPlugin` subclass (e.g., `com.example.rps.RpsPlugin`) was bundled into the same JAR or instantiated from static code, which triggers a second plugin classloader initialization during the first plugin's load.

**Our Fix:** This project contains exactly one `JavaPlugin` (the main class listed in `plugin.yml`). The POM is cleaned so that no other plugin module or shaded dependency can be merged inadvertently.

**What to check on your server:**
1. Ensure only one copy of this plugin exists in the `plugins/` folder (e.g., remove older `UltimateVotePlus-*.jar`).
2. Remove any unrelated JAR that might contain `com.example.rps.RpsPlugin`. That class should live in its own plugin JAR, not inside this one.
3. If you previously used a multi-module build or copied classes between projects, rebuild using this repo so only VotePlus code is packaged.

## Build
- Java 11, 1.16.5 (CatServer/Spigot compatible)
- `mvn package` produces `target/UltimateVotePlus-2.0.5.jar`
- GitHub Actions workflow included (see `.github/workflows/main.yml`)