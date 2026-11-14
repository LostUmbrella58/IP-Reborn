package dev.efnilite.ip.integration;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.ServerIntegration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.logging.Level;

public final class IntegrationLoader {

    private static final int MAX_PROVIDER_FAILURES = 32;

    private IntegrationLoader() {
    }

    public static List<ServerIntegration> enable(IP plugin) {
        List<ServerIntegration> enabled = new ArrayList<>();
        Iterator<ServerIntegration> iterator;
        try {
            iterator = ServiceLoader.load(ServerIntegration.class, plugin.getClass().getClassLoader()).iterator();
        } catch (ServiceConfigurationError | LinkageError error) {
            plugin.getLogger().log(Level.SEVERE, "Could not discover server integrations", error);
            return List.of();
        }

        int failures = 0;
        while (true) {
            ServerIntegration integration;
            try {
                if (!iterator.hasNext()) {
                    break;
                }
                integration = iterator.next();
            } catch (ServiceConfigurationError | LinkageError error) {
                // one broken provider entry must not stop the remaining ones from loading.
                // ServiceLoader recovery is best-effort, so cap retries to avoid spinning forever.
                plugin.getLogger().log(Level.SEVERE, "Could not load a server integration provider", error);
                if (++failures >= MAX_PROVIDER_FAILURES) {
                    break;
                }
                continue;
            }

            String id = safeId(integration);
            try {
                integration.enable(plugin);
                enabled.add(integration);
                plugin.getLogger().info("Enabled server integration: " + id);
            } catch (Exception | LinkageError exception) {
                plugin.getLogger().log(Level.SEVERE, "Could not enable server integration " + id, exception);
            }
        }
        return List.copyOf(enabled);
    }

    public static void disable(IP plugin, List<ServerIntegration> integrations) {
        for (int index = integrations.size() - 1; index >= 0; index--) {
            ServerIntegration integration = integrations.get(index);
            try {
                integration.disable();
            } catch (Exception | LinkageError exception) {
                plugin.getLogger().log(Level.SEVERE, "Could not disable server integration " + safeId(integration), exception);
            }
        }
    }

    private static String safeId(ServerIntegration integration) {
        try {
            return integration.id();
        } catch (Exception | LinkageError ex) {
            return integration.getClass().getName();
        }
    }
}
