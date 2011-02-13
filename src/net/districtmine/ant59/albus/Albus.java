/*
 * Copyright (C) 2011 <silence@immortal-forces.net>
 *
 * This file is part of the Bukkit plugin SMFWhite.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307, USA.
 */

// Modifications Copyright (C) 2011 Antony <admin@districtmine.net>

package net.districtmine.ant59.albus;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

public class Albus extends JavaPlugin {
	// Logger
	public static final Logger log = Logger.getLogger("Minecraft"); // Logger

	// Constants
	private final String PROP_KICKMESSAGE = "kick-message";
	private final String PROP_WHITELIST_ADMINS = "whitelist-admins";
	private final String PROP_DISABLE_LIST = "disable-list-command";
	private final String PROP_MYSQL_HOST = "mysql-host";
	private final String PROP_MYSQL_PORT = "mysql-port";
	private final String PROP_MYSQL_USER = "mysql-user";
	private final String PROP_MYSQL_PASS = "mysql-pass";
	private final String PROP_MYSQL_DB = "mysql-db";
	private final String PROP_MYSQL_PREFIX = "mysql-prefix";
	private final String PROP_SMF_ALLOWED_GROUPS = "smf-groups";
	// private final String FILE_WHITELIST = "whitelist.txt";
	private final String FILE_CONFIG = "albus.properties";

	// Plugin
	private String name;
	private String version;
	private PluginDescriptionFile pdfFile;

	// Attributes
	private final WLPlayerListener m_PlayerListner = new WLPlayerListener(this);
	private Timer m_Timer;
	private File m_Folder;
	private ArrayList<String> m_WhitelistAdmins;
	private ArrayList<String> m_WhitelistAllow;
	private String m_KickMessage;
	private boolean m_IsWhitelistActive;
	private boolean m_IsListCommandDisabled;

	// Database
	private MySQL sql;

	public Albus(PluginLoader pluginLoader, Server instance,
			PluginDescriptionFile desc, File folder, File plugin,
			ClassLoader cLoader) {
		super(pluginLoader, instance, desc, folder, plugin, cLoader);

		m_Folder = folder;
		m_KickMessage = "";
		m_WhitelistAdmins = new ArrayList<String>();
		m_WhitelistAllow = new ArrayList<String>();
		m_IsWhitelistActive = true;
		m_IsListCommandDisabled = false;
	}

	public void onEnable() {
		// Register our events
		PluginManager pm = getServer().getPluginManager();

		pm.registerEvent(Event.Type.PLAYER_LOGIN, m_PlayerListner,
				Priority.Low, this);
		// pm.registerEvent(Event.Type.PLAYER_COMMAND, m_PlayerListner,
		// Priority.Monitor, this);

		this.pdfFile = this.getDescription();
		this.name = this.pdfFile.getName();
		this.version = this.pdfFile.getVersion();

		// Create folders and files
		if (!m_Folder.exists()) {
			consoleLog("Config folder missing, creating...");
			m_Folder.mkdir();
			consoleLog("Folder created");
		}
		/*
		 * File fWhitelist = new File(m_Folder.getAbsolutePath() +
		 * File.separator + FILE_WHITELIST); if (!fWhitelist.exists()) {
		 * System.out.print("Whitelist: Whitelist is missing, creating..."); try
		 * { fWhitelist.createNewFile(); System.out.println("done."); } catch
		 * (IOException ex) { System.out.println("failed."); } }
		 */

		File fConfig = new File(m_Folder.getAbsolutePath() + File.separator
				+ FILE_CONFIG);
		if (!fConfig.exists()) {
			consoleLog("Config is missing, creating...");
			try {
				fConfig.createNewFile();
				Properties propConfig = new Properties();
				propConfig.setProperty(PROP_KICKMESSAGE,
						"Sorry, you are not on the whitelist!");
				propConfig.setProperty(PROP_WHITELIST_ADMINS,
						"Name1,Name2,Name3");
				propConfig.setProperty(PROP_DISABLE_LIST, "false");
				propConfig.setProperty(PROP_MYSQL_HOST, "localhost");
				propConfig.setProperty(PROP_MYSQL_PORT, "3306");
				propConfig.setProperty(PROP_MYSQL_USER, "user");
				propConfig.setProperty(PROP_MYSQL_PASS, "pass");
				propConfig.setProperty(PROP_MYSQL_DB, "forum");
				propConfig.setProperty(PROP_MYSQL_PREFIX, "smf_");
				propConfig.setProperty(PROP_SMF_ALLOWED_GROUPS, "1,2,3");
				BufferedOutputStream stream = new BufferedOutputStream(
						new FileOutputStream(fConfig.getAbsolutePath()));
				propConfig.store(stream,
						"Auto generated config file, please modify");
				consoleLog("Config created");
			} catch (IOException ex) {
				consoleWarning("Failed to create config");
			}
		}
		
		class ReloaderTask extends TimerTask {
	        public void run() {
	        	loadWhitelistSettings();
	        }
	    }
		
		// Start reloading timer
		m_Timer = new Timer(true);
		m_Timer.schedule(new ReloaderTask(), 0, 300000);

		consoleLog("Enabled!");
	}

	public void onDisable() {
		m_Timer.cancel();
		m_Timer.purge();
		m_Timer = null;
		consoleLog("Goodbye!");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd,
			String commandLabel, String[] args) {
		if (!sender.isOp())
			return true; // only whitelist admins are allowed to use /whitelist

		if (args.length < 1) {
			return false;
		}
		if (args[0].compareToIgnoreCase("help") == 0) {
			sender.sendMessage(ChatColor.YELLOW + "Commands:");
			sender.sendMessage(ChatColor.YELLOW
					+ "/whitelist reload  (reloads the whitelist and settings)");
			sender.sendMessage(ChatColor.YELLOW
					+ "/whitelist on|off  (actives/deactivates whitelist)");
			sender.sendMessage(ChatColor.YELLOW
					+ "/whitelist list  (list whitelist entries)");
			return true;
		}
		if (args[0].compareToIgnoreCase("reload") == 0) {
			if (reloadSettings())
				sender.sendMessage(ChatColor.GREEN
						+ "Settings and whitelist reloaded");
			else
				sender.sendMessage(ChatColor.RED
						+ "Could not reload whitelist...");
			return true;
		}
		if (args[0].compareToIgnoreCase("on") == 0) {
			setWhitelistActive(true);
			sender.sendMessage(ChatColor.GREEN + "Whitelist activated!");
			return true;
		}
		if (args[0].compareToIgnoreCase("off") == 0) {
			setWhitelistActive(false);
			sender.sendMessage(ChatColor.RED + "Whitelist deactivated!");
			return true;
		}
		if (args[0].compareToIgnoreCase("list") == 0
				&& !isListCommandDisabled()) {
			sender.sendMessage(ChatColor.YELLOW + "Players on whitelist: "
					+ ChatColor.GRAY + getFormatedAllowList());
			return true;
		}
		return false;
	}

	public boolean loadWhitelistSettings() {
		consoleLog("Trying to load whitelist and settings...");
		try {
			// 1. Load fWhitelist.properties
			Properties propConfig = new Properties();
			BufferedInputStream stream = new BufferedInputStream(
					new FileInputStream(m_Folder.getAbsolutePath()
							+ File.separator + FILE_CONFIG));
			propConfig.load(stream);
			m_KickMessage = propConfig.getProperty(PROP_KICKMESSAGE);
			if (m_KickMessage == null) {
				m_KickMessage = "";
			}
			m_WhitelistAdmins.clear();
			String rawAdminList = propConfig.getProperty(PROP_WHITELIST_ADMINS);
			if (rawAdminList != null) {
				String[] admins = rawAdminList.split(",");
				if (admins != null) {
					m_WhitelistAdmins.addAll(Arrays.asList(admins));
				}
			}
			String rawDisableListCommand = propConfig
					.getProperty(PROP_DISABLE_LIST);
			if (rawDisableListCommand != null) {
				m_IsListCommandDisabled = Boolean
						.parseBoolean(rawDisableListCommand);
			}

			// 2. Load database configuration and connect...
			this.sql = new MySQL(this, propConfig.getProperty(PROP_MYSQL_USER),
					propConfig.getProperty(PROP_MYSQL_PASS),
					propConfig.getProperty(PROP_MYSQL_HOST),
					propConfig.getProperty(PROP_MYSQL_PORT),
					propConfig.getProperty(PROP_MYSQL_DB));

			// 3. Load whitelist from SMF database
			m_WhitelistAllow.clear();

			ResultSet rs;
			rs = sql.trySelect("select member_name from "
					+ propConfig.getProperty(PROP_MYSQL_PREFIX)
					+ "members where id_group IN("
					+ propConfig.getProperty(PROP_SMF_ALLOWED_GROUPS) + ")");

			while (rs.next() != false) {
				String user = rs.getString("member_name");
				m_WhitelistAllow.add(user);
			}

			consoleLog("Whitelist Loaded");
		} catch (Exception ex) {
			consoleWarning("Failed to load whitelist");
			return false;
		}
		return true;
	}

	/*
	 * public boolean saveWhitelist() { try { BufferedWriter writer = new
	 * BufferedWriter( new FileWriter( (m_Folder.getAbsolutePath() +
	 * File.separator + FILE_WHITELIST))); for (String player :
	 * m_WhitelistAllow) { writer.write(player); writer.newLine(); }
	 * writer.close(); } catch (Exception ex) { consoleWarning(ex.getMessage());
	 * return false; } return true; }
	 */

	public boolean isAdmin(String playerName) {
		for (String admin : m_WhitelistAdmins) {
			if (admin.compareToIgnoreCase(playerName) == 0) {
				return true;
			}
		}
		return false;
	}

	public boolean isOnWhitelist(String playerName) {
		for (String player : m_WhitelistAllow) {
			if (player.compareToIgnoreCase(playerName) == 0) {
				return true;
			}
		}
		return false;
	}

	/*
	 * public boolean addPlayerToWhitelist(String playerName) { if
	 * (!isOnWhitelist(playerName)) { m_WhitelistAllow.add(playerName); return
	 * saveWhitelist(); } return false; }
	 * 
	 * public boolean removePlayerFromWhitelist(String playerName) { for (int i
	 * = 0; i < m_WhitelistAllow.size(); i++) { if
	 * (playerName.compareToIgnoreCase(m_WhitelistAllow.get(i)) == 0) {
	 * m_WhitelistAllow.remove(i); return saveWhitelist(); } } return false; }
	 */

	public boolean reloadSettings() {
		return loadWhitelistSettings();
	}

	public String getKickMessage() {
		return m_KickMessage;
	}

	public String getFormatedAllowList() {
		String result = "";
		for (String player : m_WhitelistAllow) {
			if (result.length() > 0) {
				result += ", ";
			}
			result += player;
		}
		return result;
	}

	public boolean isWhitelistActive() {
		return m_IsWhitelistActive;
	}

	public void setWhitelistActive(boolean isWhitelistActive) {
		m_IsWhitelistActive = isWhitelistActive;
	}

	public boolean isListCommandDisabled() {
		return m_IsListCommandDisabled;
	}

	/*public boolean needReloadWhitelist() {
		if (m_Watcher != null)
			return m_Watcher.wasFileModified();
		return false;
	}

	public void resetNeedReloadWhitelist() {
		if (m_Watcher != null)
			m_Watcher.resetFileModifiedState();
	}*/

	public void consoleLog(String msg) {
		log.info("[" + name + "] v" + version + " - " + msg);
	}

	public void consoleWarning(String msg) {
		log.warning("[" + name + "] v" + version + " - " + msg);
	}

	public void consoleError(String msg) {
		log.severe("[" + name + "] v" + version + " - " + msg);
	}
}
