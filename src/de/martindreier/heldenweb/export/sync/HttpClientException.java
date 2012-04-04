package de.martindreier.heldenweb.export.sync;

public class HttpClientException extends Exception
{

	/**
	 * For serialization.
	 */
	private static final long	serialVersionUID	= -795930055875336760L;

	/**
	 * @param message
	 * @param cause
	 */
	public HttpClientException(String message, Throwable cause)
	{
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 */
	public HttpClientException(String message)
	{
		super(message);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param cause
	 */
	public HttpClientException(Throwable cause)
	{
		super(cause);
		// TODO Auto-generated constructor stub
	}

}
