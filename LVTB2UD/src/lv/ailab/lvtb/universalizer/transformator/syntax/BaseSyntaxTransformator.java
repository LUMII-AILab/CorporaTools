package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.Utils;
import lv.ailab.lvtb.universalizer.transformator.Sentence;
import lv.ailab.lvtb.universalizer.util.XPathEngine;
import lv.ailab.lvtb.universalizer.util.Tuple;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.PrintWriter;

public class BaseSyntaxTransformator
{
	public Sentence s;
	/**
	 * Indication that transformation has failed and the obtained conll data is
	 * garbage.
	 */
	public boolean hasFailed;
	public boolean inducePhraseTags;
	public boolean debug;
	protected PhraseTransformator pTransf;
	protected DepRelLogic drLogic;
	protected PrintWriter warnOut;
	public BaseSyntaxTransformator(Sentence sent, PrintWriter warnOut,
								   boolean inducePhraseTags, boolean debug)
	{
		s = sent;
		this.warnOut = warnOut;
		this.inducePhraseTags = inducePhraseTags;
		this.debug = debug;
		hasFailed = false;
		pTransf = new PhraseTransformator(s, warnOut);
		drLogic = new DepRelLogic();
	}

	/**
	 * Remove the ellipsis nodes that can be ignored in latter processing.
	 * @return	 true if all ellipsis was removed
	 * @throws XPathExpressionException unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public boolean preprocessEmptyEllipsis() throws XPathExpressionException
	{
		// Childless, empty reductions are removed.
		NodeList ellipsisChildren = (NodeList) XPathEngine.get().evaluate(
				".//node[reduction and not(m.rf) and not(children)]", s.pmlTree, XPathConstants.NODESET);
		if (ellipsisChildren != null) for (int i = 0; i < ellipsisChildren.getLength(); i++)
		{
			Node current = ellipsisChildren.item(i);
			//Node morpho = Utils.getMNode(current);
			//Node phraseChild = Utils.getPhraseNode(current);
			//NodeList children = Utils.getAllPMLChildren(current);
			//if (morpho == null && phraseChild == null &&
			//		(children == null || children.getLength() < 1))
			current.getParentNode().removeChild(current);
		}

		// Check if there is other reductions.
		ellipsisChildren = (NodeList) XPathEngine.get().evaluate(
				".//node[reduction and not(m.rf)]", s.pmlTree, XPathConstants.NODESET);
		if (ellipsisChildren != null && ellipsisChildren.getLength() > 0) return false;
		/*for (int i = 0; i < ellipsisChildren.getLength(); i++)
		{
					NodeList morpho = (NodeList) XPathEngine.get().evaluate(
							"../../m.rf", ellipsisChildren.item(i), XPathConstants.NODESET);
			if (morpho == null || morpho.getLength() < 1) return false;
		}*/

		return true;
	}

	/**
	 * Fill in DEPREL and HEAD fields in CoNLL-U table.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public void transformBaseSyntax() throws XPathExpressionException
	{
		Node pmlPmc = (Node)XPathEngine.get().evaluate(
				"./children/pmcinfo", s.pmlTree, XPathConstants.NODE);
		transformDepSubtrees(s.pmlTree);
		if (hasFailed) return;
		transformPhraseParts(pmlPmc);
		if (hasFailed) return;

		Node newRoot = pTransf.anyPhraseToUD(pmlPmc);
		if (newRoot == null)
			throw new IllegalArgumentException("Sentence " + s.id +" has no root PMC.");
		Token conllRoot = s.pmlaToConll.get(Utils.getId(newRoot));
		s.pmlaToConll.put(Utils.getId(s.pmlTree), conllRoot);
		conllRoot.head = Tuple.of("0", null);
		conllRoot.deprel = UDv2Relations.ROOT;
		relinkDependents(s.pmlTree, newRoot);
	}

	/**
	 * Helper method: find all dependency children and process subtrees they are
	 * heads of.
	 * @param parentANode	node whose dependency children will be processed
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	protected void transformDepSubtrees(Node parentANode)
			throws XPathExpressionException
	{
		if (hasFailed) return;
		NodeList pmlDependents = (NodeList)XPathEngine.get().evaluate(
				"./children/node", parentANode, XPathConstants.NODESET);
		if (pmlDependents != null && pmlDependents.getLength() > 0)
			for (int i = 0; i < pmlDependents.getLength(); i++)
			{
				transformSubtree(pmlDependents.item(i));
				if (hasFailed) return;
			}
	}

	/**
	 * Helper method: process subtrees under each part of PML phrase.
	 * @param phraseInfoNode	node whose dependency children will be processed
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	protected void transformPhraseParts(Node phraseInfoNode)
			throws XPathExpressionException
	{
		if (hasFailed) return;
		NodeList parts = (NodeList)XPathEngine.get().evaluate(
				"./children/node", phraseInfoNode, XPathConstants.NODESET);
		if (parts != null && parts.getLength() > 0)
			for (int i = 0; i < parts.getLength(); i++)
			{
				transformSubtree(parts.item(i));
				if (hasFailed) return;
			}
	}

	/**
	 * Helper method: fill in DEPREL and HEAD fields in CoNLL-U table for given
	 * subtree.
	 * @param aNode	root of the subtree to process
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	protected void transformSubtree (Node aNode) throws XPathExpressionException
	{
		if (hasFailed) return;
		if (debug) System.out.printf("Working on node \"%s\".\n", Utils.getId(aNode));

		NodeList children = Utils.getAllPMLChildren(aNode);
		if (children == null || children.getLength() < 1) return;

		transformDepSubtrees(aNode);
		if (hasFailed) return;

		Node newRoot = aNode;
		// Valid LVTB PMLs have no more than one type of phrase - pmc, x or coord.
		Node phraseNode = Utils.getPhraseNode(aNode);

		//// Process phrase overlords.
		if (phraseNode != null)
		{
			transformPhraseParts(phraseNode);
			if (hasFailed) return;
			newRoot = pTransf.anyPhraseToUD(phraseNode);
			if (newRoot == null)
				throw new IllegalStateException(
						"Algorithmic error: phrase transformation returned \"null\" root in sentence " + s.id);

			if (inducePhraseTags)
			{
				String phraseTag = Utils.getTag(aNode);
				String newRootTag = Utils.getTag(newRoot);
				if ((phraseTag == null || phraseTag.length() < 1 || phraseTag.matches("N/[Aa]")) &&
						newRootTag != null && newRootTag.length() > 0)
				{
					String type = phraseNode.getNodeName();
					if (type.equals("xinfo") || type.equals("coordinfo"))
					{
						Node tag = (Node)XPathEngine.get().evaluate("./tag", phraseNode, XPathConstants.NODE);
						if (tag == null) tag = phraseNode.getOwnerDocument().createElement("tag");
						while (tag.getFirstChild() != null)
							tag.removeChild(tag.getFirstChild());
						tag.appendChild(phraseNode.getOwnerDocument().createTextNode(newRootTag + "[INDUCED]"));
						phraseNode.appendChild(tag);
					}
				}
			}
		}
		//// Process reduction nodes.
		else if (Utils.isReductionNode(aNode))
		{
			Node redRoot = EllipsisLogic.newParent(aNode, drLogic, warnOut);
			if (redRoot == null)
			{
				hasFailed = true;
				return;
			}
			newRoot = redRoot;
			transformSubtree(newRoot);
			if (hasFailed) return;
		}

		//// Add information about new subroot in the result structure.
		s.pmlaToConll.put(Utils.getId(aNode), s.pmlaToConll.get(Utils.getId(newRoot)));

		//// Process dependants (except the newRoot).
		relinkDependents(aNode, newRoot);
	}


	/**
	 * Helper method: fill in DEPREL and HEAD fields in CoNLL-U table for PML
	 * dependency children of the given node. If the newRoot is one of the
	 * dependents, then it must be processed before invoking this method.
	 * @param parentANode	node whose dependency children will be processed
	 * @param newRoot		node that will be the root of the coresponding UD
	 *                  	structure
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	protected void relinkDependents(Node parentANode, Node newRoot)
			throws XPathExpressionException
	{
		if (hasFailed) return;
		NodeList pmlDependents = (NodeList)XPathEngine.get().evaluate(
				"./children/node", parentANode, XPathConstants.NODESET);
		Token newRootTok = s.pmlaToConll.get(Utils.getId(newRoot));
		if (pmlDependents != null && pmlDependents.getLength() > 0)
			for (int i = 0; i < pmlDependents.getLength(); i++)
			{
				// This happens in case of ellipsis.
				if (pmlDependents.item(i).isSameNode(newRoot)) continue;
				Token conllTok = s.pmlaToConll.get(Utils.getId(pmlDependents.item(i)));
				conllTok.deprel = drLogic.depToUD(pmlDependents.item(i), warnOut);
				conllTok.head = Tuple.of(newRootTok.getFirstColumn(), newRootTok);
			}
	}

}
