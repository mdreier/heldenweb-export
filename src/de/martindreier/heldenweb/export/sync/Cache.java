package de.martindreier.heldenweb.export.sync;

import helden.framework.geld.GeldBoerse;
import helden.framework.geld.Muenze;
import helden.plugin.werteplugin.HeldAngaben;
import helden.plugin.werteplugin.PluginFernkampfWaffe;
import helden.plugin.werteplugin.PluginHeld;
import helden.plugin.werteplugin.PluginRuestungsTeil;
import helden.plugin.werteplugin.PluginSonderfertigkeit;
import helden.plugin.werteplugin.PluginTalent;
import helden.plugin.werteplugin.PluginVorteil;
import helden.plugin.werteplugin.PluginZauberInfo;
import helden.plugin.werteplugin2.PluginFernkampfWaffe2;
import helden.plugin.werteplugin2.PluginGegenstand;
import helden.plugin.werteplugin2.PluginNahkampfWaffe2;
import helden.plugin.werteplugin2.PluginSchildParadewaffe;
import helden.plugin.werteplugin3.PluginHeldenWerteWerkzeug3;
import helden.plugin.werteplugin3.PluginZauber3;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
import de.martindreier.heldenweb.export.ui.ProgressMonitor;

/**
 * The cache to store the IDs of objects.
 * 
 * @author Martin Dreier <martin@martindreier.de>
 * 
 */
public class Cache
{
	private static final String	DB_FALSE	= "0";

	private static final String	DB_TRUE		= "1";

	/**
	 * Cache keys to identify the type of object which is cached.
	 * 
	 * @author Martin Dreier <martin@martindreier.de>
	 * 
	 */
	public static enum CacheKey
	{
		TALENT, EIGENSCHAFT, TALENTART, VORTEIL, SONDERFERTIGKEIT, ZAUBER, HELD, HELD_TALENT, HELD_VORTEIL,
		HELD_SONDERFERTIGKEIT, HELD_ZAUBER, HELD_EIGENSCHAFT, NAHKAMPFWAFFE, FERNKAMPFWAFFE, RUESTUNG, SCHILD, KAMPF,
		GEGENSTAENDE, HELD_GEGENSTAENDE, MUENZEN
	};

	/**
	 * Maps attribute name to short name. This map is immutable.
	 */
	private static final Map<String, String>	attributeNameShort;

	private static final String								ATTRIBUTE_SPEED	= "Geschwindigkeit";

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
	private DocumentBuilderFactory						factory					= DocumentBuilderFactory.newInstance();	;

	/**
	 * XPath factory.
	 */
	private XPathFactory											xpathFactory		= XPathFactory.newInstance();

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
	 * @param monitor
	 * @throws HeldenWebExportException
	 */
	public void synchronizeTalents(Map<String, PluginTalent> talents, PluginHeldenWerteWerkzeug3 tool,
					ProgressMonitor monitor) throws HeldenWebExportException
	{
		try
		{
			getIdsFromServer(CacheKey.TALENT, "talent", "Talente.xml");
		}
		catch (HeldenWebExportException exception)
		{
			throw new HeldenWebExportException("Fehler beim synchronisieren der Talente", exception);
		}

		monitor.startSubtask(null, talents.size());
		for (String talentName : talents.keySet())
		{
			if (getKey(CacheKey.TALENT, talentName) == null)
			{
				sendTalentToServer(talentName, talents.get(talentName), tool);
			}
			monitor.step();
		}
		monitor.subtaskDone();
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
	private UUID sendToServer(String rootElementName, Map<String, ? extends Object> objectData, String url, String idXpath)
					throws HeldenWebExportException
	{
		// Build the XML document
		String document = buildXmlDocument(rootElementName, objectData);
		try
		{
			// Post the data to the server
			Response response = client.post(url, null, "application/xml", document);
			if (response.getResponseCode() != 200)
			{
				throw new HeldenWebExportException(MessageFormat.format(
								"Daten konnten nicht zum Server gesendet werden: {0} ({1})", response.getResponseMessage(),
								response.getResponseCode()));
			}
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

		// Map checks to attributes
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

		// Map talent types
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
	private String buildXmlDocument(String rootElementName, Map<String, ? extends Object> elements)
					throws HeldenWebExportException
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
				Object elementContent = elements.get(elementName);
				if (elementContent == null)
				{
					element.setTextContent("");
				}
				else if (elementContent instanceof Map)
				{
					Map<?, ?> elementContents = (Map<?, ?>) elementContent;
					for (Object childName : elementContents.keySet())
					{
						Element child = document.createElement(childName.toString());
						Object childContent = elementContents.get(childName);
						child.setTextContent(childContent.toString());
						element.appendChild(child);
					}
				}
				else
				{
					element.setTextContent(elementContent.toString());
				}
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
	 * @param additionalIdentifiers
	 *          Additional identifying elements. These are added to the cache key.
	 *          The order of these is preserved.
	 * @throws HeldenWebExportException
	 *           Error while reading the data from the server.
	 */
	private void getIdsFromServer(CacheKey cacheKey, String elementName, String url, String... additionalIdentifiers)
					throws HeldenWebExportException
	{
		getIdsFromServer(cacheKey, elementName, url, true, additionalIdentifiers);
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
	 * @param useDefaultIdentifier
	 *          Use the field "name" as the identifier for the object. Fallback is
	 *          "id" if name is not in result document.
	 * @param additionalIdentifiers
	 *          Additional identifying elements. These are added to the cache key.
	 *          The order of these is preserved.
	 * @throws HeldenWebExportException
	 *           Error while reading the data from the server.
	 */
	private void getIdsFromServer(CacheKey cacheKey, String elementName, String url, boolean useDefaultIdentifier,
					String... additionalIdentifiers) throws HeldenWebExportException
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
			Map<String, String> additionalIdValues = new HashMap<String, String>();
			for (String additionalId : additionalIdentifiers)
			{
				additionalIdValues.put(additionalId, "");
			}
			Document talentsDocument = parseXML(response.getResponseContent());
			// Get correct elements
			NodeList talentElements = talentsDocument.getElementsByTagName(elementName);
			// Check each element
			for (int index = 0; index < talentElements.getLength(); index++)
			{
				String id = null;
				String name = null;

				// Get all child elements
				Element objectElement = (Element) talentElements.item(index);
				if (objectElement.getParentNode() == talentsDocument)
				{
					// Current node is root node. This happens if root node and child
					// nodes have the same name
					continue;
				}
				NodeList children = objectElement.getChildNodes();
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
					else if (additionalIdValues.containsKey(node.getNodeName()))
					{
						additionalIdValues.put(node.getNodeName(), node.getTextContent());
					}
				}

				// Check for completeness and parse id
				if (id == null)
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
				StringBuilder compoundName = new StringBuilder();
				if (useDefaultIdentifier)
				{
					if (name != null)
					{
						compoundName.append(name);
					}
					else
					{
						compoundName.append(id);
					}
				}
				for (String additionalId : additionalIdentifiers)
				{
					compoundName.append(additionalIdValues.get(additionalId));
				}
				// Put into cache
				keys.put(cacheKey + compoundName.toString(), talentId);
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
	public UUID getKey(CacheKey cacheKey, String... identifiers)
	{
		if (identifiers == null || identifiers.length == 0)
		{
			throw new IllegalArgumentException("At least one identifier is required for cache retrieval");
		}
		StringBuilder compoundKey = new StringBuilder(cacheKey.toString());
		for (String additionalId : identifiers)
		{
			compoundKey.append(additionalId);
		}
		return keys.get(compoundKey.toString());
	}

	/**
	 * Clear the complete cache.
	 */
	public void clearCache()
	{
		keys.clear();
	}

	public void synchronizeAttributes(PluginHeldenWerteWerkzeug3 werkzeug, ProgressMonitor monitor)
					throws HeldenWebExportException
	{
		try
		{
			getIdsFromServer(CacheKey.EIGENSCHAFT, "eigenschaft", "Eigenschaften.xml");
		}
		catch (HeldenWebExportException exception)
		{
			throw new HeldenWebExportException("Eigenschaften konnten nicht vom Server gelesen werden", exception);
		}

		monitor.startSubtask(null, werkzeug.getEigenschaftsbezeichner().length + 1);
		for (String attributeName : werkzeug.getEigenschaftsbezeichner())
		{
			if (getKey(CacheKey.EIGENSCHAFT, attributeName) == null)
			{
				sendAttributeToServer(attributeName);
			}
			monitor.step();
		}
		// Special treatment for speed
		if (getKey(CacheKey.EIGENSCHAFT, ATTRIBUTE_SPEED) == null)
		{
			Map<String, String> attributeData = new HashMap<String, String>();
			attributeData.put("kurzbezeichnung", mapAttributeNameToShortName("GS"));
			attributeData.put("name", ATTRIBUTE_SPEED);

			UUID id = sendToServer("Eigenschaft", attributeData, "Eigenschaften.xml", "/eigenschaft/id");
			keys.put(CacheKey.EIGENSCHAFT + "Geschwindigkeit", id);
		}
		monitor.subtaskDone();
	}

	private void sendAttributeToServer(String attributeName) throws HeldenWebExportException
	{
		Map<String, String> attributeData = new HashMap<String, String>();
		attributeData.put("kurzbezeichnung", mapAttributeNameToShortName(attributeName));
		attributeData.put("name", attributeName);

		UUID id = sendToServer("Eigenschaft", attributeData, "Eigenschaften.xml", "/eigenschaft/id");
		keys.put(CacheKey.EIGENSCHAFT + attributeName, id);
	}

	public void synchronizeTalentTypes(Set<String> talentarten, ProgressMonitor monitor) throws HeldenWebExportException
	{
		try
		{
			getIdsFromServer(CacheKey.TALENTART, "talentart", "Talentarten.xml");
		}
		catch (HeldenWebExportException exception)
		{
			throw new HeldenWebExportException("Talentarten konnten nicht vom Server gelesen werden", exception);
		}

		monitor.startSubtask(null, talentarten.size());
		for (String talentTypeName : talentarten)
		{
			if (getKey(CacheKey.TALENTART, talentTypeName) == null)
			{
				sendTalentTypeToServer(talentTypeName);
			}
			monitor.step();
		}
		monitor.subtaskDone();
	}

	private void sendTalentTypeToServer(String talentTypeName) throws HeldenWebExportException
	{
		Map<String, String> talentTypeData = new HashMap<String, String>();
		talentTypeData.put("name", talentTypeName);

		UUID id = sendToServer("Talentart", talentTypeData, "Talentarten.xml", "/talentart/id");
		keys.put(CacheKey.TALENTART + talentTypeName, id);
	}

	public void synchronizeAdvantages(PluginHeldenWerteWerkzeug3 werkzeug, ProgressMonitor monitor)
					throws HeldenWebExportException
	{
		try
		{
			getIdsFromServer(CacheKey.VORTEIL, "vorteil", "Vorteile.xml");
		}
		catch (HeldenWebExportException exception)
		{
			throw new HeldenWebExportException("Vorteile konnten nicht vom Server gelesen werden", exception);
		}

		String[] vorteile = werkzeug.getVorteileAlsString();
		monitor.startSubtask(null, vorteile.length);
		for (String vorteilName : vorteile)
		{
			if (getKey(CacheKey.VORTEIL, vorteilName) == null)
			{
				sendAdvantageToServer(vorteilName, werkzeug);
			}
			monitor.step();
		}
		monitor.subtaskDone();
	}

	private String booleanToDb(boolean value)
	{
		if (value)
		{
			return DB_TRUE;
		}
		return DB_FALSE;
	}

	private void sendAdvantageToServer(String vorteilName, PluginHeldenWerteWerkzeug3 werkzeug)
					throws HeldenWebExportException
	{
		PluginVorteil advantage = werkzeug.getVorteil(vorteilName);
		Map<String, String> attributeData = new HashMap<String, String>();
		attributeData.put("name", vorteilName);
		attributeData.put("auswahl", booleanToDb(advantage.isAuswahlVorteil()));
		attributeData.put("mehrfachauswahl", booleanToDb(advantage.isMehfachAuswahlVorteil()));
		attributeData.put("nachteil", booleanToDb(advantage.isNachteil()));
		attributeData.put("wertvorteil", booleanToDb(advantage.isWertVorteil()));

		UUID id = sendToServer("Vorteil", attributeData, "Vorteile.xml", "/vorteil/id");
		keys.put(CacheKey.VORTEIL + vorteilName, id);
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

	public void synchronizeSpecialAbilities(PluginHeldenWerteWerkzeug3 werkzeug, ProgressMonitor monitor)
					throws HeldenWebExportException
	{
		try
		{
			getIdsFromServer(CacheKey.SONDERFERTIGKEIT, "sonderfertigkeit", "Sonderfertigkeiten.xml");
		}
		catch (HeldenWebExportException exception)
		{
			throw new HeldenWebExportException("Sonderfertigkeiten konnten nicht vom Server gelesen werden", exception);
		}

		String[] sonderfertigkeiten = werkzeug.getSonderfertigkeitenAlsString();
		monitor.startSubtask(null, sonderfertigkeiten.length);
		for (String sonderfertigkeitName : sonderfertigkeiten)
		{
			if (getKey(CacheKey.SONDERFERTIGKEIT, sonderfertigkeitName) == null)
			{
				sendSpecialAbilityToServer(sonderfertigkeitName, werkzeug);
			}
			monitor.step();
		}
		monitor.subtaskDone();
	}

	private void sendSpecialAbilityToServer(String sonderfertigkeitName, PluginHeldenWerteWerkzeug3 werkzeug)
					throws HeldenWebExportException
	{
		PluginSonderfertigkeit specialAbility = werkzeug.getSonderfertigkeit(sonderfertigkeitName);
		Map<String, String> attributeData = new HashMap<String, String>();
		attributeData.put("name", sonderfertigkeitName);
		attributeData.put("art", Integer.toString(specialAbility.getArt()));
		// Map talent
		if (specialAbility.getTSTalent() != null)
		{
			UUID talentId = getKey(CacheKey.TALENT, specialAbility.getTSTalent().toString());
			if (talentId == null)
			{
				throw new HeldenWebExportException(MessageFormat.format(
								"Sonderfertigkeit {0} referenziert unbekanntes Talent {1}", sonderfertigkeitName, specialAbility
												.getTSTalent().toString()));
			}
			attributeData.put("talent_id", talentId.toString());
			putBoolean(attributeData, "elfenlied", specialAbility.istElfenlied());
			putBoolean(attributeData, "fernkampf_sonderfertigkeit", specialAbility.istFernkampfsonderfertigkeit());
			putBoolean(attributeData, "gelaendekunde", specialAbility.istGelaendekunde());
			putBoolean(attributeData, "hexenfluch", specialAbility.istHexenfluch());
			putBoolean(attributeData, "kampf_sonderfertigkeit", specialAbility.istKampfSonderfertigkeit());
			putBoolean(attributeData, "klerikal", specialAbility.istKlerikal());
			putBoolean(attributeData, "liturgie", specialAbility.istLiturgie());
			putBoolean(attributeData, "liturgiekenntnis", specialAbility.istLiturgiekenntnis());
			putBoolean(attributeData, "magisch", specialAbility.istMagisch());
			putBoolean(attributeData, "manoever", specialAbility.istManoever());
			putBoolean(attributeData, "merkmalskenntnis", specialAbility.istMerkmalskenntnis());
			putBoolean(attributeData, "nahkampf_sonderfertigkeit", specialAbility.istNahkampfsonderfertigkeit());
			putBoolean(attributeData, "repraesentation", specialAbility.istRepraesentation());
			putBoolean(attributeData, "ritual", specialAbility.istRitual());
			putBoolean(attributeData, "schamanen_ritualkenntnis", specialAbility.istSchamanenRitualkenntnis());
			putBoolean(attributeData, "talentspezialisierung", specialAbility.istTalentspezialisierung());
			putBoolean(attributeData, "waffenloser_kampfstil", specialAbility.istWaffenloseKampfstil());
		}

		UUID id = sendToServer("Sonderfertigkeit", attributeData, "Sonderfertigkeiten.xml", "/sonderfertigkeit/id");
		keys.put(CacheKey.SONDERFERTIGKEIT + sonderfertigkeitName, id);
	}

	private void putBoolean(Map<String, String> objectMap, String key, boolean value)
	{
		objectMap.put(key, booleanToDb(value));
	}

	public void synchronizeSpells(PluginHeldenWerteWerkzeug3 werkzeug, ProgressMonitor monitor)
					throws HeldenWebExportException
	{
		try
		{
			getIdsFromServer(CacheKey.ZAUBER, "zauber", "Zauber.xml");
		}
		catch (HeldenWebExportException exception)
		{
			throw new HeldenWebExportException("Fehler beim synchronisieren der Zauber", exception);
		}

		String[][] spells = werkzeug.getZauberAlsString();
		monitor.startSubtask(null, spells.length);
		for (String[] spell : spells)
		{
			String spellName = spell[0];
			String representation = spell[1];
			UUID spellId = getKey(CacheKey.ZAUBER, spellName, representation);
			if (spellId == null)
			{
				sendSpellToServer(spellName, representation, werkzeug);
			}
			monitor.step();
		}
		monitor.subtaskDone();
	}

	private void sendSpellToServer(String spellName, String representation, PluginHeldenWerteWerkzeug3 werkzeug)
					throws HeldenWebExportException
	{
		Map<String, String> attributeData = new HashMap<String, String>();
		PluginZauber3 spell = werkzeug.getZauber(spellName, representation);
		PluginZauberInfo spellInfo = werkzeug.getZauberInfo(spell);
		attributeData.put("name", spellName);
		attributeData.put("repraesentation", representation);
		attributeData.put("basiskomplexitaet", werkzeug.getBasisKomplexitaet(spell));
		attributeData.put("lernkomplexitaet", werkzeug.getLernKomplexitaet(spell));
		attributeData.put("hauszauber", booleanToDb(spell.isHauszauber()));
		StringBuilder merkmale = new StringBuilder();
		for (String merkmal : spell.getMerkmale())
		{
			merkmale.append(merkmal);
			merkmale.append("\n");
		}
		attributeData.put("merkmale", merkmale.toString());
		// Map checks to attributes
		for (int i = 0; i < 3; i++)
		{
			{
				UUID probeId = getKey(CacheKey.EIGENSCHAFT, spell.getProbe()[i]);
				if (probeId == null)
				{
					throw new IllegalStateException(MessageFormat.format("Zauber {0} referenziert unbekannte Eigenschaft {1}",
									spellName, spell.getProbe()[i]));
				}
				attributeData.put("probe" + (i + 1), probeId.toString());
			}
		}
		attributeData.put("kosten", spellInfo.getKosten());
		attributeData.put("reichweite", spellInfo.getReichweite());
		attributeData.put("wirkungsdauer", spellInfo.getWirkungsdauer());
		attributeData.put("zauberdauer", spellInfo.getZauberdauer());

		UUID id = sendToServer("Zauber", attributeData, "Zauber.xml", "/zauber/id");
		keys.put(CacheKey.ZAUBER + spellName + representation, id);
	}

	public void synchronizeHeroData(PluginHeldenWerteWerkzeug3 werkzeug) throws HeldenWebExportException
	{
		try
		{
			getIdsFromServer(CacheKey.HELD, "held", "Helden.xml", false, "identifier");
		}
		catch (HeldenWebExportException exception)
		{
			throw new HeldenWebExportException("Helden konnten nicht vom Server gelesen werden", exception);
		}

		sendHeroToServer(werkzeug);
	}

	private void sendHeroToServer(PluginHeldenWerteWerkzeug3 werkzeug) throws HeldenWebExportException
	{
		String heroIdentifier = werkzeug.getHeldenID();
		UUID heroId = getKey(CacheKey.HELD, heroIdentifier);

		PluginHeld hero = werkzeug.getSelectesHeld();
		HeldAngaben description = hero.getAngaben();

		boolean update = false;
		if (heroId != null)
		{
			update = true;
		}

		Map<String, Object> objectData = new HashMap<String, Object>();
		// Basic data
		objectData.put("identifier", heroIdentifier);
		objectData.put("name", hero.toString());
		objectData.put("geschlecht", hero.getGeschlechtString());
		objectData.put("kultur", hero.getKulturString());
		objectData.put("profession", hero.getProfessionString());
		objectData.put("rasse", hero.getRasseString());
		objectData.put("stufe", Integer.toString(hero.getStufe()));
		objectData.put("zaubersprueche", booleanToDb(hero.hatZaubersprueche()));

		// Description
		Map<String, String> descriptionData = new HashMap<String, String>();
		descriptionData.put("augenfarbe", description.getAugenFarbe());
		StringBuilder text = new StringBuilder();
		for (String line : description.getAussehenText())
		{
			text.append(line);
			text.append("\n");
		}
		descriptionData.put("aussehen", text.toString());
		text = new StringBuilder();
		for (String line : description.getFamilieText())
		{
			text.append(line);
			text.append("\n");
		}
		descriptionData.put("familie", text.toString());
		descriptionData.put("geburtstag", description.getGeburtstagString());
		descriptionData.put("gewicht", Integer.toString(description.getGewicht(false)));
		descriptionData.put("groesse", Integer.toString(description.getGroesse()));
		descriptionData.put("haarfarbe", description.getHaarFarbe());
		descriptionData.put("stand", description.getStand());
		descriptionData.put("titel", description.getTitel());
		objectData.put("Beschreibung", descriptionData);

		Map<String, String> valueData = new HashMap<String, String>();
		valueData.put("ap_gesamt", Integer.toString(hero.getAbenteuerpunkte()));
		valueData.put("ap_eingesetzt", Integer.toString(werkzeug.getEingestzteAbenteuerpunkte()));
		valueData.put("ap_verfuegbar", Integer.toString(werkzeug.getVerfuegbareAbenteuerpunkte()));
		valueData.put("gp_start", Integer.toString(description.getGPStart()));
		valueData.put("gp_rest", Integer.toString(description.getGPRest()));
		objectData.put("Wert", valueData);

		cleanEmptyString(objectData);

		if (update)
		{
			objectData.put("id", heroId.toString());
			sendToServer("Held", objectData, "Helden/edit/" + heroId.toString() + ".xml", "/held/id");
		}
		else
		{
			heroId = sendToServer("Held", objectData, "Helden.xml", "/held/id");
			keys.put(CacheKey.HELD + heroIdentifier, heroId);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void cleanEmptyString(Map<String, Object> data)
	{
		for (String key : data.keySet())
		{
			Object value = data.get(key);
			if (value instanceof Map)
			{
				for (Object innerKey : ((Map) value).keySet())
				{
					if (((Map) value).get(innerKey).toString().trim().equals(""))
					{
						((Map) value).put(innerKey, " ");
					}
				}
			}
			else
			{
				if (data.get(key).toString().trim().equals(""))
				{
					data.put(key, " ");
				}
			}
		}
	}

	public void synchronizeHeroSpecialAbilities(UUID heroId, PluginHeldenWerteWerkzeug3 werkzeug, ProgressMonitor monitor)
					throws HeldenWebExportException
	{
		getIdsFromServer(CacheKey.HELD_SONDERFERTIGKEIT, "heldenSonderfertigkeit", "HeldenSonderfertigkeiten.xml",
						"Helden-Sonderfertigkeiten", false, "held_id", "sonderfertigkeit_id");

		String[] sonderfertigkeiten = werkzeug.getSonderfertigkeitenAlsString();
		monitor.startSubtask("Sonderfertigkeiten", sonderfertigkeiten.length);
		for (String specialAbility : sonderfertigkeiten)
		{
			UUID specialAbilityId = getKey(CacheKey.SONDERFERTIGKEIT, specialAbility);
			String specialization = werkzeug.getSonderfertigkeit(specialAbility).getSpezialisierung();
			Map<String, String> data = new HashMap<String, String>();
			data.put("held_id", heroId.toString());
			data.put("sonderfertigkeit_id", specialAbilityId.toString());
			data.put("spezialisierung", specialization);
			sendMappingToServer(CacheKey.HELD_SONDERFERTIGKEIT, heroId, specialAbilityId, data, "HeldenSonderfertigkeit",
							"HeldenSonderfertigkeiten");
			monitor.step();
		}
		monitor.subtaskDone();
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
	 * @param errorLabel
	 *          Object name for the error message if an exception occurred.
	 * @param useDefaultIdentifier
	 *          Use the field "name" as the identifier for the object.
	 * @param additionalIdentifiers
	 *          Additional identifying elements. These are added to the cache key.
	 *          The order of these is preserved.
	 * @throws HeldenWebExportException
	 *           Error while reading the data from the server.
	 */
	private void getIdsFromServer(CacheKey cacheKey, String elementName, String url, String errorLabel,
					boolean useDefaultIdentifier, String... additionalIdentifiers) throws HeldenWebExportException
	{
		try
		{
			getIdsFromServer(cacheKey, elementName, url, useDefaultIdentifier, additionalIdentifiers);
		}
		catch (HeldenWebExportException exception)
		{
			throw new HeldenWebExportException(
							MessageFormat.format("{0} konnten nicht vom Server gelesen werden", errorLabel), exception);
		}
	}

	public void synchronizeHeroTalents(UUID heroId, PluginHeldenWerteWerkzeug3 werkzeug, ProgressMonitor monitor)
					throws HeldenWebExportException
	{
		getIdsFromServer(CacheKey.HELD_TALENT, "heldentalent", "HeldenTalenten.xml", "Helden-Talente", false, "held_id",
						"talent_id");

		String[] talente = werkzeug.getTalenteAlsString();
		monitor.startSubtask("Talente", talente.length);
		for (String talentName : talente)
		{
			UUID talentId = getKey(CacheKey.TALENT, talentName);
			if (talentId == null)
			{
				throw new HeldenWebExportException(MessageFormat.format("Held referenziert unbekanntes Talent {0}", talentName));
			}
			PluginTalent talent = werkzeug.getTalent(talentName);
			Map<String, String> data = new HashMap<String, String>();
			data.put("held_id", heroId.toString());
			data.put("talent_id", talentId.toString());
			data.put("talentwert", Integer.toString(werkzeug.getTalentwert(talent)));
			data.put("attacke", Integer.toString(werkzeug.getAttacke(talent)));
			data.put("parade", Integer.toString(werkzeug.getParade(talent)));
			data.put("behinderung", talent.getBehinderung());
			sendMappingToServer(CacheKey.HELD_TALENT, heroId, talentId, data, "HeldenTalent", "HeldenTalenten");
			monitor.step();
		}
		monitor.subtaskDone();
	}

	/**
	 * Send hero->object mapping to the server.
	 * 
	 * @param cacheKey
	 *          Cache key identifier.
	 * @param heroId
	 *          The hero ID.
	 * @param objectId
	 *          The object's ID.
	 * @param data
	 *          The data to be sent to the server.
	 * @param rootElementName
	 *          The root element name in the generated XML.
	 * @param url
	 *          The URL, without trailing &quot;.xml&quot;.
	 * @throws HeldenWebExportException
	 */
	private void sendEquipmentToServer(CacheKey cacheKey, UUID heroId, String equipmentName, Map<String, String> data,
					String rootElementName, String url) throws HeldenWebExportException
	{
		// Cut off extension
		if (url.endsWith(".xml"))
		{
			url = url.substring(0, url.length() - 4);
		}
		UUID key = getKey(cacheKey, equipmentName, heroId.toString());
		if (key == null)
		{
			key = sendToServer(rootElementName, data, url + ".xml", "/" + rootElementName.toLowerCase() + "/id");
			keys.put(cacheKey + equipmentName + heroId.toString(), key);
		}
		else
		{
			data.put("id", key.toString());
			sendToServer(rootElementName, data, url + "/edit/" + key.toString() + ".xml", "/" + rootElementName.toLowerCase()
							+ "/id");
		}
	}

	/**
	 * Send hero->object mapping to the server.
	 * 
	 * @param cacheKey
	 *          Cache key identifier.
	 * @param heroId
	 *          The hero ID.
	 * @param objectId
	 *          The object's ID.
	 * @param data
	 *          The data to be sent to the server.
	 * @param rootElementName
	 *          The root element name in the generated XML.
	 * @param url
	 *          The URL, without trailing &quot;.xml&quot;.
	 * @throws HeldenWebExportException
	 */
	private void sendMappingToServer(CacheKey cacheKey, UUID heroId, UUID objectId, Map<String, String> data,
					String rootElementName, String url) throws HeldenWebExportException
	{
		// Cut off extension
		if (url.endsWith(".xml"))
		{
			url = url.substring(0, url.length() - 4);
		}
		UUID key = getKey(cacheKey, heroId.toString(), objectId.toString());
		if (key == null)
		{
			key = sendToServer(rootElementName, data, url + ".xml", "/" + rootElementName.toLowerCase() + "/id");
			keys.put(cacheKey + heroId.toString() + objectId.toString(), key);
		}
		else
		{
			data.put("id", key.toString());
			sendToServer(rootElementName, data, url + "/edit/" + key.toString() + ".xml", "/" + rootElementName.toLowerCase()
							+ "/id");
		}
	}

	public void synchronizeHeroAdvantages(UUID heroId, PluginHeldenWerteWerkzeug3 werkzeug, ProgressMonitor monitor)
					throws HeldenWebExportException
	{
		getIdsFromServer(CacheKey.HELD_VORTEIL, "heldenvorteil", "HeldenVorteilen.xml", "Helden-Vorteile", false,
						"held_id", "vorteil_id");

		String[] vorteile = werkzeug.getVorteileAlsString();
		monitor.startSubtask("Vorteile", vorteile.length);
		for (String vorteilName : vorteile)
		{
			UUID vorteilId = getKey(CacheKey.VORTEIL, vorteilName);
			if (vorteilId == null)
			{
				throw new HeldenWebExportException(MessageFormat.format("Held referenziert unbekannten Vorteil {0}",
								vorteilName));
			}
			PluginVorteil vorteil = werkzeug.getVorteil(vorteilName);
			Map<String, String> data = new HashMap<String, String>();
			data.put("held_id", heroId.toString());
			data.put("vorteil_id", vorteilId.toString());
			data.put("wert", Integer.toString(vorteil.getWert()));
			sendMappingToServer(CacheKey.HELD_VORTEIL, heroId, vorteilId, data, "HeldenVorteil", "HeldenVorteilen");
			monitor.step();
		}
		monitor.subtaskDone();
	}

	public void synchronizeHeroSpells(UUID heroId, PluginHeldenWerteWerkzeug3 werkzeug, ProgressMonitor monitor)
					throws HeldenWebExportException
	{
		getIdsFromServer(CacheKey.HELD_ZAUBER, "heldenzauber", "HeldenZauber.xml", "Helden-Zauber", false, "held_id",
						"zauber_id");

		String[][] spells = werkzeug.getZauberAlsString();
		monitor.startSubtask("Zauber", spells.length);
		for (String[] spellData : spells)
		{
			UUID spellId = getKey(CacheKey.ZAUBER, spellData[0], spellData[1]);
			if (spellId == null)
			{
				throw new HeldenWebExportException(MessageFormat.format(
								"Held referenziert unbekannten Zauber {0} in Repräsentation {1}", spellData[0], spellData[1]));
			}
			PluginZauber3 spell = werkzeug.getZauber(spellData[0], spellData[1]);
			Map<String, String> data = new HashMap<String, String>();
			data.put("held_id", heroId.toString());
			data.put("zauber_id", spellId.toString());
			data.put("zauberfertigkeitswert", Integer.toString(werkzeug.getZauberInfo(spell).getZauberfertigkeitsWert()));
			sendMappingToServer(CacheKey.HELD_ZAUBER, heroId, spellId, data, "HeldenZauber", "HeldenZauber");
			monitor.step();
		}
		monitor.subtaskDone();
	}

	public void synchronizeHeroAttributes(UUID heroId, PluginHeldenWerteWerkzeug3 werkzeug, ProgressMonitor monitor)
					throws HeldenWebExportException
	{
		getIdsFromServer(CacheKey.HELD_EIGENSCHAFT, "eigenschaftenheld", "EigenschaftenHelden.xml", "Helden-Eigenschaften",
						false, "held_id", "eigenschaft_id");

		String[] attributes = werkzeug.getEigenschaftsbezeichner();
		monitor.startSubtask("Eigenschaften", werkzeug.getEigenschaftsbezeichner().length + 1);
		for (String attributeName : attributes)
		{
			UUID attributeId = getKey(CacheKey.EIGENSCHAFT, attributeName);
			if (attributeId == null)
			{
				throw new HeldenWebExportException(MessageFormat.format("Held referenziert unbekannte Eigenschaft {0}",
								attributeName));
			}
			Map<String, String> data = new HashMap<String, String>();
			data.put("held_id", heroId.toString());
			data.put("eigenschaft_id", attributeId.toString());
			data.put("wert", Integer.toString(werkzeug.getEigenschaftswert(attributeName)));
			sendMappingToServer(CacheKey.HELD_EIGENSCHAFT, heroId, attributeId, data, "EigenschaftenHeld",
							"EigenschaftenHelden");
			monitor.step();
		}

		// Special treatment for speed
		UUID attributeId = getKey(CacheKey.EIGENSCHAFT, ATTRIBUTE_SPEED);
		if (attributeId == null)
		{
			throw new HeldenWebExportException(MessageFormat.format("Held referenziert unbekannte Eigenschaft {0}",
							ATTRIBUTE_SPEED));
		}
		Map<String, String> data = new HashMap<String, String>();
		data.put("held_id", heroId.toString());
		data.put("eigenschaft_id", attributeId.toString());
		data.put("wert", Integer.toString(werkzeug.getGeschwindigkeit()));
		sendMappingToServer(CacheKey.HELD_EIGENSCHAFT, heroId, attributeId, data, "EigenschaftenHeld",
						"EigenschaftenHelden");
		monitor.step();
		monitor.subtaskDone();
	}

	public void syncMeleeWeapons(UUID heldId, PluginHeldenWerteWerkzeug3 werkzeug, ProgressMonitor monitor)
					throws HeldenWebExportException
	{
		getIdsFromServer(CacheKey.NAHKAMPFWAFFE, "nahkampfwaffe", "Nahkampfwaffen.xml", "Nahkampfwaffen", true, "held_id");

		PluginNahkampfWaffe2[] waffen = werkzeug.getAusruestung2().getNahkampfWaffen();
		monitor.startSubtask("Nahkampfwaffen", waffen.length);
		for (PluginNahkampfWaffe2 waffe : waffen)
		{
			if (waffe == null)
			{
				// Not all weapons may be set
				continue;
			}
			UUID talentId = getKey(CacheKey.TALENT, waffe.getBenutztesTalent().getBezeichnung());
			if (talentId == null)
			{
				throw new HeldenWebExportException(MessageFormat.format(
								"Nahkampfwaffe {0} referenziert unbekanntes Talent {1}", waffe.getName(), waffe.getBenutztesTalent()
												.getBezeichnung()));
			}

			Map<String, String> data = new HashMap<String, String>();
			data.put("held_id", heldId.toString());
			data.put("talent_id", talentId.toString());
			data.put("name", waffe.getName());
			data.put("attacke", Integer.toString(waffe.getAttacke()));
			data.put("parade", Integer.toString(waffe.getParade()));
			data.put("trefferpunkte", tpToString(waffe.getTrefferpunkte()));
			data.put("trefferpunkte_final", tpToString(waffe.getEndTP()));
			data.put("koerperkraftzuschlag",
							String.format("%d/%d", waffe.getKoerperkraftzuschlag()[0], waffe.getKoerperkraftzuschlag()[1]));
			data.put("bruchfaktor_minimal", Integer.toString(waffe.getBF()[0]));
			data.put("bruchfaktor_aktuell", Integer.toString(waffe.getBF()[1]));
			data.put("inimodifikator", Integer.toString(waffe.getINIMod()));
			data.put("distanzklasse", arrayToString(waffe.getDistanzklasse()));
			data.put("ausdauerschaden", booleanToDb(waffe.isSchadensartAusdauer()));
			data.put("waffenmodifikator_attacke", Integer.toString(waffe.getWmAT()));
			data.put("waffenmodifikator_parade", Integer.toString(waffe.getWmPA()));

			sendEquipmentToServer(CacheKey.NAHKAMPFWAFFE, heldId, waffe.getName(), data, "Nahkampfwaffe", "Nahkampfwaffen");
			monitor.step();
		}
		monitor.subtaskDone();
	}

	private String arrayToString(String[] array)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < array.length; i++)
		{
			if (i > 0)
			{
				sb.append(", ");
			}
			sb.append(array[i]);
		}
		return sb.toString();
	}

	/**
	 * Convert hitpoint array to string.
	 * 
	 * @param tp
	 *          Trefferpunkte, 0: Anzahl Würfel; 1: Würfelart; 2: Festwert
	 * @return
	 */
	private String tpToString(int[] tp)
	{
		if (tp == null || tp.length != 3)
		{
			return "";
		}
		return String.format("%dw%d%+d", tp[0], tp[1], tp[2]);
	}

	public void syncRangedWeapons(UUID heldId, PluginHeldenWerteWerkzeug3 werkzeug, ProgressMonitor monitor)
					throws HeldenWebExportException
	{
		getIdsFromServer(CacheKey.FERNKAMPFWAFFE, "fernkampfwaffe", "Fernkampfwaffen.xml", "Fernkampfwaffen", true,
						"held_id");

		PluginFernkampfWaffe[] waffen = werkzeug.getAusruestung2().getFernkampfWaffen();
		monitor.startSubtask("Fernkampfwaffen", waffen.length);
		for (PluginFernkampfWaffe waffe : waffen)
		{
			if (waffe == null)
			{
				// Not all weapons may be set
				continue;
			}
			UUID talentId = getKey(CacheKey.TALENT, waffe.getTalent().getBezeichnung());
			if (talentId == null)
			{
				throw new HeldenWebExportException(MessageFormat.format(
								"Nahkampfwaffe {0} referenziert unbekanntes Talent {1}", waffe.toString(), waffe.getTalent()
												.getBezeichnung()));
			}

			Map<String, String> data = new HashMap<String, String>();
			data.put("held_id", heldId.toString());
			data.put("talent_id", talentId.toString());
			data.put("name", waffe.toString());
			try
			{
				// Try to use newer interface
				data.put("fernkampfwert", Integer.toString(((PluginFernkampfWaffe2) waffe).getFernkammpfWert()));
			}
			catch (ClassCastException e)
			{
				// New interface not available
				data.put("fernkampfwert", Integer.toString(werkzeug.getTalentwert(waffe.getTalent())));
			}
			data.put("trefferpunkte", tpToString(waffe.getTrefferpunkte()));
			data.put("ladezeit", Integer.toString(waffe.getLaden()));
			data.put("munitionsart", waffe.getMunitionsArt());
			int[] reichweite = waffe.getReichweite();
			int[] trefferpunkte = waffe.getTrefferpunkteModifikation();
			data.put("reichweite0", Integer.toString(reichweite[0]));
			data.put("trefferpunkte0", Integer.toString(trefferpunkte[0]));
			data.put("reichweite1", Integer.toString(reichweite[1]));
			data.put("trefferpunkte1", Integer.toString(trefferpunkte[1]));
			data.put("reichweite2", Integer.toString(reichweite[2]));
			data.put("trefferpunkte2", Integer.toString(trefferpunkte[2]));
			data.put("reichweite3", Integer.toString(reichweite[3]));
			data.put("trefferpunkte3", Integer.toString(trefferpunkte[3]));
			data.put("reichweite4", Integer.toString(reichweite[4]));
			data.put("trefferpunkte4", Integer.toString(trefferpunkte[4]));

			sendEquipmentToServer(CacheKey.FERNKAMPFWAFFE, heldId, waffe.toString(), data, "Fernkampfwaffe",
							"Fernkampfwaffen");
			monitor.step();
		}
		monitor.subtaskDone();
	}

	public void syncArmor(UUID heldId, PluginHeldenWerteWerkzeug3 werkzeug, ProgressMonitor monitor)
					throws HeldenWebExportException
	{
		getIdsFromServer(CacheKey.RUESTUNG, "ruestung", "Ruestungen.xml", "Rüstungen", true, "held_id");

		PluginRuestungsTeil[] ruestungen = werkzeug.getAusruestung2().getRuestungsTeile();
		monitor.startSubtask("Rüstungen", ruestungen.length + 1);
		for (PluginRuestungsTeil ruestung : ruestungen)
		{
			if (ruestung == null)
			{
				// Not all armor parts may be set
				continue;
			}

			sendArmorToServer(heldId, werkzeug, ruestung, false);
			monitor.step();
		}
		PluginRuestungsTeil gesamtRuestung = werkzeug.getAusruestung2().getGesammtRuestung();
		if (gesamtRuestung != null)
		{
			sendArmorToServer(heldId, werkzeug, gesamtRuestung, true);
			monitor.step();
		}
		monitor.subtaskDone();
	}

	private void sendArmorToServer(UUID heldId, PluginHeldenWerteWerkzeug3 werkzeug, PluginRuestungsTeil ruestung,
					boolean complete) throws HeldenWebExportException
	{
		Map<String, String> data = new HashMap<String, String>();
		data.put("held_id", heldId.toString());
		data.put("name", ruestung.toString());
		data.put("gesamt", booleanToDb(complete));
		data.put("zeug", booleanToDb(ruestung.istZeug()));
		data.put("anzahl_teile", Integer.toString(ruestung.getAnzahlTeile()));
		data.put("behinderung_gesamt", Integer.toString(ruestung.getGesammtBehinderung()));
		data.put("schutz_gesamt", Integer.toString(ruestung.getGesamtSchutz()));
		data.put("schutz_gesamt_zonen", Integer.toString(ruestung.getGesammtZonenSchutz()));
		data.put("schutz_bauch", Integer.toString(ruestung.getBauchSchutz()));
		data.put("schutz_brust", Integer.toString(ruestung.getBrustSchutz()));
		data.put("schutz_kopf", Integer.toString(ruestung.getKopfSchutz()));
		data.put("schutz_ruecken", Integer.toString(ruestung.getRueckenSchutz()));
		data.put("schutz_arm_links", Integer.toString(ruestung.getLinkerArmSchutz()));
		data.put("schutz_arm_rechts", Integer.toString(ruestung.getRechterArmSchutz()));
		data.put("schutz_bein_links", Integer.toString(ruestung.getLinkesBeinSchutz()));
		data.put("schutz_bein_rechts", Integer.toString(ruestung.getRechtesBeinSchutz()));
		sendEquipmentToServer(CacheKey.RUESTUNG, heldId, ruestung.toString(), data, "Ruestung", "Ruestungen");
	}

	public void syncShields(UUID heldId, PluginHeldenWerteWerkzeug3 werkzeug, ProgressMonitor monitor)
					throws HeldenWebExportException
	{
		getIdsFromServer(CacheKey.SCHILD, "schild", "Schilde.xml", "Schilde", true, "held_id");

		PluginSchildParadewaffe[] schilde = werkzeug.getAusruestung2().getSchildParadewaffe();
		monitor.startSubtask("Schilde und Paradewaffen", schilde.length);
		for (PluginSchildParadewaffe schild : schilde)
		{
			if (schild == null)
			{
				// Not all shields might be set
				continue;
			}

			Map<String, String> data = new HashMap<String, String>();
			data.put("held_id", heldId.toString());
			data.put("name", schild.getName());
			data.put("parade", Integer.toString(schild.getParade()));
			data.put("art", schild.getBenutzungsart());
			data.put("inimodifikator", Integer.toString(schild.getInitiativeModifikator()));
			data.put("waffenmodifikator_attacke", Integer.toString(schild.getWaffenModifikatorAT()));
			data.put("waffenmodifikator_parade", Integer.toString(schild.getWaffenModifikatorPA()));
			data.put("bruchfaktor_minimal", Integer.toString(schild.getBruchfaktorMin()));
			data.put("bruchfaktor_aktuell", Integer.toString(schild.getBruchfaktor()));

			sendEquipmentToServer(CacheKey.SCHILD, heldId, schild.getName(), data, "Schild", "Schilde");
			monitor.step();
		}
		monitor.subtaskDone();
	}

	public void syncCombat(UUID heldId, PluginHeldenWerteWerkzeug3 werkzeug, ProgressMonitor monitor)
					throws HeldenWebExportException
	{
		getIdsFromServer(CacheKey.KAMPF, "kampf", "Kampf.xml", "Kampfwerte", false, "held_id");

		Map<String, String> data = new HashMap<String, String>();
		data.put("held_id", heldId.toString());
		data.put("ausweichen", Integer.toString(werkzeug.getAusruestung2().getAusweichen()));
		data.put("raufen_attacke", Integer.toString(werkzeug.getAusruestung2().getRauferAttacke()));
		data.put("raufen_parade", Integer.toString(werkzeug.getAusruestung2().getRaufenParade()));
		data.put("raufen_trefferpunkte", werkzeug.getAusruestung2().getRaufenTP());
		data.put("ringen_attacke", Integer.toString(werkzeug.getAusruestung2().getRingenAttacke()));
		data.put("ringen_parade", Integer.toString(werkzeug.getAusruestung2().getRingenParade()));
		data.put("ringen_trefferpunkte", werkzeug.getAusruestung2().getRingenTP());

		UUID key = getKey(CacheKey.KAMPF, heldId.toString());
		if (key == null)
		{
			key = sendToServer("Kampf", data, "Kampf.xml", "/kampf/id");
			keys.put(CacheKey.KAMPF + heldId.toString(), key);
		}
		else
		{
			sendToServer("Kampf", data, "Kampf/edit/" + key.toString() + ".xml", "/kampf/id");
		}
	}

	public void synchronizeInventory(UUID heldId, PluginHeldenWerteWerkzeug3 werkzeug, ProgressMonitor monitor)
					throws HeldenWebExportException
	{
		getIdsFromServer(CacheKey.GEGENSTAENDE, "gegenstand", "Gegenstaende.xml", "Gegenstände", true, "held_id", "index");

		ArrayList<String> inventory = werkzeug.getInventarAlsString();
		monitor.startSubtask("Gegenstände", inventory.size());
		for (String itemName : inventory)
		{
			PluginGegenstand[] items = werkzeug.getGegenstand(itemName);
			for (int index = 0; index < items.length; index++)
			{
				String indexString = Integer.toString(index);
				PluginGegenstand item = items[index];
				if (item == null)
				{
					continue;
				}

				Map<String, String> data = new HashMap<String, String>();
				data.put("held_id", heldId.toString());
				data.put("name", item.toString());
				data.put("index", indexString);
				data.put("anzahl", Integer.toString(item.getAnzahl()));
				data.put("anzeigename", item.getName());
				data.put("gewicht", Float.toString(item.getGewicht()));
				data.put("preis", Integer.toString(item.getPreis()));

				UUID key = getKey(CacheKey.GEGENSTAENDE, itemName, heldId.toString(), indexString);
				if (key == null)
				{
					key = sendToServer("Gegenstand", data, "Gegenstaende.xml", "/gegenstand/id");
					keys.put(CacheKey.GEGENSTAENDE + itemName + heldId.toString() + indexString, key);
				}
				else
				{
					sendToServer("Gegenstand", data, "Gegenstaende/edit/" + key.toString() + ".xml", "/gegenstand/id");
				}
			}
			monitor.step();
		}
		monitor.subtaskDone();
	}

	public void syncronizeMoney(UUID heldId, PluginHeldenWerteWerkzeug3 werkzeug, ProgressMonitor monitor)
					throws HeldenWebExportException
	{
		getIdsFromServer(CacheKey.MUENZEN, "muenze", "Muenzen.xml", "Münzen", true, "held_id");

		GeldBoerse boerse = werkzeug.getGeldBoerse();
		Iterator<Muenze> münzen = boerse.getMuenzeIter();
		monitor.startSubtask("Münzen", boerse.getGeldStrings().size());
		while (münzen.hasNext())
		{
			Muenze münze = münzen.next();
			Map<String, String> data = new HashMap<String, String>();
			data.put("held_id", heldId.toString());
			data.put("name", münze.getBezeichner());
			data.put("gruppe", münze.getWaehrungsBezeichner());
			data.put("anzahl", Integer.toString(boerse.getMuenzAnzahl(münze)));

			UUID key = getKey(CacheKey.MUENZEN, münze.getBezeichner(), heldId.toString());
			if (key == null)
			{
				key = sendToServer("Muenze", data, "Muenzen.xml", "/muenze/id");
				keys.put(CacheKey.MUENZEN + münze.getBezeichner() + heldId.toString(), key);
			}
			else
			{
				sendToServer("Muenze", data, "Muenzen/edit/" + key.toString() + ".xml", "/muenze/id");
			}
			monitor.step();
		}
		monitor.subtaskDone();
	}
}
