package de.martindreier.heldenweb.export.sync;

import helden.plugin.werteplugin2.PluginHeld2;
import helden.plugin.werteplugin3.PluginHeldenWerteWerkzeug3;
import de.martindreier.heldenweb.export.Settings;

public class Synchronizer
{

	@SuppressWarnings("unused")
	private PluginHeld2[]								helden;
	private PluginHeldenWerteWerkzeug3	werkzeug;
	private HttpClient									client;
	private Cache												cache;

	public Synchronizer(PluginHeld2[] helden, PluginHeldenWerteWerkzeug3 werkzeug)
	{
		this.helden = helden;
		this.werkzeug = werkzeug;
		Settings settings = Settings.getSettings();
		client = new HttpClient(settings.getServer(), Integer.parseInt(settings.getPort()), settings.getPath(), false);
		cache = new Cache(client);
	}

	public void sync()
	{
		syncBaseData();
	}

	private void syncBaseData()
	{
		cache.syncronizeTalents();
	}

	public String getHeroName()
	{
		return werkzeug.getSelectesHeld().toString();
	}

}
