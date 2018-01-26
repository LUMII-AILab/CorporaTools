package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.LvtbRoles;
import lv.ailab.lvtb.universalizer.pml.LvtbXTypes;
import lv.ailab.lvtb.universalizer.pml.PmlANode;
import lv.ailab.lvtb.universalizer.utils.Logger;
import lv.ailab.lvtb.universalizer.utils.Tuple;
import lv.ailab.lvtb.universalizer.transformator.Sentence;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * This is the part where enhanced dependencies graph features are made. To use
 * this on a given Sentence, TreesyntaxTransformator must be used on that
 * sentence beforehand.
 */
public class GraphsyntaxTransformator
{
	/**
	 * In this sentence all the transformations are carried out.
	 */
	public Sentence s;
	// TODO - do not duplicate for GraphsyntaxTransformator and TreesyntaxTransformator?
	/**
	 * Dependency role logic.
	 */
	protected DepRelLogic dpTransf;
	/**
	 * Stream for warnings and other logs.
	 */
	protected Logger logger;



	public GraphsyntaxTransformator(Sentence sent, Logger logger)
	{
		s = sent;
		this.logger = logger;
		dpTransf = new DepRelLogic(logger);

	}

	/**
	 * Add enhanced dependencies graph features. Assumed that
	 * TreesyntaxTransformator is already used on the sendence and that ellipsis
	 * is already handled there.
	 * Currently supported features:
	 *  * conjuct propagation;
	 *  * controled/rised subjects;
	 *  * case information.
	 * TODO
	 *  * relative clauses.
	 */
	public void transformEnhancedSyntax()
	{
		s.populateCoordPartsUnder();
		propagateConjuncts();
		addControlledSubjects();
	}

	/**
	 * Add enhanced dependencies subject links between various parts of xPred
	 * and respective subjects. No links are added for xPred parts transformed
	 * to aux, auxpass or cop. No links are added for subjects transformed as
	 * something else than nsubj, nsubjpass, csubj, csubjpass.
	 * To use this, Sentence.populateCoordPartsUnder() must be called
	 * beforehand.
	 */
	protected void addControlledSubjects()
	{
		// Find all nodes consisting of xPred with dependant subj.
		List<PmlANode> xPredList = s.pmlTree.getDescendants(LvtbXTypes.XPRED);
		//NodeList xPredList = (NodeList) XPathEngine.get().evaluate(
		//		".//node[children/xinfo/xtype/text()='xPred']",
		//		s.pmlTree, XPathConstants.NODESET);
		if (xPredList != null)
			for (PmlANode xPredPhrase : xPredList)
		{
			PmlANode xPredNode = xPredPhrase.getParent();
			// Get base token.
			//Token parentTok = s.getEnhancedOrBaseToken(xPredList.item(xPredI));

			// Collect all subject nodes.
			List<PmlANode> subjs = xPredNode.getChildren(LvtbRoles.SUBJ);
			if (subjs == null) subjs = new ArrayList<>();
			boolean predIsCoordinated = false;
			PmlANode ancestor = xPredNode.getParent();
			while (ancestor != null && ancestor.getNodeType() == PmlANode.Type.COORD)
			{
				ancestor = ancestor.getParent(); // PML node
				List<PmlANode> tmp = ancestor.getChildren(LvtbRoles.SUBJ);
				if (tmp != null) subjs.addAll(tmp);
				ancestor = ancestor.getParent(); // PML node or phrase
				predIsCoordinated = true;
			}
			// If no subjects found, nothing to do.
			if (subjs.isEmpty()) continue;

			// Work on each xPred part
			List<PmlANode> xPredParts = xPredPhrase.getChildren();
			if (xPredParts != null)	for (PmlANode xPredPart : xPredParts)
			{
				// Do nothing with auxiliaries
				Token xPredPartTok = s.getEnhancedOrBaseToken(xPredPart);
				if (xPredPartTok.depsBackbone.role == UDv2Relations.AUX
						|| xPredPartTok.depsBackbone.role == UDv2Relations.AUX_PASS
						|| xPredPartTok.depsBackbone.role == UDv2Relations.COP)
					continue;
				// Do nothing with nomens
				if (xPredPartTok.xpostag != null && xPredPartTok.xpostag.matches("[napxm].*|v..pd...[ap]p.*]"))
					continue;
				// TODO what to do with past participles?

				// For each other part a ling between each subject and this part
				// must be made.
				for (PmlANode subj : subjs)
				{
					String subjLvtbRole = subj.getRole(); // It should be "subj" always.
					// Find each coordinated subject part.
					HashSet<String> subjIds = s.getCoordPartsUnderOrNode(subj);
					// Find each coordinated x-part part.
					HashSet<String> xPartIds = s.getCoordPartsUnderOrNode(xPredPart);
					// Make a link.
					for (String subjId : subjIds)
					{
						PmlANode subjNode = s.pmlTree.getDescendant(subjId);
						Token subjTok = s.getEnhancedOrBaseToken(subjNode);
						//Tuple<UDv2Relations, String> role = subjTok.depsBackbone.getRoleTuple();
						for (String xPartId : xPartIds)
						{
							PmlANode xPartNode = s.pmlTree.getDescendant(xPartId);
							// TODO tweak this, when nested xPreds will be made.
							Tuple<UDv2Relations, String> role = dpTransf.depToUDEnhanced(
									subjNode, xPredNode, subjLvtbRole);
							//subjNode, xPartNode, subjLvtbRole, warnOut);
							// Only UD subjects will have aditional link.
							//if (role.first == UDv2Relations.NSUBJ ||
							//		role.first == UDv2Relations.NSUBJ_PASS ||
							//		role.first == UDv2Relations.CSUBJ ||
							//		role.first == UDv2Relations.CSUBJ_PASS)
							s.setEnhLink(xPartNode, subjNode, role, false, false);
						}
					}
				}
			}
		}
	}

	/**
	 * Add enhanced dependencies links related to second and further coordinated
	 * parts.
	 * To use this, Sentence.populateCoordPartsUnder() must be called
	 * beforehand.
	 */
	protected void propagateConjuncts()
	{
		for (String coordId : s.coordPartsUnder.keySet())
		{
			PmlANode coordANode = s.pmlTree.getDescendant(coordId);
			for (String coordPartId : s.coordPartsUnder.get(coordId))
			{
				PmlANode partNode = s.pmlTree.getDescendant(coordPartId);
				processSingleConjunct(partNode, coordANode);
			}
		}
	}

	protected void processSingleConjunct(PmlANode coordPartNode, PmlANode wholeCoordANode)
	{
		// This is the "empty" PML node that represents a coordination as a
		// whole - it has ID, role and dependants for this coordination.
		Token wholeCoordNodeTok = s.getEnhancedOrBaseToken(wholeCoordANode);

		// This is coordinated part node.
		Token partNodeTok = s.getEnhancedOrBaseToken(coordPartNode);
		if (partNodeTok.equals(wholeCoordNodeTok)) return;

		// This is coordination's dependency head or phrase containing it.
		PmlANode coordParentNode = wholeCoordANode.getParent();
		PmlANode coordGrandParentNode = coordParentNode.getParent();

		if (coordParentNode.isPhraseNode())
		{
			// Renaming for convenience
			PmlANode phrase = coordParentNode;
			PmlANode phraseParent = coordGrandParentNode;

			// TODO saite ar vecāku gudrākā veidā?
			// Link between parent of the coordination and coordinated part.
			if (!wholeCoordNodeTok.depsBackbone.isRootDep())
				partNodeTok.deps.add(wholeCoordNodeTok.depsBackbone);

			// Links between phrase parts
			if (coordParentNode.getNodeType() == PmlANode.Type.X
					|| coordParentNode.getNodeType() == PmlANode.Type.PMC)
			{
				Token phraseRootToken = s.getEnhancedOrBaseToken(phraseParent);
				List<PmlANode> phraseParts = phrase.getChildren();
				if (phraseParts != null)
					for (PmlANode phrasePart : phraseParts)
					{
						if (phrasePart.getAnyLabel().equals(LvtbRoles.PUNCT)
								|| phrasePart.isSameNode(coordPartNode))
							continue;

						Token otherPartToken = s.getEnhancedOrBaseToken(phrasePart);
						if (otherPartToken.depsBackbone.headID.equals(phraseRootToken.getFirstColumn()))
							s.setEnhLink(coordPartNode, phrasePart,
									otherPartToken.depsBackbone.getRoleTuple(), false, false);
						// Todo: use/make analogue to DepRelLogic.getSingleton().depToUD(node, node, ...) ?
					}
			}
		} else
		{
			// Link between parent of the coordination and coordinated part.
			if (coordParentNode.getNodeType() == PmlANode.Type.ROOT
					&& !wholeCoordNodeTok.depsBackbone.isRootDep())
			{
				Tuple<UDv2Relations, String> role = dpTransf.depToUDEnhanced(
						coordPartNode, coordParentNode,
						wholeCoordANode.getRole());
				//partNodeTok.deps.add(parentNodeTok.depsBackbone);
				s.setEnhLink(coordParentNode, coordPartNode, role,
						false, false);
			}
		}

		// Links between dependants of the coordination and coordinated parts.
		List<PmlANode> dependents = wholeCoordANode.getChildren();
		if (dependents != null)
			for (PmlANode dependent : dependents)
			{
				Tuple<UDv2Relations, String> role = dpTransf.depToUDEnhanced(
						dependent, coordPartNode,
						dependent.getRole());
				s.setEnhLink(coordPartNode, dependent,
						role, false, false);
			}
	}
}
