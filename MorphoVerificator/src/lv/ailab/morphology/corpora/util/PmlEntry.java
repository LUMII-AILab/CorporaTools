package lv.ailab.morphology.corpora.util;

import java.util.ArrayList;

/**
 * Data structure to acumulate single M element from PML-M file.
 */
public class PmlEntry
{
	/**
	 * Buffer acumulating the rest of the element content and element header.
	 */
	public StringBuffer buffer;
	
	/**
	 * Important fields.
	 */
	public String form, tag, lemma;
	
	public PmlEntry()
	{
		buffer = new StringBuffer();
		form = null;
		tag = null;
		lemma = null;
	}
	
	/**
	 * Convert to XML string.
	 */
	public String toPmlString(ArrayList<String> errors)
	{
		StringBuffer res = new StringBuffer(buffer.toString());
		res.append("\r\n<form>");
		res.append(form);
		res.append("</form>\r\n<lemma>");
		res.append(lemma == null ? "N/A" : lemma);
		res.append("</lemma>\r\n<tag>");
		res.append(tag == null ? "N/A" : tag);
		res.append("</tag>");
		
		if (errors != null && errors.size() > 0)
		{
			res.append("\r\n<error>");
			
			for (String e : errors)
			{
				res.append(e);
				res.append("; ");
			}
			
			res.delete(res.lastIndexOf(";"), 2);
			res.append("</error>");
		}
		
		res.append("\r\n</m>");
		return res.toString();
	}
}