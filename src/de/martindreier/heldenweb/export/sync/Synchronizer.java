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

public class Synchronizer
{

	@SuppressWarnings("unused")
	private PluginHeld2[]								helden;
	private PluginHeldenWerteWerkzeug3	werkzeug;
	private HttpClient									client;
	private Cache												cache;
	private boolean											skipSpecialAbilities	= false;

	public Synchronizer(PluginHeld2[] helden, PluginHeldenWerteWerkzeug3 werkzeug)
	{
		this.helden = helden;
		this.werkzeug = werkzeug;
		Settings settings = Settings.getSettings();
		client = new HttpClient(settings.getServer(), Integer.parseInt(settings.getPort()), settings.getPath(), false, true);
		cache = new Cache(client);
	}

	public void sync() throws HeldenWebExportException
	{
		werkzeug.setAktivenHeld(werkzeug.getSelectesHeld());
		syncBaseData();
		syncHeld();
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
		cache.synchronizeAttributes(werkzeug);

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
		cache.synchronizeTalentTypes(talentarten);
		cache.synchronizeTalents(talente, werkzeug);

		// Vorteile
		cache.synchronizeAdvantages(werkzeug);

		// Sonderfertigkeiten
		if (!skipSpecialAbilities)
		{
			try
			{
				cache.synchronizeSpecialAbilities(werkzeug);
			}
			catch (StackOverflowError e)
			{
				// Sonderbehandlung für Fehler in alten Versionen der Helden-Software
				skipSpecialAbilities = true;
			}
		}

		// Zauber
		cache.synchronizeSpells(werkzeug);
	}

	private void syncHeld() throws HeldenWebExportException
	{
		cache.synchronizeHeroData(werkzeug);
		UUID heldId = cache.getKey(CacheKey.HELD, werkzeug.getHeldenID());
		cache.synchronizeHeroAttributes(heldId, werkzeug);
		if (!skipSpecialAbilities)
		{
			cache.synchronizeHeroSpecialAbilities(heldId, werkzeug);
		}
		cache.synchronizeHeroTalents(heldId, werkzeug);
		cache.synchronizeHeroAdvantages(heldId, werkzeug);
		cache.synchronizeHeroSpells(heldId, werkzeug);
	}

	public String getHeroName()
	{
		return werkzeug.getSelectesHeld().toString();
	}

}
