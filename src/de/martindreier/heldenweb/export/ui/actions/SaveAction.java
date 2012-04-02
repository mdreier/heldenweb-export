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
public class SaveAction extends AbstractAction
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
	 * Callback to perform the saving.
	 */
	private ISaveCallback			callback;
	/**
	 * Close the dialog even if the saving has failed.
	 */
	private boolean						closeOnFailure;

	/**
	 * Create a new {@link SaveAction}.
	 * 
	 * @param window
	 *          The window to be closed.
	 * @param callback
	 *          The callback to perform the save.
	 * @param closeOnFailure
	 *          Close the dialog even if the saving has failed.
	 */
	public SaveAction(Window window, ISaveCallback callback, boolean closeOnFailure)
	{
		super("Speichern");
		if (window == null)
		{
			throw new IllegalArgumentException("Window must not be null");
		}
		this.window = window;
		this.callback = callback;
		this.closeOnFailure = closeOnFailure;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		// Call doSave to perform the save. Close the dialog of doSave()
		// returned true or if the dialog should also be closed on a
		// failure.
		if (callback.doSave() || closeOnFailure)
		{
			window.setVisible(false);
		}
	}

	/**
	 * Callback to perform the save.
	 * 
	 * @author Martin Dreier <martin@martindreier.de>
	 * 
	 */
	public static interface ISaveCallback
	{
		/**
		 * Perform the save.
		 * 
		 * @return <code>true</code> if the saving was sucessful, <code>false</code>
		 *         if saving failed.
		 */
		public boolean doSave();
	}
}
