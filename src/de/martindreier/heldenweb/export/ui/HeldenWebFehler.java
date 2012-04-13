package de.martindreier.heldenweb.export.ui;

import java.awt.BorderLayout;
import java.awt.Window;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import de.martindreier.heldenweb.export.ui.actions.CloseAction;

/**
 * Error dialog for the HeldenWeb Export extension.
 * 
 * @author Martin Dreier <martin@martindreier.de>
 * 
 */
public class HeldenWebFehler extends AbstractDialog
{

	/**
	 * For serialization.
	 */
	private static final long	serialVersionUID	= 6570044243781352337L;

	/**
	 * Open an error dialog in its own thread so that it doas not block the
	 * current thread's execution.
	 * 
	 * @param parent
	 *          Parent window. May be <code>null</code>.
	 * @param message
	 *          The error message.
	 * @param exception
	 *          The cause of the error. May be <code>null</code>.
	 */
	public static void handleError(Window parent, String message, Throwable exception)
	{
		final HeldenWebFehler fehlerDialog = new HeldenWebFehler(parent, message, exception);
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				fehlerDialog.open();
			}
		}).start();
	}

	/**
	 * The error message.
	 */
	private String									message;
	/**
	 * The throwable.
	 */
	private Throwable								exception;
	/**
	 * Listeners to the model.
	 */
	private Set<TreeModelListener>	listeners	= new HashSet<TreeModelListener>();
	private CloseAction							closeAction;

	private HeldenWebFehler(Window parent, String message, Throwable exception)
	{
		super(parent, "HeldenWeb Export - Fehler");
		this.message = message;
		this.exception = exception;
	}

	/**
	 * Tree model for the error messages.
	 * 
	 * This model presents an stack of {@link Throwable Throwables} with each
	 * cause as the child node to it's parent.
	 * 
	 * @author Martin Dreier <martin@martindreier.de>
	 * 
	 */
	private class ErrorModel implements TreeModel
	{

		/**
		 * @see javax.swing.tree.TreeModel#getRoot()
		 */
		@Override
		public Object getRoot()
		{
			return exception;
		}

		/**
		 * @see javax.swing.tree.TreeModel#getChild(java.lang.Object, int)
		 */
		@Override
		public Object getChild(Object parent, int index)
		{
			if (parent instanceof Throwable)
			{
				if (index != 0)
				{
					throw new ArrayIndexOutOfBoundsException(MessageFormat.format(
									"Index für Fehlerursache darf nicht ungleich 0 sein (ist: {0})", index));
				}
				Throwable parentException = (Throwable) parent;
				if (parentException.getCause() != null)
				{
					return parentException.getCause();
				}
			}
			return null;
		}

		/**
		 * @see javax.swing.tree.TreeModel#getChildCount(java.lang.Object)
		 */
		@Override
		public int getChildCount(Object parent)
		{
			if (parent instanceof Throwable && ((Throwable) parent).getCause() != null)
			{
				return 1;
			}
			return 0;
		}

		/**
		 * {@link Throwable Throwables} are leaves if they do not have a cause.
		 * 
		 * @see javax.swing.tree.TreeModel#isLeaf(java.lang.Object)
		 */
		@Override
		public boolean isLeaf(Object node)
		{
			return getChildCount(node) == 0;
		}

		/**
		 * This method is not implemented.
		 * 
		 * @deprecated This method is not implemented.
		 * 
		 * @see javax.swing.tree.TreeModel#valueForPathChanged(javax.swing.tree.TreePath,
		 *      java.lang.Object)
		 */
		@Deprecated
		@Override
		public void valueForPathChanged(TreePath path, Object newValue)
		{
			throw new IllegalArgumentException("Method not implemented");
		}

		/**
		 * 
		 * @see javax.swing.tree.TreeModel#getIndexOfChild(java.lang.Object,
		 *      java.lang.Object)
		 */
		@Override
		public int getIndexOfChild(Object parent, Object child)
		{
			if (parent instanceof Throwable && ((Throwable) parent).getCause() == child)
			{
				return 0;
			}
			throw new IllegalArgumentException(MessageFormat.format("{0} is not the cause of {1}", child, parent));
		}

		/**
		 * @see javax.swing.tree.TreeModel#addTreeModelListener(javax.swing.event.TreeModelListener)
		 */
		@Override
		public void addTreeModelListener(TreeModelListener l)
		{
			listeners.add(l);
		}

		/**
		 * @see javax.swing.tree.TreeModel#removeTreeModelListener(javax.swing.event.TreeModelListener)
		 */
		@Override
		public void removeTreeModelListener(TreeModelListener l)
		{
			listeners.remove(l);
		}

	}

	/**
	 * List model for an {@link Throwable Throwable's} stack trace.
	 * 
	 * @author Martin Dreier <martin@martindreier.de>
	 * 
	 */
	public class StackTraceModel implements ListModel
	{
		/**
		 * The {@link Throwable} this model is for.
		 */
		private Throwable							exception;
		/**
		 * Listeners to the model.
		 */
		private Set<ListDataListener>	listeners	= new HashSet<ListDataListener>();

		/**
		 * Create a new model.
		 * 
		 * @param exception
		 *          The {@link Throwable} this model is for.
		 */
		public StackTraceModel(Throwable exception)
		{
			this.exception = exception;
		}

		/**
		 * @see javax.swing.ListModel#getSize()
		 */
		@Override
		public int getSize()
		{
			return exception.getStackTrace().length;
		}

		/**
		 * @see javax.swing.ListModel#getElementAt(int)
		 */
		@Override
		public Object getElementAt(int index)
		{
			return exception.getStackTrace()[index];
		}

		/**
		 * @see javax.swing.ListModel#addListDataListener(javax.swing.event.ListDataListener)
		 */
		@Override
		public void addListDataListener(ListDataListener l)
		{
			listeners.add(l);
		}

		/**
		 * @see javax.swing.ListModel#removeListDataListener(javax.swing.event.ListDataListener)
		 */
		@Override
		public void removeListDataListener(ListDataListener l)
		{
			listeners.remove(l);
		}

	}

	@Override
	protected void createActions()
	{
		closeAction = new CloseAction(this);
	}

	@Override
	protected void createDialogArea(JPanel root)
	{
		// Main panel
		JPanel mainPanel = new JPanel(new BorderLayout(5, 5));

		// Top panel with error message
		JPanel top = new JPanel();
		top.add(new JLabel(message));
		mainPanel.add(top, BorderLayout.NORTH);

		// Center panel with exception tree and stack trace.
		if (exception != null)
		{
			JSplitPane exceptions = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
			final JTree tree = new JTree(new ErrorModel());
			tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			final JList stackTrace = new JList(new StackTraceModel(exception));
			stackTrace.setVisibleRowCount(10);
			JScrollPane stackPane = new JScrollPane(stackTrace);
			// stackPane.add(stackTrace);

			// Update stack trace when exception selection changes
			tree.addTreeSelectionListener(new TreeSelectionListener()
			{
				@Override
				public void valueChanged(TreeSelectionEvent e)
				{
					Object selection = tree.getLastSelectedPathComponent();
					if (selection instanceof Throwable)
					{
						stackTrace.setModel(new StackTraceModel((Throwable) selection));
					}
				}
			});

			exceptions.add(tree, JSplitPane.TOP);
			exceptions.add(stackPane, JSplitPane.BOTTOM);
			mainPanel.add(exceptions);
		}

		// Finish dialog
		root.add(mainPanel);
	}

	@Override
	protected void addButtonsToButtonBar(ButtonBar buttonBar)
	{
		buttonBar.addButton(closeAction);
	}
}
