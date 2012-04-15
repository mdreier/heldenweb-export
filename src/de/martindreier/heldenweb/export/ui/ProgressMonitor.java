package de.martindreier.heldenweb.export.ui;

public interface ProgressMonitor
{
	public void subtaskDone();

	public void done();

	public void start(int steps);

	public void startTask(String name);

	public void startSubtask(String name, int steps);

	public void step();
}
