// Copyright (C) 2011 Antony Derham <admin@districtmine.net>

package net.districtmine.ant59.albus;

import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerLoginEvent;

public class AlbusListener extends PlayerListener {
	private final Albus plugin;

	public AlbusListener(Albus instance) {
		plugin = instance;
	}

	@Override
	public void onPlayerLogin(PlayerLoginEvent event) {
		//if (plugin.isWhitelistActive()) {
			String playerName = event.getPlayer().getName();
			if (plugin.isOnWhitelist(playerName)) {
				plugin.consoleLog(playerName + " tried to join and was allowed.");
			} else {
				plugin.consoleLog(playerName + " tried to join and was kicked.");
				if (plugin.isRegistered(playerName)) {
					event.disallow(PlayerLoginEvent.Result.KICK_OTHER, plugin.getKickMessageRegistered());
				} else {
					event.disallow(PlayerLoginEvent.Result.KICK_OTHER, plugin.getKickMessage());
				}
			}
		//}
	}
}