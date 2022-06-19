package net.fabricmc.diorite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Collectors;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.authlib.GameProfile;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.diorite.mixin.ServerLoginNetworkHandlerAccessor;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;

public final class DioriteMod implements ModInitializer {
	private DioriteConfig config;
	private Executor scheduler = new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder()
			.setDaemon(true)
			.setNameFormat("diorite-scheduler")
			.build());

	@Override
	public void onInitialize() {
		ServerLoginConnectionEvents.QUERY_START.register(this::onPreLogin);

		ServerPlayConnectionEvents.DISCONNECT.register(this::onDisconnect);

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

	private void onPreLogin(ServerLoginNetworkHandler networkHandler, MinecraftServer server, PacketSender sender, ServerLoginNetworking.LoginSynchronizer sync) {
		GameProfile profile = ((ServerLoginNetworkHandlerAccessor) networkHandler).getProfile();
		String playerUUID = profile.getId().toString();

		sync.waitFor(CompletableFuture.runAsync(() -> onPreLoginAsync(networkHandler, playerUUID), this.scheduler));
	}

	private void onPreLoginAsync(ServerLoginNetworkHandler networkHandler, String playerUUID) {
		// Decide here whether to allow connection or not by calling external HTTP API
		try {
			HashMap queryParams = this.getQueryParams(playerUUID);

			HttpURLConnection http = (HttpURLConnection) this.getURL(queryParams).openConnection();
			http.setConnectTimeout(1000);
			int statusCode = http.getResponseCode();

			if (statusCode == 401) {
				BufferedReader br = new BufferedReader(new InputStreamReader(http.getErrorStream()));
				String msg = br.lines().collect(Collectors.joining());

				networkHandler.disconnect(Text.literal(msg));
			}
		} catch (IOException error) {
			networkHandler.disconnect(Text.literal("Whitelist API errored out."));
		}
	}

	private void onDisconnect(ServerPlayNetworkHandler networkHandler, MinecraftServer server) {
		String playerUUID = networkHandler.player.getUuidAsString();

		if (this.config.callOnDisconnect) {
			try {
				HashMap queryParams = this.getQueryParams(playerUUID);

				queryParams.put("state", "disconnected");

				HttpURLConnection http = (HttpURLConnection) this.getURL(queryParams).openConnection();
				http.setConnectTimeout(1000);
				int statusCode = http.getResponseCode();

				if (statusCode == 401) {
					BufferedReader br = new BufferedReader(new InputStreamReader(http.getErrorStream()));
					String msg = br.lines().collect(Collectors.joining());

					networkHandler.disconnect(Text.literal(msg));
				}
			} catch (IOException error) {
				System.err.println("Whitelist API errored out.");
			}
		}
	}

	private HashMap<String, String> getQueryParams(String playerUUID) {
		HashMap queryParams = this.config.queryParams == null ? new HashMap<>() : new HashMap<String, String>(this.config.queryParams);

		queryParams.put("uuid", playerUUID.replaceAll("-", ""));

		return queryParams;
	}

	private URL getURL(HashMap<String, String> queryParams) throws MalformedURLException {
		return new URL(this.config.endpoint + '?' + DioriteUtil.getQueryParamsFrom(queryParams));
	}
}
