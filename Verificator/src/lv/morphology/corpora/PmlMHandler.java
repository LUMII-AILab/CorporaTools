package lv.morphology.corpora;

import java.io.*;
import java.util.ArrayList;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import lv.semti.morphology.analyzer.MarkupConverter;

import lv.morphology.corpora.util.MorphoEntry;


// Based on echoing code from
// http://docs.oracle.com/javaee/1.4/tutorial/doc/JAXPSAX3.html
public class PmlMHandler extends DefaultHandler
{
	/**
	 * Accumulator size - how many entries are accumulated for continous tests?
	 */
	public final int ACCUM_LENGTH;
	private ArrayList<MorphoEntry> accum;
	private CorpusVerificator corpVer;
	private BufferedWriter output;
	private StringBuffer content;
	
	// Lowercased path, ancestors seperated with "/".
	private String ancestors;
	
	// Data fields for single morphological token.
	//private String form, tag, lemma;
	
	public PmlMHandler (
		CorpusVerificator verificator, BufferedWriter out, int accumulatorSize)
	{
		if (accumulatorSize > 0)
			ACCUM_LENGTH = accumulatorSize;
		else
			throw new IllegalArgumentException(
				"Illegal accumulator size: " + accumulatorSize + "!");
		accum = new ArrayList<MorphoEntry>(ACCUM_LENGTH);
		
		corpVer = verificator;
		output = out;
		
		//content = null;
		ancestors = "";
//		form = null;
//		tag = null;
//		lemma = null;
	}
	
	@Override
	public void startDocument()
	throws SAXException
	{
		print("<?xml version='1.0' encoding='UTF-8'?>\r\n");
	}
	
	@Override
	public void endDocument()
	throws SAXException
	{
		print("\r\n");
	}
	
	@Override
	public void startElement(String namespaceURI, String simpleName,
		String qualifiedName, Attributes attrs)
	throws SAXException
	{
		//printTextBuffer();  // For non-wellformed XMLs.
		// Get element name.
		String elemName = simpleName;
		if ("".equals(elemName)) elemName = qualifiedName; // not namespace-aware
		
		ancestors = ancestors + "/" + elemName.toLowerCase();
		
		// Tag, lemma, form and w.rf is printed out when the end of m is
		// reached.
		if (ancestors.endsWith("/form")
			|| ancestors.endsWith("/tag")
			|| ancestors.endsWith("/lemma")
			|| ancestors.endsWith("/w.rf")
			|| ancestors.endsWith("/w.rf/LM")) return;
		
		// If new m starts, data structure for new data is added to the
		// accumulator.
		if(ancestors.endsWith("/m"))
			accum.add(new MorphoEntry(true));
			
		// Collect opening tag.
		StringBuffer staffToPrint = new StringBuffer();
		staffToPrint.append("\r\n<");
		staffToPrint.append(elemName);
		if (attrs != null)
		{
			for (int i = 0; i < attrs.getLength(); i++)
			{
				String attrName = attrs.getLocalName(i);
				if ("".equals(attrName)) attrName = attrs.getQName(i);
				staffToPrint.append(" ");
				staffToPrint.append(attrName);
				staffToPrint.append("=\"");
				staffToPrint.append(attrs.getValue(i));
				staffToPrint.append("\"");
			}
		}
		
		staffToPrint.append(">");
		
		// If we are inside m, tag is added to the current morpoelement buffer.
		if (ancestors.contains("/m/") || ancestors.endsWith("/m"))
			accum.get(accum.size() - 1).content.append(staffToPrint);
		// The rest of tags are printed out as they come.
		else print(staffToPrint.toString());
	}
	
	
	@Override
	public void endElement(
		String namespaceURI, String simpleName, String qualifiedName)
	throws SAXException
	{
		String elemName = simpleName;
  		if ("".equals(elemName)) elemName = qualifiedName; // not namespace-aware

		// If "s" is ending.
		if (ancestors.endsWith("/s"))
		{
			while(accum.size() > 0)
			{
				ArrayList<String> verdict = corpVer.processFirst(accum);
				print(accum.get(0).toXmlString(verdict));
				accum.remove(0);
			}
		}
		
		// If "m" is ending.
		if (ancestors.endsWith("/m"))
		{
			if (accum.size() >= ACCUM_LENGTH)
			{
				ArrayList<String> verdict = corpVer.processFirst(accum);
				print(accum.get(0).toXmlString(verdict));
				accum.remove(0);
			}
			
		}
		
		// Save the meaningfull fields at their end.
		else if (ancestors.endsWith("/form"))
			accum.get(accum.size() - 1).setToken(content.toString());
		else if (ancestors.endsWith("/tag"))
		{
			accum.get(accum.size() - 1).setAttributes(content.toString());
		}
		else if (ancestors.endsWith("/lemma"))
			accum.get(accum.size() - 1).setLemma(content.toString());
		else if (ancestors.endsWith("/w.rf") && content.length() > 0
				 || ancestors.endsWith("/w.rf/LM"))
			accum.get(accum.size() - 1).wRefs.add(content.toString().trim());
			
		// Store or print out other tags and their content.
		else if (ancestors.contains("/m/"))
		{
			// If we are inside m, tag is added to the current morpoelement
			// buffer.
			StringBuffer tmp = accum.get(accum.size() - 1).content;
			tmp.append(content.toString().trim());
			tmp.append("</"); tmp.append(elemName); tmp.append(">");
		} else 
		{
			// If we are outside m element, tags are printed out as they come.
			printTextBuffer();
	  		print("</" + elemName + ">");

		}
			
		content = null;
		if (ancestors.endsWith(elemName.toLowerCase()))
			ancestors = ancestors.substring(0, ancestors.lastIndexOf("/"));
	}
	
	@Override
	public void characters(char buffer[], int offset, int len)
	throws SAXException
	{
		String s = new String(buffer, offset, len);
		if (content == null) content = new StringBuffer();
		content.append(buffer, offset, len);
	} 
	
	private void printTextBuffer()
	throws SAXException
	{
		if (content == null) return;
		print(content.toString().trim());
		content = null;
	}
	
 	/**
	 * Prints string to output stream and wraps IO exceptions as SAX exceptions.
	 */
	private void print(String s)
	throws SAXException
	{
		try
		{
	    	output.write(s);
	    	output.flush();
		} catch (IOException e)
	  	{
			throw new SAXException("I/O error", e);
	  	}
	}
	
	/**
	 * Prints new line to output stream and wraps IO exceptions as SAX exceptions.
	 */
/*	private void newLine()
	throws SAXException
	{
		try
		{
	    	output.newLine();
	    	output.flush();
		} catch (IOException e)
	  	{
			throw new SAXException("I/O error", e);
	  	}
	}//*/

}