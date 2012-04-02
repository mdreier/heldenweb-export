package de.martindreier.heldenweb.export;

public class HeldenWebExportException extends Exception
{

	/**
	 * For serialization.
	 */
	private static final long	serialVersionUID	= 7158685642541845409L;

	public HeldenWebExportException(String message)
	{
		super(message);
	}

	public HeldenWebExportException(Throwable cause)
	{
		super(cause);
	}

	public HeldenWebExportException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
