package de.martindreier.heldenweb.export.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
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

	private static final int	PROGRESS_BAR_MAX	= 1000;
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
		// mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

		// Hero information
		JLabel label = new JLabel();
		label.setText(MessageFormat.format("Exportiere {0} nach HeldenWeb", synchronizer.getHeroName()));
		label.setHorizontalAlignment(SwingConstants.CENTER);
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

		JPanel progress = new JPanel(new GridLayout(0, 1));
		JLabel progressLabel = new JLabel();
		JProgressBar progressBar = new JProgressBar(0, PROGRESS_BAR_MAX);
		progress.add(progressLabel);
		progress.add(progressBar);
		mainPanel.add(progress, BorderLayout.SOUTH);
		synchronizer.setProgressMonitor(new ExportMonitor(button, progressBar, progressLabel));
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
	protected void addButtonsToButtonBar(ButtonBar buttonBar)
	{
		// buttonBar.add(new JButton(syncAction));
		buttonBar.addButton(optionsAction);
		buttonBar.addButton(closeAction);
	}

	private class ExportMonitor implements ProgressMonitor
	{
		private static final String	DEFAULT_PROGRESS_LABEL	= "Fortschritt";
		private String							mainTaskName;
		private JButton							exportButton;
		private JProgressBar				progressBar;
		private JLabel							taskLabel;
		private int									steps										= 1;
		private int									subtaskSteps						= 1;
		private float								currentStep							= 0;
		private boolean							inSubtask								= false;

		/**
		 * @param exportButton
		 * @param progressBar
		 * @param taskLabel
		 */
		public ExportMonitor(JButton exportButton, JProgressBar progressBar, JLabel taskLabel)
		{
			this.exportButton = exportButton;
			this.progressBar = progressBar;
			this.taskLabel = taskLabel;
		}

		@Override
		public void done()
		{
			exportButton.setEnabled(true);
			taskLabel.setText("Export beendet");
		}

		@Override
		public void start(int steps)
		{
			this.steps = steps;
			this.currentStep = 0;
			subtaskSteps = 1;
			exportButton.setEnabled(false);
			progressBar.setValue(0);
		}

		@Override
		public void step()
		{
			if (inSubtask)
			{
				currentStep += (float) 1 / (float) subtaskSteps;
			}
			else
			{
				currentStep += 1;
				Math.floor(currentStep);
			}
			if (currentStep > PROGRESS_BAR_MAX)
			{
				currentStep = PROGRESS_BAR_MAX;
			}
			updateProgressBar();
		}

		private void updateProgressBar()
		{
			progressBar.setValue(Math.round((currentStep / steps) * PROGRESS_BAR_MAX));
		}

		@Override
		public void startTask(String name)
		{
			if (name == null)
			{
				taskLabel.setText(DEFAULT_PROGRESS_LABEL);
			}
			else
			{
				taskLabel.setText(name);
			}
			mainTaskName = name;
		}

		@Override
		public void startSubtask(String name, int steps)
		{
			if (name == null)
			{
				if (mainTaskName == null)
				{
					taskLabel.setText(DEFAULT_PROGRESS_LABEL);
				}
				else
				{
					taskLabel.setText(mainTaskName);
				}
			}
			else
			{
				if (mainTaskName == null)
				{
					taskLabel.setText(name);
				}
				else
				{
					taskLabel.setText(mainTaskName + ": " + name);
				}
			}
			inSubtask = true;
			subtaskSteps = steps;
		}

		@Override
		public void subtaskDone()
		{
			subtaskSteps = 1;
			Math.ceil(currentStep);
			updateProgressBar();
		}

	}
}
