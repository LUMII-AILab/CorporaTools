package lv.ailab.lvtb.universalizer.pml.utils;

import lv.ailab.lvtb.universalizer.pml.LvtbCoordTypes;
import lv.ailab.lvtb.universalizer.pml.LvtbRoles;
import lv.ailab.lvtb.universalizer.utils.XPathEngine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

/**
 * Utility methods for processing PML XMLs.
 * Created on 2016-04-22. Splited in multiple classes on 2018-01-12.
 *
 * @author Lauma
 */
public class NodeUtils
{
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
		String reduction = NodeFieldUtils.getReduction(node);
		return (NodeUtils.getMNode(node) == null && reduction != null && reduction.length() > 0);
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
		String resType = NodeFieldUtils.getAnyLabel(res);
		while (resType.equals(LvtbRoles.CRDPART) ||
				resType.equals(LvtbCoordTypes.CRDCLAUSES) ||
				resType.equals(LvtbCoordTypes.CRDPARTS))
		{
			res = getPMLParent(res);
			resType = NodeFieldUtils.getAnyLabel(res);
		}

		return res;
	}

	/**
	 * Return this node, parent or the closest ancestor, that is not
	 * coordination phrase or crdPart node.
	 * @param node	node to analyze
	 * @return	PML a-level node or xinfo, pmcinfo, or coordinfo
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static Node getThisOrEffectiveAncestor(Node node)
			throws XPathExpressionException
	{
		if (node == null || isRoot(node)) return null;
		Node res = node;
		String resType = NodeFieldUtils.getAnyLabel(res);
		while (resType.equals(LvtbRoles.CRDPART) ||
				resType.equals(LvtbCoordTypes.CRDCLAUSES) ||
				resType.equals(LvtbCoordTypes.CRDPARTS))
		{
			res = getPMLParent(res);
			resType = NodeFieldUtils.getAnyLabel(res);
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


}
