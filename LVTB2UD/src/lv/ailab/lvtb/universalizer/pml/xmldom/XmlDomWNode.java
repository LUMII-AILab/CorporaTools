package lv.ailab.lvtb.universalizer.pml.xmldom;

import lv.ailab.lvtb.universalizer.pml.PmlWNode;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpressionException;

public class XmlDomWNode implements PmlWNode
{
	protected final Node domNode;

	public XmlDomWNode(Node mNode)
	{
		if (mNode == null) throw new NullPointerException(String.format(
				"%s can't be initialized with a null",
				this.getClass().getSimpleName()));
		domNode = mNode;
		// TODO add check-up, if the node has the correct name.
	}

	/**
	 * Find ID attribute.
	 * @return attribute value
	 */
	@Override
	public String getId()
	{
		if (domNode == null) return null;
		try
		{
			return XPathEngine.get().evaluate("./@id", domNode);
		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Find token.
	 * @return token value
	 */
	@Override
	public String getToken()
	{
		if (domNode == null) return null;
		try
		{
			return XPathEngine.get().evaluate("./token", domNode);
		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Find flag value for if there is no space after this token.
	 * @return true, if there is no space after this token, false otherwise
	 */
	@Override
	public boolean noSpaceAfter()
	{
		try
		{
			return "1".equals(XPathEngine.get().evaluate(
					"./no_space_after", domNode));
		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}
}
