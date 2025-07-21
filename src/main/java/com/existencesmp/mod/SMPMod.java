package com.existencesmp.mod;

//import com.existencesmp.mod.command.AFKCommand;
import com.existencesmp.mod.command.SMPCommand;
import de.maxhenkel.admiral.MinecraftAdmiral;
import dev.ashhhleyyy.playerpronouns.api.Pronouns;
import dev.ashhhleyyy.playerpronouns.api.PronounsApi;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.impl.PolymerResourcePackMod;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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

public class SMPMod implements ModInitializer {
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
			InputStream in = SMPMod.class.getResourceAsStream("/assets/existence_smp/icon.png");
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
			SMPMod.server = server;
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

		CommandRegistrationCallback.EVENT.register(
				(commandDispatcher, commandRegistryAccess, registrationEnvironment) ->
						MinecraftAdmiral.builder(commandDispatcher, commandRegistryAccess).addCommandClasses(
								SMPCommand.class
//								AFKCommand.class
						).build()
		);
    }

	public static void downloadResourcePack(MinecraftServer server, boolean regenerate) throws Exception {
		if (levelName == null) throw new Exception("Unknown level");
		try {
			File file = FabricLoader.getInstance().getGameDir().resolve(levelName + "/resourcepacks/existence_community_resource_pack.zip").toFile();
			if (!file.exists()) file.getParentFile().mkdirs();
			if (!file.canWrite() && file.exists()) throw new Exception("Cannot write to file");
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