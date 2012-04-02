package de.martindreier.heldenweb.export.ui.actions;

import java.awt.Window;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

/**
 * This action closes a window.
 * 
 * @author Martin Dreier <martin@martindreier.de>
 * 
 */
public class CloseAction extends AbstractAction
{

	/**
	 * For serialization.
	 */
	private static final long	serialVersionUID	= 5446632041195729843L;
	/**
	 * Window instance.
	 */
	private Window						window;

	/**
	 * Create a new {@link CloseAction}.
	 * 
	 * @param window
	 *          The window to be closed.
	 */
	public CloseAction(Window window)
	{
		super("Schließen");
		if (window == null)
		{
			throw new IllegalArgumentException("Window must not be null");
		}
		this.window = window;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		window.setVisible(false);
	}

}
