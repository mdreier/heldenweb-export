package de.martindreier.heldenweb.export.ui;

import java.awt.GridLayout;
import java.awt.Window;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import de.martindreier.heldenweb.export.HeldenWebExportException;
import de.martindreier.heldenweb.export.Settings;
import de.martindreier.heldenweb.export.ui.actions.CloseAction;
import de.martindreier.heldenweb.export.ui.actions.SaveAction;
import de.martindreier.heldenweb.export.ui.actions.SaveAction.ISaveCallback;

public class SettingsDialog extends AbstractDialog implements ISaveCallback
{

	private static enum Setting
	{
		SERVER, PORT, PATH, USER, PASSWORD
	}

	/**
	 * For serialization.
	 */
	private static final long					serialVersionUID	= -2985475064107783533L;
	/**
	 * Action: Close window.
	 */
	private Action										closeAction;
	/**
	 * Action: Save settings.
	 */
	private Action										saveAction;
	/**
	 * Maps setting to input field.
	 */
	private Map<Setting, JTextField>	mappings;

	/**
	 * Create a new settings dialog.
	 * 
	 * @param parent
	 *          The parent window, or <code>null</code> if this dialog has no
	 *          parent.
	 */
	public SettingsDialog(Window parent)
	{
		this(parent, false);
	}

	/**
	 * Create a new settings dialog.
	 * 
	 * @param parent
	 *          The parent window, or <code>null</code> if this dialog has no
	 *          parent.
	 * @param forceSettings
	 *          Enforce that settings are saved. Disables the cancel button.
	 */
	public SettingsDialog(Window parent, boolean forceSettings)
	{
		super(parent, "HeldenWeb Export Einstellungen");
		mappings = new HashMap<SettingsDialog.Setting, JTextField>();
		if (forceSettings)
		{
			closeAction.setEnabled(false);
			setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		}
	}

	/**
	 * Create the dialog area.
	 * 
	 * @param parent
	 *          The main panel.
	 */
	@Override
	protected void createDialogArea(JPanel parent)
	{
		JPanel dialogArea = new JPanel();
		dialogArea.setLayout(new GridLayout(0, 2));
		parent.add(dialogArea);
		createSettingInput(dialogArea, "HeldenWeb Server",
						"Der Hostname des Servers, auf dem HeldenWeb läuft (z.B. www.meinServer.example)", Setting.SERVER, false);
		createSettingInput(dialogArea, "Port", "Der Port, auf dem der Server auf Verbindungen wartet (meist 80)",
						Setting.PORT, false);
		createSettingInput(dialogArea, "Pfad", "Der Phad zum HeldenWeb (z.B. /HeldenWeb)", Setting.PATH, false);
		createSettingInput(dialogArea, "Benutzername", "Der Benutzer, mit dem du dich am HeldenWeb-Server anmeldest",
						Setting.USER, false);
		createSettingInput(
						dialogArea,
						"Passwort",
						"Das Passwort, um dich am HeldenWeb-Server anzumelden. Du kannst dieses Feld leer lassen, dann wirst du bei der Synchronisation nach dem Passwort gefragt",
						Setting.PASSWORD, true);
		loadSettings();
	}

	/**
	 * Creates a label and an input field for a setting.
	 * 
	 * @param dialogArea
	 *          The parent component.
	 * @param name
	 *          The name to be shown on the label.
	 * @param tooltip
	 *          The tooltip text for the input field. May be <code>null</code> to
	 *          show no tooltip.
	 * @param settingsKey
	 *          The key for the setting.
	 * @param password
	 *          Show the input field as a password field.
	 */
	private void createSettingInput(JPanel dialogArea, String name, String tooltip, Setting settingsKey, boolean password)
	{
		JLabel label = new JLabel(name);
		dialogArea.add(label);
		if (tooltip != null)
		{
			label.setToolTipText(tooltip);
		}
		JTextField inputField;
		if (password)
		{
			inputField = new JPasswordField();
		}
		else
		{
			inputField = new JTextField();
		}
		dialogArea.add(inputField);
		if (tooltip != null)
		{
			inputField.setToolTipText(tooltip);
		}
		mappings.put(settingsKey, inputField);
	}

	/**
	 * Get the text for all input fields from the settings.
	 */
	private void loadSettings()
	{
		for (Setting setting : mappings.keySet())
		{
			JTextField associatedInput = mappings.get(setting);
			associatedInput.setText(getSetting(setting));
		}
	}

	/**
	 * Get the text for all input fields from the settings.
	 */
	private void saveSettings()
	{
		for (Setting setting : mappings.keySet())
		{
			JTextField associatedInput = mappings.get(setting);
			putSetting(setting, associatedInput.getText());
		}
	}

	/**
	 * Save a new value for a setting.
	 * 
	 * @param settingsKey
	 *          The setting.
	 * @param newValue
	 *          The new value.
	 */
	private void putSetting(Setting settingsKey, String newValue)
	{
		switch (settingsKey)
		{
			case SERVER:
				Settings.getSettings().setServer(newValue);
				break;
			case PORT:
				Settings.getSettings().setPort(newValue);
				break;
			case PATH:
				Settings.getSettings().setPath(newValue);
				break;
			case USER:
				Settings.getSettings().setUsername(newValue);
				break;
			case PASSWORD:
				Settings.getSettings().setPassword(newValue);
				break;
		}
	}

	/**
	 * Get the current saved value for a setting.
	 * 
	 * @param settingsKey
	 *          The setting.
	 * @return The current value.
	 */
	private String getSetting(Setting settingsKey)
	{
		switch (settingsKey)
		{
			case SERVER:
				return Settings.getSettings().getServer();
			case PORT:
				return Settings.getSettings().getPort();
			case PATH:
				return Settings.getSettings().getPath();
			case USER:
				return Settings.getSettings().getUsername();
			case PASSWORD:
				return Settings.getSettings().getPassword();
		}
		return "::" + settingsKey + "::";
	}

	/**
	 * Create the actions for this dialog.
	 */
	@Override
	protected void createActions()
	{
		saveAction = new SaveAction(this, this, false);
		closeAction = new CloseAction(this);
	}

	/**
	 * @see de.martindreier.heldenweb.export.ui.actions.SaveAction.ISaveCallback#doSave()
	 */
	@Override
	public boolean doSave()
	{
		saveSettings();
		try
		{
			Settings.getSettings().save();
		}
		catch (HeldenWebExportException exception)
		{
			HeldenWebFehler.handleError(this, "Einstellungen konnten nicht gespeichert werden", exception);
		}
		return true;
	}

	/**
	 * @see de.martindreier.heldenweb.export.ui.AbstractDialog#addButtonsToButtonBar(javax.swing.JPanel)
	 */
	@Override
	protected void addButtonsToButtonBar(ButtonBar buttonBar)
	{
		buttonBar.addButton(saveAction);
		buttonBar.addButton(closeAction);
	}
}
