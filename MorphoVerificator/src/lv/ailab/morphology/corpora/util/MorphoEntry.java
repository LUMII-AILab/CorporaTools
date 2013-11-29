package lv.ailab.morphology.corpora.util;

import java.util.ArrayList;
import lv.semti.morphology.analyzer.MarkupConverter;
import lv.semti.morphology.attributes.AttributeValues;

/**
 * Single entry of the MorphoList.
 */
public class MorphoEntry
{
	/**
	 * Token (in PML-M coresponds to element "form").
	 */
	public String token;
	/**
	 * Morphological attributes parsed from tag.
	 */
	public AttributeValues attributes;
	/**
	 * Lemma.
	 */
	public String lemma;
	/**
	 * References to w layer in case of PML-M processing, otherwise null.
	 */
	public ArrayList<String> wRefs = null;
	
	/**
	 * Other content in case of XML processing, otherwise null.
	 */
	public StringBuffer content = null;
	
	/**
	 * Is this entry used to store data for XML procesing.
	 */
	public final boolean XML;
	
	/**
	 * Create new entry.
	 */
	public MorphoEntry(boolean fromXml)
	{
		this.token = null;
		this.attributes = null;
		this.lemma = null;
		XML = fromXml;
		if (fromXml)
		{
			wRefs = new ArrayList<String>();
			content = new StringBuffer();
		}
	}	
	
	/**
	 * Create new entry.
	 */
	public MorphoEntry(
		String token, AttributeValues attributes, String lemma, boolean fromXml)
	{
		this.attributes = attributes;
		setLemma(lemma);
		setToken(token);
		XML = fromXml;
		if (fromXml)
		{
			wRefs = new ArrayList<String>();
			content = new StringBuffer();
		}
	}
	
	/**
	 * Create new entry (checks if lemma and tag is not "N/A").
	 */
	public MorphoEntry(String token, String tag, String lemma, boolean fromXml)
	{
		setToken(token);
		setLemma(lemma);
		setAttributes(tag.toLowerCase());
		XML = fromXml;
		if (fromXml)
		{
			wRefs = new ArrayList<String>();
			content = new StringBuffer();
		}
	}
	
	/**
	 * Sets lemma, checking if parameter is not "N/A" or "null".
	 */
	public void setLemma(String lemma)
	{
		this.lemma = lemma.trim();
		if ("N/A".equalsIgnoreCase(this.lemma)
			|| "null".equalsIgnoreCase(this.lemma)) this.lemma = null;
	}
	
	/**
	 * Creates attributes from tag.
	 */
	public void setAttributes(String tag)
	{
		//tag = tag.trim().replace('-', '_');
		String normTag = tag.trim().replace('-', '_')	//TODO
			.toLowerCase().replace("[", "").replace("]", "");
		if (normTag.length() < 1
			|| "N/A".equalsIgnoreCase(normTag)
			|| "null".equalsIgnoreCase(normTag)
			|| "_".equalsIgnoreCase(normTag)) this.attributes = null;
		else
		{
			this.attributes = MarkupConverter.fromKamolsMarkup(normTag);
			
			// Print out mismatch warning.
			String loaded = MarkupConverter.toKamolsMarkupNoDefaults(this.attributes);
			if (!tag.trim().startsWith(loaded) && !normTag.startsWith(loaded)
				&& !loaded.startsWith(normTag))
				System.out.println(
					"Warning: " + tag.trim() + " parsed as " + loaded);
		}
	}
	
	/**
	 * Set token.
	 */
	public void setToken(String token)
	{
		this.token = token.trim();
	}

	/**
	 * Convert to PML m element. No conteiner tags added.
	 */
	public String toXmlString(ArrayList<String> verdict)
	{
		String tag = attributes == null ? "N/A" : 
			MarkupConverter.toKamolsMarkupNoDefaults(attributes);
		String l = lemma == null ? "N/A" : lemma;
		
		StringBuffer res = new StringBuffer(content.toString());
		if (wRefs != null && wRefs.size() > 0)
		{
			res.append("\r\n<w.rf>");
			if (wRefs.size() < 2) res.append(wRefs.get(0));
			else
			{
				for (String ref : wRefs)
				{
					res.append("\r\n<LM>");
					res.append(ref);
					res.append("</LM>");
				}
				res.append("\r\n");
			}
			res.append("</w.rf>");
		}
		
		res.append("\r\n<form>");
		res.append(token);
		res.append("</form>\r\n<lemma>");
		res.append(l);
		res.append("</lemma>\r\n<tag>");
		res.append(tag);
		res.append("</tag>\r\n");
		
		if (verdict != null && verdict.size() > 0)
		{
			res.append("<errors>");
			for (String v : verdict) res.append(v + "; ");
			res.append("</errors>\r\n");
		}
		res.append("</m>\r\n");
		return res.toString();
	}
	
	/**
	 * Indicates whether some other object is "equal to" this one.
	 */
	@Override
	public boolean equals(Object o)
	{
		if (o == null) return false;
		try
		{
			MorphoEntry tmp = (MorphoEntry)o;
			return (token == tmp.token || token.equals(tmp.token))
				&& (attributes == tmp.attributes || 
					MarkupConverter.toKamolsMarkupNoDefaults(attributes).equals(
						MarkupConverter.toKamolsMarkupNoDefaults(tmp.attributes)))
				&& (lemma == tmp.lemma || lemma.equals(tmp.lemma));
			
		} catch (Exception e)
		{
			return false;
		}
	}
	
	/**
	 * Returns a hash code value for the object.
	 */
	@Override
	public int hashCode()
	{
		int res = (token == null ? 0 : token.hashCode());
		res = res + 2003 * (lemma == null ? 0 : lemma.hashCode());
		return res;
	}
}
