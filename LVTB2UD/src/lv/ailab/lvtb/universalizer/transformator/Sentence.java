package lv.ailab.lvtb.universalizer.transformator;

import lv.ailab.lvtb.universalizer.LvtbToUdUI;
import lv.ailab.lvtb.universalizer.conllu.EnhencedDep;
import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.Utils;
import lv.ailab.lvtb.universalizer.transformator.syntax.PhrasePartDepLogic;
import lv.ailab.lvtb.universalizer.util.Tuple;
import lv.ailab.lvtb.universalizer.util.XPathEngine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
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

	public Sentence(Node pmlTree) throws XPathExpressionException
	{
		this.pmlTree = pmlTree;
		id = XPathEngine.get().evaluate("./@id", this.pmlTree);
	}

	public String toConllU()
	{
		StringBuilder res = new StringBuilder();
		res.append("# sent_id = ");
		if (LvtbToUdUI.CHANGE_IDS) res.append(id.replace("LETA", "newswire"));
		else res.append(id);
		res.append("\n");
		res.append("# text = ");
		res.append(text);
		res.append("\n");
		for (Token t : conll)
			res.append(t.toConllU());
		res.append("\n");
		return res.toString();
	}

	/**
	 * Make a list of given nodes children of the designated parent. Set UD
	 * deprel for each child. If designated parent is included in child list
	 * node, circular dependency is not made, role is not set.
	 * @param newRoot		designated parent
	 * @param children		list of child nodes
	 * @param phraseType    phrase type from PML data, used for obtaining
	 *                      correct UD role for children.
	 * @param childDeprel	value to sent for DEPREL field for child nodes, or
	 *                      null, if DepRelLogic.phrasePartRoleToUD() should
	 *                      be used to obtain DEPREL for child nodes.
	 * @param warnOut 		where all the warnings goes
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public void allAsDependents(
			Node newRoot, NodeList children, String phraseType,
			UDv2Relations childDeprel, PrintWriter warnOut)
	throws XPathExpressionException
	{
		allAsDependents(newRoot, Utils.asList(children), phraseType, childDeprel, warnOut);
	}

	/**
	 * Make a list of given nodes children of the designated parent. Set UD
	 * deprel for each child. If designated parent is included in child list
	 * node, circular dependency is not made, role is not set.
	 * @param newRoot		designated parent
	 * @param children		list of child nodes
	 * @param phraseType    phrase type from PML data, used for obtaining
	 *                      correct UD role for children.
	 * @param childDeprel	value to sent for DEPREL field for child nodes, or
	 *                      null, if DepRelLogic.phrasePartRoleToUD() should
	 *                      be used to obtain DEPREL for child nodes.
	 * @param warnOut 		where all the warnings goes
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public void allAsDependents(
			Node newRoot, List<Node> children, String phraseType,
			UDv2Relations childDeprel, PrintWriter warnOut)
	throws XPathExpressionException
	{
		if (children == null || children.isEmpty()) return;

		// Process children.
		for (Node child : children)
		{
			addAsDependent(newRoot, child, phraseType, childDeprel, warnOut);
		}
	}
	/**
	 * Make a given node a child of the designated parent. Set UD role for the
	 * child. If designated parent is the same as child node, circular
	 * dependency is not made, role is not set.
	 * @param parent		designated parent
	 * @param child			designated child
	 * @param phraseType    phrase type from PML data, used for obtaining
	 *                      correct UD role for children.
	 * @param childDeprel	value to sent for DEPREL field for child nodes, or
	 *                      null, if DepRelLogic.phrasePartRoleToUD() should
	 *                      be used to obtain DEPREL for child nodes.
	 * @param warnOut 		where all the warnings goes
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public void addAsDependent (
			Node parent, Node child, String phraseType, UDv2Relations childDeprel,
			PrintWriter warnOut)
	throws XPathExpressionException
	{
		if (child == null ) return;
		if (child.equals(parent) || child.isSameNode(parent)) return;

		if (childDeprel == null) childDeprel =
				PhrasePartDepLogic.phrasePartRoleToUD(child, phraseType, warnOut);
		setLink(parent, child, childDeprel, childDeprel, true);
		// Process root.
		/*Token rootBaseToken = pmlaToConll.get(Utils.getId(parent));

		// Process child.
		Token childBaseToken = pmlaToConll.get(Utils.getId(child));
		childBaseToken.head = Tuple.of(rootBaseToken.getFirstColumn(), rootBaseToken);
		if (childDeprel == null)
			childBaseToken.deprel = PhrasePartDepLogic.phrasePartRoleToUD(child, phraseType, warnOut);
		else childBaseToken.deprel = childDeprel;*/

	}

	/**
	 * For the given node find first children of the given type and make all
	 * other children depend on it. Set UD deprel for each child.
	 * @param phraseNode		node whose children must be processed
	 * @param phraseType    	phrase type from PML data, used for obtaining
	 *                      	correct UD role for children
	 * @param newRootType		rubroot for new UD structure will be searched
	 *                          between PML nodes with this type/role
	 * @param childDeprel		value to sent for DEPREL field for child nodes,
	 *                          or null, if DepRelLogic.phrasePartRoleToUD()
	 *                          should be used to obtain DEPREL for child nodes
	 * @param warnMoreThanOne	whether to warn if more than one potential root
	 *                          is found
	 * @param warnOut 		where all the warnings goes
	 * @return root of the corresponding dependency structure
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public Node allUnderFirst(
			Node phraseNode, String phraseType, String newRootType,
			UDv2Relations childDeprel, boolean warnMoreThanOne, PrintWriter warnOut)
	throws XPathExpressionException
	{
		NodeList children = (NodeList)XPathEngine.get().evaluate(
				"./children/*", phraseNode, XPathConstants.NODESET);
		NodeList potentialRoots = (NodeList)XPathEngine.get().evaluate(
				"./children/node[role='" + newRootType +"']", phraseNode, XPathConstants.NODESET);
		if (warnMoreThanOne && potentialRoots != null && potentialRoots.getLength() > 1)
			warnOut.printf("\"%s\" in sentence \"%s\" has more than one \"%s\".\n",
					phraseType, id, newRootType);
		Node newRoot = Utils.getFirstByDescOrd(potentialRoots);
		if (newRoot == null)
		{
			warnOut.printf("\"%s\" in sentence \"%s\" has no \"%s\".\n",
					phraseType, id, newRootType);
			newRoot = Utils.getFirstByDescOrd(children);
		}
		if (newRoot == null)
			throw new IllegalArgumentException(
					"\"" + phraseType +"\" in sentence \"" + id + "\" seems to be empty.\n");
		allAsDependents(newRoot, children, phraseType, childDeprel, warnOut);
		return newRoot;
	}

	/**
	 * For the given node find first children of the given type and make all
	 * other children depend on it. Set UD deprel for each child.
	 * @param phraseNode		node whose children must be processed
	 * @param phraseType    	phrase type from PML data, used for obtaining
	 *                      	correct UD role for children
	 * @param newRootType		subroot for new UD structure will be searched
	 *                          between PML nodes with this type/role
	 * @param newRootBackUpType backUpRole, if no nodes of newRootType is found
	 * @param childDeprel		value to sent for DEPREL field for child nodes,
	 *                          or null, if DepRelLogic.phrasePartRoleToUD()
	 *                          should be used to obtain DEPREL for child nodes
	 * @param warnMoreThanOne	whether to warn if more than one potential root
	 *                          is found
	 * @param warnOut 			where all the warnings goes
	 * @return root of the corresponding dependency structure
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public Node allUnderLast(
			Node phraseNode, String phraseType, String newRootType, String newRootBackUpType,
			UDv2Relations childDeprel, boolean warnMoreThanOne, PrintWriter warnOut)
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
		Node newRoot = Utils.getLastByDescOrd(potentialRoots);
		if (warnMoreThanOne && potentialRoots != null && potentialRoots.getLength() > 1)
			warnOut.printf("\"%s\" in sentence \"%s\" has more than one \"%s\".\n",
					phraseType, id, Utils.getAnyLabel(newRoot));
		if (newRoot == null)
		{
			warnOut.printf("\"%s\" in sentence \"%s\" has no \"%s\".\n",
					phraseType, id, newRootType);
			newRoot = Utils.getLastByDescOrd(children);
		}
		if (newRoot == null)
			throw new IllegalArgumentException(
					"\"" + phraseType +"\" in sentence \"" + id + "\" seems to be empty.\n");
		allAsDependents(newRoot, children, phraseType, childDeprel, warnOut);
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
	 * @param cleanOldDeps	whether previous contents from deps field should be
	 *                      removed
	 * @throws XPathExpressionException unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public void setLink (Node parent, Node child, UDv2Relations baseDep, UDv2Relations enhancedDep,
						 boolean cleanOldDeps)
			throws XPathExpressionException
	{
		Token rootBaseToken = pmlaToConll.get(Utils.getId(parent));
		Token rootEnhToken = pmlaToEnhConll.get(Utils.getId(parent));
		if (rootEnhToken == null) rootEnhToken = rootBaseToken;
		Token childBaseToken = pmlaToConll.get(Utils.getId(child));
		Token childEnhToken = pmlaToEnhConll.get(Utils.getId(child));
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
			childEnhToken.deps.add(new EnhencedDep(rootEnhToken, enhancedDep));
		}
	}

	/**
	 * Set both base and enhanced dependency links as root for token(s)
	 * coressponding to the given PML node. It is expecte that pmlaToEnhConll
	 * (if needed) and pmlaToConll contains links from given PML nodes's IDs to
	 * corresponding tokens.
	 * @param node 			PML node to be made root
	 * @param cleanOldDeps	whether previous contents from deps field should be
	 *                      removed
	 * @throws XPathExpressionException unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public void setRoot (Node node, boolean cleanOldDeps)
			throws XPathExpressionException
	{
		Token childBaseToken = pmlaToConll.get(Utils.getId(node));
		Token childEnhToken = pmlaToEnhConll.get(Utils.getId(node));
		if (childEnhToken == null) childEnhToken = childBaseToken;

		// Set base dependency.
		childBaseToken.head = Tuple.of("0", null);;
		childBaseToken.deprel = UDv2Relations.ROOT;;

		// Set enhanced dependencies.
		if (cleanOldDeps) childEnhToken.deps.clear();
		childEnhToken.deps.add(EnhencedDep.root());

	}
	/**
	 * Changes the heads for all dependencies set (both base and enhanced) for
	 * given childnode. It is expected that pmlaToEnhConll (if needed) and
	 * pmlaToConll contains links from given PML nodes's IDs to corresponding
	 * tokens.
	 * @param newParent	new parent
	 * @param child		child node whose attachment should be changed
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public void changeHead (Node newParent, Node child)
			throws XPathExpressionException
	{
		Token rootBaseToken = pmlaToConll.get(Utils.getId(newParent));
		Token rootEnhToken = pmlaToEnhConll.get(Utils.getId(newParent));
		if (rootEnhToken == null) rootEnhToken = rootBaseToken;
		Token childBaseToken = pmlaToConll.get(Utils.getId(child));
		Token childEnhToken = pmlaToEnhConll.get(Utils.getId(child));
		if (childEnhToken == null) childEnhToken = childBaseToken;

		// Set base dependency, but avoid circular dependencies.
		// FIXME is "childBaseToken.head != null" ok?
		if (!rootBaseToken.equals(childBaseToken) && childBaseToken.head != null)
			childBaseToken.head = Tuple.of(rootBaseToken.getFirstColumn(), rootBaseToken);

		// Set enhanced dependencies, but avoid circular.
		if (!childEnhToken.equals(rootEnhToken) && !childEnhToken.deps.isEmpty())
		{
			ArrayList<EnhencedDep> newDeps = new ArrayList<>();
			for (EnhencedDep ed : childEnhToken.deps)
				newDeps.add(new EnhencedDep(rootEnhToken, ed.role));
			childEnhToken.deps = newDeps;
		}
	}
}
