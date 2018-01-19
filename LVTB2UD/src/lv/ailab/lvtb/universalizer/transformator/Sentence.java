package lv.ailab.lvtb.universalizer.transformator;

import lv.ailab.lvtb.universalizer.conllu.EnhencedDep;
import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.LvtbRoles;
import lv.ailab.lvtb.universalizer.pml.utils.NodeFieldUtils;
import lv.ailab.lvtb.universalizer.pml.utils.NodeListUtils;
import lv.ailab.lvtb.universalizer.pml.utils.NodeUtils;
import lv.ailab.lvtb.universalizer.transformator.syntax.PhrasePartDepLogic;
import lv.ailab.lvtb.universalizer.utils.Tuple;
import lv.ailab.lvtb.universalizer.utils.XPathEngine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
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
	public Node pmlTree;
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
	 * Indication that transformation has failed and the obtained conll data is
	 * garbage.
	 */
	public boolean hasFailed;

	public Sentence(Node pmlTree) throws XPathExpressionException
	{
		this.pmlTree = pmlTree;
		id = XPathEngine.get().evaluate("./@id", this.pmlTree);
		hasFailed = false;
	}

	public String toConllU()
	{
		StringBuilder res = new StringBuilder();
		res.append("# sent_id = ");
		//if (params.CHANGE_IDS) res.append(id.replace("LETA", "newswire"));
		//else
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

	public void populateCoordPartsUnder()
	throws XPathExpressionException
	{
		coordPartsUnder = new HashMap<>();
		populateCoordPartsUnder(pmlTree);
	}

	protected void populateCoordPartsUnder(Node aNode)
	throws XPathExpressionException
	{
		if (aNode == null) return;
		NodeList dependants = NodeUtils.getPMLNodeChildren(aNode);
		if (dependants != null) for (int i = 0; i < dependants.getLength(); i++)
			populateCoordPartsUnder(dependants.item(i));
		Node phrase = NodeUtils.getPhraseNode(aNode);
		if (phrase == null) return;
		NodeList phraseParts = NodeUtils.getPMLNodeChildren(phrase);
		if (phraseParts != null) for (int i = 0; i < phraseParts.getLength(); i++)
			populateCoordPartsUnder(phraseParts.item(i));

		String id = NodeFieldUtils.getId(aNode);
		if (phrase.getNodeName().equals("coordinfo"))
		{
			HashSet<String> eqs = coordPartsUnder.get(id);
			if (eqs == null) eqs = new HashSet<>();
			if (phraseParts != null) for (int i = 0; i < phraseParts.getLength(); i++)
			{
				String partId = NodeFieldUtils.getId(phraseParts.item(i));
				String role = NodeFieldUtils.getRole(phraseParts.item(i));
				if (LvtbRoles.CRDPART.equals(role))
				{
					if (coordPartsUnder.containsKey(partId))
						eqs.addAll(coordPartsUnder.get(partId));
					else eqs.add(partId);
				}
			}
			coordPartsUnder.put(id, eqs);
		}
		/*else if (phrase.getNodeName().equals("xinfo")
			|| phrase.getNodeName().equals("pmcinfo"))
		{}//*/
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
	 * @throws XPathExpressionException	unsuccessfull XPath evaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public void allAsDependents(
			Node newRoot, NodeList children, String phraseType, String phraseTag,
			Tuple<UDv2Relations, String> childDeprel, Logger logger)
	throws XPathExpressionException
	{
		allAsDependents(newRoot, NodeListUtils.asList(children), phraseType, phraseTag, childDeprel, logger);
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
	 * @throws XPathExpressionException	unsuccessfull XPath evaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public void allAsDependents(
			Node newRoot, List<Node> children, String phraseType, String phraseTag,
			Tuple<UDv2Relations, String> childDeprel, Logger logger)
	throws XPathExpressionException
	{
		if (children == null || children.isEmpty()) return;

		// Process children.
		for (Node child : children)
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
	 * @throws XPathExpressionException	unsuccessfull XPath evaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public void addAsDependent (
			Node parent, Node child, String phraseType, String phraseTag,
			Tuple<UDv2Relations, String> childDeprel, Logger logger)
	throws XPathExpressionException
	{
		if (child == null ) return;
		if (child.equals(parent) || child.isSameNode(parent)) return;

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
	 * @throws XPathExpressionException	unsuccessfull XPath evaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public Node allUnderFirst(
			Node phraseNode, String phraseType, String phraseTag, String newRootType,
			Tuple<UDv2Relations, String> childDeprel, boolean warnMoreThanOne,
			Logger logger)
	throws XPathExpressionException
	{
		NodeList children = (NodeList)XPathEngine.get().evaluate(
				"./children/*", phraseNode, XPathConstants.NODESET);
		NodeList potentialRoots = (NodeList)XPathEngine.get().evaluate(
				"./children/node[role='" + newRootType +"']", phraseNode, XPathConstants.NODESET);
		if (warnMoreThanOne && potentialRoots != null && potentialRoots.getLength() > 1)
			logger.doInsentenceWarning(String.format(
					"\"%s\" in sentence \"%s\" has more than one \"%s\".",
					phraseType, id, newRootType));
			//warnOut.printf("\"%s\" in sentence \"%s\" has more than one \"%s\".\n", phraseType, id, newRootType);
		Node newRoot = NodeListUtils.getFirstByDescOrd(potentialRoots);
		if (newRoot == null)
		{
			logger.doInsentenceWarning(String.format(
					"\"%s\" in sentence \"%s\" has no \"%s\".",
					phraseType, id, newRootType));
			//warnOut.printf("\"%s\" in sentence \"%s\" has no \"%s\".\n", phraseType, id, newRootType);
			newRoot = NodeListUtils.getFirstByDescOrd(children);
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
	 * @throws XPathExpressionException	unsuccessfull XPath evaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public Node allUnderLast(
			Node phraseNode, String phraseType, String phraseTag, String newRootType,
			String newRootBackUpType, Tuple<UDv2Relations, String> childDeprel,
			boolean warnMoreThanOne, Logger logger)
	throws XPathExpressionException
	{
		NodeList children = (NodeList)XPathEngine.get().evaluate(
				"./children/*", phraseNode, XPathConstants.NODESET);
		NodeList potentialRoots = (NodeList)XPathEngine.get().evaluate(
				"./children/node[role='" + newRootType +"']", phraseNode, XPathConstants.NODESET);
		if (newRootBackUpType != null &&
				(potentialRoots == null || potentialRoots.getLength() < 1))
			potentialRoots = (NodeList)XPathEngine.get().evaluate(
					"./children/node[role='" + newRootBackUpType +"']", phraseNode, XPathConstants.NODESET);
		Node newRoot = NodeListUtils.getLastByDescOrd(potentialRoots);
		if (warnMoreThanOne && potentialRoots != null && potentialRoots.getLength() > 1)
			logger.doInsentenceWarning(String.format(
					"\"%s\" in sentence \"%s\" has more than one \"%s\".",
					phraseType, id, NodeFieldUtils.getAnyLabel(newRoot)));
			//warnOut.printf("\"%s\" in sentence \"%s\" has more than one \"%s\".\n", phraseType, id, NodeFieldUtils.getAnyLabel(newRoot));
		if (newRoot == null)
		{
			//warnOut.printf("\"%s\" in sentence \"%s\" has no \"%s\".\n", phraseType, id, newRootType);
			logger.doInsentenceWarning(String.format(
					"\"%s\" in sentence \"%s\" has no \"%s\".",
					phraseType, id, newRootType));
			newRoot = NodeListUtils.getLastByDescOrd(children);
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
	 * @param parent 		PML node describing parent
	 * @param child			PML node describing child
	 * @param baseDep		label to be used for base dependency
	 * @param enhancedDep	label to be used for enhanced dependency
	 * @param setBackbone	if enhanced dependency is made, should it be set as
	 *                      backbone for child node
	 * @param cleanOldDeps	whether previous contents from deps field should be
	 *                      removed
	 * @throws XPathExpressionException unsuccessfull XPath evaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public void setLink (Node parent, Node child, UDv2Relations baseDep, Tuple<UDv2Relations, String> enhancedDep,
						 boolean setBackbone, boolean cleanOldDeps)
			throws XPathExpressionException
	{
		Token rootBaseToken = pmlaToConll.get(NodeFieldUtils.getId(parent));
		Token rootEnhToken = pmlaToEnhConll.get(NodeFieldUtils.getId(parent));
		if (rootEnhToken == null) rootEnhToken = rootBaseToken;
		Token childBaseToken = pmlaToConll.get(NodeFieldUtils.getId(child));
		Token childEnhToken = pmlaToEnhConll.get(NodeFieldUtils.getId(child));
		if (childEnhToken == null) childEnhToken = childBaseToken;

		// Set base dependency, but avoid circular dependencies.
		if (!rootBaseToken.equals(childBaseToken))
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
	 * @throws XPathExpressionException unsuccessfull XPath evaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public void setEnhLink (Node parent, Node child, Tuple<UDv2Relations, String> enhancedDep,
						    boolean setBackbone, boolean cleanOldDeps)
			throws XPathExpressionException
	{
		Token rootBaseToken = pmlaToConll.get(NodeFieldUtils.getId(parent));
		Token rootEnhToken = pmlaToEnhConll.get(NodeFieldUtils.getId(parent));
		if (rootEnhToken == null) rootEnhToken = rootBaseToken;
		Token childBaseToken = pmlaToConll.get(NodeFieldUtils.getId(child));
		Token childEnhToken = pmlaToEnhConll.get(NodeFieldUtils.getId(child));
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
	 * nodes, but do not set circular dependencies. It is expected that
	 * pmlaToConll contains links from given PML nodes's IDs to corresponding
	 * tokens.
	 * @param parent 		PML node describing parent
	 * @param child			PML node describing child
	 * @param baseDep	label to be used for enhanced dependency
	 * @throws XPathExpressionException unsuccessfull XPath evaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public void setBaseLink (Node parent, Node child, UDv2Relations baseDep)
			throws XPathExpressionException
	{
		Token rootBaseToken = pmlaToConll.get(NodeFieldUtils.getId(parent));
		Token childBaseToken = pmlaToConll.get(NodeFieldUtils.getId(child));

		// Set base dependency, but avoid circular dependencies.
		if (!rootBaseToken.equals(childBaseToken))
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
	 * @throws XPathExpressionException unsuccessfull XPath evaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public void setRoot (Node node, boolean cleanOldDeps)
			throws XPathExpressionException
	{
		Token childBaseToken = pmlaToConll.get(NodeFieldUtils.getId(node));
		Token childEnhToken = pmlaToEnhConll.get(NodeFieldUtils.getId(node));
		if (childEnhToken == null) childEnhToken = childBaseToken;

		// Set base dependency.
		childBaseToken.head = Tuple.of("0", null);;
		childBaseToken.deprel = UDv2Relations.ROOT;;

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
	 * @throws XPathExpressionException	unsuccessfull XPath evaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public void changeHead (Node newParent, Node child)
			throws XPathExpressionException
	{
		Token rootBaseToken = pmlaToConll.get(NodeFieldUtils.getId(newParent));
		Token rootEnhToken = pmlaToEnhConll.get(NodeFieldUtils.getId(newParent));
		if (rootEnhToken == null) rootEnhToken = rootBaseToken;
		Token childBaseToken = pmlaToConll.get(NodeFieldUtils.getId(child));
		Token childEnhToken = pmlaToEnhConll.get(NodeFieldUtils.getId(child));
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
	 * @throws XPathExpressionException unsuccessfull XPath evaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public Token getEnhancedOrBaseToken(Node aNode)
			throws XPathExpressionException
	{
		if (aNode == null) return null;
		String id = NodeFieldUtils.getId(aNode);
		if (id == null) return null;
		Token resToken = pmlaToEnhConll.get(id);
		if (resToken == null) resToken = pmlaToConll.get(id);
		return resToken;
	}

	/**
	 * Find PML node by given ID.
	 * @param id	an ID to search
	 * @return	first node found
	 * @throws XPathExpressionException unsuccessfull XPath evaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public Node findPmlNode(String id) throws XPathExpressionException
	{
		NodeList res = (NodeList) XPathEngine.get().evaluate(
				".//node[@id='"+ id + "']", pmlTree, XPathConstants.NODESET);
		if (res == null || res.getLength() < 1) return null;
		return res.item(0);
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
	public HashSet<String> getCoordPartsUnderOrNode (Node aNode)
	throws XPathExpressionException
	{
		return getCoordPartsUnderOrNode(NodeFieldUtils.getId(aNode));
	}
}
