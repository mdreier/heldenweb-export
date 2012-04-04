package de.martindreier.heldenweb.export.ui.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import de.martindreier.heldenweb.export.sync.Synchronizer;

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

	public SyncAction(Synchronizer synchronizer)
	{
		super("Exportieren");
		this.synchronizer = synchronizer;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		synchronizer.sync();
	}

}
