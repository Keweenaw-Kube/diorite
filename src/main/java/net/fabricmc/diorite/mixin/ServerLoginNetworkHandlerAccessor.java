package net.fabricmc.diorite.mixin;

import net.minecraft.server.network.ServerLoginNetworkHandler;

import com.mojang.authlib.GameProfile;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerLoginNetworkHandler.class)
public interface ServerLoginNetworkHandlerAccessor {
	@Accessor("profile")
	public GameProfile getProfile();
}
