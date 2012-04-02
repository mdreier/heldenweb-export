package de.martindreier.heldenweb.export;

import helden.Fehlermeldung;
import helden.framework.Einstellungen;
import helden.framework.pfadeEinstellungen.PfadNichtGefundenException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Properties;

public class Settings
{
	private static Settings							instance;
	private static final String					SETTINGS_PATH			= "pluginPfad";
	private static final String					SETTINGS_FILE			= "heldenWebExport.properties";
	private static final Fehlermeldung	FEHLER						= new Fehlermeldung();
	private static final String					SETTING_SERVER		= "server";
	private static final String					SETTING_PORT			= "port";
	private static final String					SETTING_PATH			= "path";
	private static final String					SETTING_USER			= "username";
	private static final String					SETTING_PASSWORD	= "password";

	private File												settingsFile;
	private Properties									settings;

	private static final Properties			DEFAULT_SETTINGS	= new Properties();

	static
	{
		DEFAULT_SETTINGS.setProperty(SETTING_SERVER, "localhost");
		DEFAULT_SETTINGS.setProperty(SETTING_PORT, "80");
		DEFAULT_SETTINGS.setProperty(SETTING_USER, "");
		DEFAULT_SETTINGS.setProperty(SETTING_PASSWORD, "");
	}

	private Settings()
	{
		// Get settings path from global settings
		String settingsPath = Einstellungen.getInstance().getPfade().getPfad(SETTINGS_PATH);
		if (settingsPath == null)
		{
			throw new PfadNichtGefundenException("Einstellungspfad nicht gefunden");
		}
		// Load settings for HeldenWeb
		File settingsDirectory = new File(settingsPath);
		if (!settingsDirectory.exists())
		{
			// Create directory if necessary
			settingsDirectory.mkdirs();
		}
		settingsFile = new File(settingsDirectory, SETTINGS_FILE);
	}

	/**
	 * Get the global settings instance.
	 * 
	 * @return The global instance.
	 */
	public static Settings getSettings()
	{
		synchronized (Settings.class)
		{
			if (instance == null)
			{
				instance = new Settings();
			}
			return instance;
		}
	}

	/**
	 * (Re-) load the settings from the settings file.
	 * 
	 * @return Returns <code>false</code> if the settings file does not exist and
	 *         no settings could be loaded. Returns <code>true</code> if settings
	 *         have been loaded from the settings file.
	 */
	public synchronized boolean reload() throws HeldenWebExportException
	{
		settings = new Properties(DEFAULT_SETTINGS);
		if (settingsFile.exists())
		{
			InputStream in = null;
			try
			{
				in = new FileInputStream(settingsFile);
				settings.load(in);
				return true;
			}
			catch (IOException e)
			{
				throw new HeldenWebExportException("Einstellungsdateien konnten nicht geladen werden", e);
			}
			finally
			{
				if (in != null)
				{
					try
					{
						in.close();
					}
					catch (IOException exception)
					{
						FEHLER.handle(exception);
					}
				}
			}
		}
		return false;
	}

	/**
	 * Save the settings.
	 * 
	 * @throws HeldenWebExportException
	 *           Thrown if the settings could not be saved.
	 */
	public synchronized void save() throws HeldenWebExportException
	{
		Writer out = null;
		try
		{
			out = new FileWriter(settingsFile);
			String comments = MessageFormat.format("HeldenWeb Export Einstellungen -- Gespeichert {0}", new Date());
			settings.store(out, comments);
			out.flush();
		}
		catch (IOException exception)
		{
			throw new HeldenWebExportException("Einstellungen konnten nicht gespeichert werden", exception);
		}
		finally
		{
			try
			{
				out.close();
			}
			catch (IOException exception)
			{
				FEHLER.handle(exception);
			}
		}
	}

	public String getServer()
	{
		return settings.getProperty(SETTING_SERVER);
	}

	public void setServer(String server)
	{
		settings.setProperty(SETTING_SERVER, server);
	}

	public String getPort()
	{
		return settings.getProperty(SETTING_PORT);
	}

	public void setPort(String port)
	{
		settings.setProperty(SETTING_PORT, port);
	}

	public String getPath()
	{
		return settings.getProperty(SETTING_PATH);
	}

	public void setPath(String path)
	{
		settings.setProperty(SETTING_PATH, path);
	}

	public String getUsername()
	{
		return settings.getProperty(SETTING_USER);
	}

	public void setUsername(String username)
	{
		settings.setProperty(SETTING_USER, username);
	}

	public String getPassword()
	{
		return settings.getProperty(SETTING_PASSWORD);
	}

	public void setPassword(String password)
	{
		settings.setProperty(SETTING_PASSWORD, password);
	}
}
