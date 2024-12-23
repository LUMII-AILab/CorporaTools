package lv.ailab.lvtb.universalizer;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;

/**
 * Created on 2016-04-17.
 *
 * @author Lauma
 */
public class PmlXmlLoader
{
	public static Document loadPmlXml(String path)
	throws ParserConfigurationException, IOException, SAXException
	{
		File pmlFile = new File(path);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(pmlFile);
		// This sorts out split text:
		// https://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
		doc.getDocumentElement().normalize();
		return doc;
	}

	public static NodeList getTrees(Document pmlDoc) throws XPathExpressionException
	{
		XPath xPath = XPathFactory.newInstance().newXPath();
		return (NodeList)xPath.evaluate("/lvadata/trees/LM",
				pmlDoc.getDocumentElement(), XPathConstants.NODESET);
	}

	public static NodeList getTrees(String path)
	throws IOException, SAXException, ParserConfigurationException,
			XPathExpressionException
	{
		return getTrees(loadPmlXml(path));
	}
}
