package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.MiscKeys;
import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.pml.LvtbXTypes;
import lv.ailab.lvtb.universalizer.pml.PmlANode;
import lv.ailab.lvtb.universalizer.transformator.morpho.*;
import lv.ailab.lvtb.universalizer.utils.Logger;
import lv.ailab.lvtb.universalizer.transformator.Sentence;
import lv.ailab.lvtb.universalizer.transformator.TransformationParams;

import java.util.List;

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
	/**
	 * In this sentence all the transformations are carried out.
	 */
	public Sentence s;

	protected TransformationParams params;
	protected PhraseTransformator pTransf;
	// TODO - do not duplicate for GraphsyntaxTransformator and TreesyntaxTransformator?
	protected DepRelLogic dpTransf;
	/**
	 * Stream for warnings.
	 */
	protected Logger logger;

	public TreesyntaxTransformator(Sentence sent, TransformationParams params,
								   Logger logger)
	{
		s = sent;
		this.logger = logger;
		this.params = params;
		pTransf = new PhraseTransformator(s, logger);
		dpTransf = new DepRelLogic(logger);
	}

	/**
	 * Remove the ellipsis nodes that can be ignored in latter processing.
	 * Replace empty xPreds with just ellipsis nodes.
	 * @return	 true if all ellipsis was removed
	 */
	public boolean preprocessEmptyEllipsis()
	{
		// Childless, empty reductions are removed.
		List<PmlANode> ellipsisChildren = s.pmlTree.getEllipsisDescendants(true);
		while (ellipsisChildren != null && !ellipsisChildren.isEmpty())
		{
			for (PmlANode ellipsisChild : ellipsisChildren)
			{
				PmlANode parent = ellipsisChild.getParent();
				ellipsisChild.delete();
				if (LvtbXTypes.XPRED.equals(parent.getPhraseType()))
				{
					List<PmlANode> children = parent.getChildren();
					if (children != null && !children.isEmpty()) continue;

					PmlANode grandparent = parent.getParent();
					String xTag = parent.getPhraseTag();
					if (xTag.contains("[")) xTag = xTag.substring(0, xTag.indexOf("["));
					grandparent.setReductionTag(xTag);
					parent.delete();
				}
			}
			ellipsisChildren = s.pmlTree.getEllipsisDescendants(true);
		}

		// Check if there is other reductions.
		ellipsisChildren = s.pmlTree.getEllipsisDescendants(false);
		return ellipsisChildren == null || ellipsisChildren.size() <= 0;
	}

	/**
	 * Fill in DEPREL and HEAD fields in CoNLL-U table.
	 */
	public void transformBaseSyntax()
	{
		PmlANode pmlPmc = s.pmlTree.getPhraseNode();
		if (pmlPmc == null || pmlPmc.getNodeType() != PmlANode.Type.PMC)
		{
			s.hasFailed = true;
			return;
		}
		transformDepSubtrees(s.pmlTree);
		if (s.hasFailed) return;
		transformPhraseParts(pmlPmc);
		if (s.hasFailed) return;

		PmlANode newRoot = pTransf.anyPhraseToUD(pmlPmc);
		if (newRoot == null)
			throw new IllegalArgumentException(String.format(
					"Sentence %s has no root PMC.", s.id));
		s.pmlaToConll.put(s.pmlTree.getId(), s.pmlaToConll.get(newRoot.getId()));
		if (s.pmlaToEnhConll.containsKey(newRoot.getId()))
			s.pmlaToEnhConll.put(s.pmlTree.getId(), s.pmlaToEnhConll.get(newRoot.getId()));
		s.setRoot(newRoot, true);
		relinkDependents(s.pmlTree, newRoot, newRoot);
	}

	/**
	 * Helper method: find all dependency children and process subtrees they are
	 * heads of.
	 * @param parentANode	node whose dependency children will be processed

	 */
	protected void transformDepSubtrees(PmlANode parentANode)
	{
		if (s.hasFailed) return;
		List<PmlANode> pmlDependents = parentANode.getChildren();
		if (pmlDependents == null || pmlDependents.isEmpty()) return;
		for (PmlANode pmlDependent : pmlDependents)
		{
			transformSubtree(pmlDependent);
			if (s.hasFailed) return;
		}
	}

	/**
	 * Helper method: process subtrees under each part of PML phrase.
	 * @param phraseInfoNode	node whose dependency children will be processed
	 */
	protected void transformPhraseParts(PmlANode phraseInfoNode)
	{
		if (s.hasFailed) return;
		List<PmlANode> parts = phraseInfoNode.getChildren();
		if (parts == null || parts.isEmpty()) return;
		for (PmlANode part : parts)
		{
			transformSubtree(part);
			if (s.hasFailed) return;
		}
	}

	/**
	 * Helper method: fill in DEPREL and HEAD fields in CoNLL-U table for given
	 * subtree.
	 * @param aNode	root of the subtree to process
	 */
	protected void transformSubtree (PmlANode aNode)
	{
		if (s.hasFailed) return;
		if (params.DEBUG)
			System.out.printf("Working on node \"%s\".\n", aNode.getId());

		List<PmlANode> children = aNode.getChildren();
		PmlANode phraseNode = aNode.getPhraseNode();
		if (phraseNode == null && (children == null || children.size() < 1))
			return;

		transformDepSubtrees(aNode);
		if (s.hasFailed) return;

		PmlANode newBasicRoot = aNode;
		PmlANode newEnhancedRoot = aNode;
		// Valid LVTB PMLs have no more than one type of phrase - pmc, x or coord.

		//// Process phrase overlords.
		if (phraseNode != null)
		{
			transformPhraseParts(phraseNode);
			if (s.hasFailed) return;
			newBasicRoot = pTransf.anyPhraseToUD(phraseNode);
			newEnhancedRoot = newBasicRoot;
			if (newBasicRoot == null)
				throw new IllegalStateException(
						"Algorithmic error: phrase transformation returned \"null\" root in sentence " + s.id);

			if (params.INDUCE_PHRASE_TAGS)
			{
				String phraseTag = aNode.getAnyTag();
				String newRootTag = newBasicRoot.getAnyTag();
				if ((phraseTag == null || phraseTag.length() < 1 || phraseTag.matches("N/[Aa]")) &&
						newRootTag != null && newRootTag.length() > 0)
				{
					PmlANode.Type type = phraseNode.getNodeType();
					if (type == PmlANode.Type.X || type == PmlANode.Type.COORD)
						phraseNode.setPhraseTag(newRootTag + "[INDUCED]");
				}
			}
		}
		//// Process reduction nodes.
		else if (aNode.isPureReductionNode())
		{
			String nodeId = aNode.getId();
			PmlANode redRoot = EllipsisLogic.newParent(aNode, dpTransf, logger);
			if (redRoot == null)
			{
				s.hasFailed = true;
				return;
			}
			newBasicRoot = redRoot;

			String redXPostag = XPosLogic.getXpostag(aNode.getReductionTagPart());
			// TODO more precise restriction?
			if (redXPostag.matches("v..([^p].*|p[du].*)") || ! params.UD_STANDARD_NULLNODES)
			{
				// Make new token for ellipsis.
				// Decimal token (reduction node) must be inserted after newRootToken.
				Token newRootToken = s.pmlaToConll.get(newBasicRoot.getId());
				int position = s.conll.indexOf(newRootToken) + 1;
				while (position < s.conll.size() && newRootToken.idBegin == s.conll.get(position).idBegin)
					position++;
				Token decimalToken = new Token();
				decimalToken.idBegin = newRootToken.idBegin;
				decimalToken.idSub = s.conll.get(position-1).idSub+1;
				decimalToken.idEnd = decimalToken.idBegin;
				decimalToken.xpostag = redXPostag;
				decimalToken.form = aNode.getReductionFormPart();
				if (decimalToken.xpostag == null || decimalToken.xpostag.isEmpty() || decimalToken.xpostag.equals("_"))
					//warnOut.printf("Ellipsis node %s with reduction field \"%s\" has no tag.\n", NodeFieldUtils.getId(aNode), NodeFieldUtils.getReduction(aNode));
					logger.doInsentenceWarning(String.format(
							"Ellipsis node %s with reduction field \"%s\" has no tag.",
							nodeId, aNode.getReduction()));
				else
				{
					if (decimalToken.form != null && !decimalToken.form.isEmpty())
						decimalToken.lemma = AnalyzerWrapper.getLemma(
								decimalToken.form, decimalToken.xpostag, logger);
					decimalToken.upostag = UPosLogic.getUPosTag(decimalToken.form,
							decimalToken.lemma, decimalToken.xpostag, logger);
					decimalToken.feats = FeatsLogic.getUFeats(decimalToken.form,
							decimalToken.lemma, decimalToken.xpostag, logger);
					//decimalToken.upostag = UPosLogic.getUPosTag(decimalToken.form,
					//		decimalToken.lemma, decimalToken.xpostag, aNode, logger);
					//decimalToken.feats = FeatsLogic.getUFeats(decimalToken.form,
					//		decimalToken.lemma, decimalToken.xpostag, aNode, logger);
				}
				if (params.ADD_NODE_IDS && nodeId != null && !nodeId.isEmpty())
				{
					decimalToken.addMisc(MiscKeys.LVTB_NODE_ID, nodeId);//decimalToken.misc.add("LvtbNodeId=" + nodeId);
					logger.addIdMapping(s.id, decimalToken.getFirstColumn(), nodeId);
				}
				s.conll.add(position, decimalToken);
				s.pmlaToEnhConll.put(nodeId, decimalToken);
			}

			if (s.hasFailed) return;

			transformSubtree(newBasicRoot);
		}

		//// Add information about new subroot in the result structure.
		s.pmlaToConll.put(aNode.getId(), s.pmlaToConll.get(newBasicRoot.getId()));
		if (s.pmlaToEnhConll.containsKey(newEnhancedRoot.getId()))
			s.pmlaToEnhConll.put(aNode.getId(), s.pmlaToEnhConll.get(newEnhancedRoot.getId()));

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
	 */
	protected void relinkDependents(
			PmlANode parentANode, PmlANode newBaseDepRoot, PmlANode newEnhDepRoot)
	{
		if (s.hasFailed) return;
		if (newEnhDepRoot == null) newEnhDepRoot = newBaseDepRoot;
		if (s.pmlaToConll.get(newBaseDepRoot.getId()) != s.pmlaToConll.get(parentANode.getId()) ||
				!s.getEnhancedOrBaseToken(newEnhDepRoot).equals(s.getEnhancedOrBaseToken(parentANode)))
		{
			logger.doInsentenceWarning(String.format(
					"Can't relink dependents from %s to %s!",
					parentANode.getId(), newBaseDepRoot.getId()));
			s.hasFailed = true;
			return;
		}

		List<PmlANode> pmlDependents = parentANode.getChildren();
		if (pmlDependents != null && pmlDependents.size() > 0)
			for (PmlANode pmlDependent : pmlDependents)
			{
				s.setBaseLink(newBaseDepRoot, pmlDependent,
						dpTransf.depToUDBase(pmlDependent));
				s.setEnhLink(newEnhDepRoot, pmlDependent,
						dpTransf.depToUDEnhanced(pmlDependent),
						true, true);
			}
	}
}
