package lv.ailab.lvtb.universalizer.pml.xmldom;

import lv.ailab.lvtb.universalizer.pml.LvtbFormChange;
import lv.ailab.lvtb.universalizer.pml.PmlMNode;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.HashSet;

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

	/**
	 * Get all form_change values.
	 * @return unordered set of values
	 */
	public HashSet<LvtbFormChange> getFormChange()
	{
		if (domNode == null) return null;
		try
		{
			HashSet<LvtbFormChange> res = new HashSet<>();
			NodeList formChange = (NodeList) XPathEngine.get().evaluate(
					"./form_change/LM", domNode, XPathConstants.NODESET);
			if (formChange == null || formChange.getLength() < 1)
				formChange = (NodeList) XPathEngine.get().evaluate(
						"./form_change", domNode, XPathConstants.NODESET);
			if (formChange != null && formChange.getLength() > 0)
				for (int i = 0; i < formChange.getLength(); i++)
			{
				String encoded = formChange.item(i).getTextContent().trim();
				LvtbFormChange decoded = LvtbFormChange.fromString(encoded);
				if (decoded != null) res.add(decoded);
				else if (!encoded.isEmpty())
					throw new IllegalArgumentException(String.format(
							"Illegal form_change value \"%s\"", encoded));
			}
			if (res.isEmpty()) return null;
			return res;
		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}
	/**
	 * Reconstruct source string (before error corrections) from underlying w
	 * level tokens.
	 * @return	source string without leading or trailing whitespace
	 */
	public String getSourceString()
	{
		try
		{
			NodeList wNodes = (NodeList) XPathEngine.get().evaluate(
					"./w.rf/LM", domNode, XPathConstants.NODESET);
			if (wNodes == null || wNodes.getLength() < 1)
				wNodes = (NodeList) XPathEngine.get().evaluate(
						"./w.rf", domNode, XPathConstants.NODESET);
			if (wNodes == null || wNodes.getLength() < 1) return null;
			StringBuilder res = new StringBuilder();
			for (int i = 0; i < wNodes.getLength(); i++)
			{
				res.append(XPathEngine.get().evaluate(
						"./token", wNodes.item(i)));
				if (!"1".equals(XPathEngine.get().evaluate(
						"./no_space_after", wNodes.item(i))))
					res.append(" ");
			}
			return res.toString().trim();

		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}
}
