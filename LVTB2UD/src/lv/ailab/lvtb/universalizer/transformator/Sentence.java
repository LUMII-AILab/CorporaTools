package lv.ailab.lvtb.universalizer.transformator;

import lv.ailab.lvtb.universalizer.conllu.EnhencedDep;
import lv.ailab.lvtb.universalizer.conllu.MiscKeys;
import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.LvtbCoordTypes;
import lv.ailab.lvtb.universalizer.pml.LvtbRoles;
import lv.ailab.lvtb.universalizer.pml.LvtbXTypes;
import lv.ailab.lvtb.universalizer.pml.PmlANode;
import lv.ailab.lvtb.universalizer.pml.utils.PmlANodeListUtils;
import lv.ailab.lvtb.universalizer.transformator.morpho.*;
import lv.ailab.lvtb.universalizer.transformator.syntax.DepRelLogic;
import lv.ailab.lvtb.universalizer.transformator.syntax.PhrasePartDepLogic;
import lv.ailab.lvtb.universalizer.utils.Tuple;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Sentence data and some generic often used UD subtree construction routines.
 * Created on 2016-04-20.
 *
 * @author Lauma
 */
public class Sentence
{
	/**
	 * Sentence ID.
	 */
	public String id;
	/**
	 * Original text.
	 */
	public String text;
	/**
	 * LVTB PML representation of the tree.
	 */
	public PmlANode pmlTree;
	/**
	 * UD dependency tree as and conll-style array.
	 */
	public ArrayList<Token> conll = new ArrayList<>();
	/**
	 * Mapping from A-level ids to CoNLL tokens.
	 * Here goes phrase representing empty nodes, if it has been resolved, which
	 * child will be the parent of the dependency subtree.
	 */
	public HashMap<String, Token> pmlaToConll = new HashMap<>();
	/**
	 * Additional mapping for enhanced dependencies. Only updated, when mapping
	 * is different from pmlaToConll.
	 */
	public HashMap<String, Token> pmlaToEnhConll = new HashMap<>();

	/**
	 * Mapping from node ID to IDs of coordinated parts that are direct or
	 * indirect part of this node. Must be populated before transformation.
	 * After populating this structure, each set contains at least node itself.
	 */
	public HashMap<String, HashSet<String>> coordPartsUnder = new HashMap<>();

	/**
	 * Mapping from PML phrase node ID to constituent node ID that is root foor
	 * corresponding dependency subtree (head constituent). Populated during
	 * transformation.
	 */
	public HashMap<String, String> phraseHeadConstituents = new HashMap<>();

	/**
	 * Mapping (multimap) between controled and rised subjects and nodes they
	 * should be linked to. Indexed by subject node IDs. Governor nodes are
	 * sorted by the depth in the tree, starting with deepest, then by deep ord
	 * descending. Includes standard subject links, i.e. cases when subject is
	 * directly dependent from the node. Must be populated before
	 * transformation.
	 */
	public HashMap<String, ArrayList<String>> subj2gov = new HashMap<>();

	/**
	 * Set containing currently known ellipted predicates for which core
	 * argument or adjunct is elevated and, thus, some of dependants might need
	 * orphan role. Populated during transformation.
	 */
	public HashSet<String> ellipsisWithOrphans = new HashSet<>();

	/**
	 * Postfix used to append to the ellipsis node ID during the non-empty
	 * ellipsis split to create ID for new node.
	 */
	public static final String ID_POSTFIX = "-SPLIT";

	public Sentence(PmlANode pmlTree)
	{
		this.pmlTree = pmlTree;
		id = pmlTree.getId();
	}

	public String toConllU()
	{
		StringBuilder res = new StringBuilder();
		res.append("# sent_id = ");
		res.append(id);
		res.append("\n");
		res.append("# text = ");
		res.append(text);
		res.append("\n");
		for (Token t : conll)
			res.append(t.toConllU());
		res.append("\n");
		return res.toString();
	}

	// ===== Pre-transformation preparations. ==================================

	//TODO incorporate in constructor?
	/**
	 * Do all the necessary preparation steps before transformation, collect all
	 * beforehand needed information.
	 */
	public void prepare()
	{
		repopulateCoordPartsUnder();
		repopulateSubjectMap();
	}

	/**
	 * This populates subj2gov map by collecting controlled subjects. Links to
	 * auxVerb labeled xPred parts with true auxiliary verb lemma not collected.
	 */
	public void repopulateSubjectMap()
	{
		HashMap<String, HashSet<String>> gov2subj = new HashMap<>();
		gov2subj = getGov2subj(pmlTree, gov2subj);
		for (String govNode : gov2subj.keySet()) for (String subj : gov2subj.get(govNode))
		{
			ArrayList<String> tmp = subj2gov.get(subj);
			if (tmp == null) tmp = new ArrayList<>();
			//if (!node.equals(pmlTree.getDescendant(subj).getParent().getId()))
				tmp.add(govNode);
			if (!tmp.isEmpty()) subj2gov.put(subj, tmp);
		}

		for (String subjNode : subj2gov.keySet())
		{
			ArrayList<String> tmp = subj2gov.get(subjNode).stream()
					.map(id -> pmlTree.getThisOrDescendant(id))
					.map(n -> Tuple.of(n, n.getDepthInTree()))
					.sorted((t1, t2) -> t2.second.equals(t1.second)
							? t2.first.getDeepOrd().compareTo(t1.first.getDeepOrd())
							: t2.second.compareTo(t1.second))
					.map(t -> t.first.getId()).collect(Collectors.toCollection(ArrayList::new));
			subj2gov.put(subjNode, tmp);
		}
	}

	/**
	 * This collects everything like controlled or direct subject, excluding
	 * links to auxVerb labeled xPred parts with true auxiliary verb lemma.
	 * @param aNode				node whose subtree should be surveyed
	 * @param resultAccumulator	result accumulator for recursive function
	 * @return	multimapping from potential subject parent node IDs to subject
	 * 			node IDs.
	 */
	protected HashMap<String, HashSet<String>> getGov2subj(PmlANode aNode, HashMap<String, HashSet<String>> resultAccumulator)
	{
		if (aNode == null) return null;
		if (resultAccumulator == null) resultAccumulator = new HashMap<>();
		String id = aNode.getId();

		// Update coresponding subject list with dependant subjects.
		HashSet<String> collectedSubjs = resultAccumulator.get(id);
		if (collectedSubjs == null) collectedSubjs = new HashSet<>();
		List<PmlANode> subjs = aNode.getChildren(LvtbRoles.SUBJ);
		if (subjs!= null) subjs.addAll(aNode.getChildren(LvtbRoles.SUBJCL));
		else subjs = aNode.getChildren(LvtbRoles.SUBJCL);
		if (subjs != null) for (PmlANode s : subjs)
			collectedSubjs.add(s.getId());
		if (!collectedSubjs.isEmpty()) resultAccumulator.put(id, collectedSubjs);

		// Find xPred and update their parts' subject lists.
		PmlANode phrase = aNode.getPhraseNode();
		List<PmlANode> phraseParts = null;
		if (phrase != null)
		{
			phraseParts = phrase.getChildren();
			String phraseType = phrase.getPhraseType();
			if (LvtbXTypes.XPRED.equals(phraseType) ||
					LvtbCoordTypes.CRDPARTS.equals(phraseType))
				for (PmlANode phrasePart : phraseParts)
			{
				// Do not add standard auxiliaries used as aux.
				String partRole = phrasePart.getRole();
				if (LvtbXTypes.XPRED.equals(phraseType) && LvtbRoles.AUXVERB.equals(partRole)
						&& ((phrasePart.getM() != null && MorphoTransformator.isTrueAux(phrasePart.getM().getLemma()))
							|| MorphoTransformator.isTrueAux(phrasePart.getReductionLemma())))
					continue;
				// Do not add coordination conjuctions and punctuation.
				if (LvtbCoordTypes.CRDPARTS.equals(phraseType) && !LvtbRoles.CRDPART.equals(partRole))
					continue;
				// Do not add split ellipsis nodes.
				if (LvtbRoles.ELLIPSIS_TOKEN.equals(partRole))
					continue;

				String partId = phrasePart.getId();
				HashSet<String> collectedPartSubjs = resultAccumulator.get(partId);
				if (collectedPartSubjs == null)
					collectedPartSubjs = new HashSet<>();
				collectedPartSubjs.addAll(collectedSubjs);
				if (!collectedPartSubjs.isEmpty()) resultAccumulator.put(partId, collectedPartSubjs);
			}
		}

		// Posprocess children.
		List<PmlANode> dependants = aNode.getChildren();
		if (dependants != null) for (PmlANode dependant : dependants)
			getGov2subj(dependant, resultAccumulator);
		if (phraseParts != null) for (PmlANode phrasePart : phraseParts)
			getGov2subj(phrasePart, resultAccumulator);
		return resultAccumulator;
	}

	public void repopulateCoordPartsUnder()
	{
		coordPartsUnder = new HashMap<>();
		populateCoordPartsUnder(pmlTree);
		/*System.out.println(
			coordPartsUnder.keySet().stream().sorted()
				.filter(k -> coordPartsUnder.containsKey(k))
				.filter(k -> coordPartsUnder.get(k) != null)
				.map(k -> k + " -> " + coordPartsUnder.get(k).stream().sorted().reduce((a, b) -> a + ", " + b).orElse("NULL"))
				.reduce((k1, k2) -> k1 + System.lineSeparator() + k2).orElse("NULL"));
				//*/
	}

	protected void populateCoordPartsUnder(PmlANode aNode)
	{
		if (aNode == null) return;
		String id = aNode.getId();
		HashSet<String> eqs = new HashSet<String>(){{add(id);}};

		// Preprocess dependency children.
		List<PmlANode> dependants = aNode.getChildren();
		if (dependants != null) for (PmlANode dependant : dependants)
			populateCoordPartsUnder(dependant);

		PmlANode phrase = aNode.getPhraseNode();
		if (phrase != null)
		{
			// Preprocess phrase parts.
			List<PmlANode> phraseParts = phrase.getChildren();
			if (phraseParts != null) for (PmlANode phrasePart : phraseParts)
				populateCoordPartsUnder(phrasePart);

			// Add coordinated subparts for this node.
			if (phrase.getNodeType() == PmlANode.Type.COORD)
			{
				List<PmlANode> importantParts = phrase.getChildren(LvtbRoles.CRDPART);
				if (importantParts != null) for (PmlANode phrasePart : importantParts)
				{
					String partId = phrasePart.getId();
					if (coordPartsUnder.containsKey(partId))
						eqs.addAll(coordPartsUnder.get(partId));
				}
			}
		}
		coordPartsUnder.put(id, eqs);
	}


	// ===== After-transformation clean-ups. ===================================

	public void removeUnlabeledDeps()
	{
		for (Token t : conll)
		{
			t.deps = t.deps.stream()
					.filter(ed -> ed.role != null && ed.role != UDv2Relations.DEP)
					.collect(Collectors.toCollection(HashSet::new));
			/*HashSet<String> goodEnhDepHeads = new HashSet<>(t.deps.stream()
					.filter(d -> (d.role != UDv2Relations.DEP))
					.map(d -> d.headID).collect(Collectors.toSet()));
			HashSet<EnhencedDep> noRoleEnhDepsToDelete = new HashSet<>(t.deps.stream()
					.filter(d -> (d.role == UDv2Relations.DEP && goodEnhDepHeads.contains(d.headID)
							&& (d.rolePostfix == null || d.rolePostfix.isEmpty())))
					.collect(Collectors.toSet()));
			t.deps.removeAll(noRoleEnhDepsToDelete);*/
		}
	}

	// ===== Getters and elaborated getters. ===================================

	/**
	 * Get the enhanced token assigned for this node. If there is no enhanced
	 * token assigned, return assigned base token.
	 * @param aNode	node whose token must be found
	 * @return	enhanced token or base token, or null (in that order)
	 */
	public Token getEnhancedOrBaseToken(PmlANode aNode)
	{
		if (aNode == null) return null;
		String id = aNode.getId();
		if (id == null) return null;
		Token resToken = pmlaToEnhConll.get(id);
		if (resToken == null) resToken = pmlaToConll.get(id);
		return resToken;
	}

	/**
	 * For a given node either return IDs of the coordinated parts this node
	 * represents (if this node is coordination) or node's ID otherwise. In case
	 * a part is a coordination itself, its coordinated parts are included in
	 * the result instead of part itself.
	 * @param aNodeId	Id of node whose coordination parts are needed
	 * @return	IDs of coordinated parts or node itself
	 */
	public HashSet<String> getCoordPartsUnderOrNode (String aNodeId)
	{
		if (aNodeId == null) return null;
		HashSet<String> res = new HashSet<>();
		if (coordPartsUnder.containsKey(aNodeId))
			res.addAll(coordPartsUnder.get(aNodeId));
		else res.add(aNodeId);
		return res;
	}

	/**
	 * For a given node either return IDs of the coordinated parts this node
	 * represents (if this node is coordination) or node's ID otherwise. In case
	 * a part is a coordination itself, its coordinated parts are included in
	 * the result instead of part itself.
	 * @param aNode	node whose coordination parts are needed
	 * @return	IDs of coordinated parts or node itself
	 */
	public HashSet<String> getCoordPartsUnderOrNode (PmlANode aNode)
	{
		return getCoordPartsUnderOrNode(aNode.getId());
	}

	/**
	 * For a certain node ID find its head constituent list and then find all
	 * coordinated parts for each node in that list. Iterate this process
	 * until nothing new can be found.
	 * @param aNodeId				node ID to indicate node
	 * @param includeSelfCoordParts	should the result include coordPartsUnder
	 *                              for given node?
	 * @return
	 */
	public HashSet<String> getAllAlternatives(String aNodeId, boolean includeSelfCoordParts)
	{
		if (aNodeId == null) return null;
		HashSet<String> result = getCoordPartsUnderHeadConstits1Lvl(aNodeId, includeSelfCoordParts);
		if (!includeSelfCoordParts)
			result.remove(aNodeId);
		int oldSize = 1;
		while (oldSize < result.size())
		{
			oldSize = result.size();
			HashSet<String> newResult = new HashSet<>();
			for (String tmpId : result)
				newResult.addAll(getCoordPartsUnderHeadConstits1Lvl(tmpId, true));
			result = newResult;
		}
		result.add(aNodeId);
		return result;
	}

	/**
	 * For a certain node ID find its head constituent list and then find all
	 * coordinated parts for each node in that list. Do not include coordinated
	 * analogues of the given node.
	 * @param aNodeId				node ID to indicate node
	 * @param includeSelfCoordParts	should the result include coordPartsUnder
	 *                              for given node?
	 * @return
	 */
	protected HashSet<String> getCoordPartsUnderHeadConstits1Lvl(
			String aNodeId, boolean includeSelfCoordParts)
	{
		if (aNodeId == null) return null;
		ArrayList<String> headConstituentList = this.getHeadConstituentList(aNodeId);
		HashSet<String> result = new HashSet<>();
		result.add(aNodeId);
		if (!includeSelfCoordParts) headConstituentList.remove(aNodeId);
		for (String headConstituent : headConstituentList)
			result.addAll(getCoordPartsUnderOrNode(headConstituent));
		return result;
	}

	/**
	 * For a certain node ID find if it is a phrase with known head constituent.
	 * If it is, repeat the same with the found constituent node. Return a list
	 * with the first given node and then all head constituents further found.
	 * @param aNodeId	node whose head constituent (or constituent list) should
	 *                  be found
	 * @return	null, if argument is null; other wise list with given node, its
	 * 			head constituent, its head constituent etc...
	 */
	public ArrayList<String> getHeadConstituentList(String aNodeId)
	{
		if (aNodeId == null) return null;
		ArrayList<String> result = new ArrayList<>();
		result.add(aNodeId);
		while (phraseHeadConstituents.containsKey(aNodeId))
		{
			aNodeId = phraseHeadConstituents.get(aNodeId);
			result.add(aNodeId);
		}
		return result;
	}

	// ===== Making new nodes. =================================================

	/**
	 * Create "empty" token for ellipsis.
	 * @param aNode			PML A node, for which respective ellipsis token must
	 *                      be made.
	 * @param baseTokenId	ID for token after which the new token must be
	 *                      inserted.
	 * @param addNodeId		should the ID of the aNode be added to the MISC
	 *                      field of the new token?
	 */
	public void createNewEnhEllipsisNode(
			PmlANode aNode, String baseTokenId, boolean addNodeId)
	{
		String nodeId = aNode.getId();
		String redXPostag = XPosLogic.getXpostag(aNode.getReductionTagPart());

		// Decimal token (reduction node) must be inserted after newRootToken.
		Token newRootToken = pmlaToConll.get(baseTokenId);
		int position = conll.indexOf(newRootToken) + 1;
		while (position < conll.size() && newRootToken.idBegin == conll.get(position).idBegin)
			position++;

		// Fill the fields for the new token.
		Token decimalToken = new Token();
		decimalToken.idBegin = newRootToken.idBegin;
		decimalToken.idSub = conll.get(position-1).idSub+1;
		decimalToken.idEnd = decimalToken.idBegin;
		decimalToken.xpostag = redXPostag;
		decimalToken.form = aNode.getReductionFormPart();
		if (decimalToken.xpostag == null || decimalToken.xpostag.isEmpty() || decimalToken.xpostag.equals("_"))
			StandardLogger.l.doInsentenceWarning(String.format(
					"Ellipsis node %s with reduction field \"%s\" has no tag.",
					nodeId, aNode.getReduction()));
		else
		{
			String assumedLvtbLemma = null;
			if (decimalToken.form != null && !decimalToken.form.isEmpty())
				assumedLvtbLemma = AnalyzerWrapper.getLemma(
						decimalToken.form, decimalToken.xpostag);
			decimalToken.upostag = UPosLogic.getUPosTag(decimalToken.form,
					assumedLvtbLemma, decimalToken.xpostag);
			decimalToken.feats = FeatsLogic.getUFeats(decimalToken.form,
					assumedLvtbLemma, decimalToken.xpostag);
			decimalToken.lemma = LemmaLogic.getULemma(assumedLvtbLemma, redXPostag);
		}
		if (addNodeId && nodeId != null && !nodeId.isEmpty())
		{
			decimalToken.addMisc(MiscKeys.LVTB_NODE_ID, nodeId);
			StandardLogger.l.addIdMapping(id, decimalToken.getFirstColumn(), nodeId);
		}

		// Add the new token to the sentence data structures.
		conll.add(position, decimalToken);
		pmlaToEnhConll.put(nodeId, decimalToken);
	}


	// ===== Simple link setting. ==============================================

	/**
	 * Set both base and enhanced dependency links for tokens coressponding to
	 * the given PML nodes, but do not set circular dependencies. It is expected
	 * that pmlaToEnhConll (if needed) and pmlaToConll contains links from given
	 * PML nodes's IDs to corresponding tokens.
	 * Return silently, if there was no token for given child node. Fail, if
	 * there was no token for given parent node. This asymmetry is done because
	 * inserted nodes have no corresponding UD token, and childless nodes should
	 * just be ignored, while missing node in the middle of the tree, is a major
	 * error.
	 * TODO Maybe we should keep ignore-node list and check agains that?
	 * @param parent 		PML node describing parent
	 * @param child			PML node describing child
	 * @param baseDep		label to be used for base dependency
	 * @param enhancedDep	label to be used for enhanced dependency
	 * @param setBackbone	if enhanced dependency is made, should it be set as
	 *                      backbone for child node
	 * @param cleanOldDeps	whether previous contents from deps field should be
	 *                      removed
	 * @param forbidHeadDuplicates	should multiple enhanced links with the same
	 *                              head be allowed
	 */
	public void setLink (
			PmlANode parent, PmlANode child, UDv2Relations baseDep,
			Tuple<UDv2Relations, String> enhancedDep, boolean setBackbone,
			boolean cleanOldDeps, boolean forbidHeadDuplicates)
	{
		Token rootBaseToken = pmlaToConll.get(parent.getId());
		Token rootEnhToken = pmlaToEnhConll.get(parent.getId());
		if (rootEnhToken == null) rootEnhToken = rootBaseToken;
		Token childBaseToken = pmlaToConll.get(child.getId());
		Token childEnhToken = pmlaToEnhConll.get(child.getId());
		if (childEnhToken == null) childEnhToken = childBaseToken;
		if (childBaseToken == null) return;

		// Set base dependency, but avoid circular dependencies.
		if (!rootBaseToken.equals(childBaseToken) && childBaseToken.idSub < 1)
		{
			childBaseToken.head = Tuple.of(rootBaseToken.getFirstColumn(), rootBaseToken);
			childBaseToken.deprel = baseDep;
		}

		// Set enhanced dependencies, but avoid circular.
		childEnhToken.setEnhancedHead(rootEnhToken, enhancedDep, setBackbone,
				cleanOldDeps, forbidHeadDuplicates);
	}

	/**
	 * Set enhanced dependency link for tokens coressponding to the given PML
	 * nodes, but do not set circular dependencies. It is expected that
	 * pmlaToEnhConll (if needed) and pmlaToConll contains links from given
	 * PML nodes's IDs to corresponding tokens.
	 * @param parent 		PML node describing parent
	 * @param child			PML node describing child
	 * @param enhancedDep	label to be used for enhanced dependency
	 * @param setBackbone	if enhanced dependency is made, should it be set as
	 *                      backbone for child node
	 * @param cleanOldDeps	whether previous contents from deps field should be
	 *                      removed
	 * @param forbidHeadDuplicates	should multiple enhanced links with the same
	 *                              head be allowed
	 */
	public void setEnhLink (
			PmlANode parent, PmlANode child, Tuple<UDv2Relations, String> enhancedDep,
			boolean setBackbone, boolean cleanOldDeps, boolean forbidHeadDuplicates)
	{
		Token rootBaseToken = pmlaToConll.get(parent.getId());
		Token rootEnhToken = pmlaToEnhConll.get(parent.getId());
		if (rootEnhToken == null) rootEnhToken = rootBaseToken;
		Token childBaseToken = pmlaToConll.get(child.getId());
		Token childEnhToken = pmlaToEnhConll.get(child.getId());
		if (childEnhToken == null) childEnhToken = childBaseToken;

		// Set enhanced dependencies, but avoid circular.
		childEnhToken.setEnhancedHead(rootEnhToken, enhancedDep, setBackbone,
				cleanOldDeps, forbidHeadDuplicates);
	}

	/**
	 * Set basic dependency link for tokens coressponding to the given PML
	 * nodes, but do not set circular dependencies and do not set anything as a
	 * parent to decimal node. It is expected that pmlaToConll contains links
	 * from given PML nodes's IDs to corresponding
	 * tokens.
	 * @param parent 		PML node describing parent
	 * @param child			PML node describing child
	 * @param baseDep	label to be used for enhanced dependency
	 */
	public void setBaseLink (PmlANode parent, PmlANode child, UDv2Relations baseDep)
	{
		Token rootBaseToken = pmlaToConll.get(parent.getId());
		Token childBaseToken = pmlaToConll.get(child.getId());

		// Set base dependency, but avoid circular dependencies.
		if (!rootBaseToken.equals(childBaseToken) && childBaseToken.idSub < 1)
		{
			childBaseToken.head = Tuple.of(rootBaseToken.getFirstColumn(), rootBaseToken);
			childBaseToken.deprel = baseDep;
		}
	}

	/**
	 * setLink() + sets coordination propagation crosslinks from parent's
	 * coordinated equivalents to child's coordinated equivalents. Can be used
	 * only if all crosslinks have the same role as main enhanced dependency
	 * link.
	 * Use for relinking phrasal constituents.
	 * @param parent 		PML node describing parent
	 * @param child			PML node describing child
	 * @param baseDep		label to be used for base dependency
	 * @param enhancedDep	label to be used for enhanced dependency and for
	 *                      crosslinks
	 * @param setBackbone	if enhanced dependency is made, should it be set as
	 *                      backbone for child node
	 * @param cleanOldDeps	whether previous contents from deps field should be
	 *                      removed
	 * @param forbidHeadDuplicates	should multiple enhanced links with the same
	 *                              head be allowed
	 */
	public void setLinkAndCorsslinksPhrasal(
			PmlANode parent, PmlANode child, UDv2Relations baseDep,
			Tuple<UDv2Relations, String> enhancedDep, boolean setBackbone,
			boolean cleanOldDeps, boolean forbidHeadDuplicates)
	{
		setLink(parent, child, baseDep, enhancedDep, setBackbone, cleanOldDeps,
				forbidHeadDuplicates);
		addFixedRoleCrosslinks(parent, child, enhancedDep, forbidHeadDuplicates);
	}

	/**
	 * Set both base and enhanced dependency links as root for token(s)
	 * coressponding to the given PML node. It is expecte that pmlaToEnhConll
	 * (if needed) and pmlaToConll contains links from given PML nodes's IDs to
	 * corresponding tokens. This dependency is set as backbone by default.
	 * @param node 			PML node to be made root
	 * @param cleanOldDeps	whether previous contents from deps field should be
	 *                      removed
	 */
	public void setRoot (PmlANode node, boolean cleanOldDeps)
	{
		Token childBaseToken = pmlaToConll.get(node.getId());
		Token childEnhToken = pmlaToEnhConll.get(node.getId());
		if (childEnhToken == null) childEnhToken = childBaseToken;

		// Set base dependency.
		if (childBaseToken.idSub < 1)
		{
			childBaseToken.head = Tuple.of("0", null);
			childBaseToken.deprel = UDv2Relations.ROOT;
		}

		// Set enhanced dependencies.
		childEnhToken.setEnhancedHeadRoot(true, cleanOldDeps);
	}


	// ===== Simple relinking. =================================================

	/*
	 * Changes the heads for all dependencies set (both base and enhanced) for
	 * given childnode. It is expected that pmlaToEnhConll (if needed) and
	 * pmlaToConll contains links from given PML nodes's IDs to corresponding
	 * tokens.
	 * @param newParent	new parent
	 * @param child		child node whose attachment should be changed
	 */
/*	public void changeHead (PmlANode newParent, PmlANode child)
	{
		Token rootBaseToken = pmlaToConll.get(newParent.getId());
		Token rootEnhToken = pmlaToEnhConll.get(newParent.getId());
		if (rootEnhToken == null) rootEnhToken = rootBaseToken;
		Token childBaseToken = pmlaToConll.get(child.getId());
		Token childEnhToken = pmlaToEnhConll.get(child.getId());
		if (childEnhToken == null) childEnhToken = childBaseToken;

		// Set base dependency, but avoid circular dependencies.
		// FIXME is "childBaseToken.head != null" ok?
		if (!rootBaseToken.equals(childBaseToken) && childBaseToken.head != null)
			childBaseToken.head = Tuple.of(rootBaseToken.getFirstColumn(), rootBaseToken);

		// Set enhanced dependencies, but avoid circular.
		if (!childEnhToken.equals(rootEnhToken) && !childEnhToken.deps.isEmpty())
		{
			HashSet<EnhencedDep> newDeps = new HashSet<>();
			for (EnhencedDep ed : childEnhToken.deps)
				newDeps.add(new EnhencedDep(rootEnhToken, ed.role));
			childEnhToken.deps = newDeps;
			if (childEnhToken.depsBackbone != null)childEnhToken.depsBackbone =
					new EnhencedDep(rootEnhToken, childEnhToken.depsBackbone.role);
		}
	}//*/


	// ===== Dependency related linking and relinking. =========================

	/**
	 * Make a list of given nodes UD dependents of the designated parent. Set UD
	 * deprel, deps and deps backbone for each child. If designated parent is
	 * included in child list node, circular dependency is not made, role is not
	 * set.
	 * Use for relinking depdenencies.
	 * @param parent	PML parent node in the hybrid tree (in case of
	 * 	 *              phrase nodes, corresponding tokens must be set correctly)
	 * @param children	list of child nodes
	 * @param addCoordPropCrosslinks    should also coordination propagation be
	 *                                  done?
	 * @param forbidHeadDuplicates		should multiple enhanced links with the
	 *                              	same head be allowed
	 */
	public void relinkAllDependants(
			PmlANode parent, List<PmlANode> children,
			boolean addCoordPropCrosslinks, boolean addControledSubjects,
			boolean forbidHeadDuplicates)
	{
		if (children == null || children.isEmpty()) return;
		// Process children.
		for (PmlANode child : children)
		{
			Tuple<UDv2Relations, String> role = relinkSingleDependant(parent, child,
					addCoordPropCrosslinks, forbidHeadDuplicates);
			if (addControledSubjects && (
					role.first == UDv2Relations.NSUBJ || role.first == UDv2Relations.NSUBJ_PASS))
				addSubjectsControlers(child, false, addCoordPropCrosslinks, forbidHeadDuplicates);
			if (addControledSubjects && (
					role.first == UDv2Relations.CSUBJ || role.first == UDv2Relations.CSUBJ_PASS))
				addSubjectsControlers(child, true, addCoordPropCrosslinks, forbidHeadDuplicates);
		}
	}

	/**
	 * Based on previously populated subject map add propagated subjects
	 * for given node.
	 * @param subject		subject node from which controll links sould be made
	 * @param isClausal		clausal subject (true) or ordinary (false)
	 * @param addCoordPropCrosslinks    should also coordination propagation be
	 * 									done?
	 * @param forbidHeadDuplicates		should multiple enhanced links with the
	 *                              	same head be allowed
	 */
	public void addSubjectsControlers(
			PmlANode subject, boolean isClausal, boolean addCoordPropCrosslinks,
			boolean forbidHeadDuplicates)
	{
		if (subject == null) return;
		String subjectId = subject.getId();
		ArrayList <String> parentIds = subj2gov.get(subjectId);
		if (parentIds == null || parentIds.isEmpty()) return;

		for (String parentId : parentIds)
		{
			PmlANode parent = pmlTree.getDescendant(parentId);
			Tuple<UDv2Relations, String> enhRole = DepRelLogic.cRSubjToUD(
					subject, parent, isClausal);
			setEnhLink(parent, subject, enhRole, false, false, forbidHeadDuplicates);
			if (addCoordPropCrosslinks && UDv2Relations.canPropagatePrecheck(enhRole.first))
				addDependencyCrosslinks(parent, subject, forbidHeadDuplicates,
						(isClausal ? DepCroslinkParams.CLAUSE_CRSUBJ : DepCroslinkParams.NORMAL_CRSUBJ));
			//System.out.println(pmlaToConll.get(subject.getId()).toConllU());
		}
	}

	/**
	 * Make a given node a child of the designated parent. Set UD role for the
	 * child. Set enhanced dependency and deps backbone. If designated parent is
	 * the same as child node, circular dependency is not made, role is not set.
	 * Use for relinking depdenencies.
	 * @param parent	PML parent node in the hybrid tree (in case of
	 *                  phrase nodes, corresponding tokens must be set correctly)
	 * @param child		designated child
	 * @param addCoordPropCrosslinks	should also coordination propagation be
	 *                                  done?
	 * @param forbidHeadDuplicates		should multiple enhanced links with the
	 *                              	same head be allowed
	 * @return enhanced role for main link
	 */
	protected Tuple<UDv2Relations, String> relinkSingleDependant(
			PmlANode parent, PmlANode child, boolean addCoordPropCrosslinks,
			boolean forbidHeadDuplicates)
	{
		UDv2Relations baseRole = DepRelLogic.depToUDBase(child,
				ellipsisWithOrphans.contains(parent.getId()));
		Tuple<UDv2Relations, String> enhRole = DepRelLogic.depToUDEnhanced(child);
		setBaseLink(parent, child, baseRole);
		setEnhLink(parent, child, enhRole, true, true, forbidHeadDuplicates);
		if (addCoordPropCrosslinks && UDv2Relations.canPropagatePrecheck(enhRole.first))
			addDependencyCrosslinks(parent, child, forbidHeadDuplicates, DepCroslinkParams.NO_CRSUBJ);
		return enhRole;
	}

	/**
	 * Utility function for dependency transformation. When dependency link
	 * between two nodes is transformed to UD, this can be used to add enhanced
	 * links (conjunct propagation) between nodes that are coordinated with
	 * parent or child.
	 * Uses UDv2Relations.canPropagateAftercheck() to determine if role should
	 * be made.
	 * Use for relinking dependencies.
	 * @param parent		phrase part to become dependency parent
	 * @param child			phrase part to become dependency child
	 * @param forbidHeadDuplicates	should multiple enhanced links with the same
	 *                              head be allowed
	 */
	protected void addDependencyCrosslinks (
			PmlANode parent, PmlANode child, boolean forbidHeadDuplicates,
			DepCroslinkParams croslinkParam)
	{
		HashSet<String> altParentKeys = coordPartsUnder.get(parent.getId());
		HashSet<String> altChildKeys = coordPartsUnder.get(child.getId());
		for (String altParentKey : altParentKeys) for (String altChildKey : altChildKeys)
		{
			PmlANode altParent = null;
			if (pmlTree.getId().equals(altParentKey)) altParent = pmlTree;
			else altParent = pmlTree.getDescendant(altParentKey);
			PmlANode altChild = pmlTree.getDescendant(altChildKey);
			// Role for ehnanced link is obtained by actual parent,
			// actual child, but by "place-holder" role obtained
			// from the original child.
			Tuple<UDv2Relations, String> childDeprel = null;
			switch (croslinkParam)
			{
				case NO_CRSUBJ:
					childDeprel = DepRelLogic.depToUDEnhanced(altChild, altParent, child.getRole());
					break;
				case NORMAL_CRSUBJ:
					childDeprel = DepRelLogic.cRSubjToUD(altChild, altParent, false);
					break;
				case CLAUSE_CRSUBJ:
					childDeprel = DepRelLogic.cRSubjToUD(altChild, altParent, true);
			}

			if (UDv2Relations.canPropagateAftercheck(childDeprel.first))
			{
				HashSet<String> altParentKeys2 = getAllAlternatives(altParentKey, false);
				HashSet<String> altChildKeys2 = getAllAlternatives(altChildKey, false);
				for (String altParentKey2 : altParentKeys2) for (String altChildKey2 : altChildKeys2)
				{
					PmlANode altParent2 = null;
					if (pmlTree.getId().equals(altParentKey2))
						altParent2 = pmlTree;
					else altParent2 = pmlTree.getDescendant(altParentKey2);
					PmlANode altChild2 = pmlTree.getDescendant(altChildKey2);
					setEnhLink(altParent2, altChild2, childDeprel, false,
							false, forbidHeadDuplicates);
				}
			}
		}
	}

	// ===== Constituency related linking & relinking. =========================

	/**
	 * For the given node find first children of the given type and make all
	 * other children depend on it. Set UD deprel and enhanced backbone for each
	 * child.
	 * Use for relinking phrasal constituents.
	 * @param phraseNode		node whose children must be processed, and whose
	 *                          type and tag is used, if needed, to obtain
	 *                          correct UD role for children
	 * @param newRootType		subroot for new UD structure will be searched
	 *                          between PML nodes with this type/role
	 * @param warnMoreThanOne	whether to warn if more than one potential root
	 *                          is found
	 * @param addCoordPropCrosslinks	should also coordination propagation be
	 *                                  done?
	 * @param forbidHeadDuplicates		should multiple enhanced links with the
	 *                              	same head be allowed
	 * @return root of the corresponding dependency structure
	 */
	public PmlANode allUnderFirstConstituent(
			PmlANode phraseNode, String newRootType, boolean warnMoreThanOne,
			boolean addCoordPropCrosslinks, boolean forbidHeadDuplicates)
	{
		List<PmlANode> children = phraseNode.getChildren();
		List<PmlANode> potentialRoots = phraseNode.getChildren(newRootType);
		if (warnMoreThanOne && potentialRoots != null && potentialRoots.size() > 1)
			StandardLogger.l.doInsentenceWarning(String.format(
					"\"%s\" in sentence \"%s\" has more than one \"%s\".",
					phraseNode.getPhraseType(), id, newRootType));
		PmlANode newRoot = PmlANodeListUtils.getFirstByDeepOrd(potentialRoots);
		if (newRoot == null)
		{
			StandardLogger.l.doInsentenceWarning(String.format(
					"\"%s\" in sentence \"%s\" has no \"%s\".",
					phraseNode.getPhraseType(), id, newRootType));
			newRoot = PmlANodeListUtils.getFirstByDeepOrd(children);
		}
		if (newRoot == null)
			throw new IllegalArgumentException(String.format(
					"\"%s\" in sentence \"%s\" seems to be empty.\n",
					phraseNode.getPhraseType(), id));
		relinkAllConstituents(newRoot, children, phraseNode, addCoordPropCrosslinks, forbidHeadDuplicates);
		return newRoot;
	}

	/**
	 * For the given node find first children of the given type and make all
	 * other children depend on it. Set UD deprel and enhanced backbone for each
	 * child.
	 * Use for relinking phrasal constituents.
	 * @param phraseNode        node whose children must be processed, and whose
	 *                          type and tag is used, if needed, to obtain
	 *                          correct UD role for children
	 * @param newRootType		subroot for new UD structure will be searched
	 *                          between PML nodes with this type/role
	 * @param newRootBackUpType backUpRole, if no nodes of newRootType is found
	 * @param warnMoreThanOne	whether to warn if more than one potential root
	 *                          is found
	 * @param addCoordPropCrosslinks	should also coordination propagation be
	 *                                  done?
	 * @param forbidHeadDuplicates		should multiple enhanced links with the
	 *                              	same head be allowed
	 * @return root of the corresponding dependency structure
	 */
	public PmlANode allUnderLastConstituent(
			PmlANode phraseNode, String newRootType, String newRootBackUpType,
			boolean warnMoreThanOne, boolean addCoordPropCrosslinks,
			boolean forbidHeadDuplicates)
	{
		List<PmlANode> children = phraseNode.getChildren();
		List<PmlANode> potentialRoots = phraseNode.getChildren(newRootType);
		if (newRootBackUpType != null &&
				(potentialRoots == null || potentialRoots.size() < 1))
			potentialRoots = phraseNode.getChildren(newRootBackUpType);
		PmlANode newRoot = PmlANodeListUtils.getLastByDeepOrd(potentialRoots);
		if (warnMoreThanOne && potentialRoots != null && potentialRoots.size() > 1)
			StandardLogger.l.doInsentenceWarning(String.format(
					"\"%s\" in sentence \"%s\" has more than one \"%s\".",
					phraseNode.getPhraseType(), id, newRoot.getAnyLabel()));
		if (newRoot == null)
		{
			StandardLogger.l.doInsentenceWarning(String.format(
					"\"%s\" in sentence \"%s\" has no \"%s\".",
					phraseNode.getPhraseType(), id, newRootType));
			newRoot = PmlANodeListUtils.getLastByDeepOrd(children);
		}
		if (newRoot == null)
			throw new IllegalArgumentException(String.format(
					"\"%s\" in sentence \"%s\" seems to be empty.\n",
					phraseNode.getPhraseType(), id));
		relinkAllConstituents(newRoot, children, phraseNode,
				addCoordPropCrosslinks, forbidHeadDuplicates);
		return newRoot;
	}

	/**
	 * Make a list of given nodes children of the designated parent. Set UD
	 * deprel, deps and deps backbone for each child. If designated parent is
	 * included in child list node, circular dependency is not made, role is not
	 * set.
	 * Use for relinking phrasal constituents.
	 * @param newRoot		designated parent
	 * @param children		list of child nodes
	 * @param phraseNode    phrase data node from PML data, used for obtaining
	 *                      correct UD role for children
	 * @param addCoordPropCrosslinks    should also coordination propagation be
	 *                                  done?
	 * @param forbidHeadDuplicates		should multiple enhanced links with the
	 *                              	same head be allowed
	 */
	public void relinkAllConstituents(
			PmlANode newRoot, List<PmlANode> children, PmlANode phraseNode,
			boolean addCoordPropCrosslinks, boolean forbidHeadDuplicates)
	{
		if (children == null || children.isEmpty()) return;

		// Process children.
		for (PmlANode child : children)
			relinkSingleConstituent(newRoot, child, phraseNode,
					addCoordPropCrosslinks, forbidHeadDuplicates);
	}

	/**
	 * Make a given node a child of the designated parent. Set UD role for the
	 * child. Set enhanced dependency and deps backbone. If designated parent is
	 * the same as child node, circular dependency is not made, role is not set.
	 * Use for relinking phrasal constituents.
	 * @param parent		designated parent
	 * @param child			designated child
	 * @param phraseNode    phrase data node from PML data, used for obtaining
	 *                      correct UD role for children
	 * @param addCoordPropCrosslinks	should also coordination propagation be
	 *                                  done?
	 * @param forbidHeadDuplicates		should multiple enhanced links with the
	 *                              	same head be allowed
	 */
	public void relinkSingleConstituent(
			PmlANode parent, PmlANode child, PmlANode phraseNode,
			boolean addCoordPropCrosslinks, boolean forbidHeadDuplicates)
	{
		if (child == null || child.isSameNode(parent)) return;

		Tuple<UDv2Relations, String> childDeprel =
				PhrasePartDepLogic.phrasePartRoleToUD(child, phraseNode);
		setLink(parent, child, childDeprel.first, childDeprel, true,
				true, forbidHeadDuplicates);
		if (addCoordPropCrosslinks && UDv2Relations.canPropagatePrecheck(childDeprel.first))
			addPhrasalConjunctCrosslinks(parent, child, phraseNode, forbidHeadDuplicates);
	}

	/**
	 * Utility function for phrase transformation. When phrase phrase is
	 * transformed to UD and a dependency link between two constituents is
	 * established, this can be used to add enhanced links (conjunct propagation)
	 * between nodes that are coordinated with parent or child.
	 * Uses UDv2Relations.canPropagateAftercheck() to determine if role should
	 * be made.
	 * Use for relinking phrasal constituents.
	 * @param parent		phrase part to become dependency parent
	 * @param child			phrase part to become dependency child
	 * @param phraseNode	phrase information by which role will be determined
	 * @param forbidHeadDuplicates	should multiple enhanced links with the same
	 *                              head be allowed
	 */
	protected void addPhrasalConjunctCrosslinks (
			PmlANode parent, PmlANode child, PmlANode phraseNode,
			boolean forbidHeadDuplicates)
	{
		if (parent == null || child == null) return;
		Tuple<UDv2Relations, String> childDeprel =
				PhrasePartDepLogic.phrasePartRoleToUD(child, phraseNode);
		if (!UDv2Relations.canPropagatePrecheck(childDeprel.first)) return;
		if (!UDv2Relations.canPropagateAftercheck(childDeprel.first)) return;

		HashSet<String> altParentKeys = getAllAlternatives(parent.getId(), true);
		HashSet<String> altChildKeys = getAllAlternatives(child.getId(), true);

		for (String altParentKey : altParentKeys)
			for (String altChildKey : altChildKeys)
			{
				PmlANode altParent = pmlTree.getDescendant(altParentKey);
				PmlANode altChild = pmlTree.getDescendant(altChildKey);
				setEnhLink(altParent, altChild, childDeprel, false,
						false, forbidHeadDuplicates);
			}
	}

	// ===== Utility for both donstituency and dependency related linking &
	// relinking. ==============================================================

	/**
	 * Utility function for either phrase or dependency transformation - this
	 * can be used to add enhanced links (conjunct propagation) between nodes
	 * that are coordinated with parent or child.
	 * NB. Use this sparingly as this function assumes that all crosslinks have the
	 * same role!
	 * Uses both UDv2Relations.canPropagatePrecheck() and
	 * UDv2Relations.canPropagateAftercheck() to determine if role should be
	 * made.
	 * @param parent		node to become dependency parent
	 * @param child			node to become dependency child
	 * @param childDeprel	fixed role for all crosslinks
	 * @param forbidHeadDuplicates	should multiple enhanced links with the same
	 *                              head be allowed
	 */
	protected void addFixedRoleCrosslinks(
			PmlANode parent, PmlANode child, Tuple<UDv2Relations,
			String> childDeprel, boolean forbidHeadDuplicates)
	{
		if (parent == null || child == null) return;
		if (!UDv2Relations.canPropagatePrecheck(childDeprel.first)) return;
		if (!UDv2Relations.canPropagateAftercheck(childDeprel.first)) return;

		HashSet<String> altParentKeys = getAllAlternatives(parent.getId(), true);
		HashSet<String> altChildKeys = getAllAlternatives(child.getId(), true);

		for (String altParentKey : altParentKeys) for (String altChildKey : altChildKeys)
		{
			PmlANode altParent = pmlTree.getDescendant(altParentKey);
			PmlANode altChild = pmlTree.getDescendant(altChildKey);
			setEnhLink(altParent, altChild, childDeprel,false,
					false, forbidHeadDuplicates);
		}
	}

	// ===== Other. ============================================================

	public boolean removeFromSubjectMap(String subjectId, String governorId)
	{
		if (!subj2gov.containsKey(subjectId)) return false;
		ArrayList<String> tmp = subj2gov.get(subjectId);
		if (!tmp.contains(governorId)) return false;
		tmp.remove(governorId);
		if (tmp.isEmpty()) subj2gov.remove(subjectId);
		else subj2gov.put(subjectId, tmp);
		return true;
	}

	public boolean removeGovFromSubjectMap(String governorId)
	{
		int removed = 0;
		for (String subjectId : subj2gov.keySet())
		{
			ArrayList<String> tmp = subj2gov.get(subjectId);
			if (!tmp.contains(governorId)) continue;
			tmp.remove(governorId);
			if (tmp.isEmpty()) subj2gov.remove(subjectId);
			else subj2gov.put(subjectId, tmp);
			removed++;
		}
		return removed > 0;
	}
	protected static enum DepCroslinkParams
	{
		NO_CRSUBJ, CLAUSE_CRSUBJ, NORMAL_CRSUBJ;
	}

	/**
	 * Check if given token is linked to the tree with projective dependency.
	 * Algorithm from https://github.com/UniversalDependencies/tools/blob/1a47bf7324ba0ec8256ef7986e8533f99869939c/validate.py
	 * @param token	token to analyze
	 * @return false if the link is nonprojective
	 */
	public boolean isProjective(Token token)
	{
		if (token == null || token.head == null) return true;
		Token parent = token.head.second;
		boolean hasStartedGap = parent == null;
		HashSet<Token> nonproj = new HashSet<>();
		// Find nodes in gap between punctuation and its parent.
		// Then check if these nodes are punctuation parents descendants.
		for (Token otherTok : conll)
		{
			// Process begining and the end of the gap.
			if (otherTok.equals(token) || otherTok.equals(parent))
			{
				if (hasStartedGap) break;
				else hasStartedGap = true;
			}
			// Process a node in the gap. Only basic dependencies are
			// checked.
			else if (hasStartedGap && otherTok.idSub < 1)
			{
				Token tmpParent = otherTok;
				// Travel up the ancestry chain.
				while (tmpParent != null && !tmpParent.equals(parent))
					tmpParent = tmpParent.head.second;
				// Null means root. If the root is reached without meeting
				// punctuation nodes parent, then there is non-projectivity.
				if (tmpParent == null) nonproj.add(otherTok);
			}
		}
		return nonproj.isEmpty();
	}

	/**
	 *  Checks whether a node is in a gap of a nonprojective edge. Report true
	 *  only if the node's parent is not in the same gap. Used to check that a
	 *  punctuation node does not cause nonprojectivity. But if it has been
	 *  dragged to the gap with a larger subtree, then it itself is not blamed.
	 *  Algorithm from https://github.com/UniversalDependencies/tools/blob/1a47bf7324ba0ec8256ef7986e8533f99869939c/validate.py
	 * @param token token to analyze
	 * @return true if this node creates some nonprojectivity
	 */
	public boolean createsNonprojectivity(Token token)
	{
		if (token == null || token.head == null) return false;
		Token parent = token.head.second;
		HashSet<Token> ancestry = getTokenAncestry(token);
		boolean parentIsBefore = parent.idBegin < token.idBegin;
		ArrayList<Token> nonprojectives = new ArrayList<>();
		for (Token otherTok : conll)
		{
			Token otherParent = null;
			if (otherTok.head != null) otherParent = otherTok.head.second;
			// Enhanced-only nodes are not checked.
			if (otherTok.idSub > 0) continue;
			// Ancestors are not considered.
			else if (ancestry.contains(otherTok)) continue;
			// If parent is before node, then tokens before parent are not considered.
			else if (parentIsBefore && otherTok.idBegin < parent.idBegin) continue;
			// If parent is after node, then tokens after parent are not considered.
			else if (!parentIsBefore && otherTok.idBegin > parent.idBegin) break;
			// "leftcross"
			else if (otherTok.idBegin < token.idBegin)
			{
				if ((otherParent != null && otherParent.idBegin > token.idBegin) // This means, link spans across token in question
						&& (parentIsBefore || otherParent.idBegin < parent.idBegin)) // This means, link doesn't span across both token in question and its parent
					nonprojectives.add(otherTok);
			}
			// "rightcross"
			else if (otherTok.idBegin > token.idBegin)
			{
				if ((otherParent == null || otherParent.idBegin < token.idBegin) // This means, link spans across token in question
						&& (!parentIsBefore || otherParent != null && otherParent.idBegin > parent.idBegin)) // This means, link doesn't span across both token in question and its parent
					nonprojectives.add(otherTok);
			}
		}
		return !nonprojectives.isEmpty();
	}

	protected HashSet<Token> getTokenAncestry (Token token)
	{
		HashSet<Token> result = new HashSet<>();
		while (token != null)
		{
			result.add(token);
			token = token.head.second;
		}
		return result;
	}

	/**
	 * Get the token following the given one in the sentence.
	 * @param token token whose following token must be found
	 * @return next token or null if this is the last one.
	 */
	public Token getNextSurfaceToken (Token token)
	{
		LinkedList<Token> tempRes = new LinkedList<>();
		boolean hasMetToken = false;
		for (Token t : conll)
		{
			if (hasMetToken && t.idSub < 1) tempRes.add(t);
			else if (t.equals(token)) hasMetToken = true;
		}
		return tempRes.getFirst();
	}
	/**
	 * Get the token before the given one in the sentence.
	 * @param token token whose previous token must be found
	 * @return previous token or null if this is the first one.
	 */
	public Token getPrevSurfaceToken (Token token)
	{
		LinkedList<Token> tempRes = new LinkedList<>();
		for (Token t : conll)
		{
			if (t.equals(token)) break;
			if (t.idSub < 1) tempRes.add(t);
		}
		return tempRes.getLast();
	}

	protected ArrayList<Token> getTokenChildren (Token token)
	{
		ArrayList<Token> result = new ArrayList<>();
		for (Token t : conll)
		{
			if (t.head.second == token || t.head.second != null &&
					t.head.second.equals(token))
				result.add(t);
		}
		return result;
	}

	public int countUdBaseRole (UDv2Relations role)
	{
		if (role == null) return 0;
		int result = 0;
		for (Token token : conll)
			if (token.deprel != null && token.deprel.equals(role)) result++;
		return result;
	}

	public int countUdEnhRole (UDv2Relations role)
	{
		if (role == null) return 0;
		int result = 0;
		for (Token token : conll)
			if (token != null && token.deps != null && !token.deps.isEmpty())
				for (EnhencedDep dep : token.deps)
					if (dep != null && dep.role != null && dep.role.equals(role)) result ++;
		return result;
	}
}
