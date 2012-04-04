package de.martindreier.heldenweb.export.ui;

import helden.Fehlermeldung;
import helden.plugin.werteplugin2.PluginHeld2;
import helden.plugin.werteplugin3.PluginHeldenWerteWerkzeug3;
import javax.swing.JFrame;
import de.martindreier.heldenweb.export.HeldenWebExportException;
import de.martindreier.heldenweb.export.Settings;
import de.martindreier.heldenweb.export.sync.Synchronizer;

public class ExportUIController
{
	public ExportUIController(PluginHeld2[] helden, PluginHeldenWerteWerkzeug3 werkzeug)
	{
		this(null, helden, werkzeug);
	}

	public ExportUIController(JFrame parent, PluginHeld2[] helden, PluginHeldenWerteWerkzeug3 werkzeug)
	{
		try
		{
			boolean optionsLoaded = Settings.getSettings().reload();
			if (!optionsLoaded)
			{
				// First execution, show options dialog first
				new SettingsDialog(parent, true).open();
			}
			Synchronizer synchronizer = new Synchronizer(helden, werkzeug);
			new ExportDialog(parent, synchronizer).open();
		}
		catch (HeldenWebExportException exception)
		{
			Fehlermeldung meldung = new Fehlermeldung();
			meldung.handle(exception);
		}
	}
}
