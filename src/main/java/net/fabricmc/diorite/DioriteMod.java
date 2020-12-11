package net.fabricmc.diorite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

import com.mojang.authlib.GameProfile;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.util.Util;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.diorite.mixin.ServerLoginNetworkHandlerAccessor;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;

import net.minecraft.text.LiteralText;

public final class DioriteMod implements ModInitializer {
	private DioriteConfig config;

	@Override
	public void onInitialize() {
		ServerLoginConnectionEvents.QUERY_START.register(this::onLoginStart);

		try {
			this.config = DioriteConfig.loadConfig();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
			return;
		}

		if (!DioriteUtil.isURLValid(config.endpoint)) {
			throw new Error("Diorite config has an invalid endpoint. Please edit the config file.");
		}
	}

	private void onLoginStart(ServerLoginNetworkHandler networkHandler, MinecraftServer server, PacketSender sender, ServerLoginNetworking.LoginSynchronizer synchronizer) {
		GameProfile profile = ((ServerLoginNetworkHandlerAccessor) networkHandler).getProfile();

		String playerUUID = profile.getId().toString();

		FutureTask<?> future = new FutureTask<>(() -> {
			// Decide here whether to allow connection or not by calling external HTTP API
			try {
				HashMap<String, String> queryParams = new HashMap<String, String>(this.config.queryParams);

				queryParams.put("uuid", playerUUID.replaceAll("-", ""));

				URL url = new URL(this.config.endpoint + '?' + DioriteUtil.getQueryParamsFrom(queryParams));

				HttpURLConnection http = (HttpURLConnection) url.openConnection();
				http.setConnectTimeout(1000);
				int statusCode = http.getResponseCode();

				if (statusCode == 401) {
					BufferedReader br = new BufferedReader(new InputStreamReader(http.getErrorStream()));
					String msg = br.lines().collect(Collectors.joining());

					networkHandler.disconnect(new LiteralText(msg));
				}
			} catch (IOException error) {
				networkHandler.disconnect(new LiteralText("Whitelist API errored out."));
			}

			return null;
		});

		Util.getMainWorkerExecutor().execute(future);
		synchronizer.waitFor(future);
	}
}
