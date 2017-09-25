package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.pml.Utils;
import lv.ailab.lvtb.universalizer.transformator.Sentence;
import lv.ailab.lvtb.universalizer.transformator.morpho.AnalyzerWrapper;
import lv.ailab.lvtb.universalizer.transformator.morpho.FeatsLogic;
import lv.ailab.lvtb.universalizer.transformator.morpho.MorphoTransformator;
import lv.ailab.lvtb.universalizer.transformator.morpho.PosLogic;
import lv.ailab.lvtb.universalizer.util.XPathEngine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.PrintWriter;

/**
 * This is the part of the transformation where base UD tree is made. This part
 * is also responsible for creating ellipsis tokens for ehnahnced dependencies.
 * This class creates creates ellipsis-related enhanced dependency links and
 * copies those dependency links, which are the same for both base UD and
 * enhanced. Thus, the part of enhanced dependency graph made by this class is
 * a backbone tree connecting all nodes.
 */
public class TreesyntaxTransformator
{
	public Sentence s;
	/**
	 * Indication that transformation has failed and the obtained conll data is
	 * garbage.
	 */
	public boolean hasFailed;
	/**
	 * Should missing phrase tags be filled with some kind of heuristics.
	 */
	public boolean inducePhraseTags;
	/**
	 * Print debug info.
	 */
	public boolean debug;

	protected PhraseTransformator pTransf;
	/**
	 * Stream for warnings.
	 */
	protected PrintWriter warnOut;

	public TreesyntaxTransformator(Sentence sent, PrintWriter warnOut,
								   boolean inducePhraseTags, boolean debug)
	{
		s = sent;
		this.warnOut = warnOut;
		this.inducePhraseTags = inducePhraseTags;
		this.debug = debug;
		hasFailed = false;
		pTransf = new PhraseTransformator(s, warnOut);
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
			current.getParentNode().removeChild(current);
		}

		// Check if there is other reductions.
		ellipsisChildren = (NodeList) XPathEngine.get().evaluate(
				".//node[reduction and not(m.rf)]", s.pmlTree, XPathConstants.NODESET);
		if (ellipsisChildren != null && ellipsisChildren.getLength() > 0) return false;

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
		s.pmlaToConll.put(Utils.getId(s.pmlTree), s.pmlaToConll.get(Utils.getId(newRoot)));
		if (s.pmlaToEnhConll.containsKey(Utils.getId(newRoot)))
			s.pmlaToEnhConll.put(Utils.getId(s.pmlTree), s.pmlaToEnhConll.get(Utils.getId(newRoot)));
		s.setRoot(newRoot, true);
		relinkDependents(s.pmlTree, newRoot, newRoot);
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

		Node newBasicRoot = aNode;
		Node newEnhancedRoot = aNode;
		// Valid LVTB PMLs have no more than one type of phrase - pmc, x or coord.
		Node phraseNode = Utils.getPhraseNode(aNode);

		//// Process phrase overlords.
		if (phraseNode != null)
		{
			transformPhraseParts(phraseNode);
			if (hasFailed) return;
			newBasicRoot = pTransf.anyPhraseToUD(phraseNode);
			newEnhancedRoot = newBasicRoot;
			if (newBasicRoot == null)
				throw new IllegalStateException(
						"Algorithmic error: phrase transformation returned \"null\" root in sentence " + s.id);

			if (inducePhraseTags)
			{
				String phraseTag = Utils.getTag(aNode);
				String newRootTag = Utils.getTag(newBasicRoot);
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

			Node redRoot = EllipsisLogic.newParent(aNode, warnOut);
			if (redRoot == null)
			{
				hasFailed = true;
				return;
			}
			newBasicRoot = redRoot;

			// Make new token for ellipsis.
			Token newRootToken = s.pmlaToConll.get(Utils.getId(newBasicRoot));
			int position = s.conll.indexOf(newRootToken) + 1;
			while (newRootToken.idBegin == s.conll.get(position).idBegin)
				position++;
			Token decimalToken = new Token();
			decimalToken.idBegin = newRootToken.idBegin;
			decimalToken.idSub = s.conll.get(position-1).idSub+1;
			decimalToken.idEnd = decimalToken.idBegin;
			decimalToken.xpostag = MorphoTransformator.getXpostag(
					Utils.getReductionTagPart(aNode), null);
			decimalToken.form = Utils.getReductionFormPart(aNode);
			if (decimalToken.xpostag == null || decimalToken.xpostag.isEmpty() || decimalToken.xpostag.equals("_"))
				warnOut.printf("Ellipsis node %s with reduction field \"%s\" has no tag.\n",
						Utils.getId(aNode), Utils.getReduction(aNode));
			else
			{
				if (decimalToken.form != null && !decimalToken.form.isEmpty())
					decimalToken.lemma = AnalyzerWrapper.getLemma(
							decimalToken.form, decimalToken.xpostag, warnOut);
				decimalToken.upostag = PosLogic.getUPosTag(
						decimalToken.lemma, decimalToken.xpostag, aNode, warnOut);
				decimalToken.feats = FeatsLogic.getUFeats(
						decimalToken.form, decimalToken.lemma, decimalToken.xpostag, aNode, warnOut);
			}
			s.conll.add(position, decimalToken);
			s.pmlaToEnhConll.put(Utils.getId(aNode), decimalToken);
			if (hasFailed) return;

			transformSubtree(newBasicRoot);
		}

		//// Add information about new subroot in the result structure.
		s.pmlaToConll.put(Utils.getId(aNode), s.pmlaToConll.get(Utils.getId(newBasicRoot)));
		if (s.pmlaToEnhConll.containsKey(Utils.getId(newEnhancedRoot)))
			s.pmlaToEnhConll.put(Utils.getId(aNode), s.pmlaToEnhConll.get(Utils.getId(newEnhancedRoot)));

		//// Process dependants (except the newRoot).
		relinkDependents(aNode, newBasicRoot, newEnhancedRoot);
	}


	/**
	 * Helper method: fill in DEPREL and HEAD fields in CoNLL-U table for PML
	 * dependency children of the given node. If the newRoot is one of the
	 * dependents, then it must be processed before invoking this method.
	 * @param parentANode		node whose dependency children will be processed
	 * @param newBaseDepRoot	node that will be the root of the coresponding
	 *                  		base UD structure
	 * @param newEnhDepRoot		node that will be the root of the coresponding
	 *                  		enhanced UD structure
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	protected void relinkDependents(Node parentANode, Node newBaseDepRoot, Node newEnhDepRoot)
			throws XPathExpressionException
	{
		if (hasFailed) return;
		if (newEnhDepRoot == null) newEnhDepRoot = newBaseDepRoot;
		if (s.pmlaToConll.get(Utils.getId(newBaseDepRoot)) != s.pmlaToConll.get(Utils.getId(parentANode)) ||
				!s.getEnhancedOrBaseToken(newEnhDepRoot).equals(s.getEnhancedOrBaseToken(parentANode)))
		{
			/*System.out.println("sentence " + s.id);
			System.out.println("base " + s.pmlaToConll.get(Utils.getId(parentANode)).getFirstColumn() + " vs. " + s.pmlaToConll.get(Utils.getId(newBaseDepRoot)).getFirstColumn());
			System.out.println("enhanced " + s.getEnhancedOrBaseToken(parentANode).getFirstColumn() + " vs. " + s.getEnhancedOrBaseToken(newEnhDepRoot).getFirstColumn());
			System.out.printf("Can't relink dependents from %s to %s\n", Utils.getId(parentANode), Utils.getId(newBaseDepRoot));//*/
			warnOut.printf("Can't relink dependents from %s to %s\n", Utils.getId(parentANode), Utils.getId(newBaseDepRoot));
			hasFailed = true;
			return;
		}

		NodeList pmlDependents = (NodeList)XPathEngine.get().evaluate(
				"./children/node", parentANode, XPathConstants.NODESET);
		if (pmlDependents != null && pmlDependents.getLength() > 0)
			for (int i = 0; i < pmlDependents.getLength(); i++)
			{
				s.setBaseLink(newBaseDepRoot, pmlDependents.item(i),
						DepRelLogic.getSingleton().depToUD(pmlDependents.item(i), false, warnOut));
				s.setEnhLink(newEnhDepRoot, pmlDependents.item(i),
						DepRelLogic.getSingleton().depToUD(pmlDependents.item(i), true, warnOut),
						true,true);
				/*s.setLink(parentANode, pmlDependents.item(i),
						DepRelLogic.getSingleton().depToUD(pmlDependents.item(i), false, warnOut),
						DepRelLogic.getSingleton().depToUD(pmlDependents.item(i), true, warnOut),
						true,true);*/
			}
	}
}
