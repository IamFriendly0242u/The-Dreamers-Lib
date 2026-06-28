package net.thedreamers.lib;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Thedreamers_lib implements ModInitializer {

	public static final String MOD_ID = "thedreamers_lib";
	public static final Logger LOGGER = LoggerFactory.getLogger("TheDreamersLib");

	@Override
	public void onInitialize() {
		LOGGER.info("The Dreamers Lib has been successfully initialized!");
	}
}