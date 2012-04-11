package de.martindreier.heldenweb.export.sync;

import helden.plugin.werteplugin.PluginTalent;
import helden.plugin.werteplugin2.PluginHeld2;
import helden.plugin.werteplugin3.PluginHeldenWerteWerkzeug3;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import de.martindreier.heldenweb.export.HeldenWebExportException;
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
		// Disabled because of bug.
		// cache.synchronizeSpecialAbilities(werkzeug);

		// Zauber
		cache.synchronizeSpells(werkzeug);
	}

	private void syncHeld() throws HeldenWebExportException
	{
		cache.synchronizeHeroData(werkzeug);
	}

	public String getHeroName()
	{
		return werkzeug.getSelectesHeld().toString();
	}

}
