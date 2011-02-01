/*
 * Copyright (C) 2011 Antony <admin@districtmine.net>
 *
 * This file is part of the Bukkit m_Plugin SMFWhite.
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

package com.bukkit.ant59.smfwhite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Logger;

import com.mysql.jdbc.Driver;

public class MySQL {
	private final SMFWHite m_Plugin;

	private Connection MySQLConnection;
	private Statement MySQLStatement;
	@SuppressWarnings("unused")
	private Driver MySQLDriver;
	private String MySQLUser, MySQLPass, MySQLHost, MySQLPort, MySQLDataBase,
			MySQLURL;

	public MySQL(SMFWHite instance) {
		m_Plugin = instance;
		System.out.println("Whitelist: Executing scheduled whitelist reload.");
		try {
			MySQLUser = Settings.getSetting("settings/kapital.ini",
					"mysql_user", "root")[0];
			MySQLPass = Settings.getSetting("settings/kapital.ini",
					"mysql_pass", "passwd")[0];
			MySQLHost = Settings.getSetting("settings/kapital.ini",
					"mysql_host", "localhost")[0];
			MySQLPort = Settings.getSetting("settings/kapital.ini",
					"mysql_port", "3306")[0];
			MySQLDataBase = Settings.getSetting("settings/kapital.ini",
					"mysql_db", "minecraft")[0];
			MySQLURL = "jdbc:mysql://" + MySQLHost + ":" + MySQLPort + "/"
					+ MySQLDataBase;
			Class.forName("com.mysql.jdbc.Driver");
			MySQLConnection = DriverManager.getConnection(MySQLURL, MySQLUser,
					MySQLPass);
			MySQLStatement = MySQLConnection.createStatement();
			MySQLConnection.setAutoCommit(true);
		} catch (Exception e) {
			m_Plugin.consoleWarning("MySQL connection failed: " + e.toString());
		} finally {
		}
	}

	public Connection getConnection() {
		return MySQLConnection;
	}

	public Statement getStatement() {
		return MySQLStatement;
	}

	public void tryUpdate(String sqlString) {
		try {
			getStatement().executeUpdate(sqlString);
		} catch (Exception e) {
			m_Plugin.consoleWarning("The following statement failed: "
					+ sqlString);
			m_Plugin.consoleWarning("Statement failed: " + e.toString());
		} finally {
		}
	}

	public ResultSet trySelect(String sqlString) {
		try {
			System.out.println(getStatement().toString());
			return getStatement().executeQuery(sqlString);
		} catch (Exception e) {
			m_Plugin.consoleWarning("The following statement failed: "
					+ sqlString);
			m_Plugin.consoleWarning("Statement failed: " + e.toString());
		} finally {
		}
		return null;
	}
}
