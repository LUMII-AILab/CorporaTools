package lv.ailab.lvtb.universalizer.utils;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

/**
 * Singular XPath engine to be used for all the PML XML querying.
 * In case unified specific setup needed, to it here.
 * Created on 2016-04-22.
 *
 * @author Lauma
 */
public class XPathEngine
{
	protected static XPath xPathEngineSing = null;

	public static XPath get()
	{
		if(xPathEngineSing == null) xPathEngineSing = XPathFactory.newInstance().newXPath();
		return xPathEngineSing;
	}
}
