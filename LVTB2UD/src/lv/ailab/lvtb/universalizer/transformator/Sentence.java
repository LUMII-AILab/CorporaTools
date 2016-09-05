package lv.ailab.lvtb.universalizer.transformator;

import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.URelations;
import lv.ailab.lvtb.universalizer.pml.Utils;
import lv.ailab.lvtb.universalizer.transformator.syntax.PhrasePartDepLogic;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
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
	public static boolean CHANGE_IDS = true;
	public String id;
	public Node pmlTree;
	public ArrayList<Token> conll = new ArrayList<>();
	//public HashMap<Token, Node> conllToPmla = new HashMap<>();
	/**
	 * Mapping from A-level ids to CoNLL tokens.
	 * Here goes phrase representing empty nodes, if it has been resolved, which
	 * child will be the parent of the dependency subtree.
	 */
	public HashMap<String, Token> pmlaToConll = new HashMap<>();

	public Sentence(Node pmlTree) throws XPathExpressionException
	{
		this.pmlTree = pmlTree;
		id = XPathEngine.get().evaluate("./@id", this.pmlTree);
	}

	public String toConllU()
	{
		StringBuilder res = new StringBuilder();
		res.append("# ");
		if (CHANGE_IDS) res.append(id.replace("LETA", "newswire"));
		else res.append(id);
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
	 * @throws XPathExpressionException
	 */
	public void allAsDependents(
			Node newRoot, NodeList children, String phraseType,
			URelations childDeprel)
	throws XPathExpressionException
	{
		allAsDependents(newRoot, Utils.asList(children), phraseType, childDeprel);
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
	 * @throws XPathExpressionException
	 */
	public void allAsDependents(
			Node newRoot, List<Node> children, String phraseType,
			URelations childDeprel)
	throws XPathExpressionException
	{
		if (children == null || children.isEmpty()) return;

		// Process root.
		Token rootToken = pmlaToConll.get(Utils.getId(newRoot));

		// Process children.
		for (Node child : children)
		{
			if (child.equals(newRoot) || child.isSameNode(newRoot)) continue;
			Token childToken = pmlaToConll.get(Utils.getId(child));

			childToken.head = rootToken.idBegin;
			if (childDeprel == null)
				childToken.deprel = PhrasePartDepLogic.phrasePartRoleToUD(child, phraseType);
			else childToken.deprel = childDeprel;
		}
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
	 * @return root of the corresponding dependency structure
	 * @throws XPathExpressionException
	 */
	public Node allUnderFirst(
			Node phraseNode, String phraseType, String newRootType,
			URelations childDeprel, boolean warnMoreThanOne)
	throws XPathExpressionException
	{
		NodeList children = (NodeList)XPathEngine.get().evaluate(
				"./children/*", phraseNode, XPathConstants.NODESET);
		NodeList potentialRoots = (NodeList)XPathEngine.get().evaluate(
				"./children/node[role='" + newRootType +"']", phraseNode, XPathConstants.NODESET);
		if (warnMoreThanOne && potentialRoots != null && potentialRoots.getLength() > 1)
			System.err.printf("\"%s\" in sentence \"%s\" has more than one \"%s\".\n",
					phraseType, id, newRootType);
		Node newRoot = Utils.getFirstByOrd(potentialRoots);
		if (newRoot == null)
		{
			System.err.printf("\"%s\" in sentence \"%s\" has no \"%s\".\n",
					phraseType, id, newRootType);
			newRoot = Utils.getFirstByOrd(children);
		}
		if (newRoot == null)
			throw new IllegalArgumentException(
					"\"" + phraseType +"\" in sentence \"" + id + "\" seems to be empty.\n");
		allAsDependents(newRoot, children, phraseType, childDeprel);
		return newRoot;
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
	 * @return root of the corresponding dependency structure
	 * @throws XPathExpressionException
	 */
	public Node allUnderLast(
			Node phraseNode, String phraseType, String newRootType,
			URelations childDeprel, boolean warnMoreThanOne)
	throws XPathExpressionException
	{
		NodeList children = (NodeList)XPathEngine.get().evaluate(
				"./children/*", phraseNode, XPathConstants.NODESET);
		NodeList potentialRoots = (NodeList)XPathEngine.get().evaluate(
				"./children/node[role='" + newRootType +"']", phraseNode, XPathConstants.NODESET);
		Node newRoot = Utils.getLastByOrd(potentialRoots);
		if (warnMoreThanOne && potentialRoots != null && potentialRoots.getLength() > 1)
			System.err.printf("\"%s\" in sentence \"%s\" has more than one \"%s\".\n",
					phraseType, id, newRoot);
		if (newRoot == null)
		{
			System.err.printf("\"%s\" in sentence \"%s\" has no \"%s\".\n",
					phraseType, id, newRoot);
			newRoot = Utils.getLastByOrd(children);
		}
		if (newRoot == null)
			throw new IllegalArgumentException(
					"\"" + phraseType +"\" in sentence \"" + id + "\" seems to be empty.\n");
		allAsDependents(newRoot, children, phraseType, childDeprel);
		return newRoot;
	}
}
