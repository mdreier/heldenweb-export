package de.martindreier.heldenweb.export;

import helden.plugin.HeldenWertePlugin3;
import helden.plugin.werteplugin2.PluginHeld2;
import helden.plugin.werteplugin3.PluginHeldenWerteWerkzeug3;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import de.martindreier.heldenweb.export.ui.ExportUIController;

public class HeldenWebExport implements HeldenWertePlugin3
{

	@Override
	public void doWork(JFrame arg0)
	{
		// This method is no longer called.
	}

	@Override
	public ImageIcon getIcon()
	{
		ProtectionDomain currentProtectionDomain = getClass().getProtectionDomain();
		CodeSource codeSource = currentProtectionDomain.getCodeSource();
		URL iconUrl = new URLClassLoader(new URL[] { codeSource.getLocation() }).getResource("icons/heldenweb-export.png");
		if (iconUrl == null)
		{
			return null;
		}
		return new ImageIcon(iconUrl);
	}

	@Override
	public String getMenuName()
	{
		return "Export nach HeldenWeb";
	}

	@Override
	public String getToolTipText()
	{
		return "Exportiere den gewählten Helden nach HeldenWeb und halte ihn auf dem neuesten Stand";
	}

	@Override
	public String getType()
	{
		return HeldenWertePlugin3.DATEN3;
	}

	@Override
	public void doWork(JFrame parentWindow, PluginHeld2[] heroes, PluginHeldenWerteWerkzeug3 tool)
	{
		new ExportUIController(parentWindow);
	}

}
