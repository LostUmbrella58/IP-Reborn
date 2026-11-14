package dev.efnilite.ip.foundation.util;

import org.bukkit.plugin.Plugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker {

    private static final Pattern LATEST_NAME = Pattern.compile("\"name\": ?\"(.*?)\"");
    private static final Pattern NUMERIC_PREFIX = Pattern.compile("^\\s*[vV]?(\\d+(?:\\.\\d+)*)");

    public static void check(Plugin plugin, int resourceId) {
        Task.create(plugin)
                .async()
                .execute(() -> {
                    String tag;
                    try {
                        HttpClient client = HttpClient.newHttpClient();
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create("https://api.spiget.org/v2/resources/%s/versions/latest".formatted(resourceId)))
                                .build();
                        tag = parseLatestTag(client.send(request, HttpResponse.BodyHandlers.ofString()).body());
                    } catch (Exception ex) {
                        tag = "";
                    }

                    if (tag.isEmpty()) {
                        plugin.getLogger().info("Failed to check for updates");
                        return;
                    }

                    String version;
                    try {
                        version = plugin.getPluginMeta().getVersion();
                    } catch (NoSuchMethodError ignored) {
                        version = plugin.getDescription().getVersion();
                    }

                    if (isNewer(tag, version)) {
                        plugin.getLogger().info("A new version is available: %s -> %s".formatted(version, tag));
                        plugin.getLogger().info("Download at https://spigotmc.org/resources/%s".formatted(resourceId));
                    }
                })
                .run();
    }

    /**
     * Whether the published version is strictly newer than the running one. Only the leading
     * dotted number is compared, so "6.0" equals "6.0.0-SNAPSHOT" and "6.0.1" equals a local
     * "6.0.1-26.3-SNAPSHOT" build. A version without a numeric prefix is never reported as newer.
     */
    static boolean isNewer(String remote, String local) {
        int[] r = numericPrefix(remote);
        int[] l = numericPrefix(local);
        if (r == null || l == null) {
            return false;
        }
        for (int i = 0; i < Math.max(r.length, l.length); i++) {
            int a = i < r.length ? r[i] : 0;
            int b = i < l.length ? l[i] : 0;
            if (a != b) {
                return a > b;
            }
        }
        return false;
    }

    private static int[] numericPrefix(String version) {
        Matcher matcher = NUMERIC_PREFIX.matcher(version);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Arrays.stream(matcher.group(1).split("\\.")).mapToInt(Integer::parseInt).toArray();
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String parseLatestTag(String responseBody) {
        Matcher matcher = LATEST_NAME.matcher(responseBody);
        return matcher.find() ? matcher.group(1) : "";
    }
}
