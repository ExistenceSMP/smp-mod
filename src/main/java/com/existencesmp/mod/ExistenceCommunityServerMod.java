package com.existencesmp.mod;

import dev.ashhhleyyy.playerpronouns.api.Pronouns;
import dev.ashhhleyyy.playerpronouns.api.PronounsApi;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.impl.PolymerResourcePackMod;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class ExistenceCommunityServerMod implements ModInitializer {
	public static final String MOD_ID = "existence_smp";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final URL RESOURCE_PACK_URL;
	public static String levelName;
	public static byte[] iconBytes;
	public static MinecraftServer server;

    static {
        try {
            RESOURCE_PACK_URL = new URL("https", "github.com", "/ExistenceSMP/community-resource-pack/releases/latest/download/existence_community_resource_pack.zip");
			LOGGER.info("Reading pack icon...");
			InputStream in = ExistenceCommunityServerMod.class.getResourceAsStream("/assets/existence_smp/icon.png");
			iconBytes = IOUtils.toByteArray(in);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

	@Override
	public void onInitialize() {
		LOGGER.info("Setting pack icon...");
		PolymerResourcePackUtils.getInstance().setPackIcon(iconBytes);

		Placeholders.register(
				Identifier.of(MOD_ID, "pronouns"),
				(ctx, arg) -> {
					if (ctx.hasPlayer()) {
						Pronouns pronouns = PronounsApi.getReader().getPronouns(ctx.player());
						if (pronouns != null) {
							return PlaceholderResult.value(
									Text.literal(" [")
											.append(pronouns.raw())
											.append("]")
											.formatted(Formatting.GRAY)
							);
						} else {
							return PlaceholderResult.value("");
						}
					} else return PlaceholderResult.invalid("Not a player");
				}
		);

		ServerWorldEvents.LOAD.register((server, world) -> {
			levelName = server.getSaveProperties().getLevelName();
			LOGGER.info("Got level name: " + levelName);
			ExistenceCommunityServerMod.server = server;
        });

		ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
			downloadResourcePack(server, true);
		});

		PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(builder -> {
			LOGGER.info("Setting pack description...");
			builder.getPackMcMetaBuilder().description(
					Text.literal("Existence SMP").formatted(Formatting.DARK_RED)
							.append(Text.literal("\n"))
							.append(Text.literal("Community Resource Pack").formatted(Formatting.GRAY))
			);
			builder.copyResourcePackFromPath(FabricLoader.getInstance().getGameDir().resolve(levelName + "/resourcepacks/existence_community_resource_pack.zip"));
		});
    }

	public static void downloadResourcePack(MinecraftServer server, boolean regenerate) {
		if (levelName == null) return;
		try {
			File file = FabricLoader.getInstance().getGameDir().resolve(levelName + "/resourcepacks/existence_community_resource_pack.zip").toFile();
			if (!file.exists()) file.getParentFile().mkdirs();
			if (!file.canWrite()) return;
			ReadableByteChannel rbc = Channels.newChannel(RESOURCE_PACK_URL.openStream());
			FileOutputStream fos = new FileOutputStream(file);
			LOGGER.info("Downloading resource pack...");
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			LOGGER.info("Successfully downloaded resource pack to " + file.getPath());
			if (regenerate) {
				LOGGER.info("Asking Polymer to generate resource pack...");
				PolymerResourcePackMod.generateAndCall(server, false, x -> server.getCommandSource().sendFeedback(() -> x, true), () -> {});
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}