package lv.ailab.lvtb.universalizer.pml.xmldom;

import lv.ailab.lvtb.universalizer.pml.LvtbCoordTypes;
import lv.ailab.lvtb.universalizer.pml.LvtbHelperRoles;
import lv.ailab.lvtb.universalizer.pml.LvtbRoles;
import lv.ailab.lvtb.universalizer.pml.PmlANode;
import lv.ailab.lvtb.universalizer.pml.utils.PmlANodeListUtils;
import lv.ailab.lvtb.universalizer.transformator.morpho.AnalyzerWrapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.List;

// TODO what to do with the XPathExpressionException?

/**
 * PmlANode implementation with an underlying PML-XML DOM fragment.
 *
 * Created on 2018-01-24.
 * @author Lauma
 */
public class XmlDomANode implements PmlANode
{
	protected Node domNode;

	public XmlDomANode(Node aNode)
	{
		if (aNode == null)  throw new NullPointerException(String.format(
				"%s can't be initialized with a null",
				this.getClass().getSimpleName()));
		domNode = aNode;
		// TODO add check-up, if the node has the correct name.
	}

	//=== Field querying =======================================================

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
	 * Determine node type.
	 * @return 	type of the node - Type.X, Type.COORD or Type.PMC for ID-less
	 *			nodes, Type. ROOT for root of the tree, Type.NODE for others.
	 */
	@Override
	public Type getNodeType()
	{
		String name = domNode.getNodeName();
		switch (name)
		{
			case "xinfo": return Type.X;
			case "coordinfo": return Type.COORD;
			case "pmcinfo": return Type.PMC;
			case "trees": return Type.ROOT;
			case "LM":
				Node parent = null;
				try
				{
					parent = (Node) XPathEngine.get().evaluate(
							"../..", domNode, XPathConstants.NODE);
					if ("trees".equals(parent.getNodeName())) return Type.ROOT;
				} catch (XPathExpressionException e)
				{
					throw new IllegalArgumentException(e);
				}
			default: return Type.NODE;
		}
	}

	/**
	 * Method for convenience: check, if given node is a phrase node.
	 * @return	true, if node's type is PmlANode.Type.X or PmlANode.Type.COORD
	 * 			or PmlANode.Type.PMC
	 */
	@Override
	public boolean isPhraseNode()
	{
		if (domNode == null) return false;
		PmlANode.Type type = getNodeType();
		return type == Type.X || type == Type.COORD || type == Type.PMC;
	}

	/**
	 * Find the role of this node.
	 * @return	role value for NODE, nothing for other node types
	 */
	@Override
	public String getRole()
	{
		if (domNode == null) return null;
		try
		{
			return XPathEngine.get().evaluate("./role", domNode);
		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Find pmctype, coordtype or xtype for this node.
	 * @return	phrase type for X, COORD, PMC, nothing for other node types
	 */
	@Override
	public String getPhraseType()
	{
		if (domNode == null)
			return null;
		try
		{
			return XPathEngine.get().evaluate(
					"./pmctype|./coordtype|./xtype", domNode);
		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Find pmctype, coordtype, xtype or role for this node.
	 * @return	phrase type or dependency role or LVtbHelperRoles.ROOT for root
	 */
	@Override
	public String getAnyLabel()
	{
		if (domNode == null) return null;
		if (getNodeType() == Type.ROOT) return LvtbHelperRoles.ROOT;
		try
		{
			return XPathEngine.get().evaluate(
					"./role|./pmctype|./coordtype|./xtype", domNode);
		}
		catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Find the closest ancestor (given node included), whose label is not
	 * crdPart, crdParts or crdClauses, and return its role or phrase label.
	 * @return	phrase type or dependency role or LVtbHelperRoles.ROOT for root.
	 */
	@Override
	public String getEffectiveLabel()
	{
		String label = getAnyLabel();
		if (label.equals(LvtbRoles.CRDPART) ||
				label.equals(LvtbCoordTypes.CRDCLAUSES) ||
				label.equals(LvtbCoordTypes.CRDPARTS))
			return getEffectiveAncestor().getAnyLabel();
		return label;
	}

	/**
	 * Find the phrase tag of this node.
	 * @return	tag value for X or COORD having a tag value, nothing for other
	 * 			nodes and node types
	 */
	@Override
	public String getPhraseTag()
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
	 * Find tag attribute. Use either morphotag or x-word tag or coordination
	 * tag. For tokenless reduction nodes return reduction tag. For PMC node
	 * return first basElem's tag. For coordinations with no given tag return
	 * tag obtained from first coordinated part.
	 * Based on assumption, that single node has no more than one phrase-child.
	 * @return	tag
	 */
	@Override
	public String getAnyTag()
	{
		if (domNode == null) return null;
		Node phraseNode;

		try
		{
			if (!isPhraseNode())
			{
				String tag = XPathEngine.get().evaluate("./m.rf/tag", domNode);
				if (tag != null && tag.length() > 0) return tag;
				tag = getReduction();
				if (tag != null && tag.contains("("))
					tag = tag.substring(0, tag.indexOf("(")).trim();
				if (tag != null && tag.length() > 0) return tag;
				phraseNode = ((XmlDomANode) getPhraseNode()).domNode;
			} else phraseNode = domNode;

			if (phraseNode == null) return null;
			String tag = XPathEngine.get().evaluate("./tag", phraseNode);
			if (tag != null && tag.length() > 0) return tag;

			NodeList baseParts = (NodeList) XPathEngine.get().evaluate(
					"./children/node[role='" + LvtbRoles.PRED + "']",
					phraseNode, XPathConstants.NODESET);
			if (baseParts != null && baseParts.getLength() > 0)
				return PmlANodeListUtils.getFirstByDeepOrd(asList(baseParts)).getAnyTag();
			baseParts = (NodeList) XPathEngine.get().evaluate(
					"./children/node[role='" + LvtbRoles.BASELEM + "']",
					phraseNode, XPathConstants.NODESET);
			if (baseParts != null && baseParts.getLength() > 0)
				return PmlANodeListUtils.getFirstByDeepOrd(asList(baseParts)).getAnyTag();
			baseParts = (NodeList) XPathEngine.get().evaluate(
					"./children/node[role='" + LvtbRoles.CRDPART + "']",
					phraseNode, XPathConstants.NODESET);
			if (baseParts != null && baseParts.getLength() > 0)
				return PmlANodeListUtils.getFirstByDeepOrd(asList(baseParts)).getAnyTag();
		}
		catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
		return null;
	}

	/**
	 * Check, if this node is a reduction node without corresponding node in
	 * the morphological level.
	 * @return	true, if node has no morphology field and has nonempty reduction
	 * 			field
	 */
	@Override
	public boolean isPureReductionNode()
	{
		if (domNode == null) return false;
		String reduction = getReduction();
		try
		{
			Node morpho =  (Node) XPathEngine.get().evaluate(
				"./m.rf", domNode, XPathConstants.NODE);
			return (morpho == null && reduction != null && reduction.length() > 0);
		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Find reduction field value.
	 * @return	reduction value
	 */
	@Override
	public String getReduction()
	{
		if (domNode == null) return null;
		try
		{
			return XPathEngine.get().evaluate("./reduction", domNode);
		}
		catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Find reduction field value and cut off the ending part in braces.
	 * @return	reduction tag
	 */
	@Override
	public String getReductionTagPart()
	{
		if (domNode == null) return null;
		try
		{
			String red = XPathEngine.get().evaluate("./reduction", domNode);
			if (red != null && !red.isEmpty() && red.contains("("))
				return red.substring(0, red.indexOf('('));
			return red;
		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Find reduction field value and cut off the begining part before braces
	 * and braces themselves.
	 * @return	reduction wordform
	 */
	@Override
	public String getReductionFormPart()
	{
		if (domNode == null) return null;
		try
		{
			String red = XPathEngine.get().evaluate("./reduction", domNode);
			if (red == null || red.isEmpty() || !red.contains("("))
				return null;
			red = red.substring(red.indexOf('(')+1);
			if (red.endsWith(")")) red = red.substring(0, red.length()-1);
			return red;
		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Find reduction field value, split in tag and lemma, and then induce lemma
	 * with the help of morphological analyzer.
	 * @return	reduction lemma
	 */
	@Override
	public String getReductionLemma()
	{
		String tag = getReductionTagPart();
		String form = getReductionFormPart();
		if (tag == null || form == null || tag.isEmpty() || form.isEmpty())
			return null;
		return AnalyzerWrapper.getLemma(form, tag);
	}

	/**
	 * Find ord value for this node, if there is one.
	 * @return	ord value, or 0, if no ord found, or null if node is null.
	 */
	@Override
	public Integer getOrd()

	{
		if (domNode == null) return null;
		try
		{
			String ordStr = XPathEngine.get().evaluate("./ord", domNode);
			int ord = 0;
			if (ordStr != null && ordStr.length() > 0)
				ord = Integer.parseInt(ordStr);
			return ord;
		}
		catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}
	/**
	 * Get ord value for given node, if there is one. Otherwise, if this node
	 * has a phrase node (or is a phrase node), use this function on its
	 * constituents and return the smallest. Other-otherwise, if this node is
	 * an empty reduction node, use this function on its children and return the
	 * smallest.
	 * @return	ord value, or 0, if ord can't be found (no children with ord
	 * 			values etc.), or null if node is null.
	 */
	public Integer getDeepOrd ()
	{
		if (domNode == null) return null;
		Integer ord = getOrd();
		if (ord != null && ord > 0) return ord;

		List<PmlANode> children = new ArrayList<>();
		PmlANode phrase = getPhraseNode();
		if (phrase != null) children.add(phrase);
		else if (this.isPhraseNode() || this.isPureReductionNode())
			children.addAll(getChildren());
		if (children.size() < 1) return 0;
		int smallestOrd = 0;
		for (PmlANode child : children)
		{
			int childOrd = child.getDeepOrd();
			if (childOrd > 0 && childOrd < smallestOrd || smallestOrd == 0)
				smallestOrd = childOrd;
		}
		return smallestOrd;
	}

	/**
	 * Find smallest ord number found in the subtree rooted in this node.
	 * @return	ord number or null if none of the nodes in the subtree has an
	 *			ord number.
	 */
	@Override
	public Integer getMinDescOrd()
	{
		int smallestOrd = Integer.MAX_VALUE;
		boolean found = false;
		try
		{
			NodeList ords = (NodeList) XPathEngine.get().evaluate(
						".//ord", domNode, XPathConstants.NODESET);
			for (int j = 0; j < ords.getLength(); j ++)
			{
				int ord = Integer.parseInt(ords.item(j).getTextContent());
				if (ord < smallestOrd)
				{
					smallestOrd = ord;
					found = true;
				}
			}
			if (found) return smallestOrd;
			else return null;
		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}
	/**
	 * Find biggest ord number found in the subtree rooted in this node.
	 * @return	ord number or null if none of the nodes in the subtree has an
	 *			ord number.
	 */
	@Override
	public Integer getMaxDescOrd()
	{
		int biggestOrd = Integer.MIN_VALUE;
		boolean found = false;
		try
		{
			NodeList ords = (NodeList) XPathEngine.get().evaluate(
					".//ord", domNode, XPathConstants.NODESET);
			for (int j = 0; j < ords.getLength(); j ++)
			{
				int ord = Integer.parseInt(ords.item(j).getTextContent());
				if (ord > biggestOrd)
				{
					biggestOrd = ord;
					found = true;
				}
			}
			if (found) return biggestOrd;
			else return null;
		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	//=== Functions to access children, parent, ancestors, etc. ================

	/**
	 * Get underlying PML-M level node.
	 * @return PML-M node or null if such node was not found.
	 */
	@Override
	public XmlDomMNode getM()
	{
		if (domNode == null) return null;
		try
		{
			Node mDom = (Node) XPathEngine.get().evaluate(
					"./m.rf", domNode, XPathConstants.NODE);
			if (mDom == null) return null;
			return new XmlDomMNode(mDom);
			// TODO memorize?
		}
		catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Find PML parent for this node or phrase structure. For a node
	 * representing a phrase constituent this will return phrase node (Type.X,
	 * Type.COORD or Type.PMC) containing it. For a phrase node this will return
	 * node (Type.NODE or Type.ROOT) representing phrase in the sentence.
	 * @return	null for root, otherwise any PML node (can be also phrase node)
	 */
	@Override
	public XmlDomANode getParent()
	{
		if (domNode == null || this.getNodeType() == Type.ROOT) return null;
		try
		{
			return new XmlDomANode( (Node) XPathEngine.get().evaluate(
					"../..", domNode, XPathConstants.NODE));
		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * If this node is a constituent node, find is pmcinfo, coordinfo or xinfo
	 * structure.
	 * @return	phrase, coordination or x-word structure, or null, if there is
	 * 			none.
	 */
	@Override
	public XmlDomANode getPhraseNode()
	{
		if (domNode == null) return null;
		try
		{
			Node tempRes = (Node) XPathEngine.get().evaluate(
					"./children/pmcinfo|./children/coordinfo|./children/xinfo",
					domNode, XPathConstants.NODE);
			if (tempRes == null) return null;
			return new XmlDomANode(tempRes);
		}
		catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Find all node children in PML sense - for normal node this is returns all
	 * dependents, for phrase node - all constituents.
	 * @return	children list with no guaranteed order
	 */
	@Override
	public ArrayList<PmlANode> getChildren()
	{
		if (domNode == null) return null;
		try
		{
			NodeList tempRes = (NodeList) XPathEngine.get().evaluate(
					"./children/node", domNode, XPathConstants.NODESET);
			return XmlDomANode.asList(tempRes);
		}
		catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}
	/**
	 * Find all node children with the given role. For normal node this returns
	 * all dependents with given role, for phrase node - all constituents.
	 * @param role	role restriction
	 * @return	children list with no guaranteed order
	 */
	@Override
	public ArrayList<PmlANode> getChildren(String role)
	{
		if (domNode == null) return null;
		try
		{
			NodeList tempRes = (NodeList) XPathEngine.get().evaluate(
					"./children/node[role='" + role + "']",
					domNode, XPathConstants.NODESET);
			return XmlDomANode.asList(tempRes);
		}
		catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Find descendant node by given ID (thus, no phrase nodes will be found)
	 * @param id	an ID to search
	 * @return	first node found
	 */
	@Override
	public XmlDomANode getDescendant(String id)
	{
		try
		{
			NodeList res = (NodeList) XPathEngine.get().evaluate(
					".//node[@id='"+ id + "']", domNode, XPathConstants.NODESET);
			if (res == null || res.getLength() < 1) return null;
			return new XmlDomANode(res.item(0));
		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Find this or descendant node by given ID (thus, no phrase nodes will be
	 * found)
	 * @param id	an ID to search
	 * @return	first node found
	 */
	@Override
	public XmlDomANode getThisOrDescendant(String id)
	{
		if (id == null) return null;
		if (id.equals(getId())) return this;
		try
		{
			NodeList res = (NodeList) XPathEngine.get().evaluate(
					".//node[@id='"+ id + "']", domNode, XPathConstants.NODESET);
			if (res == null || res.getLength() < 1) return null;
			return new XmlDomANode(res.item(0));
		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Get any descendants of any type. Root is not included.
	 * @return	descendant list
	 */
	@Override
	public ArrayList<PmlANode> getDescendants()
	{
		try
		{
			NodeList res = (NodeList) XPathEngine.get().evaluate(
					".//xinfo|.//coordinfo|.//pmcinfo|.//node",
					domNode, XPathConstants.NODESET);
			if (res == null || res.getLength() < 1) return null;
			return asList(res);
		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Get any descendants whose role, xtype, coordtype, or pmctype matches
	 * the given label. Root is not included.
	 * @param anyLabel	label to restrict search
	 * @return	list with found descendants
	 */
	@Override
	public ArrayList<PmlANode> getDescendants(String anyLabel)
	{
		try
		{
			NodeList res = (NodeList) XPathEngine.get().evaluate(
					".//xinfo[xtype/text()='"+ anyLabel + "']|" +
					".//coordinfo[coordtype/text()='"+ anyLabel + "']|" +
					".//pmcinfo[pmctype/text()='"+ anyLabel + "']|" +
					".//node[role/text()='"+ anyLabel + "']",
					domNode, XPathConstants.NODESET);
			if (res == null || res.getLength() < 1) return null;
			return asList(res);
		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Get all descendants having both morphology and ord value. Root is not
	 * included.
	 * @return	descendant list sorted by ord values
	 */
	@Override
	public ArrayList<PmlANode> getDescendantsWithOrdAndM()
	{
		try
		{
			NodeList res = (NodeList) XPathEngine.get().evaluate(
					".//node[m.rf and ord]",
					domNode, XPathConstants.NODESET);
			if (res == null || res.getLength() < 1) return null;
			return PmlANodeListUtils.asOrderedList(asList(res));
		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Find pure ellipsis (no corresponding token) in the subtree headed by this
	 * node. Parameter allows to find either all ellipsis or only leaf nodes.
	 * @param leafsOnly	if true, only leaf nodes are returned
	 * @return	list of ellipsis nodes in no particular order
	 */
	@Override
	public ArrayList<PmlANode> getPureEllipsisDescendants(boolean leafsOnly)
	{
		String pattern = leafsOnly
				? ".//node[reduction and not(m.rf) and not(children)]"
				: ".//node[reduction and not(m.rf)]";
		try
		{
			NodeList tempRes = (NodeList) XPathEngine.get().evaluate(
					pattern, domNode, XPathConstants.NODESET);
			return XmlDomANode.asList(tempRes);
		}
		catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Find ellipsis nodes with corresponding token in the subtree headed by
	 * this node. Parameter allows to find either all ellipsis or only leaf
	 * nodes.
	 * @param leafsOnly	if true, only leaf nodes are returned
	 * @return	list of ellipsis nodes in no particular order
	 */
	@Override
	public List<PmlANode> getMorphoEllipsisDescendants(boolean leafsOnly)
	{
		String pattern = leafsOnly
				? ".//node[reduction and m.rf and not(children)]"
				: ".//node[reduction and m.rf]";
		try
		{
			NodeList tempRes = (NodeList) XPathEngine.get().evaluate(
					pattern, domNode, XPathConstants.NODESET);
			return XmlDomANode.asList(tempRes);
		}
		catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Find nodes having m-token, but no w-token.
	 * @return list of inserted token nodes in no particular order
	 */
	@Override
	public List<PmlANode> getInsertedMorphoDescendants()
	{
		String pattern = ".//node[m.rf and not(w.rf)]";
		try
		{
			NodeList tempRes = (NodeList) XPathEngine.get().evaluate(
					pattern, domNode, XPathConstants.NODESET);
			return XmlDomANode.asList(tempRes);
		}
		catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Find parent or the closest ancestor, that is not coordination phrase or
	 * crdPart node.
	 * @return	PML a-level node or xinfo, pmcinfo, or coordinfo
	 */
	@Override
	public XmlDomANode getEffectiveAncestor()
	{
		if (domNode == null || getNodeType() == Type.ROOT) return null;
		return getParent().getThisOrEffectiveAncestor();
	}

	/**
	 * Return this node, parent or the closest ancestor, that is not
	 * coordination phrase or crdPart node.
	 * @return	PML a-level node or xinfo, pmcinfo, or coordinfo
	 */
	@Override
	public XmlDomANode getThisOrEffectiveAncestor()
	{
		if (domNode == null || getNodeType() == Type.ROOT) return null;
		XmlDomANode res = this;
		String resType = res.getAnyLabel();
		while (resType.equals(LvtbRoles.CRDPART) ||
				resType.equals(LvtbCoordTypes.CRDCLAUSES) ||
				resType.equals(LvtbCoordTypes.CRDPARTS))
		{
			res = res.getParent();
			resType = res.getAnyLabel();
		}
		return res;
	}

	//=== Comparison ===========================================================

	/**
	 * Returns whether this node is the same node as the given one. This is used
	 * to determine if no circular dependencies are drawn etc. This
	 * implementation is not smart enough to compare PmlANodes from different
	 * implementation classes.
	 * @param other	the node to test against
	 * @return	true if the nodes are the same, false otherwise.
	 */
	@Override
	public boolean isSameNode(PmlANode other)
	{
		if (this == other || this.equals(other)) return true;
		if (other == null) return false;
		try
		{
			XmlDomANode castedOther = (XmlDomANode) other;
			if (domNode == castedOther.domNode
					|| domNode.equals(castedOther.domNode)
					|| domNode.isSameNode(castedOther.domNode))
				return true;
		}
		catch (ClassCastException e) {};
		return false;
	}

	/**
	 * Returns the lenght of the shortest path connecting this node and root.
	 * @return	0 for root node, 1 for root's dependents and constituents, 2 for
	 * 			for their dependents and constituents, etc.
	 */
	@Override
	public Integer getDepthInTree()
	{
		try
		{
			NodeList res = (NodeList) XPathEngine.get().evaluate(
					"ancestor-or-self::node",
					domNode, XPathConstants.NODESET);
			if (res == null || res.getLength() < 1) return 0;
			return res.getLength();
		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	//=== Tree modification ====================================================

	/**
	 * Remove this node from tree.
	 */
	public void delete()
	{
		domNode.getParentNode().removeChild(domNode);
	}

	/**
	 * Remove this nodes m-node.
	 */
	@Override
	public void deleteM()
	{
		try
		{
			Node mDom = (Node) XPathEngine.get().evaluate(
					"./m.rf", domNode, XPathConstants.NODE);
			domNode.removeChild(mDom);
		}
		catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Set a phraseTag for X or COORD node, return false for other node types.
	 * @param tag	tag value to set
	 * @return	true if tag was set
	 */
	@Override
	public boolean setPhraseTag(String tag)
	{
		PmlANode.Type type = getNodeType();
		if (type != PmlANode.Type.X && type != PmlANode.Type.COORD)
			return false;

		try
		{
			Node tagNode = (Node) XPathEngine.get().evaluate(
					"./tag", domNode, XPathConstants.NODE);
			if (tagNode == null)
				tagNode = domNode.getOwnerDocument().createElement("tag");
			while (tagNode.getFirstChild() != null)
				tagNode.removeChild(tagNode.getFirstChild());
			tagNode.appendChild(domNode.getOwnerDocument().createTextNode(tag));
			domNode.appendChild(tagNode);
			return true;
		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Set a reduction tag, if there is none, return false otherwise.
	 * @param tag	tag value to set
	 * @return	true if tag was set
	 */
	public boolean setReductionTag(String tag)
	{
		String oldRedTag = getReductionTagPart();
		if (oldRedTag != null && !oldRedTag.trim().isEmpty()) return false;
		String oldRedForm = getReductionFormPart();
		String newRedField = tag;
		if (newRedField == null) newRedField = "";
		newRedField = newRedField.trim();
		if (oldRedForm != null) newRedField = newRedField + "(" + oldRedForm + ")";
		try
		{
			Node tagNode = (Node) XPathEngine.get().evaluate(
					"./reduction", domNode, XPathConstants.NODE);
			if (tagNode == null) tagNode = domNode.getOwnerDocument().createElement("reduction");
			while (tagNode.getFirstChild() != null)
				tagNode.removeChild(tagNode.getFirstChild());
			//if (tag != null && !tag.isEmpty())
			//{
				tagNode.appendChild(domNode.getOwnerDocument().createTextNode(newRedField));
				domNode.appendChild(tagNode);
			//}
			return true;
		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}
	/**
	 * Set a reduction form, if there is none, return false otherwise.
	 * @param form	form value to set
	 * @return	true if form was set
	 */
	@Override
	public boolean setReductionForm(String form)
	{
		String oldRedForm = getReductionFormPart();
		if (oldRedForm != null && !oldRedForm.trim().isEmpty()) return false;
		String oldRedTag = getReductionTagPart();
		String newRedField = "";
		if (form != null) newRedField = "(" + form.trim() + ")";
		if (oldRedTag != null) newRedField = oldRedTag + newRedField;
		try
		{
			Node tagNode = (Node) XPathEngine.get().evaluate(
					"./reduction", domNode, XPathConstants.NODE);
			if (tagNode == null) tagNode = domNode.getOwnerDocument().createElement("reduction");
			while (tagNode.getFirstChild() != null)
				tagNode.removeChild(tagNode.getFirstChild());
			//if (form != null && !form.isEmpty())
			//{
				tagNode.appendChild(domNode.getOwnerDocument().createTextNode(newRedField));
				domNode.appendChild(tagNode);
			//}
			return true;
		} catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}


	/**
	 * Split nonempty ellipsis node into empty ellipsis node and dependant
	 * child.
	 * @param idPostfix	string to append to the node ID to create ID for new
	 *                  node.
	 * @return	if an actual split was done
	 */
	@Override
	public boolean splitMorphoEllipsis(String idPostfix)
	{
		if (isPureReductionNode()) return false;
		String reductionField = getReduction();
		if (reductionField == null || reductionField.isEmpty()) return false;
		try
		{
			XmlDomANode parentToAppend = getParent();
			//if (!parentToAppend.isPhraseNode()) parentToAppend = this;
			if (!parentToAppend.isPhraseNode() || parentToAppend.getNodeType().equals(Type.COORD))
				parentToAppend = this;
			Document ownerDoc = domNode.getOwnerDocument();
			// Children container
			Node childenNode = (Node) XPathEngine.get().evaluate(
					"./children", parentToAppend.domNode, XPathConstants.NODE);
			if (childenNode == null)
			{
				childenNode = ownerDoc.createElement("children");
				parentToAppend.domNode.appendChild(childenNode);
			}

			// Node itself
			Element newTokenNode = ownerDoc.createElement("node");
			childenNode.appendChild(newTokenNode);

			// id attribute
			String newId = getId() + idPostfix;
			newTokenNode.setAttribute("id", newId);

			// Move morphology
			Node mDom = (Node) XPathEngine.get().evaluate(
					"./m.rf", domNode, XPathConstants.NODE);
			domNode.removeChild(mDom);
			newTokenNode.appendChild(mDom);

			// Move ord
			Node ord = (Node) XPathEngine.get().evaluate(
					"./ord", domNode, XPathConstants.NODE);
			domNode.removeChild(ord);
			newTokenNode.appendChild(ord);

			// Role.
			Node roleNode = ownerDoc.createElement("role");
			newTokenNode.appendChild(roleNode);
			roleNode.appendChild(ownerDoc.createTextNode(LvtbRoles.ELLIPSIS_TOKEN));

			return true;
		}
		catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	//=== Helpers ==============================================================

	/**
	 * Transform NodeList to ArrayList of PmlANode.
	 * @param nodes	list to tranform
	 * @return	transformed list (original node ordering is preserved)
	 * TODO: is there a more optimal implementation?
	 */
	protected static ArrayList<PmlANode> asList (NodeList nodes)
	{
		if (nodes == null) return null;
		ArrayList<PmlANode> res = new ArrayList<>();
		for (int i = 0; i < nodes.getLength(); i++)
			res.add(new XmlDomANode(nodes.item(i)));
		return res;
	}
}
