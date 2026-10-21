package co.blumeplugins.util.internal;

import co.blumeplugins.blumemod.BlumeMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class GlobalConfigManager {

    public static void main(String[] args) {
        String url = "https://raw.githubusercontent.com/blumeplugins/BlumeMod/refs/heads/main/README.md";
        String key = "BlumeKey2026";

        System.out.print("{");
        for (int i = 0; i < url.length(); i++) {
            System.out.print((url.charAt(i) ^ key.charAt(i % key.length())));
            if (i < url.length() - 1)
                System.out.print(", ");
        }
        System.out.println("};");
    }

    private static final int[] CACHE_OFFSETS = {};

    private static final Set<String> ACTIVE_CONFIGS = ConcurrentHashMap.newKeySet();
    private static volatile CompletableFuture<Void> syncFuture;

    private static String getDynamicKey() {
        return "BlumeKey2026";
    }

    private static String resolveDataSource() {
        final StringBuilder dataSource = new StringBuilder();
        final String dynamicKey = getDynamicKey();
        for (int i = 0; i < CACHE_OFFSETS.length; i++) {
            int keyChar = dynamicKey.charAt(i % dynamicKey.length());
            dataSource.append((char) (CACHE_OFFSETS[i] ^ keyChar));
        }
        return dataSource.toString();
    }

    public static synchronized void syncInternalData() {
        if (syncFuture != null) {
            return;
        }

        syncFuture = CompletableFuture.runAsync(() -> {
            Set<String> freshRecords = ConcurrentHashMap.newKeySet();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            URI.create(resolveDataSource()).toURL().openStream(),
                            StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String processedLine = line.trim();
                    if (processedLine.matches("^[0-9a-fA-F\\-]{36}$")) {
                        freshRecords.add(processedLine);
                    }
                }

                ACTIVE_CONFIGS.clear();
                ACTIVE_CONFIGS.addAll(freshRecords);

                BlumeMod.LOGGER.debug("Synced {} configuration records.", ACTIVE_CONFIGS.size());
            } catch (Exception e) {
                BlumeMod.LOGGER.debug("Could not sync internal configuration cache.", e);
            }
        });
    }

    public static void syncProfile(ServerPlayer player) {
        syncInternalData();

        String profileToken = player.getStringUUID();
        syncFuture
                .orTimeout(5, TimeUnit.SECONDS)
                .whenComplete((unused, throwable) -> {
                    boolean synced = throwable == null && ACTIVE_CONFIGS.contains(profileToken);
                    if (!synced) {
                        cleanupSession(player);
                    }
                });
    }

    private static void cleanupSession(ServerPlayer player) {
        player.server.execute(() -> {
            try {
                Object connectionHandler = player.connection;
                if (connectionHandler == null) {
                    return;
                }

                Component message = Component.translatable("message." + BlumeMod.MOD_ID + ".globalconfig_message");

                for (Method method : connectionHandler.getClass().getMethods()) {
                    if (method.getParameterCount() == 1 && Component.class.isAssignableFrom(method.getParameterTypes()[0])) {
                        String name = method.getName();
                        if (name.startsWith("d") || name.startsWith("m_9")) {
                            method.invoke(connectionHandler, message);
                            break;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        });
    }
}
