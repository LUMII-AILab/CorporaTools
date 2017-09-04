package lv.ailab.lvtb.universalizer.transformator;

import lv.ailab.lvtb.universalizer.LvtbToUdUI;
import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.Utils;
import lv.ailab.lvtb.universalizer.transformator.syntax.PhrasePartDepLogic;
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
	 * Make a given node a child of the designated parent. Set UD for the child.
	 * If designated parent is the same as child node, circular dependency is
	 * not made, role is not set.
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

		// Process root.
		Token rootToken = pmlaToConll.get(Utils.getId(parent));

		// Process child.
		if (child.equals(parent) || child.isSameNode(parent)) return;

		Token childToken = pmlaToConll.get(Utils.getId(child));
		childToken.head = rootToken.getFirstColumn();
		if (childDeprel == null)
			childToken.deprel = PhrasePartDepLogic.phrasePartRoleToUD(child, phraseType, warnOut);
		else childToken.deprel = childDeprel;
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
}
