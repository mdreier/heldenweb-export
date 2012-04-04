package de.martindreier.heldenweb.export.sync;

import helden.Fehlermeldung;
import de.martindreier.heldenweb.export.sync.HttpClient.Response;

public class Cache
{

	private HttpClient	client;

	public Cache(HttpClient client)
	{
		this.client = client;
	}

	public void syncronizeTalents()
	{
		try
		{
			Response response = client.get("Talente.xml", null);
			if (response.getResonseCode() < 200 || response.getResonseCode() >= 300)
			{
				handleHttpError(response);
			}
		}
		catch (HttpClientException exception)
		{
			new Fehlermeldung().handle(exception);
		}
	}

	private void handleHttpError(Response response)
	{
		System.err.println(response.getResonseCode() + " - " + response.getResponseMessage());
	}
}
