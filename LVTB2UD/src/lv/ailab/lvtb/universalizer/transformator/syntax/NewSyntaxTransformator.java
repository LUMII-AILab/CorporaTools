package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.*;
import lv.ailab.lvtb.universalizer.pml.PmlANode;
import lv.ailab.lvtb.universalizer.transformator.Sentence;
import lv.ailab.lvtb.universalizer.transformator.StandardLogger;
import lv.ailab.lvtb.universalizer.transformator.TransformationParams;
import lv.ailab.lvtb.universalizer.transformator.morpho.XPosLogic;
import lv.ailab.lvtb.universalizer.utils.Tuple;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the reimplemented syntax converter. It creates enhanced dependency
 * graph together with basic dependency tree, while the first one (created for
 * first LV UD paper) tried to create enhanced graph as an afterthought from
 * the basic tree.
 */
public class NewSyntaxTransformator
{
	/**
	 * In this sentence all the transformations are carried out.
	 */
	public Sentence s;

	protected TransformationParams params;
	protected PhraseTransformator pTransf;


	public NewSyntaxTransformator(Sentence sent, TransformationParams params)
	{
		s = sent;
		this.params = params;
		pTransf = new PhraseTransformator(s, params);
	}

	public void prepare()
	{
		s.prepare();
		if (params.DEBUG)
		{
			System.out.println("Subject map: ");
			System.out.println(s.subj2gov.keySet().stream().sorted()
					.map(id -> "\t" + id + " -> " + s.subj2gov.get(id).stream().reduce((a, b) -> a + ", " + b).orElse("NULL"))
					.reduce((a, b) -> a + "\n" + b).orElse("EMPTY"));
			System.out.println("Coordination map: ");
			System.out.println(s.coordPartsUnder.keySet().stream().sorted()
					.map(id -> "\t" + id + " -> " + s.coordPartsUnder.get(id).stream().sorted().reduce((a, b) -> a + ", " + b).orElse("NULL"))
					.reduce((a, b) -> a + "\n" + b).orElse("EMPTY"));
		}
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
		PmlANode newRoot = pTransf.anyPhraseToUD(pmlPmc);
		if (newRoot == null) throw new IllegalArgumentException(String.format(
				"Sentence %s has non-transformable root PMC.", s.id));
		s.pmlaToConll.put(s.pmlTree.getId(), s.pmlaToConll.get(newRoot.getId()));
		if (s.pmlaToEnhConll.containsKey(newRoot.getId()))
			s.pmlaToEnhConll.put(s.pmlTree.getId(), s.pmlaToEnhConll.get(newRoot.getId()));
		s.setRoot(newRoot, true);
		relinkDependents(s.pmlTree, newRoot, newRoot);
	}

	public void aftercare()
	{
		if (params.NORMALIZE_PUNCT_ATTACHMENT) noPunctUnderPunct();
		if (params.NORMALIZE_NONPROJ_PUNCT) fixPunctProjectivity();
		if (params.NORMALIZE_PUNCT_ATTACHMENT && params.NORMALIZE_NONPROJ_PUNCT)
			noPunctUnderPunct();
		if (params.CLEANUP_UNLABELED_EDEPS) s.removeUnlabeledDeps();
	}

	//=== Transformation details. ==============================================

	protected void transformSubtree(PmlANode aNode)
	{
		if (params.DEBUG)
			System.out.printf("Working on node \"%s\".\n", aNode.getId());

		// Find children.
		List<PmlANode> children = aNode.getChildren();
		PmlANode phraseNode = aNode.getPhraseNode();
		// If no children, nothing to do.
		if (phraseNode == null && (children == null || children.isEmpty()))
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
			newBasicRoot = pTransf.anyPhraseToUD(phraseNode);
			newEnhancedRoot = newBasicRoot;
			if (newBasicRoot == null)
				throw new IllegalStateException(
						"Algorithmic error: phrase transformation returned \"null\" root in sentence " + s.id);

			String phraseTag = aNode.getAnyTag();

			// Fill in empty phrase tags if set to do so.
			if (params.INDUCE_PHRASE_TAGS)
			{
				String newRootTag = newBasicRoot.getAnyTag();
				if ((phraseTag == null || phraseTag.isEmpty() || phraseTag.matches("N/[Aa]")) &&
						newRootTag != null && !newRootTag.isEmpty())
				{
					PmlANode.Type type = phraseNode.getNodeType();
					if (type == PmlANode.Type.X || type == PmlANode.Type.COORD)
						phraseNode.setPhraseTag(newRootTag + "[INDUCED]");
				}
			}
			ArrayList<UDv2Feat> syntFeats = FeatsLogic.getUFeatsFromPhraseNode(
					phraseNode.getPhraseType(), phraseTag);
			s.pmlaToConll.get(newBasicRoot.getId()).feats.addAll(syntFeats);
			// In this spot both roots should be the same.
			//if (newBasicRoot != newEnhancedRoot)
			//	s.pmlaToConll.get(newEnhancedRoot.getId()).feats.addAll(syntFeats);
		}

		//// Process reduction nodes.
		else if (aNode.isPureReductionNode())
		{
			// Find, what will be elevated in basic dependencies.
			String nodeId = aNode.getId();
			Tuple<PmlANode, Boolean> redRoot = EllipsisLogic.newParent(aNode);
			if (redRoot == null) throw new IllegalArgumentException(String.format(
					"No child was raised for ellipsis node %s.", nodeId));
			String redXPostag = XPosLogic.getXpostag(aNode.getReductionTagPart());
			newBasicRoot = redRoot.first;

			// Create ellipsis node for enhanced dependencies, if allowed to do so.
			// TODO more precise restriction?
			boolean isVerbal = false;
			if (redXPostag.matches("v..([^p].*|p[du].*)"))
			{
				s.ellipsisWithOrphans.add(nodeId);
				isVerbal = true;
			}

			if (isVerbal || ! params.UD_STANDARD_NULLNODES)
			{
				String newIdStub = newBasicRoot.getId();
				//List<PmlANode> tokenNodes = aNode.getChildren(LvtbRoles.ELLIPSIS_TOKEN);
				//if (tokenNodes != null && !tokenNodes.isEmpty())
				//	newIdStub = PmlANodeListUtils.getFirstByDescOrd(tokenNodes).getId();
				PmlANode tokenNode = s.pmlTree.getDescendant(aNode.getId() + Sentence.ID_POSTFIX);
				if (tokenNode != null) newIdStub = tokenNode.getId();
				s.createNewEnhEllipsisNode(aNode, newIdStub, params.ADD_NODE_IDS);
			}

		}

		//// Add information about new subroot in the result structure.
		s.pmlaToConll.put(aNode.getId(), s.pmlaToConll.get(newBasicRoot.getId()));
		if (s.pmlaToEnhConll.containsKey(newEnhancedRoot.getId()))
			s.pmlaToEnhConll.put(aNode.getId(), s.pmlaToEnhConll.get(newEnhancedRoot.getId()));

		//// Process dependants (except the newRoot).
		relinkDependents(aNode, newBasicRoot, newEnhancedRoot);

		// TODO: where?
		//if (params.ADD_CONTROL_SUBJ) s.???(???, params.PROPAGATE_CONJUNCTS);

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
	 * dependency children of the given node. If the UD root is one of the
	 * dependents, then it must be processed before invoking this method.
	 * To use this function, previous should have set that conllu tokens who
	 * correspond old and new parent are the same.
	 * @param parentANode		node whose dependency children will be processed
	 * @param newBaseDepRoot	node that will be the root of the corresponding
	 *                  		base UD structure
	 * @param newEnhDepRoot		node that will be the root of the corresponding
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
		s.relinkAllDependants(parentANode, pmlDependents, params.PROPAGATE_CONJUNCTS,
				params.ADD_CONTROL_SUBJ, params.NO_EDEP_DUPLICATES);

	}

	/**
	 * Postprocessing: rise all punctuation which are children of other
	 * punctuation.
	 */
	protected void noPunctUnderPunct()
	{
		boolean doStuff = true;
		while (doStuff)
		{
			doStuff = false;
			for (Token token : s.conll)
				if (token.deprel == UDv2Relations.PUNCT)
			{
				Token parent = token.head.second;
				if (parent != null && parent.deprel == UDv2Relations.PUNCT)
				{
					doStuff = true;
					token.head = parent.head;
					EnhancedDep oldEnhHead = new EnhancedDep(parent, UDv2Relations.PUNCT);
					if (token.deps.contains(oldEnhHead))
					{
						token.deps.remove(oldEnhHead);
						token.deps.add(new EnhancedDep(parent.head.second, UDv2Relations.PUNCT));
					}
					break;
				}
			}
		}
	}

	/**
	 * Postprocessing: move non-projective punctuation to be children of the
	 * previous node.
	 */
	protected void fixPunctProjectivity()
	{
		for (Token t : s.conll)
		{
			if (t.idSub > 0 || t.deprel != UDv2Relations.PUNCT) continue;
			boolean isProjective = s.isProjective(t);
			boolean createsNonproj = s.createsNonprojectivity(t);
			if (isProjective && !createsNonproj) continue;
			Token newParent = s.getPrevSurfaceToken(t);
			if (newParent == null) newParent = s.getNextSurfaceToken(t);
			if (newParent != null)
			{
				EnhancedDep oldEnhHead = new EnhancedDep(t.head.second, UDv2Relations.PUNCT);
				if (t.deps.contains(oldEnhHead))
				{
					t.deps.remove(oldEnhHead);
					t.deps.add(new EnhancedDep(newParent, UDv2Relations.PUNCT));
				}
				t.head = Tuple.of(newParent.getFirstColumn(), newParent);

				StandardLogger.l.doInsentenceWarning(String.format(
						"CONLL token \"%s\" %s in sentence \"%s\" relinked to preserve projectivity.",
						t.getFirstColumn(), t.misc.get(MiscKeys.LVTB_NODE_ID), s.id));
			}
			else
				StandardLogger.l.doInsentenceWarning(String.format(
						"CONLL token \"%s\" %s in sentence \"%s\" should be relinked to preserve projectivity, but where?",
						t.getFirstColumn(), t.misc.get(MiscKeys.LVTB_NODE_ID), s.id));

		}
	}

}
