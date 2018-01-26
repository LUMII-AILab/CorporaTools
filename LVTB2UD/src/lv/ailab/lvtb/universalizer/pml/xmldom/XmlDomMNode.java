package lv.ailab.lvtb.universalizer.pml.xmldom;

import lv.ailab.lvtb.universalizer.pml.PmlMNode;
import org.w3c.dom.Node;
import javax.xml.xpath.XPathExpressionException;

public class XmlDomMNode implements PmlMNode
{
	protected final Node domNode;

	public XmlDomMNode(Node mNode)
	{
		if (mNode == null) throw new NullPointerException(String.format(
				"%s can't be initialized with a null",
				this.getClass().getSimpleName()));
		domNode = mNode;
		// TODO add check-up, if the node has the correct name.
	}

	/**
	 * Find ID attribute.
	 * @return	attribute value
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
	 * Find lemma.
	 * @return lemma
	 */
	@Override
	public String getLemma()
	{
		if (domNode == null) return null;
		try
		{
			return XPathEngine.get().evaluate("./lemma", domNode);
		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Find tag.
	 * @return tag
	 */
	@Override
	public String getTag()
	{
		if (domNode == null) return null;
		try
		{
			return XPathEngine.get().evaluate("./tag", domNode);
		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Find form.
	 * @return form
	 */
	@Override
	public String getForm()
	{
		if (domNode == null) return null;
		try
		{
			return XPathEngine.get().evaluate("./form", domNode);
		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Determine, if final token in this morphological unit has no_space_after
	 * set.
	 * @return	true, if there is no space after this unit
	 */
	@Override
	public Boolean getNoSpaceAfter()
	{
		if (domNode == null) return null;
		try
		{
			return "1".equals(XPathEngine.get().evaluate(
					"./w.rf/no_space_after|./w.rf/LM[last()]/no_space_after", domNode));
		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}
}
