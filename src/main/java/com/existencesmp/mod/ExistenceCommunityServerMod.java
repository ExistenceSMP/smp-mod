package com.existencesmp.mod;

import dev.ashhhleyyy.playerpronouns.api.Pronouns;
import dev.ashhhleyyy.playerpronouns.api.PronounsApi;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import net.fabricmc.api.ModInitializer;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExistenceCommunityServerMod implements ModInitializer {
	public static final String MOD_ID = "existence_smp";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		Placeholders.register(
				Identifier.of(MOD_ID, "pronouns"),
				(ctx, arg) -> {
					if (ctx.hasPlayer()) {
						Pronouns pronouns = PronounsApi.getReader().getPronouns(ctx.player());
						if (pronouns != null) {
							return PlaceholderResult.value(
									Text.empty().copy()
											.append(" [")
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
	}
}