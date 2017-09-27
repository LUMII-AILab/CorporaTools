package lv.ailab.lvtb.universalizer.pml;

import lv.ailab.lvtb.universalizer.util.XPathEngine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Utility methods for processing PML XMLs.
 * Created on 2016-04-22.
 *
 * @author Lauma
 */
public class Utils
{
	/**
	 * Find ord value for given node, if there is one.
	 * @param node node to analyze
	 * @return	ord value, or 0, if no ord found, or -1 if node is null.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static int getOrd(Node node) throws XPathExpressionException
	{
		if (node == null) return -1;
		String ordStr = XPathEngine.get().evaluate("./ord", node);
		int ord = 0;
		if (ordStr != null && ordStr.length() > 0) ord = Integer.parseInt(ordStr);
		return ord;
	}

	/**
	 * Get ord value for given node, if there is one. Otherwise get smallest
	 * children ord value.
	 * @param node	node to analyze
	 * @return	ord value, or 0, if no ord found, or -1 if node is null.
	 */
	public static int getDeepOrd (Node node) throws XPathExpressionException
	{
		if (node == null) return -1;
		String ordStr = XPathEngine.get().evaluate("./ord", node);
		if (ordStr != null && ordStr.length() > 0) return Integer.parseInt(ordStr);
		NodeList children = Utils.getAllPMLChildren(node);
		if (children == null || children.getLength() < 1) return 0;
		int smallestOrd = 0;
		for (int i = 0; i < children.getLength(); i++)
		{
			int childOrd = Utils.getDeepOrd(children.item(i));
			if (childOrd > 0 && childOrd < smallestOrd || smallestOrd == 0)
				smallestOrd = childOrd;
		}
		return smallestOrd;
	}

	/**
	 * Find id attribute for given node.
	 * @param node node to analyze
	 * @return	attribute value
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static String getId(Node node) throws XPathExpressionException
	{
		if (node == null) return null;
		return XPathEngine.get().evaluate("./@id", node);
	}

	/**
	 * Find m level id attribute for given node.
	 * @param node node to analyze
	 * @return	attribute value
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static String getMId(Node node) throws XPathExpressionException
	{
		if (node == null) return null;
		return XPathEngine.get().evaluate("./m.rf/@id", node);
	}

	/**
	 * Find reduction field value for given node.
	 * @param node node to analyze
	 * @return	reduction value
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static String getReduction(Node node) throws XPathExpressionException
	{
		if (node == null) return null;
		return XPathEngine.get().evaluate("./reduction", node);
	}

	/**
	 * Find reduction field value for given node and cut off the ending part in braces.
	 * @param node node to analyze
	 * @return	reduction tag
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static String getReductionTagPart(Node node) throws XPathExpressionException
	{
		if (node == null) return null;
		String red = XPathEngine.get().evaluate("./reduction", node);
		if (red != null && !red.isEmpty() && red.contains("("))
			return red.substring(0, red.indexOf('('));
		return red;
	}
	/**
	 * Find reduction field value for given node and cut off the begining part
	 * before braces and braces themselves.
	 * @param node node to analyze
	 * @return	reduction wordform
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static String getReductionFormPart(Node node) throws XPathExpressionException
	{
		if (node == null) return null;
		String red = XPathEngine.get().evaluate("./reduction", node);
		if (red == null || red.isEmpty() || !red.contains("("))
			return null;
		red = red.substring(red.indexOf('(')+1);
		if (red.endsWith(")")) red = red.substring(0, red.length()-1);
		return red;
	}
	/**
	 * Check, if given node is a reduction node
	 * @param node node to analyze
	 * @return	true, if node has no morphology field and has nonempty reduction
	 * 			field.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static boolean isReductionNode(Node node)
	throws XPathExpressionException
	{
		if (node == null) return false;
		String reduction = getReduction(node);
		return (Utils.getMNode(node) == null && reduction != null && reduction.length() > 0);
	}

	/**
	 * Check, if given node is a phrase node
	 * @param node node to analyze
	 * @return	true, if node has xtype, pmctype or coordtype.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static boolean isPhraseNode(Node node)
			throws XPathExpressionException
	{
		if (node == null) return false;
		return (node.getNodeName().equals("xinfo")
				|| node.getNodeName().equals("coordinfo")
				|| node.getNodeName().equals("pmcinfo"));
	}
	/**
	 * Find role for given node.
	 * @param node node to analyze
	 * @return	role value
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static String getRole(Node node) throws XPathExpressionException
	{
		if (node == null) return null;
		return XPathEngine.get().evaluate("./role", node);
	}

	/**
	 * FInd m node for for given node.
	 * @param node node to analyze
	 * @return	m node
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static Node getMNode(Node node) throws XPathExpressionException
	{
		if (node == null) return null;
		return (Node) XPathEngine.get().evaluate(
				"./m.rf", node, XPathConstants.NODE);
	}

	/**
	 * Find lemma for given node.
	 * @param node node to analyze
	 * @return	lemma
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static String getLemma(Node node) throws XPathExpressionException
	{
		if (node == null) return null;
		return XPathEngine.get().evaluate("./m.rf/lemma", node);
	}

	/**
	 * Find tag attribute A-level node. Use either morphotag or x-word tag or
	 * coordination tag. For tokenless reduction nodes returns reduction tag.
	 * For PMC node returns first basElem's tag. Based on assumption, that
	 * single aNode has no more than one phrase-child. For coordinations with no
	 * given tag return tag obtained from first coordinated part.
	 * @param aNode	node to analyze
	 * @return	tag
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static String getTag(Node aNode) throws XPathExpressionException
	{
		if (aNode == null) return null;
		String tag = XPathEngine.get().evaluate("./m.rf/tag|./children/xinfo/tag|./children/coordinfo/tag", aNode);
		if (tag != null && tag.length() > 0) return tag;
		tag = Utils.getReduction(aNode);
		if (tag != null && tag.contains("(")) tag = tag.substring(0, tag.indexOf("(")).trim();
		if (tag != null && tag.length() > 0) return tag;

		NodeList baseParts = (NodeList) XPathEngine.get().evaluate(
				"./children/pmcinfo/children/node[role='" + LvtbRoles.PRED + "']",
				aNode, XPathConstants.NODESET);
		if (baseParts != null && baseParts.getLength() > 0)
			return getTag(getFirstByDescOrd(baseParts));
		baseParts = (NodeList) XPathEngine.get().evaluate(
				"./children/pmcinfo/children/node[role='" + LvtbRoles.BASELEM + "']",
				aNode, XPathConstants.NODESET);
		if (baseParts != null && baseParts.getLength() > 0)
			return getTag(getFirstByDescOrd(baseParts));
		baseParts = (NodeList) XPathEngine.get().evaluate(
				"./children/coordinfo/children/node[role='" + LvtbRoles.CRDPART + "']",
				aNode, XPathConstants.NODESET);
		if (baseParts != null && baseParts.getLength() > 0)
			return getTag(getFirstByDescOrd(baseParts));
		return null;
	}

	/**
	 * Find pmctype, coordtype or xtype for a given node.
	 * @param phraseNode	node to analyze
	 * @return	phrase type
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static String getPhraseType(Node phraseNode)
	throws XPathExpressionException
	{
		if (phraseNode == null) return null;
		return XPathEngine.get().evaluate(
				"./pmctype|./coordtype|./xtype", phraseNode);
	}

	/**
	 * Find pmctype, coordtype, xtype or role for a given node.
	 * @param node	node to analyze
	 * @return	phrase type or dependency role or LVtbHelperRoles.ROOT for root.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static String getAnyLabel(Node node)
	throws XPathExpressionException
	{
		if (node == null) return null;
		if (isRoot(node)) return LvtbHelperRoles.ROOT;
		return XPathEngine.get().evaluate(
				"./role|./pmctype|./coordtype|./xtype", node);
	}

	public static boolean isRoot (Node node)
			throws XPathExpressionException
	{
		String name = node.getNodeName();
		if ("trees".equals(name)) return true;
		else if ("LM".equals(name))
		{
			Node parent = (Node) XPathEngine.get().evaluate(
					"../..", node, XPathConstants.NODE);
			if (parent == null) return true;
			String parentName = parent.getNodeName();
			return "trees".equals(parentName);
		}
		return false;
	}

	/**
	 * Find the closest ancestor (given node included), whose label is not
	 * crdPart, crdParts or crdClauses.
	 * @param node	node to analyze
	 * @return	phrase type or dependency role or LVtbHelperRoles.ROOT for root.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static String getEffectiveLabel(Node node)
	throws XPathExpressionException
	{
		String label = getAnyLabel(node);
		if (label.equals(LvtbRoles.CRDPART) ||
				label.equals(LvtbCoordTypes.CRDCLAUSES) ||
				label.equals(LvtbCoordTypes.CRDPARTS))
			return getAnyLabel(getEffectiveAncestor(node));
		return label;
	}

	/**
	 * If this node is a constituent node, find is pmcinfo, coordinfo or xinfo
	 * structure.
	 * @param aNode	node to analyze
	 * @return	phrase, coordination or x-word structure
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static Node getPhraseNode(Node aNode)
	throws XPathExpressionException
	{
		if (aNode == null) return null;
		return (Node) XPathEngine.get().evaluate(
				"./children/pmcinfo|./children/coordinfo|./children/xinfo", aNode, XPathConstants.NODE);
	}

	/**
	 * Find all children of the given node in PML sense. xinfo, pmcinfo and
	 * coordinfo are included in result, if present.
	 * @param node	node to analyze
	 * @return	children set
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static NodeList getAllPMLChildren(Node node)
	throws XPathExpressionException
	{
		if (node == null) return null;
		return (NodeList)XPathEngine.get().evaluate(
				"./children/*", node, XPathConstants.NODESET);
	}
	/**
	 * Find all descendants of the given node in PML sense. xinfo, pmcinfo and
	 * coordinfo are included in result, if present.
	 * @param node	node to analyze
	 * @return	ancestor set
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static NodeList getAllPMLDescendants(Node node)
	throws XPathExpressionException
	{
		if (node == null) return null;
		return (NodeList)XPathEngine.get().evaluate(
				".//children/*", node, XPathConstants.NODESET);
	}

	/**
	 * Find all node children of the given node in PML sense - for normal node
	 * this is returns all dependents, for phrase node - all constituents.
	 * @param node	node to analyze
	 * @return	children set
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static NodeList getPMLNodeChildren(Node node)
	throws XPathExpressionException
	{
		if (node == null) return null;
		return (NodeList)XPathEngine.get().evaluate(
				"./children/node", node, XPathConstants.NODESET);
	}
	/**
	 * Find parent node (or phrase structure) in PML sense.
	 * @param node	node to analyze
	 * @return	PML a-level node or xinfo, pmcinfo, or coordinfo
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static Node getPMLParent(Node node) throws XPathExpressionException
	{
		if (node == null || isRoot(node)) return null;
		return (Node) XPathEngine.get().evaluate(
				"../..", node, XPathConstants.NODE);
	}

	/**
	 * Find parent or the closest ancestor, that is not coordination phrase or
	 * crdPart node.
	 * @param node	node to analyze
	 * @return	PML a-level node or xinfo, pmcinfo, or coordinfo
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static Node getEffectiveAncestor(Node node) throws XPathExpressionException
	{
		if (node == null || isRoot(node)) return null;
		Node res = getPMLParent(node);
		String resType = getAnyLabel(res);
		while (resType.equals(LvtbRoles.CRDPART) ||
				resType.equals(LvtbCoordTypes.CRDCLAUSES) ||
				resType.equals(LvtbCoordTypes.CRDPARTS))
		{
			res = getPMLParent(res);
			resType = getAnyLabel(res);
		}

		return res;
	}

	/**
	 * Find grandparent node (or phrase structure) in PML sense.
	 * @param node	node to analyze
	 * @return	PML a-level node or xinfo, pmcinfo, or coordinfo
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static Node getPMLGrandParent(Node node) throws XPathExpressionException
	{
		if (node == null) return null;
		return (Node) XPathEngine.get().evaluate(
				"../../../..", node, XPathConstants.NODE);
	}

	/**
	 * Find great grandparent node (or phrase structure) in PML sense.
	 * @param node	node to analyze
	 * @return	PML a-level node or xinfo, pmcinfo, or coordinfo
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static Node getPMLGreatGrandParent(Node node) throws XPathExpressionException
	{
		if (node == null) return null;
		return (Node) XPathEngine.get().evaluate(
				"../../../../../..", node, XPathConstants.NODE);
	}


	/**
	 * Create splice array that contains all nodes from the given array, to whom
	 * begin <= ord < end.
	 * @param nodes	list of nodes to splice
	 * @param begin smallest index (inclusive)
	 * @param end	largest index (excluse)
	 * @return	list with all elements satisfying the criterion, ordered in the
	 * 			same order as in input data
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */

	public static ArrayList<Node> ordSplice(List<Node> nodes, int begin, int end)
	throws XPathExpressionException
	{
		if (nodes == null) return null;
		ArrayList<Node> res = new ArrayList<>();
		for (Node n : nodes)
		{
			int ord = getDeepOrd(n);
			if (ord >= begin && ord < end) res.add(n);
		}
		return res;
	}

	/**
	 * Find node with the smallest ord value in its descendants.
	 * @param nodes list of nodes where to search
	 * @return	node with smallest given ord value in descendants
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static Node getFirstByDescOrd(NodeList nodes)
	throws XPathExpressionException
	{
		if (nodes == null) return null;
		if (nodes.getLength() == 1) return nodes.item(0);
		int smallestOrd = Integer.MAX_VALUE;
		Node bestNode = null;
		for (int i = 0; i < nodes.getLength(); i++)
		{
			NodeList ords = (NodeList)XPathEngine.get().evaluate(
					".//ord", nodes.item(i), XPathConstants.NODESET);
			for (int j = 0; j < ords.getLength(); j ++)
			{
				int ord = Integer.parseInt(ords.item(j).getTextContent());
				if (ord < smallestOrd)
				{
					smallestOrd = ord;
					bestNode = nodes.item(i);
				}
			}
		}
		return bestNode;
	}

	/**
	 * Find node with the biggest ord value in its descendants.
	 * @param nodes list of nodes where to search
	 * @return	node with smallest given ord value in descendants
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static Node getLastByDescOrd(NodeList nodes)
	throws XPathExpressionException
	{
		if (nodes == null) return null;
		if (nodes.getLength() == 1) return nodes.item(0);
		int biggestOrd = Integer.MIN_VALUE;
		Node bestNode = null;
		for (int i = 0; i < nodes.getLength(); i++)
		{
			NodeList ords = (NodeList)XPathEngine.get().evaluate(
					".//ord", nodes.item(i), XPathConstants.NODESET);
			for (int j = 0; j < ords.getLength(); j ++)
			{
				int ord = Integer.parseInt(ords.item(j).getTextContent());
				if (ord > biggestOrd)
				{
					biggestOrd = ord;
					bestNode = nodes.item(i);
				}
			}
		}
		return bestNode;
	}

	/**
	 * Find node with the biggest ord value. Nodes with no ord are ignored.
	 * @param nodes list of nodes where to search
	 * @return	node with largest given ord value
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static Node getLastByOrd(NodeList nodes)
	throws XPathExpressionException
	{
		if (nodes == null) return null;
		if (nodes.getLength() == 1) return nodes.item(0);
		int biggestOrd = Integer.MIN_VALUE;
		Node bestNode = null;
		for (int i = 0; i < nodes.getLength(); i++)
		{
			int ord = Utils.getOrd(nodes.item(i));
			if (ord > 0 && ord > biggestOrd)
			{
				biggestOrd = ord;
				bestNode = nodes.item(i);
			}
		}
		return bestNode;
	}

	/**
	 * Transform NodeList to ArrayList of Node.
	 * @param nodes	list to tranform
	 * @return	transformed list (original node ordering is preserved)
	 * TODO: is there a more optimal implementation?
	 */
	public static ArrayList<Node> asList (NodeList nodes)
	{
		if (nodes == null) return null;
		ArrayList<Node> res = new ArrayList<>();
		for (int i = 0; i < nodes.getLength(); i++)
			res.add(nodes.item(i));
		return res;
	}

	/**
	 * Transform NodeList to ArrayList of Node and sort list by ord values.
	 * @param nodes	list to sort
	 * @return	sorted list
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static ArrayList<Node> asOrderedList(NodeList nodes)
	throws XPathExpressionException
	{
		// TODO: is there some more effective way?
		TreeMap<Integer, ArrayList<Node>> semiRes = new TreeMap<>();

		for (int i = 0; i < nodes.getLength(); i++)
		{
			/*int smallestOrd = Integer.MAX_VALUE;
			NodeList ords = (NodeList)XPathEngine.get().evaluate(".//ord", nodes.item(i), XPathConstants.NODESET);
			for (int j = 0; j < ords.getLength(); j ++)
			{
				int ord = Integer.parseInt(ords.item(j).getTextContent());
				if (ord < smallestOrd)smallestOrd = ord;
			}
			if (smallestOrd == Integer.MAX_VALUE) smallestOrd = 0;
			if (!semiRes.containsKey(smallestOrd)) semiRes.put(smallestOrd, new ArrayList<>());
			semiRes.get(smallestOrd).add(nodes.item(i));*/
			Node n = nodes.item(i);
			int ord = Utils.getDeepOrd(n);
			if (!semiRes.containsKey(ord)) semiRes.put(ord, new ArrayList<>());
			semiRes.get(ord).add(n);
		}
		ArrayList<Node> res = new ArrayList<>();
		for (Integer ordKey : semiRes.keySet())
			res.addAll(semiRes.get(ordKey));

		return res;
	}
}
