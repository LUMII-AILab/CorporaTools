package lv.ailab.lvtb.universalizer.transformator;

import lv.ailab.lvtb.universalizer.conllu.EnhencedDep;
import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.LvtbRoles;
import lv.ailab.lvtb.universalizer.pml.LvtbXTypes;
import lv.ailab.lvtb.universalizer.pml.PmlANode;
import lv.ailab.lvtb.universalizer.pml.utils.PmlANodeListUtils;
import lv.ailab.lvtb.universalizer.transformator.syntax.PhrasePartDepLogic;
import lv.ailab.lvtb.universalizer.utils.Logger;
import lv.ailab.lvtb.universalizer.utils.Tuple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

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
	 * indirect part of this node.
	 */
	public HashMap<String, HashSet<String>> coordPartsUnder = new HashMap<>();

	/**
	 * Mapping from node ID to IDs of nodes that are subjects for this node.
	 */
	public HashMap<String, HashSet<String>> subjects = new HashMap<>();

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

	public void populateXPredSubjs()
	{
		subjects = new HashMap<>();
		populateXPredSubjs(pmlTree);
	}

	protected void populateXPredSubjs(PmlANode aNode)
	{
		if (aNode == null) return;
		String id = aNode.getId();

		// Update coresponding subject list with dependant subjects.
		HashSet<String> collectedSubjs = subjects.get(id);
		if (collectedSubjs == null) collectedSubjs = new HashSet<>();
		List<PmlANode> subjs = aNode.getChildren(LvtbRoles.SUBJ);
		if (subjs != null) for (PmlANode s : subjs)
			collectedSubjs.add(s.getId());
		subjects.put(id, collectedSubjs);

		// Fid xPred and update their parts' subject lists.
		PmlANode phrase = aNode.getPhraseNode();
		List<PmlANode> phraseParts = null;
		if (phrase != null)
		{
			phraseParts = phrase.getChildren();
			if (LvtbXTypes.XPRED.equals(phrase.getPhraseType()))
				for (PmlANode phrasePart : phraseParts)
			{
				String partId = phrasePart.getId();
				HashSet<String> collectedPartSubjs = subjects.get(partId);
				if (collectedPartSubjs == null)
					collectedPartSubjs = new HashSet<>();
				collectedPartSubjs.addAll(collectedSubjs);
				subjects.put(partId, collectedPartSubjs);
			}
		}

		// Posprocess children.
		List<PmlANode> dependants = aNode.getChildren();
		if (dependants != null) for (PmlANode dependant : dependants)
			populateXPredSubjs(dependant);
		if (phraseParts != null) for (PmlANode phrasePart : phraseParts)
			populateXPredSubjs(phrasePart);
	}

	public void populateCoordPartsUnder()
	{
		coordPartsUnder = new HashMap<>();
		populateCoordPartsUnder(pmlTree);
	}

	protected void populateCoordPartsUnder(PmlANode aNode)
	{
		// Preprocess children.
		if (aNode == null) return;
		List<PmlANode> dependants = aNode.getChildren();
		if (dependants != null) for (PmlANode dependant : dependants)
			populateCoordPartsUnder(dependant);
		PmlANode phrase = aNode.getPhraseNode();
		if (phrase == null) return;
		List<PmlANode> phraseParts = phrase.getChildren();
		if (phraseParts != null) for (PmlANode phrasePart : phraseParts)
			populateCoordPartsUnder(phrasePart);

		// Add coordinated subparts for this node.
		String id = aNode.getId();
		if (phrase.getNodeType() == PmlANode.Type.COORD)
		{
			HashSet<String> eqs = coordPartsUnder.get(id);
			if (eqs == null) eqs = new HashSet<>();
			if (phraseParts != null) for (PmlANode phrasePart : phraseParts)
			{
				String partId = phrasePart.getId();
				String role = phrasePart.getRole();
				if (LvtbRoles.CRDPART.equals(role))
				{
					if (coordPartsUnder.containsKey(partId))
						eqs.addAll(coordPartsUnder.get(partId));
					else eqs.add(partId);
				}
			}
			coordPartsUnder.put(id, eqs);
		}
	}

	/**
	 * Make a list of given nodes children of the designated parent. Set UD
	 * deprel, deps and deps backbone for each child. If designated parent is
	 * included in child list node, circular dependency is not made, role is not
	 * set.
	 * @param newRoot		designated parent
	 * @param children		list of child nodes
	 * @param phraseType    phrase type from PML data, used for obtaining
	 *                      correct UD role for children; can be null, if
	 *                      childDeprel is given
	 * @param phraseTag     phrase tag from PML data, used for obtaining
	 *                      correct UD role for children; can be null, if
	 *                      childDeprel is given
	 * @param childDeprel	dependency role + enhanced dependencies postfix to
	 *                      be used for DEPREL field and enhanced backbone for
	 *                      child nodes, or null, if
	 *                      DepRelLogic.phrasePartRoleToUD() should be used to
	 *                      get this info
	 * @param logger 		where all the warnings goes
	 */
	public void allAsDependents(
			PmlANode newRoot, List<PmlANode> children, String phraseType, String phraseTag,
			Tuple<UDv2Relations, String> childDeprel, Logger logger)
	{
		if (children == null || children.isEmpty()) return;

		// Process children.
		for (PmlANode child : children)
		{
			addAsDependent(newRoot, child, phraseType, phraseTag, childDeprel, logger);
		}
	}
	/**
	 * Make a given node a child of the designated parent. Set UD role for the
	 * child. Set enhanced dependency and deps backbone. If designated parent is
	 * the same as child node, circular dependency is not made, role is not set.
	 * @param parent		designated parent
	 * @param child			designated child
	 * @param phraseType    phrase type from PML data, used for obtaining
	 *                      correct UD role for children; can be null, if
	 *                      childDeprel is given
	 * @param phraseTag     phrase tag from PML data, used for obtaining
	 *                      correct UD role for children; can be null, if
	 *                      childDeprel is given
	 * @param childDeprel	dependency role + enhanced dependencies postfix to
	 *                      be used for DEPREL field and enhanced backbone for
	 *                      child nodes, or null, if
	 *                      DepRelLogic.phrasePartRoleToUD() should be used to
	 *                      get this info
	 * @param logger 		where all the warnings goes
	 */
	public void addAsDependent (
			PmlANode parent, PmlANode child, String phraseType, String phraseTag,
			Tuple<UDv2Relations, String> childDeprel, Logger logger)
	{
		if (child == null || child.isSameNode(parent)) return;

		if (childDeprel == null) childDeprel =
				PhrasePartDepLogic.phrasePartRoleToUD(child, phraseType, phraseTag, logger);
		setLink(parent, child, childDeprel.first, childDeprel, true,true);
	}

	/**
	 * For the given node find first children of the given type and make all
	 * other children depend on it. Set UD deprel and enhanced backbone for each
	 * child.
	 * @param phraseNode		node whose children must be processed
	 * @param phraseType    	phrase type from PML data, used for obtaining
	 *                      	correct UD role for children
	 * @param phraseTag	    	phrase tag from PML data, used for obtaining
	 *                      	correct UD role for children
	 * @param newRootType		rubroot for new UD structure will be searched
	 *                          between PML nodes with this type/role
	 * @param childDeprel		dependency role + enhanced dependencies postfix
	 *                          to be used for DEPREL field and enhanced
	 *                          backbone for child nodes, or null, if
	 *                      	DepRelLogic.phrasePartRoleToUD() should be used
	 *                      	to get this info.
	 * @param warnMoreThanOne	whether to warn if more than one potential root
	 *                          is found
	 * @param logger 			where all the warnings goes
	 * @return root of the corresponding dependency structure
	 */
	public PmlANode allUnderFirst(
			PmlANode phraseNode, String phraseType, String phraseTag, String newRootType,
			Tuple<UDv2Relations, String> childDeprel, boolean warnMoreThanOne,
			Logger logger)
	{

		//NodeList children = (NodeList)XPathEngine.get().evaluate(
		//		"./children/*", phraseNode, XPathConstants.NODESET);
		List<PmlANode> children = phraseNode.getChildren();
		List<PmlANode> potentialRoots = phraseNode.getChildren(newRootType);
		if (warnMoreThanOne && potentialRoots != null && potentialRoots.size() > 1)
			logger.doInsentenceWarning(String.format(
					"\"%s\" in sentence \"%s\" has more than one \"%s\".",
					phraseType, id, newRootType));
			//warnOut.printf("\"%s\" in sentence \"%s\" has more than one \"%s\".\n", phraseType, id, newRootType);
		PmlANode newRoot = PmlANodeListUtils.getFirstByDeepOrd(potentialRoots);
		if (newRoot == null)
		{
			logger.doInsentenceWarning(String.format(
					"\"%s\" in sentence \"%s\" has no \"%s\".",
					phraseType, id, newRootType));
			//warnOut.printf("\"%s\" in sentence \"%s\" has no \"%s\".\n", phraseType, id, newRootType);
			newRoot = PmlANodeListUtils.getFirstByDeepOrd(children);
		}
		if (newRoot == null)
			throw new IllegalArgumentException(String.format(
					"\"%s\" in sentence \"%s\" seems to be empty.\n",
					phraseType, id));
		allAsDependents(newRoot, children, phraseType, phraseTag, childDeprel, logger);
		return newRoot;
	}

	/**
	 * For the given node find first children of the given type and make all
	 * other children depend on it. Set UD deprel and enhanced backbone for each
	 * child.
	 * @param phraseNode		node whose children must be processed
	 * @param phraseType    	phrase type from PML data, used for obtaining
	 *                      	correct UD role for children
	 * @param phraseTag	    	phrase tag from PML data, used for obtaining
	 *                      	correct UD role for children
	 * @param newRootType		subroot for new UD structure will be searched
	 *                          between PML nodes with this type/role
	 * @param newRootBackUpType backUpRole, if no nodes of newRootType is found
	 * @param childDeprel		dependency role + enhanced dependencies postfix
	 *                          to be used for DEPREL field and enhanced
	 *                          backbone for child nodes, or null, if
	 *                      	DepRelLogic.phrasePartRoleToUD() should be used
	 *                      	to get this info.
	 * @param warnMoreThanOne	whether to warn if more than one potential root
	 *                          is found
	 * @param logger 			where all the warnings goes
	 * @return root of the corresponding dependency structure
	 */
	public PmlANode allUnderLast(
			PmlANode phraseNode, String phraseType, String phraseTag, String newRootType,
			String newRootBackUpType, Tuple<UDv2Relations, String> childDeprel,
			boolean warnMoreThanOne, Logger logger)
	{
		List<PmlANode> children = phraseNode.getChildren();
		List<PmlANode> potentialRoots = phraseNode.getChildren(newRootType);
		if (newRootBackUpType != null &&
				(potentialRoots == null || potentialRoots.size() < 1))
			potentialRoots = phraseNode.getChildren(newRootBackUpType);
		PmlANode newRoot = PmlANodeListUtils.getLastByDeepOrd(potentialRoots);
		if (warnMoreThanOne && potentialRoots != null && potentialRoots.size() > 1)
			logger.doInsentenceWarning(String.format(
					"\"%s\" in sentence \"%s\" has more than one \"%s\".",
					phraseType, id, newRoot.getAnyLabel()));
		if (newRoot == null)
		{
			logger.doInsentenceWarning(String.format(
					"\"%s\" in sentence \"%s\" has no \"%s\".",
					phraseType, id, newRootType));
			newRoot = PmlANodeListUtils.getLastByDeepOrd(children);
		}
		if (newRoot == null)
			throw new IllegalArgumentException(String.format(
					"\"%s\" in sentence \"%s\" seems to be empty.\n",
					phraseType, id));
		allAsDependents(newRoot, children, phraseType, phraseTag, childDeprel, logger);
		return newRoot;
	}

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
	 */
	public void setLink (PmlANode parent, PmlANode child, UDv2Relations baseDep,
						 Tuple<UDv2Relations, String> enhancedDep,
						 boolean setBackbone, boolean cleanOldDeps)
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
		if (!childEnhToken.equals(rootEnhToken))
		{
			if (cleanOldDeps) childEnhToken.deps.clear();
			EnhencedDep newDep = new EnhencedDep(rootEnhToken, enhancedDep.first, enhancedDep.second);
			childEnhToken.deps.add(newDep);
			if (setBackbone) childEnhToken.depsBackbone = newDep;
		}
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
	 */
	public void setEnhLink (PmlANode parent, PmlANode child,
							Tuple<UDv2Relations, String> enhancedDep,
						    boolean setBackbone, boolean cleanOldDeps)
	{
		Token rootBaseToken = pmlaToConll.get(parent.getId());
		Token rootEnhToken = pmlaToEnhConll.get(parent.getId());
		if (rootEnhToken == null) rootEnhToken = rootBaseToken;
		Token childBaseToken = pmlaToConll.get(child.getId());
		Token childEnhToken = pmlaToEnhConll.get(child.getId());
		if (childEnhToken == null) childEnhToken = childBaseToken;

		// Set enhanced dependencies, but avoid circular.
		if (!childEnhToken.equals(rootEnhToken))
		{
			if (cleanOldDeps) childEnhToken.deps.clear();
			EnhencedDep newDep = new EnhencedDep(rootEnhToken, enhancedDep.first, enhancedDep.second);
			childEnhToken.deps.add(newDep);
			if (setBackbone) childEnhToken.depsBackbone = newDep;
		}
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
		if (cleanOldDeps) childEnhToken.deps.clear();
		EnhencedDep newDep = EnhencedDep.root();
		childEnhToken.deps.add(newDep);
		childEnhToken.depsBackbone = newDep;
	}
	/**
	 * Changes the heads for all dependencies set (both base and enhanced) for
	 * given childnode. It is expected that pmlaToEnhConll (if needed) and
	 * pmlaToConll contains links from given PML nodes's IDs to corresponding
	 * tokens.
	 * @param newParent	new parent
	 * @param child		child node whose attachment should be changed
	 */
	public void changeHead (PmlANode newParent, PmlANode child)
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
	}

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

}
