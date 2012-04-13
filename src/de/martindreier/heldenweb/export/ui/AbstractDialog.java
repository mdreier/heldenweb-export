package de.martindreier.heldenweb.export.ui;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Window;
import javax.swing.Action;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

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
		root.setBorder(new EmptyBorder(5, 5, 5, 5));

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
		JPanel buttonBar = new JPanel();
		GroupLayout layout = new GroupLayout(buttonBar);
		buttonBar.setBorder(new EmptyBorder(5, 10, 5, 10));
		ParallelGroup group = layout.createParallelGroup(Alignment.CENTER, true);

		addButtonsToButtonBar(new ButtonBar(group));

		parent.add(buttonBar, BorderLayout.PAGE_END);
	}

	/**
	 * Wrapper for the button bar.
	 * 
	 * @author Martin Dreier <martin@martindreier.de>
	 * 
	 */
	protected class ButtonBar
	{

		/**
		 * The button bar control.
		 */
		private ParallelGroup	buttonBar;

		private boolean				firstComponent	= true;

		private ButtonBar(ParallelGroup group)
		{
			this.buttonBar = group;
		}

		/**
		 * Add a new button.
		 * 
		 * @param button
		 *          The button.
		 */
		public void addButton(JButton button)
		{
			if (firstComponent)
			{
				firstComponent = false;
			}
			else
			{
				buttonBar.addGap(5);
			}
			// button.setBorder(new CompoundBorder(new EmptyBorder(0, 5, 0, 5),
			// button.getBorder()));
			buttonBar.addComponent(button);
		}

		/**
		 * Add a new button.
		 * 
		 * @param action
		 *          The action which is called when the button is selected.
		 */
		public void addButton(Action action)
		{
			addButton(new JButton(action));
		}
	}

	/**
	 * Add the buttons to the button bar.
	 * 
	 * @param buttonBar
	 *          The button bar container.
	 */
	protected abstract void addButtonsToButtonBar(ButtonBar buttonBar);

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
