package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.pml.PmlANode;
import lv.ailab.lvtb.universalizer.transformator.Sentence;
import lv.ailab.lvtb.universalizer.transformator.TransformationParams;
import lv.ailab.lvtb.universalizer.transformator.morpho.XPosLogic;

import java.util.List;

public class NewTransformator
{
	/**
	 * In this sentence all the transformations are carried out.
	 */
	public Sentence s;

	protected TransformationParams params;
	protected PhraseTransformator pTransf;


	public NewTransformator(Sentence sent, TransformationParams params)
	{
		s = sent;
		this.params = params;
		pTransf = new PhraseTransformator(s);
	}

	public void prepare()
	{
		s.populateCoordPartsUnder();
		s.populateXPredSubjs();
	}


	public void transform()
	{
		PmlANode pmlPmc = s.pmlTree.getPhraseNode();
		if (pmlPmc == null || pmlPmc.getNodeType() != PmlANode.Type.PMC)
			throw new IllegalArgumentException(String.format(
					"Sentence %s has no root PMC.", s.id));

		// Bottom-up: process dependency children.
		transformDependants(s.pmlTree);
		transformPhraseParts(pmlPmc);

		// Process root PMC.
		PmlANode newRoot = pTransf.anyPhraseToUD(pmlPmc, params.PROPAGATE_CONJUNCTS);
		if (newRoot == null) throw new IllegalArgumentException(String.format(
				"Sentence %s has untransformable root PMC.", s.id));
		s.pmlaToConll.put(s.pmlTree.getId(), s.pmlaToConll.get(newRoot.getId()));
		if (s.pmlaToEnhConll.containsKey(newRoot.getId()))
			s.pmlaToEnhConll.put(s.pmlTree.getId(), s.pmlaToEnhConll.get(newRoot.getId()));
		s.setRoot(newRoot, true);
		relinkDependents(s.pmlTree, newRoot, newRoot);
	}

	protected void transformSubtree(PmlANode aNode)
	{
		if (params.DEBUG)
			System.out.printf("Working on node \"%s\".\n", aNode.getId());

		// Find children.
		List<PmlANode> children = aNode.getChildren();
		PmlANode phraseNode = aNode.getPhraseNode();
		// If no children, nothing to do.
		if (phraseNode == null && (children == null || children.size() < 1))
			return;

		// Bottom-up: process dependency children.
		transformDependants(aNode);

		// Now do something with phrases.
		// Valid LVTB PMLs have no more than one type of phrase - pmc, x or coord.

		PmlANode newBasicRoot = aNode;
		PmlANode newEnhancedRoot = aNode;

		//// Process phrase related stuff.
		if (phraseNode != null)
		{
			// Bottom-up: process phrase parts.
			transformPhraseParts(phraseNode);

			// Find new subroot.
			newBasicRoot = pTransf.anyPhraseToUD(phraseNode, params.PROPAGATE_CONJUNCTS);
			newEnhancedRoot = newBasicRoot;
			if (newBasicRoot == null)
				throw new IllegalStateException(
						"Algorithmic error: phrase transformation returned \"null\" root in sentence " + s.id);

			// Fill in empty phrase tags if set to do so.
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
			// Find, what will be elevated in basic sependencies.
			String nodeId = aNode.getId();
			PmlANode redRoot = EllipsisLogic.newParent(aNode);
			if (redRoot == null) throw new IllegalArgumentException(String.format(
					"No child was raised for ellipsis node %s.", nodeId));
			String redXPostag = XPosLogic.getXpostag(aNode.getReductionTagPart());
			newBasicRoot = redRoot;

			// Create ellipsis node for enhanced dependencies, if allowed to do so.
			// TODO more precise restriction?
			if (redXPostag.matches("v..([^p].*|p[du].*)") || ! params.UD_STANDARD_NULLNODES)
				s.createNewEnhEllipsisNode(aNode, newBasicRoot.getId(), params.ADD_NODE_IDS);

			// TODO: isn't this repetative?
			//transformSubtree(newBasicRoot);
		}

		//// Add information about new subroot in the result structure.
		s.pmlaToConll.put(aNode.getId(), s.pmlaToConll.get(newBasicRoot.getId()));
		if (s.pmlaToEnhConll.containsKey(newEnhancedRoot.getId()))
			s.pmlaToEnhConll.put(aNode.getId(), s.pmlaToEnhConll.get(newEnhancedRoot.getId()));

		//// Process dependants (except the newRoot).
		relinkDependents(aNode, newBasicRoot, newEnhancedRoot);

	}

	/**
	 * Helper method: find all dependency children and process subtrees they are
	 * heads of.
	 * @param parentANode	node whose dependency children will be processed
	 */
	protected void transformDependants(PmlANode parentANode)
	{
		List<PmlANode> pmlDependents = parentANode.getChildren();
		if (pmlDependents == null || pmlDependents.isEmpty()) return;
		for (PmlANode pmlDependent : pmlDependents)
			transformSubtree(pmlDependent);
	}

	/**
	 * Helper method: process subtrees under each part of PML phrase.
	 * @param phraseInfoNode	node whose dependency children will be processed
	 */
	protected void transformPhraseParts(PmlANode phraseInfoNode)
	{
		List<PmlANode> parts = phraseInfoNode.getChildren();
		if (parts == null || parts.isEmpty()) return;
		for (PmlANode part : parts)
			transformSubtree(part);
	}

	/**
	 * Helper method: fill in DEPREL and HEAD fields in CoNLL-U table for PML
	 * dependency children of the given node. If the newRoot is one of the
	 * dependents, then it must be processed before invoking this method.
	 * To use this function, previous should have set that conllu tokens who
	 * correspond old and new parent are the same.
	 * @param parentANode		node whose dependency children will be processed
	 * @param newBaseDepRoot	node that will be the root of the coresponding
	 *                  		base UD structure
	 * @param newEnhDepRoot		node that will be the root of the coresponding
	 *                  		enhanced UD structure (if null, newBaseDepRoot
	 *                  		used instead)
	 */
	protected void relinkDependents(
			PmlANode parentANode, PmlANode newBaseDepRoot, PmlANode newEnhDepRoot)
	{
		if (newEnhDepRoot == null) newEnhDepRoot = newBaseDepRoot;
		// To use this function, previous it should have been set that conllu
		// tokens who correspond old and new parent are the same.
		if (!(s.pmlaToConll.get(newBaseDepRoot.getId()) == s.pmlaToConll.get(parentANode.getId()) &&
				s.getEnhancedOrBaseToken(newEnhDepRoot).equals(s.getEnhancedOrBaseToken(parentANode))))
			throw new IllegalArgumentException(String.format(
					"Can't relink dependents from %s to %s!",
					parentANode.getId(), newBaseDepRoot.getId()));

		List<PmlANode> pmlDependents = parentANode.getChildren();
		// TODO Pašlaik novelk enh linkus no vecās un jaunās saknes, bet ja nu pa vidu arī kaut kas ir?
		// Ko darīt ar tiem?
		s.relinkAllDependants(parentANode, newBaseDepRoot, pmlDependents, params.PROPAGATE_CONJUNCTS);
	}

}
