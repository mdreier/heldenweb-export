package de.martindreier.heldenweb.export.ui;

import helden.Fehlermeldung;
import javax.swing.JFrame;
import de.martindreier.heldenweb.export.HeldenWebExportException;
import de.martindreier.heldenweb.export.Settings;

public class ExportUIController
{
	public ExportUIController()
	{
		this(null);
	}

	public ExportUIController(JFrame parent)
	{
		try
		{
			boolean optionsLoaded = Settings.getSettings().reload();
			if (!optionsLoaded)
			{
				// First execution, show options dialog first
				new SettingsDialog(parent, true).setVisible(true);
			}
			new ExportDialog(parent).setVisible(true);
		}
		catch (HeldenWebExportException exception)
		{
			Fehlermeldung meldung = new Fehlermeldung();
			meldung.handle(exception);
		}
	}
}
