package lv.ailab.lvtb.universalizer.transformator;

import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.URelations;
import lv.ailab.lvtb.universalizer.pml.*;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;

/**
 * Logic for creating dependency structures from LVTB phrase-style structures.
 * No change is done in PML tree, all results are stored in CoNLL-U table only.
 * Created on 2016-04-20.
 *
 * @author Lauma
 */
public class PhraseTransformator
{
	/**
	 * Default phrase transformation: used when no phrase transformation rule
	 * is defined.
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException
	 */
	public static Node missingTransform(Node phraseNode, Sentence sent)
	throws XPathExpressionException
	{
		NodeList children = (NodeList)XPathEngine.get().evaluate(
				"./children/*", phraseNode, XPathConstants.NODESET);
		String phraseType = XPathEngine.get().evaluate("./pmctype", phraseNode);
		if (phraseType == null || phraseType.length() < 1)
			phraseType = XPathEngine.get().evaluate("./coordtype", phraseNode);
		if (phraseType == null || phraseType.length() < 1)
			phraseType = XPathEngine.get().evaluate("./xtype", phraseNode);

		Node newRoot = Utils.getFirstByOrd(children);
		allAsDependents(sent, newRoot, children, phraseType, null);
		return newRoot;
	}

	/**
	 * Transform PMC phrase UD structure.
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException
	 */
	public static Node pmcToUD(Node pmcNode, Sentence sent)
	throws XPathExpressionException
	{
		String pmcType = XPathEngine.get().evaluate("./pmctype", pmcNode);
		NodeList children = (NodeList)XPathEngine.get().evaluate(
				"./children/*", pmcNode, XPathConstants.NODESET);

		if (pmcType.equals(LvtbPmcTypes.SENT) ||
				pmcType.equals(LvtbPmcTypes.UTER))
		{
			// Find the structure root.
			NodeList preds = (NodeList)XPathEngine.get().evaluate(
					"./children/node[role='" + LvtbRoles.PRED +"']", pmcNode, XPathConstants.NODESET);
			Node newRoot = null;
			if (preds != null && preds.getLength() > 1)
				System.err.printf("Sentence \"%s\" has more than one \"%s\" in \"%s\".\n",
						sent.id, LvtbRoles.PRED, pmcType);
			if (preds != null && preds.getLength() > 0) newRoot = Utils.getFirstByOrd(preds);
			else
			{
				preds = (NodeList)XPathEngine.get().evaluate(
						"./children/node[role='" + LvtbRoles.BASELEM +"']", pmcNode, XPathConstants.NODESET);
				newRoot = Utils.getFirstByOrd(preds);
			}
			if (newRoot == null)
			{
				System.err.printf("Sentence \"%s\" has no \"%s\", \"%s\" in \"%s\".\n",
						sent.id, LvtbRoles.PRED, LvtbRoles.BASELEM, pmcType);
				newRoot = Utils.getFirstByOrd(children);
			}
			if (newRoot == null)
				throw new IllegalArgumentException("Sentence \"" + sent.id + "\" seems to be empty.\n");

			// Create dependency structure in conll table.
			allAsDependents(sent, newRoot, children, pmcType, null);

			return newRoot;
		}
		if (pmcType.equals(LvtbPmcTypes.SUBRCL) || pmcType.equals(LvtbPmcTypes.MAINCL)
				|| pmcType.equals(LvtbPmcTypes.INSPMC) || pmcType.equals(LvtbPmcTypes.SPCPMC)
				|| pmcType.equals(LvtbPmcTypes.PARTICLE) || pmcType.equals(LvtbPmcTypes.DIRSPPMC)
				|| pmcType.equals(LvtbPmcTypes.QUOT) || pmcType.equals(LvtbPmcTypes.ADRESS)
				|| pmcType.equals(LvtbPmcTypes.INTERJ))
		{
			// Find the structure root.
			NodeList preds = (NodeList)XPathEngine.get().evaluate(
					"./children/node[role='" + LvtbRoles.PRED +"']", pmcNode, XPathConstants.NODESET);
			Node newRoot = null;
			if (preds != null && preds.getLength() > 1)
				System.err.printf("\"%s\" in sentence \"%s\" has more than one \"%s\".\n",
						pmcType, sent.id, LvtbRoles.PRED);
			if (preds != null && preds.getLength() > 0) newRoot = Utils.getFirstByOrd(preds);
			if (newRoot == null)
				throw new IllegalArgumentException(
						"\"" + pmcType +"\" in sentence \"" + sent.id + "\" has no \"pred\".\n");

			// Create dependency structure in conll table.
			allAsDependents(sent, newRoot, children, pmcType, null);
			return newRoot;
		}
		System.err.printf("Sentence \"%s\" has unrecognized \"%s\".\n",
				sent.id, pmcType);
		return missingTransform(pmcNode, sent);
	}

	/**
	 * Transform coordination phrase UD structure.
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException
	 */
	public static Node coordToUD(Node coordNode, Sentence sent)
	throws XPathExpressionException
	{
		String coordType = XPathEngine.get().evaluate("./coordtype", coordNode);
		NodeList children = (NodeList)XPathEngine.get().evaluate(
				"./children/*", coordNode, XPathConstants.NODESET);

		if (coordType.equals(LvtbCoordTypes.CRDPARTS))
			return coordPartsChildListToUD(Utils.asOrderedList(children), sent, coordType);

		if (coordType.equals(LvtbCoordTypes.CRDCLAUSES))
		{
			NodeList semicolons = (NodeList)XPathEngine.get().evaluate(
					"./children/node[m.rf/lemma=';']", coordNode, XPathConstants.NODESET);
			if (semicolons == null || semicolons.getLength() < 1)
				return coordPartsChildListToUD(Utils.asOrderedList(children), sent, coordType);
			ArrayList<Node> sortedSemicolons = Utils.asOrderedList(semicolons);
			ArrayList<Node> sortedChildren = Utils.asOrderedList(children);
			int semicOrd = Utils.getOrd(sortedSemicolons.get(0));
			Node newRoot = coordPartsChildListToUD(
					Utils.ordSplice(sortedChildren, 0, semicOrd), sent, coordType);
			Token newRootToken = sent.pmlaToConll.get(Utils.getId(newRoot));
			for (int i  = 1; i < sortedSemicolons.size(); i++)
			{
				int nextSemicOrd = Utils.getOrd(sortedSemicolons.get(i));
				Node newSubroot = coordPartsChildListToUD(
						Utils.ordSplice(sortedChildren, semicOrd, nextSemicOrd), sent, coordType);
				Token subrootToken = sent.pmlaToConll.get(Utils.getId(newSubroot));
				subrootToken.deprel = URelations.PARATAXIS;
				subrootToken.head = newRootToken.idBegin;
			}

			return newRoot;
		}
		System.err.printf("Sentence \"%s\" has unrecognized \"%s\".\n",
				sent.id, coordType);
		return missingTransform(coordNode, sent);
	}

	/**
	 * Transform X-word phrase to UD structure.
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException
	 */
	public static Node xToUD(Node xNode, Sentence sent)
	throws XPathExpressionException
	{
		String xType = XPathEngine.get().evaluate("./xtype", xNode);
		NodeList children = (NodeList)XPathEngine.get().evaluate(
				"./children/*", xNode, XPathConstants.NODESET);

		// Multiple basElem, root is the last.
		if (xType.equals(LvtbXTypes.XAPP) || xType.equals(LvtbXTypes.XNUM))
			return allUnderLastBasElem(sent, xNode, xType, null, false);

		// Multiple basElem, root is the first.
		if (xType.equals(LvtbXTypes.PHRASELEM) || xType.equals(LvtbXTypes.NAMEDENT) ||
				xType.equals(LvtbXTypes.COORDANAL))
			return allUnderFirstBasElem(sent, xNode, xType, null, false);

		// Only one basElem
		if (xType.equals(LvtbXTypes.XPREP) || xType.equals(LvtbXTypes.XPARTICLE))
			return allUnderLastBasElem(sent, xNode, xType, null, true);

		// Specific.
		if (xType.equals(LvtbXTypes.UNSTRUCT))
		{
			NodeList foreigns = (NodeList)XPathEngine.get().evaluate(
					"./children/node[m.rf/tag='xf']", xNode, XPathConstants.NODESET);

			if (foreigns != null && children.getLength() == foreigns.getLength())
				return allUnderFirstBasElem(sent, xNode, xType, URelations.FOREIGN, false);
			else return allUnderFirstBasElem(sent, xNode, xType, null, false);
		}

		if (xType.equals(LvtbXTypes.XSIMILE))
			// If relinking (like for "vairāk nekā" constructions) will be
			// needed, it will be done when processing the parent node.
			return allUnderLastBasElem(sent, xNode, xType, null, true);

		if (xType.equals(LvtbXTypes.SUBRANAL))
		{
			// Tricky part, where subordinated xSimile structure also must be
			// rearanged.
			Node first = Utils.getFirstByOrd(children);
			Node last = Utils.getLastByOrd(children);
			if (children != null && children.getLength() == 2 &&
					"vairāk".equals(Utils.getLemma(first)) &&
					LvtbXTypes.XSIMILE.equals(XPathEngine.get().evaluate("./children/xinfo/xtype", last)))
			{
				NodeList simileConjs = (NodeList) XPathEngine.get().evaluate(
						"./children/node[role='" + LvtbRoles.CONJ + "']", last, XPathConstants.NODESET);
				Token newRootToken = sent.pmlaToConll.get(Utils.getId(last));
				Token vToken = sent.pmlaToConll.get(Utils.getId(first));
				vToken.head = newRootToken.idBegin;
				vToken.deprel = URelations.ADVMOD;
				if (simileConjs != null) for (int i = 0; i < simileConjs.getLength(); i++)
				{
					Token conjToken = sent.pmlaToConll.get(Utils.getId(simileConjs.item(i)));
					conjToken.deprel = URelations.MWE;
					conjToken.head = vToken.idBegin;
				}
				return last;
			}
			else return allUnderFirstBasElem(sent, xNode, xType, null, false);
		}

		System.err.printf("Sentence \"%s\" has unrecognized \"%s\".\n",
				sent.id, xType);
		return missingTransform(xNode, sent);
	}

	/**
	 * Helper function: make a list of given nodes children of the designated
	 * parent. Set UD deprel for each child. If designated parent is included in
	 * child list node, circular dependency is not made, role is not set.
	 * @param sent			sentence data
	 * @param newRoot		designated parent
	 * @param children		list of child nodes
	 * @param phraseType    phrase type from PML data, used for obtaining
	 *                      correct UD role for children.
	 * @param childDeprel	value to sent for DEPREL field for child nodes, or
	 *                      null, if DepRelLogic.getUDepFromPhrasePart() should
	 *                      be used to obtain DEPREL for child nodes.
	 * @throws XPathExpressionException
	 */
	protected static void allAsDependents(
			Sentence sent, Node newRoot, NodeList children, String phraseType,
			URelations childDeprel)
	throws XPathExpressionException
	{
		allAsDependents(sent, newRoot, Utils.asList(children), phraseType, childDeprel);
	}

	/**
	 * Helper function: make a list of given nodes children of the designated
	 * parent. Set UD deprel for each child. If designated parent is included in
	 * child list node, circular dependency is not made, role is not set.
	 * @param sent			sentence data
	 * @param newRoot		designated parent
	 * @param children		list of child nodes
	 * @param phraseType    phrase type from PML data, used for obtaining
	 *                      correct UD role for children.
	 * @param childDeprel	value to sent for DEPREL field for child nodes, or
	 *                      null, if DepRelLogic.getUDepFromPhrasePart() should
	 *                      be used to obtain DEPREL for child nodes.
	 * @throws XPathExpressionException
	 */
	protected static void allAsDependents(
			Sentence sent, Node newRoot, ArrayList<Node> children, String phraseType,
			URelations childDeprel)
	throws XPathExpressionException
	{
		// Process root.
		Token rootToken = sent.pmlaToConll.get(Utils.getId(newRoot));
		//sent.conllToPmla.put(rootToken, newRoot);
		//if (phraseNode != null) sent.pmlaToConll.put(phraseNode, rootToken);
		// Process children.
		for (Node child : children)
		{
			if (child.equals(newRoot)) continue;
			Token childToken = sent.pmlaToConll.get(Utils.getId(child));
			childToken.head = rootToken.idBegin;
			if (childDeprel == null)
				childToken.deprel = DepRelLogic.getUDepFromPhrasePart(child, phraseType);
			else childToken.deprel = childDeprel;
		}
	}

	/**
	 * Helper function: find first basElem children of the given node and make
	 * all other children depend on the found basElem. Set UD deprel for each
	 * child.
	 * @param sent				sentence data
	 * @param phraseNode		node whose children must be processed
	 * @param phraseType    	phrase type from PML data, used for obtaining
	 *                      	correct UD role for children
	 * @param childDeprel		value to sent for DEPREL field for child nodes,
	 *                          or null, if DepRelLogic.getUDepFromPhrasePart()
	 *                          should be used to obtain DEPREL for child nodes
	 * @param warnMoreThanOne	whether to warn if more than one basElem found
	 * @return root of the corresponding dependency structure
	 * @throws XPathExpressionException
	 */
	public static Node allUnderFirstBasElem(
			Sentence sent, Node phraseNode, String phraseType,
			URelations childDeprel, boolean warnMoreThanOne)
	throws XPathExpressionException
	{
		NodeList children = (NodeList)XPathEngine.get().evaluate(
				"./children/*", phraseNode, XPathConstants.NODESET);
		NodeList basElems = (NodeList)XPathEngine.get().evaluate(
				"./children/node[role='" + LvtbRoles.BASELEM +"']", phraseNode, XPathConstants.NODESET);
		if (warnMoreThanOne && basElems != null && basElems.getLength() > 1)
			System.err.printf("\"%s\" in sentence \"%s\" has more than one \"%s\".\n",
					phraseType, sent.id, LvtbRoles.BASELEM);
		Node newRoot = Utils.getFirstByOrd(basElems);
		if (newRoot == null)
		{
			System.err.printf("\"%s\" in sentence \"%s\" has no \"%s\".\n",
					phraseType, sent.id, LvtbRoles.BASELEM);
			newRoot = Utils.getFirstByOrd(children);
		}
		if (newRoot == null)
			throw new IllegalArgumentException(
					"\"" + phraseType +"\" in sentence \"" + sent.id + "\" seems to be empty.\n");
		allAsDependents(sent, newRoot, children, phraseType, childDeprel);
		return newRoot;
	}

	/**
	 * Helper function: find last basElem children of the given node and make
	 * all other children depend on the found basElem. Set UD deprel for each
	 * child.
	 * @param sent				sentence data
	 * @param phraseNode		node whose children must be processed
	 * @param phraseType    	phrase type from PML data, used for obtaining
	 *                      	correct UD role for children.
	 * @param childDeprel		value to sent for DEPREL field for child nodes,
	 *                          or null, if DepRelLogic.getUDepFromPhrasePart()
	 *                          should be used to obtain DEPREL for child nodes
	 * @param warnMoreThanOne	whether to warn if more than one basElem found
	 * @return root of the corresponding dependency structure
	 * @throws XPathExpressionException
	 */
	public static Node allUnderLastBasElem(
			Sentence sent, Node phraseNode, String phraseType,
			URelations childDeprel, boolean warnMoreThanOne)
	throws XPathExpressionException
	{
		NodeList children = (NodeList)XPathEngine.get().evaluate(
				"./children/*", phraseNode, XPathConstants.NODESET);
		NodeList basElems = (NodeList)XPathEngine.get().evaluate(
				"./children/node[role='" + LvtbRoles.BASELEM +"']", phraseNode, XPathConstants.NODESET);
		Node newRoot = Utils.getLastByOrd(basElems);
		if (warnMoreThanOne && basElems != null && basElems.getLength() > 1)
			System.err.printf("\"%s\" in sentence \"%s\" has more than one \"%s\".\n",
					phraseType, sent.id, LvtbRoles.BASELEM);
		if (newRoot == null)
		{
			System.err.printf("\"%s\" in sentence \"%s\" has no \"%s\".\n",
					phraseType, sent.id, LvtbRoles.BASELEM);
			newRoot = Utils.getLastByOrd(children);
		}
		if (newRoot == null)
			throw new IllegalArgumentException(
					"\"" + phraseType +"\" in sentence \"" + sent.id + "\" seems to be empty.\n");
		allAsDependents(sent, newRoot, children, phraseType, childDeprel);
		return newRoot;
	}

	/**
	 * Helper function, split out from coordToUD(): do the transformation,
	 * assuming that resulting structure has one root and everything else is
	 * directly depending on that one root.
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException
	 */
	protected static Node coordPartsChildListToUD(
			ArrayList<Node> sordedNodes, Sentence sent, String coordType)
	throws XPathExpressionException
	{
		// Find the structure root.
		Node newRoot = null;
		for (Node n : sordedNodes)
			if (LvtbRoles.CRDPART.equals(XPathEngine.get().evaluate("./role", n)))
			{
				newRoot = n;
				break;
			}
		if (newRoot == null)
		{
			System.err.printf("Sentence \"%s\" has no \"%s\" in \"%s\".\n",
					sent.id, LvtbRoles.CRDPART, coordType);
			newRoot = sordedNodes.get(0);
		}
		if (newRoot == null)
			throw new IllegalArgumentException(
					"\"" + coordType +"\" in entence \"" + sent.id + "\" seems to be empty.\n");

		// Create dependency structure in conll table.
		allAsDependents(sent, newRoot, sordedNodes, coordType, null);
		return newRoot;
	}
}
