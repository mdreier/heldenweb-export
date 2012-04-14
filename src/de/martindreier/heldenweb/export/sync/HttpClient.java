package de.martindreier.heldenweb.export.sync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HttpClient
{
	public static enum Method
	{
		GET, POST, PUT, DELETE
	}

	private String	server;
	private boolean	secure;
	private int			port;
	private String	basePath;

	/**
	 * Create a new HTTP client.
	 * 
	 * @param server
	 *          The hostname of the server.
	 * @param port
	 *          The port number of the server.
	 * @param basePath
	 *          The base path.
	 * @param secure
	 *          Set to <code>true</code> to use a secure connection to the server.
	 */
	public HttpClient(String server, int port, String basePath, boolean secure, boolean redirect)
	{
		this.server = server;
		this.port = port;
		if (!basePath.startsWith("/"))
		{
			basePath = "/" + basePath;
		}
		if (!basePath.endsWith("/"))
		{
			basePath = basePath + "/";
		}
		this.basePath = basePath;
		this.secure = secure;
		if (redirect)
		{
			HttpURLConnection.setFollowRedirects(true);
		}
	}

	public Response post(String path, Map<String, String> queryParameters, String contentType, String content)
					throws HttpClientException
	{
		return sendRequest(path, queryParameters, content, contentType, Method.POST);
	}

	/**
	 * Send a GET request.
	 * 
	 * @param path
	 *          The path.
	 * @param queryParameters
	 *          Query parameters.
	 * @return The server's response to the request.
	 * @throws HttpClientException
	 */
	public Response get(String path, Map<String, String> queryParameters) throws HttpClientException
	{
		return sendRequest(path, queryParameters, null, null, Method.GET);
	}

	/**
	 * Send a request to the server.
	 * 
	 * @param path
	 *          The path. The {@link #basePath} will be prepended to this path.
	 * @param queryParameters
	 *          Query parameters. May be <code>null</code>.
	 * @param data
	 *          The data to send to the server. If this is <code>null</code>, no
	 *          data will be send to the server.
	 * @param contentType
	 *          The content type of the <code>data</code>. May be
	 *          <code>null</code> but should be set if any data is sent.
	 * @param method
	 *          The request method.
	 * @return The server's response to the request.
	 * @throws HttpClientException
	 */
	private Response sendRequest(String path, Map<String, String> queryParameters, String data, String contentType,
					Method method) throws HttpClientException
	{
		URL url;
		try
		{
			url = buildUrl(path, queryParameters);
		}
		catch (MalformedURLException exception)
		{
			throw new HttpClientException(exception);
		}
		HttpURLConnection connection = null;
		try
		{
			// Set up connection properties
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod(method.toString());
			connection.setDoInput(true);
			if (data != null)
			{
				// Prepare sending of data
				connection.setDoOutput(true);
				if (contentType != null)
				{
					connection.setRequestProperty("Content-Type", contentType);
				}
				connection.setRequestProperty("Content-Length", "" + Integer.toString(data.getBytes().length));
			}
			// Open connection
			connection.connect();

			// Send data, if required
			if (data != null)
			{
				OutputStream output = null;
				try
				{
					output = connection.getOutputStream();
					OutputStreamWriter writer = new OutputStreamWriter(output);
					writer.write(data);
					writer.flush();
					output.flush();
					writer.close();
				}
				finally
				{
					if (output != null)
					{
						output.close();
					}
				}
			}

			// Build response
			Response response = new Response();
			response.resonseCode = connection.getResponseCode();
			response.responseMessage = connection.getResponseMessage();
			response.responseHeaders = connection.getHeaderFields();

			if (response.resonseCode < 500)
			{
				// Read data from server
				InputStream in = null;
				try
				{
					in = connection.getInputStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(in));
					String line = null;
					StringBuilder recievedData = new StringBuilder();
					while ((line = reader.readLine()) != null)
					{
						recievedData.append(line).append("\n");
					}
					reader.close();
					response.responseContent = recievedData.toString();
				}
				finally
				{
					if (in != null)
					{
						in.close();
					}
				}
			}
			return response;
		}
		catch (IOException exception)
		{
			throw new HttpClientException(exception);
		}
		finally
		{
			if (connection != null)
			{
				connection.disconnect();
			}
		}
	}

	/**
	 * Build a URL to the {@link #server}.
	 * 
	 * @param path
	 *          The path component.
	 * @param queryParameters
	 *          The query paramters.
	 * @return The finished URL. The path will be the <code>path</code> parameter,
	 *         prepended by the {@link #basePath} and appended by the
	 *         <code>queryParameters</code>.
	 * @throws MalformedURLException
	 *           Thrown if the result is not a valid URL.
	 */
	private URL buildUrl(String path, Map<String, String> queryParameters) throws MalformedURLException
	{
		if (path.startsWith("/"))
		{
			path = path.substring(1);
		}

		StringBuilder fullPath = new StringBuilder();
		fullPath.append(basePath);
		fullPath.append(path);
		if (queryParameters != null && queryParameters.size() > 0)
		{
			fullPath.append("?");
			Iterator<String> iter = queryParameters.keySet().iterator();
			while (iter.hasNext())
			{
				String key = iter.next();
				fullPath.append(key);
				fullPath.append("=");
				fullPath.append(queryParameters.get(key));
				if (iter.hasNext())
				{
					fullPath.append("&");
				}
			}
		}
		return new URL(secure ? "https" : "http", server, port, fullPath.toString());
	}

	public class Response
	{
		private int												resonseCode;
		private String										responseMessage;
		private String										responseContent;
		private Map<String, List<String>>	responseHeaders;

		public int getResponseCode()
		{
			return resonseCode;
		}

		public String getResponseMessage()
		{
			return responseMessage;
		}

		public String getResponseContent()
		{
			return responseContent;
		}

		public Map<String, List<String>> getResponseHeaders()
		{
			return responseHeaders;
		}

	}
}
