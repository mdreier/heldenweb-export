package de.martindreier.heldenweb.export.ui.actions;

import java.awt.Window;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import de.martindreier.heldenweb.export.ui.SettingsDialog;

/**
 * This action opens the {@link SettingsDialog settings dialog}.
 * 
 * @author Martin Dreier <martin@martindreier.de>
 * 
 */
public class OptionsAction extends AbstractAction
{

	/**
	 * For serialization.
	 */
	private static final long	serialVersionUID	= 7489019567211233567L;
	/**
	 * Parent window for the dialog.
	 */
	private Window						parent;

	/**
	 * @param parent
	 */
	public OptionsAction(Window parent)
	{
		super("Optionen");
		if (parent == null)
		{
			throw new IllegalArgumentException("Parent frame may not be null");
		}
		this.parent = parent;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		new SettingsDialog(parent).setVisible(true);
	}

}
