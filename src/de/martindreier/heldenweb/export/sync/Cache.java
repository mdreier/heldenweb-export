package de.martindreier.heldenweb.export.sync;

import helden.plugin.werteplugin.PluginTalent;
import helden.plugin.werteplugin3.PluginHeldenWerteWerkzeug3;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import de.martindreier.heldenweb.export.HeldenWebExportException;
import de.martindreier.heldenweb.export.sync.HttpClient.Response;

/**
 * The cache to store the IDs of objects.
 * 
 * @author Martin Dreier <martin@martindreier.de>
 * 
 */
public class Cache
{
	/**
	 * Cache keys to identify the type of object which is cached.
	 * 
	 * @author Martin Dreier <martin@martindreier.de>
	 * 
	 */
	private static enum CacheKey
	{
		TALENT, EIGENSCHAFT, TALENTART
	};

	/**
	 * Maps attribute name to short name. This map is immutable.
	 */
	private static final Map<String, String>	attributeNameShort;

	static
	{
		// Initialize attribute names
		Map<String, String> attNames = new HashMap<String, String>();
		attNames.put("Astralenergie", "AsE");
		attNames.put("Attacke", "AT");
		attNames.put("Ausdauer", "AU");
		attNames.put("Charisma", "CH");
		attNames.put("Fernkampf-Basis", "FK");
		attNames.put("Fingerfertigkeit", "FF");
		attNames.put("Gewandtheit", "GE");
		attNames.put("Initiative", "INI");
		attNames.put("Intuition", "IN");
		attNames.put("Karmaenergie", "KaE");
		attNames.put("Klugheit", "KL");
		attNames.put("Konstitution", "KO");
		attNames.put("Körperkraft", "KK");
		attNames.put("Lebensenergie", "LE");
		attNames.put("Magieresistenz", "MR");
		attNames.put("Mut", "MU");
		attNames.put("Parade", "PA");
		attNames.put("Sozialstatus", "SO");
		attributeNameShort = Collections.unmodifiableMap(attNames);
	}

	/**
	 * The client to communicate with the server.
	 */
	private HttpClient												client;

	/**
	 * Name to ID mapping. The key is the {@link CacheKey} identifying the type
	 * concatenated with the name of the object.
	 */
	private Map<String, UUID>									keys;

	/**
	 * Document builder factory.
	 */
	private DocumentBuilderFactory						factory				= DocumentBuilderFactory.newInstance();	;

	/**
	 * XPath factory.
	 */
	private XPathFactory											xpathFactory	= XPathFactory.newInstance();

	/**
	 * Create a new cache instance.
	 * 
	 * @param client
	 *          The client to communicate with the server.
	 */
	public Cache(HttpClient client)
	{
		this.client = client;
		keys = new HashMap<String, UUID>();
	}

	/**
	 * Synchronize talents. After this method has been executed, all talents in
	 * the map are stored on the server and all IDs on the server are known to the
	 * cache.
	 * 
	 * @param talents
	 *          The talents to synchronize.
	 * @param tool
	 *          The tool.
	 * @throws HeldenWebExportException
	 */
	public void synchronizeTalents(Map<String, PluginTalent> talents, PluginHeldenWerteWerkzeug3 tool)
					throws HeldenWebExportException
	{
		try
		{
			getIdsFromServer(CacheKey.TALENT, "talent", "Talente.xml");
		}
		catch (HeldenWebExportException exception)
		{
			throw new HeldenWebExportException("Fehler beim synchronisieren der Talente", exception);
		}

		for (String talentName : talents.keySet())
		{
			if (getKey(CacheKey.TALENT, talentName) == null)
			{
				sendTalentToServer(talentName, talents.get(talentName), tool);
			}
		}
	}

	/**
	 * Send an object to the server.
	 * 
	 * @param rootElementName
	 *          The root element name of the created XML document.
	 * @param objectData
	 *          The attributes of the object, see
	 *          {@link #buildXmlDocument(String, Map)}.
	 * @param url
	 *          The URL where the data should be <code>POST</code>ed.
	 * @param idXpath
	 *          The XPath expression where the object's new ID can be found in the
	 *          XML response.
	 * @return The UUID of the created object.
	 * @throws HeldenWebExportException
	 */
	private UUID sendToServer(String rootElementName, Map<String, String> objectData, String url, String idXpath)
					throws HeldenWebExportException
	{
		// Build the XML document
		String document = buildXmlDocument(rootElementName, objectData);
		try
		{
			// Post the data to the server
			Response response = client.post(url, null, "application/xml", document);
			// Parse the response
			Document talentsDocument = parseXML(response.getResponseContent());
			// Get the UUID
			XPath xpath = xpathFactory.newXPath();
			String id = xpath.evaluate(idXpath, talentsDocument);
			try
			{
				return UUID.fromString(id);
			}
			catch (IllegalArgumentException exception)
			{
				throw new HeldenWebExportException(MessageFormat.format("Server lieferte ungültige ID: {0}", id), exception);
			}
		}
		catch (HttpClientException exception)
		{
			throw new HeldenWebExportException("Daten konnten nicht auf dem Server gespeichert werden", exception);
		}
		catch (XPathExpressionException exception)
		{
			throw new HeldenWebExportException("Fehler beim Lesen der gespeicherten Daten", exception);
		}
	}

	/**
	 * Save a talent on the server.
	 * 
	 * @param talentName
	 *          The name of the talent.
	 * @param pluginTalent
	 *          The talent data.
	 * @param tool
	 *          The tool.
	 * @throws HeldenWebExportException
	 */
	private void sendTalentToServer(String talentName, PluginTalent pluginTalent, PluginHeldenWerteWerkzeug3 tool)
					throws HeldenWebExportException
	{
		Map<String, String> talentMap = new HashMap<String, String>();
		talentMap.put("name", talentName);
		if (tool.getSprachKomplexitaet(pluginTalent) != null && tool.getSprachKomplexitaet(pluginTalent).length() > 0)
		{
			talentMap.put("sprachkomplexitaet", tool.getSprachKomplexitaet(pluginTalent));
		}
		if (pluginTalent.getProbe() != null && pluginTalent.getProbe().length == 3)
		{
			for (int i = 0; i < 3; i++)
			{
				UUID probeId = getKey(CacheKey.EIGENSCHAFT, pluginTalent.getProbe()[i]);
				if (probeId == null)
				{
					throw new IllegalStateException(MessageFormat.format("Talent {0} referenziert unbekannte Eigenschaft {1}",
									talentName, pluginTalent.getProbe()[i]));
				}
				talentMap.put("probe" + (i + 1), probeId.toString());
			}
		}
		if (pluginTalent.getTalentart() != null)
		{
			UUID talentTypeId = getKey(CacheKey.TALENTART, pluginTalent.getTalentart());
			if (talentTypeId == null)
			{
				throw new IllegalStateException(MessageFormat.format("Talent {0} referenziert unbekannte Talentart {1}",
								talentName, pluginTalent.getTalentart()));
			}
			talentMap.put("talentart_id", talentTypeId.toString());
		}

		UUID talentId = sendToServer("Talent", talentMap, "Talente.xml", "/talent/id");
		keys.put(CacheKey.TALENT + talentName, talentId);
	}

	/**
	 * Build a simple XML element. The structure will be: <code>
	 * &lt;rootElementName&gt;<br>
	 * &nbsp;&nbsp;&lt;elements[0].key&gt;elements[0].value&lt;/elements[0].key&gt;<br>
	 * &nbsp;&nbsp;&lt;elements[1].key&gt;elements[1].value&lt;/elements[1].key&gt;<br>
	 * &nbsp;&nbsp;&lt;elements[2].key&gt;elements[2].value&lt;/elements[2].key&gt;<br>
	 * &nbsp;&nbsp;...
	 * &lt;/rootElementName&gt;<br>
	 * </code>
	 * 
	 * @param rootElementName
	 *          The root element name.
	 * @param elements
	 *          The elements to be added to the document. Keys are used as element
	 *          names and must conform to the restrictions for XML element names.
	 *          Values are used as the elements' text values and are encoded
	 *          before transmission.
	 * @return The completed document.
	 * @throws HeldenWebExportException
	 *           Error while creating the document.
	 */
	private String buildXmlDocument(String rootElementName, Map<String, String> elements) throws HeldenWebExportException
	{
		try
		{
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.newDocument();
			Element talentRoot = document.createElement(rootElementName);
			document.appendChild(talentRoot);
			for (String elementName : elements.keySet())
			{
				Element element = document.createElement(elementName);
				element.setTextContent(elements.get(elementName));
				talentRoot.appendChild(element);
			}
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(document), new StreamResult(writer));
			writer.flush();
			return writer.toString();
		}
		catch (ParserConfigurationException exception)
		{
			throw new HeldenWebExportException("Fehler beim Erstellen des XML-Dokuments", exception);
		}
		catch (TransformerConfigurationException exception)
		{
			throw new HeldenWebExportException("Fehler beim Erstellen des XML-Dokuments", exception);
		}
		catch (TransformerException exception)
		{
			throw new HeldenWebExportException("Fehler beim Erstellen des XML-Dokuments", exception);
		}
	}

	/**
	 * Read all current IDs from the server and put them in the cache.
	 * 
	 * @param cacheKey
	 *          The cache key for this type of data.
	 * @param elementName
	 *          The element name in the resulting XML document.
	 * @param url
	 *          The URL where the data is requested.
	 * @throws HeldenWebExportException
	 *           Error while reading the data from the server.
	 */
	private void getIdsFromServer(CacheKey cacheKey, String elementName, String url) throws HeldenWebExportException
	{
		try
		{
			// Request data from server
			Response response = client.get(url, null);
			// Check response code (2xx = OK)
			if (response.getResponseCode() < 200 || response.getResponseCode() >= 300)
			{
				handleHttpError(response);
			}
			// Parse XML response
			Document talentsDocument = parseXML(response.getResponseContent());
			// Get correct elements
			NodeList talentElements = talentsDocument.getElementsByTagName(elementName);
			// Check each element
			for (int index = 0; index < talentElements.getLength(); index++)
			{
				String id = null;
				String name = null;

				// Get all child elements
				Element talentElement = (Element) talentElements.item(index);
				NodeList children = talentElement.getChildNodes();
				// Find name and id
				for (int innerIndex = 0; innerIndex < children.getLength(); innerIndex++)
				{
					Node node = children.item(innerIndex);
					if (node.getNodeName().equalsIgnoreCase("id"))
					{
						id = node.getTextContent();
					}
					else if (node.getNodeName().equalsIgnoreCase("name"))
					{
						name = node.getTextContent();
					}
				}

				// Check for completeness and parse id
				if (name == null || id == null)
				{
					throw new HeldenWebExportException("Dokument ist nicht vollständig");
				}
				UUID talentId;
				try
				{
					talentId = UUID.fromString(id);
				}
				catch (IllegalArgumentException e)
				{
					throw new HeldenWebExportException(MessageFormat.format("ID {0} ist keine gültige UUID", id));
				}
				// Put into cache
				keys.put(cacheKey + name, talentId);
			}
		}
		catch (HttpClientException exception)
		{
			throw new HeldenWebExportException(MessageFormat.format("Fehler bei der Kommunikation mit dem Server (URL: {0})",
							url), exception);
		}
	}

	/**
	 * Read an XML document into a {@link Document}.
	 * 
	 * @param content
	 *          The XML content.
	 * @return The document.
	 * @throws HeldenWebExportException
	 *           If an error occurs while parsing the document.
	 */
	private Document parseXML(String content) throws HeldenWebExportException
	{
		try
		{
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new ByteArrayInputStream(content.getBytes(Charset.forName("UTF-8"))));
			return document;
		}
		catch (ParserConfigurationException exception)
		{
			throw new HeldenWebExportException("XML Document Builder konnte nicht erstellt werden", exception);
		}
		catch (SAXException exception)
		{
			throw new HeldenWebExportException("Der Server lieferte kein gültiges XML-Dokument", exception);
		}
		catch (IOException exception)
		{
			throw new HeldenWebExportException("Fehler beim Lesen des XML-Dokuments", exception);
		}
	}

	/**
	 * Handle an error returned from the server.
	 * 
	 * @param response
	 *          The response.
	 * @throws HeldenWebExportException
	 *           The resulting exception.
	 */
	private void handleHttpError(Response response) throws HeldenWebExportException
	{
		Throwable cause = null;
		if (response.getResponseContent() != null && response.getResponseContent().trim().length() > 0)
		{
			cause = new HeldenWebExportException(MessageFormat.format("Server-Antwort: {0}", response.getResponseContent()));
		}
		String message = MessageFormat.format("Fehlerhafte Anfrage; Antwort {0} ({1})", response.getResponseMessage(),
						response.getResponseCode());
		throw new HeldenWebExportException(message, cause);
	}

	/**
	 * Get the cached key for an object.
	 * 
	 * @param cacheKey
	 *          The cache key.
	 * @param objectName
	 *          The object's name.
	 * @return The key for the object, or <code>null</code> if no key is cached
	 *         for this object.
	 */
	public UUID getKey(CacheKey cacheKey, String objectName)
	{
		return keys.get(cacheKey + objectName);
	}

	/**
	 * Clear the complete cache.
	 */
	public void clearCache()
	{
		keys.clear();
	}

	public void synchronizeAttributes(PluginHeldenWerteWerkzeug3 werkzeug) throws HeldenWebExportException
	{
		try
		{
			getIdsFromServer(CacheKey.EIGENSCHAFT, "eigenschaft", "Eigenschaften.xml");
		}
		catch (HeldenWebExportException exception)
		{
			throw new HeldenWebExportException("Eigenschaften konnten nicht vom Server gelesen werden", exception);
		}

		for (String attributeName : werkzeug.getEigenschaftsbezeichner())
		{
			if (getKey(CacheKey.EIGENSCHAFT, attributeName) == null)
			{
				sendAttributeToServer(attributeName);
			}
		}
	}

	private void sendAttributeToServer(String attributeName) throws HeldenWebExportException
	{
		Map<String, String> attributeData = new HashMap<String, String>();
		attributeData.put("kurzbezeichnung", mapAttributeNameToShortName(attributeName));
		attributeData.put("name", attributeName);

		UUID id = sendToServer("Eigenschaft", attributeData, "Eigenschaften.xml", "/eigenschaft/id");
		keys.put(CacheKey.EIGENSCHAFT + attributeName, id);
	}

	public void synchronizeTalentTypes(Set<String> talentarten) throws HeldenWebExportException
	{
		try
		{
			getIdsFromServer(CacheKey.TALENTART, "talentart", "Talentarten.xml");
		}
		catch (HeldenWebExportException exception)
		{
			throw new HeldenWebExportException("Talentarten konnten nicht vom Server gelesen werden", exception);
		}

		for (String talentTypeName : talentarten)
		{
			if (getKey(CacheKey.TALENTART, talentTypeName) == null)
			{
				sendTalentTypeToServer(talentTypeName);
			}
		}

	}

	private void sendTalentTypeToServer(String talentTypeName) throws HeldenWebExportException
	{
		Map<String, String> talentTypeData = new HashMap<String, String>();
		talentTypeData.put("name", talentTypeName);

		UUID id = sendToServer("Talentart", talentTypeData, "Talentarten.xml", "/talentart/id");
		keys.put(CacheKey.TALENTART + talentTypeName, id);
	}

	public void synchronizeAdvantages(PluginHeldenWerteWerkzeug3 werkzeug) throws HeldenWebExportException
	{
		// TODO Auto-generated method stub

	}

	/**
	 * Map an attribute name to it's short version.
	 * 
	 * @param attributeName
	 *          The attribute name.
	 * @return The short version.
	 */
	private String mapAttributeNameToShortName(String attributeName)
	{
		if (attributeNameShort.containsKey(attributeName))
		{
			return attributeNameShort.get(attributeName);
		}
		return attributeName.substring(0, 2).toUpperCase();
	}

	public void synchronizeSpecialAbilities(PluginHeldenWerteWerkzeug3 werkzeug) throws HeldenWebExportException
	{
		// TODO Auto-generated method stub

	}

	public void synchronizeSpells(PluginHeldenWerteWerkzeug3 werkzeug) throws HeldenWebExportException
	{
		// TODO Auto-generated method stub

	}
}
