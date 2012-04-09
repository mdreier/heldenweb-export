package de.martindreier.heldenweb.export.ui.actions;

import java.awt.Window;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import de.martindreier.heldenweb.export.HeldenWebExportException;
import de.martindreier.heldenweb.export.sync.Synchronizer;
import de.martindreier.heldenweb.export.ui.HeldenWebFehler;

public class SyncAction extends AbstractAction
{

	/**
	 * For serialization.
	 */
	private static final long	serialVersionUID	= 2756015464916424583L;
	/**
	 * The synchronizer.
	 */
	private Synchronizer			synchronizer;
	/**
	 * Parent window of the action.
	 */
	private Window						parent;

	public SyncAction(Window parent, Synchronizer synchronizer)
	{
		super("Exportieren");
		this.synchronizer = synchronizer;
		this.parent = parent;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		try
		{
			synchronizer.sync();
		}
		catch (HeldenWebExportException exception)
		{
			HeldenWebFehler.handleError(parent, "Fehler beim Export des Helden", exception);
		}
	}
}
