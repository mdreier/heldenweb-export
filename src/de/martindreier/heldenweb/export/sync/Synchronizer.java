package de.martindreier.heldenweb.export.sync;

import helden.plugin.werteplugin.PluginTalent;
import helden.plugin.werteplugin2.PluginHeld2;
import helden.plugin.werteplugin3.PluginHeldenWerteWerkzeug3;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import de.martindreier.heldenweb.export.HeldenWebExportException;
import de.martindreier.heldenweb.export.Settings;
import de.martindreier.heldenweb.export.sync.Cache.CacheKey;
import de.martindreier.heldenweb.export.ui.ProgressMonitor;

public class Synchronizer
{

	private static final int						SYNC_STEPS						= 13;
	@SuppressWarnings("unused")
	private PluginHeld2[]								helden;
	private PluginHeldenWerteWerkzeug3	werkzeug;
	private HttpClient									client;
	private Cache												cache;
	private boolean											skipSpecialAbilities	= false;
	private ProgressMonitor							monitor;

	public Synchronizer(PluginHeld2[] helden, PluginHeldenWerteWerkzeug3 werkzeug)
	{
		this.monitor = new NullProgressMonitor();
		this.helden = helden;
		this.werkzeug = werkzeug;
		Settings settings = Settings.getSettings();
		client = new HttpClient(settings.getServer(), Integer.parseInt(settings.getPort()), settings.getPath(), false, true);
		cache = new Cache(client);
	}

	public void sync() throws HeldenWebExportException
	{
		monitor.start(SYNC_STEPS);
		try
		{
			werkzeug.setAktivenHeld(werkzeug.getSelectesHeld());
			syncBaseData();
			syncHeld();
		}
		finally
		{
			monitor.done();
		}
	}

	/**
	 * Synchronize basic data. This includes:
	 * <ul>
	 * <li>Attributes (Eigenschaften)</li>
	 * <li>Talent Types (Talentarten)</li>
	 * <li>Talents (Talente)</li>
	 * <li>(Dis-)Advantages (Vor-/Nachteile)</li>
	 * <li>Special Abilities (Sonderfertigkeiten)</li>
	 * <li>Spells (Zauber)</li>
	 * <li>Equipment (Ausrüstung)</li>
	 * </ul>
	 * 
	 * @throws HeldenWebExportException
	 */
	private void syncBaseData() throws HeldenWebExportException
	{
		// Eigenschaften
		monitor.startTask("Übertrage Attribute");
		cache.synchronizeAttributes(werkzeug, monitor);
		monitor.step();

		// Talentarten und Talente
		String[] talentNamen = werkzeug.getTalenteAlsString();
		Map<String, PluginTalent> talente = new TreeMap<String, PluginTalent>();
		Set<String> talentarten = new HashSet<String>();
		for (String talentName : talentNamen)
		{
			PluginTalent talent = werkzeug.getTalent(talentName);
			talente.put(talentName, talent);
			talentarten.add(talent.getTalentart());
		}
		monitor.startTask("Übertrage Talentarten");
		cache.synchronizeTalentTypes(talentarten, monitor);
		monitor.step();

		monitor.startTask("Übertrage Talente");
		cache.synchronizeTalents(talente, werkzeug, monitor);
		monitor.step();

		// Vorteile
		monitor.startTask("Übertrage Vorteile");
		cache.synchronizeAdvantages(werkzeug, monitor);
		monitor.step();

		// Sonderfertigkeiten
		monitor.startTask("Übertrage Sonderfertigkeiten");
		if (!skipSpecialAbilities)
		{
			try
			{
				cache.synchronizeSpecialAbilities(werkzeug, monitor);
			}
			catch (StackOverflowError e)
			{
				// Sonderbehandlung für Fehler in alten Versionen der Helden-Software
				skipSpecialAbilities = true;
			}
		}
		monitor.step();

		// Zauber
		monitor.startTask("Übertrage Zauber");
		cache.synchronizeSpells(werkzeug, monitor);
		monitor.step();
	}

	private void syncHeld() throws HeldenWebExportException
	{
		monitor.startTask("Übertrage Held");
		cache.synchronizeHeroData(werkzeug);
		monitor.step();
		UUID heldId = cache.getKey(CacheKey.HELD, werkzeug.getHeldenID());
		cache.synchronizeHeroAttributes(heldId, werkzeug, monitor);
		if (!skipSpecialAbilities)
		{
			cache.synchronizeHeroSpecialAbilities(heldId, werkzeug, monitor);
		}
		cache.synchronizeHeroTalents(heldId, werkzeug, monitor);
		cache.synchronizeHeroAdvantages(heldId, werkzeug, monitor);
		cache.synchronizeHeroSpells(heldId, werkzeug, monitor);
	}

	public String getHeroName()
	{
		return werkzeug.getSelectesHeld().toString();
	}

	public void setProgressMonitor(ProgressMonitor monitor)
	{
		this.monitor = monitor;
	}

	private static class NullProgressMonitor implements ProgressMonitor
	{

		@Override
		public void subtaskDone()
		{}

		@Override
		public void done()
		{}

		@Override
		public void start(int steps)
		{}

		@Override
		public void startTask(String name)
		{}

		@Override
		public void startSubtask(String name, int steps)
		{}

		@Override
		public void step()
		{}

	}
}
