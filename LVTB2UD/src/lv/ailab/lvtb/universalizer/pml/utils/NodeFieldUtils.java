package lv.ailab.lvtb.universalizer.pml.utils;

import lv.ailab.lvtb.universalizer.pml.LvtbCoordTypes;
import lv.ailab.lvtb.universalizer.pml.LvtbHelperRoles;
import lv.ailab.lvtb.universalizer.pml.LvtbRoles;
import lv.ailab.lvtb.universalizer.transformator.Logger;
import lv.ailab.lvtb.universalizer.transformator.morpho.AnalyzerWrapper;
import lv.ailab.lvtb.universalizer.utils.XPathEngine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.PrintWriter;

/**
 * Utility methods for processing PML XML node's fields.
 * Created on 2016-04-22. Splited in multiple classes on 2018-01-12.
 *
 * @author Lauma
 */
public class NodeFieldUtils
{
	/**
	 * Find ord value for given node, if there is one.
	 * @param node node to analyze
	 * @return	ord value, or 0, if no ord found, or -1 if node is null.
	 * @throws XPathExpressionException    unsuccessfull XPathevaluation (anywhere
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
		NodeList children = NodeUtils.getAllPMLChildren(node);
		if (children == null || children.getLength() < 1) return 0;
		int smallestOrd = 0;
		for (int i = 0; i < children.getLength(); i++)
		{
			int childOrd = getDeepOrd(children.item(i));
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
	 * Find reduction field value for given node, split in tag and lemma, and
	 * then induce lemma with the help of morphological analyzer.
	 * @param node		node to analyze
	 * @param logger	where to print errors
	 * @return	reduction lemma
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static String getReductionLemma(Node node, Logger logger)
	throws XPathExpressionException
	{
		String tag = getReductionTagPart(node);
		String form = getReductionFormPart(node);
		if (tag == null || form == null || tag.isEmpty() || form.isEmpty())
			return null;
		return AnalyzerWrapper.getLemma(form, tag, logger);
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
		Node phraseNode;

		if (!NodeUtils.isPhraseNode(aNode))
		{
			String tag = XPathEngine.get().evaluate("./m.rf/tag", aNode);
			if (tag != null && tag.length() > 0) return tag;
			tag = getReduction(aNode);
			if (tag != null && tag.contains("("))
				tag = tag.substring(0, tag.indexOf("(")).trim();
			if (tag != null && tag.length() > 0) return tag;
			phraseNode = NodeUtils.getPhraseNode(aNode);
		}
		else phraseNode = aNode;

		if (phraseNode == null) return null;
		String tag = XPathEngine.get().evaluate("./tag", phraseNode);
		if (tag != null && tag.length() > 0) return tag;

		NodeList baseParts = (NodeList) XPathEngine.get().evaluate(
				"./children/node[role='" + LvtbRoles.PRED + "']",
				phraseNode, XPathConstants.NODESET);
		if (baseParts != null && baseParts.getLength() > 0)
			return getTag(NodeListUtils.getFirstByDescOrd(baseParts));
		baseParts = (NodeList) XPathEngine.get().evaluate(
				"./children/node[role='" + LvtbRoles.BASELEM + "']",
				phraseNode, XPathConstants.NODESET);
		if (baseParts != null && baseParts.getLength() > 0)
			return getTag(NodeListUtils.getFirstByDescOrd(baseParts));
		baseParts = (NodeList) XPathEngine.get().evaluate(
				"./children/node[role='" + LvtbRoles.CRDPART + "']",
				phraseNode, XPathConstants.NODESET);
		if (baseParts != null && baseParts.getLength() > 0)
			return getTag(NodeListUtils.getFirstByDescOrd(baseParts));
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
		if (NodeUtils.isRoot(node)) return LvtbHelperRoles.ROOT;
		return XPathEngine.get().evaluate(
				"./role|./pmctype|./coordtype|./xtype", node);
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
			return getAnyLabel(NodeUtils.getEffectiveAncestor(node));
		return label;
	}
}
