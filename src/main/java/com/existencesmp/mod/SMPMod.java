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
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SMPMod implements ModInitializer {
	public static final String MOD_ID = "existence_smp";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final URL RESOURCE_PACK_URL;
	public static final URL PACKSQUASH_URL;
	public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
	public static String levelName;
	public static byte[] iconBytes;
	public static MinecraftServer server;

    static {
        try {
            RESOURCE_PACK_URL = new URL("https", "github.com", "/ExistenceSMP/community-resource-pack/releases/latest/download/existence_community_resource_pack.zip");
			if (IS_WINDOWS) {
				PACKSQUASH_URL = new URL("https", "github.com", "/ComunidadAylas/PackSquash/releases/download/v0.4.0/PackSquash.CLI.executable.x86_64-pc-windows-gnu.zip ");
			} else {
				PACKSQUASH_URL = new URL("https", "github.com", "/ComunidadAylas/PackSquash/releases/download/v0.4.0/PackSquash.CLI.executable.x86_64-unknown-linux-musl.zip");
			}
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

		File file = IS_WINDOWS ? FabricLoader.getInstance().getGameDir().resolve("packsquash.exe").toFile() : FabricLoader.getInstance().getGameDir().resolve("packsquash").toFile();
		if (!file.exists()) {
			Thread downloadPackSquashThread = new Thread(new DownloadPackSquash());
			downloadPackSquashThread.start();
		} else {
			LOGGER.info("PackSquash already exists");
		}

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
			if (!file.canWrite() && file.exists()) throw new Exception("Cannot write resource pack");
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

	private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
		File destFile = new File(destinationDir, zipEntry.getName());

		String destDirPath = destinationDir.getCanonicalPath();
		String destFilePath = destFile.getCanonicalPath();

		if (!destFilePath.startsWith(destDirPath + File.separator)) {
			throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
		}

		return destFile;
	}

	private static class DownloadPackSquash implements Runnable {
		@Override
		public void run() {
			try {
				File zipfile = FabricLoader.getInstance().getGameDir().resolve("packsquash.zip").toFile();
				if (!zipfile.exists()) zipfile.getParentFile().mkdirs();
				if (!zipfile.canWrite() && zipfile.exists()) throw new Exception("Cannot write packsquash");
				ReadableByteChannel zrbc = Channels.newChannel(PACKSQUASH_URL.openStream());
				FileOutputStream zfos = new FileOutputStream(zipfile);
				LOGGER.info("Downloading PackSquash...");
				zfos.getChannel().transferFrom(zrbc, 0, Long.MAX_VALUE);
				LOGGER.info("Successfully downloaded PackSquash to " + zipfile.getPath());
				File outDir = FabricLoader.getInstance().getGameDir().toAbsolutePath().resolve("packsquash").toFile();
				byte[] buffer = new byte[1024];
				ZipInputStream zis = new ZipInputStream(new FileInputStream(zipfile));
				ZipEntry zipEntry = zis.getNextEntry();
				while (zipEntry != null) {
					File newFile = newFile(outDir, zipEntry);
					if (zipEntry.isDirectory()) {
						if (!newFile.isDirectory() && !newFile.mkdirs()) {
							throw new IOException("Failed to create directory " + newFile);
						}
					} else {
						File parent = newFile.getParentFile();
						if (!parent.isDirectory() && !parent.mkdirs()) {
							throw new IOException("Failed to create directory " + parent);
						}

						FileOutputStream fos = new FileOutputStream(newFile);
						int len;
						while ((len = zis.read(buffer)) > 0) {
							fos.write(buffer, 0, len);
						}
						fos.close();
					}
					zipEntry = zis.getNextEntry();
				}
				zis.closeEntry();
				zis.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static class RunPackSquash implements Runnable {
		@Override
		public void run() {
			final String packSquashExecutablePath = IS_WINDOWS ? FabricLoader.getInstance().getGameDir().resolve("packsquash").resolve("packsquash.exe").toAbsolutePath().toString() : FabricLoader.getInstance().getGameDir().resolve("packsquash").resolve("packsquash").toAbsolutePath().toString();
			try {
				File outDir = FabricLoader.getInstance().getGameDir().toAbsolutePath().resolve("packsquash").resolve("pack").toFile();
				File zipfile = FabricLoader.getInstance().getGameDir().resolve("polymer").resolve("resource_pack.zip").toFile();
				byte[] buffer = new byte[1024];
				ZipInputStream zis = new ZipInputStream(new FileInputStream(zipfile));
				ZipEntry zipEntry = zis.getNextEntry();
				while (zipEntry != null) {
					File newFile = newFile(outDir, zipEntry);
					if (zipEntry.isDirectory()) {
						if (!newFile.isDirectory() && !newFile.mkdirs()) {
							throw new IOException("Failed to create directory " + newFile);
						}
					} else {
						File parent = newFile.getParentFile();
						if (!parent.isDirectory() && !parent.mkdirs()) {
							throw new IOException("Failed to create directory " + parent);
						}

						FileOutputStream fos = new FileOutputStream(newFile);
						int len;
						while ((len = zis.read(buffer)) > 0) {
							fos.write(buffer, 0, len);
						}
						fos.close();
					}
					zipEntry = zis.getNextEntry();
				}
				zis.closeEntry();
				zis.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			final File packFolder = FabricLoader.getInstance().getGameDir().resolve("packsquash").resolve("pack").toFile();
			final InputStream partialSettingsStream = SMPMod.class.getResourceAsStream("/packsquash.toml");
			final ProcessBuilder processBuilder = new ProcessBuilder(packSquashExecutablePath);
			processBuilder.directory(packFolder);
			processBuilder.redirectInput(ProcessBuilder.Redirect.PIPE);
			processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE);
			processBuilder.redirectErrorStream(true);
			try {
				final Process packSquashProcess;
				packSquashProcess = processBuilder.start();
				final OutputStream packSquashInputStream = packSquashProcess.getOutputStream();
				try {
					packSquashInputStream.write(
							("pack_directory = \".\"" + System.lineSeparator()).getBytes(StandardCharsets.UTF_8)
					);
					partialSettingsStream.transferTo(packSquashInputStream);
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					packSquashInputStream.close();
				}
				final InputStream packSquashOutputStream = new BufferedInputStream(packSquashProcess.getInputStream());
				try {
					int outputByte;
					while ((outputByte = packSquashOutputStream.read()) != -1) {
						System.out.write(outputByte);
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					packSquashOutputStream.close();
				}
				try {
					System.out.println("- PackSquash finished with code " + packSquashProcess.waitFor());
				} catch (final InterruptedException exc) {
					System.out.println("- Thread interrupted while waiting for PackSquash to finish!");
					exc.printStackTrace();
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}