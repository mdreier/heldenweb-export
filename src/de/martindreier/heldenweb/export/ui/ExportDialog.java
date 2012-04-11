package de.martindreier.heldenweb.export.ui;

import java.awt.BorderLayout;
import java.awt.Window;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import de.martindreier.heldenweb.export.sync.Synchronizer;
import de.martindreier.heldenweb.export.ui.actions.CloseAction;
import de.martindreier.heldenweb.export.ui.actions.OptionsAction;
import de.martindreier.heldenweb.export.ui.actions.SyncAction;

public class ExportDialog extends AbstractDialog
{
	/**
	 * For serialization.
	 */
	private static final long	serialVersionUID	= 8010518368352442412L;
	/**
	 * Action: Start synchronization.
	 */
	private Action						syncAction;
	/**
	 * Action: Close dialog.
	 */
	private Action						closeAction;
	/**
	 * Action: Show settings dialog.
	 */
	private Action						optionsAction;
	/**
	 * The synchronizer.
	 */
	private Synchronizer			synchronizer;

	/**
	 * Create a new export dialog.
	 * 
	 * @param parent
	 *          The parent window, or <code>null</code> if this dialog has no
	 *          parent.
	 * @param synchronizer
	 */
	public ExportDialog(Window parent, Synchronizer synchronizer)
	{
		super(parent, "HeldenWeb Export");
		this.synchronizer = synchronizer;
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
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout(10, 10));

		// Hero information
		JLabel label = new JLabel();
		label.setText(MessageFormat.format("Exportiere {0} nach HeldenWeb", synchronizer.getHeroName()));
		mainPanel.add(label, BorderLayout.PAGE_START);

		// Synchronize button
		JButton button = new JButton(syncAction);;
		ProtectionDomain currentProtectionDomain = getClass().getProtectionDomain();
		CodeSource codeSource = currentProtectionDomain.getCodeSource();
		URL iconUrl = new URLClassLoader(new URL[] { codeSource.getLocation() }).getResource("icons/heldenweb-export.png");
		if (iconUrl != null)
		{
			Icon icon = new ImageIcon(iconUrl);
			button.setIcon(icon);
		}
		mainPanel.add(button);
		parent.add(mainPanel);
	}

	/**
	 * Create the actions for this dialog.
	 */
	@Override
	protected void createActions()
	{
		syncAction = new SyncAction(this, synchronizer);
		closeAction = new CloseAction(this);
		optionsAction = new OptionsAction(this);
	}

	@Override
	protected void addButtonsToButtonBar(Box buttonBar)
	{
		// buttonBar.add(new JButton(syncAction));
		buttonBar.add(new JButton(optionsAction));
		buttonBar.add(new JButton(closeAction));
	}
}
