package de.martindreier.heldenweb.export.ui;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Window;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JPanel;

public abstract class AbstractDialog extends JDialog
{
	/**
	 * For serialization.
	 */
	private static final long	serialVersionUID	= 11260060686990960L;

	protected AbstractDialog(Window parent, String title)
	{
		super(parent, title, ModalityType.APPLICATION_MODAL);
		if (parent != null)
		{
			centerOnParent(parent);
		}
	}

	/**
	 * Center the location of this dialog on the parent window.
	 * 
	 * @param parent
	 *          The parent window.
	 */
	private void centerOnParent(Window parent)
	{
		Point parentLocation = parent.getLocation();
		Point newLocation = new Point(parentLocation.x, parentLocation.y);
		newLocation.x += parent.getWidth() / 2 - getWidth() / 2;
		newLocation.y += parent.getHeight() / 2 - getHeight() / 2;
		setLocation(newLocation);
	}

	/**
	 * Initialize the UI.
	 */
	private void init()
	{
		// Base panel
		JPanel root = new JPanel();
		getContentPane().add(root);
		root.setLayout(new BorderLayout(5, 5));

		// Actions
		createActions();

		// Main dialog area
		createDialogArea(root);

		// Buttons
		createButtonBar(root);

		pack();
	}

	/**
	 * Create the actions.
	 */
	protected abstract void createActions();

	/**
	 * Create the dialog area.
	 * 
	 * @param root
	 */
	protected abstract void createDialogArea(JPanel root);

	/**
	 * Create the dialog's button bar.
	 * 
	 * @param parent
	 *          The main panel. Should have a {@link BorderLayout}.
	 */
	private void createButtonBar(JPanel parent)
	{
		Box buttonBar = new Box(BoxLayout.LINE_AXIS);
		buttonBar.setAlignmentX(0.5f);

		addButtonsToButtonBar(buttonBar);

		parent.add(buttonBar, BorderLayout.PAGE_END);
	}

	/**
	 * Add the buttons to the button bar.
	 * 
	 * @param buttonBar
	 *          The button bar container.
	 */
	protected abstract void addButtonsToButtonBar(Box buttonBar);

	/**
	 * Initialize and open the dialog.
	 */
	public void open()
	{
		init();
		setVisible(true);
	}

	/**
	 * Close the dialog.
	 */
	public void close()
	{
		setVisible(false);
	}
}
