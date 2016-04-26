package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.URelations;
import lv.ailab.lvtb.universalizer.pml.*;
import lv.ailab.lvtb.universalizer.transformator.Sentence;
import lv.ailab.lvtb.universalizer.transformator.XPathEngine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
		NodeList children = Utils.getPMLChildren(phraseNode);
		String phraseType = Utils.getPhraseType(phraseNode);
		Node newRoot = Utils.getFirstByOrd(children);
		Generic.allAsDependents(sent, newRoot, children, phraseType, null);
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
		NodeList children = Utils.getPMLChildren(pmcNode);

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
			Generic.allAsDependents(sent, newRoot, children, pmcType, null);

			return newRoot;
		}
		if (pmcType.equals(LvtbPmcTypes.SUBRCL) || pmcType.equals(LvtbPmcTypes.MAINCL)
				|| pmcType.equals(LvtbPmcTypes.INSPMC) || pmcType.equals(LvtbPmcTypes.SPCPMC)
				|| pmcType.equals(LvtbPmcTypes.PARTICLE) || pmcType.equals(LvtbPmcTypes.DIRSPPMC)
				|| pmcType.equals(LvtbPmcTypes.QUOT) || pmcType.equals(LvtbPmcTypes.ADRESS)
				|| pmcType.equals(LvtbPmcTypes.INTERJ))
			return Generic.allUnderFirst(sent, pmcNode, pmcType, LvtbRoles.PRED, null, true);

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
			return Generic.allUnderLast(sent, xNode, xType, LvtbRoles.BASELEM, null, false);

		// Multiple basElem, root is the first.
		if (xType.equals(LvtbXTypes.PHRASELEM) || xType.equals(LvtbXTypes.NAMEDENT) ||
				xType.equals(LvtbXTypes.COORDANAL))
			return Generic.allUnderFirst(sent, xNode, xType, LvtbRoles.BASELEM, null, false);

		// Only one basElem
		if (xType.equals(LvtbXTypes.XPREP) || xType.equals(LvtbXTypes.XPARTICLE))
			return Generic.allUnderLast(sent, xNode, xType, LvtbRoles.BASELEM, null, true);
		if (xType.equals(LvtbXTypes.XSIMILE))
			// If relinking (for "vairāk nekā" constructions) will be needed, it
			// will be done when processing the parent node.
			return Generic.allUnderLast(sent, xNode, xType, LvtbRoles.BASELEM, null, true);

		// Specific.
		if (xType.equals(LvtbXTypes.UNSTRUCT))
		{
			NodeList foreigns = (NodeList)XPathEngine.get().evaluate(
					"./children/node[m.rf/tag='xf']", xNode, XPathConstants.NODESET);

			if (foreigns != null && children.getLength() == foreigns.getLength())
				return Generic.allUnderFirst(sent, xNode, xType, LvtbRoles.BASELEM, URelations.FOREIGN, false);
			else return Generic.allUnderFirst(sent, xNode, xType, LvtbRoles.BASELEM, null, false);
		}
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
			else return Generic.allUnderFirst(sent, xNode, xType, LvtbRoles.BASELEM, null, false);
		}

		if (xType.equals(LvtbXTypes.XPRED))
		{
			NodeList mods = (NodeList) XPathEngine.get().evaluate(
					"./children/node[role='" + LvtbRoles.MOD +"']", xNode, XPathConstants.NODESET);
			NodeList auxs = (NodeList) XPathEngine.get().evaluate(
					"./children/node[role='" + LvtbRoles.AUXVERB +"']", xNode, XPathConstants.NODESET);
			NodeList basElems = (NodeList) XPathEngine.get().evaluate(
					"./children/node[role='" + LvtbRoles.BASELEM +"']", xNode, XPathConstants.NODESET);
			Node basElem = Utils.getLastByOrd(basElems);
			if (basElem == null)
				throw new IllegalArgumentException(
						"\"" + xType +"\" in entence \"" + sent.id + "\" has no basElem.\n");
			boolean nominal = Utils.getTag(basElem).matches("[napx].*|v..pd...[ap]p.*]");
			boolean passive = Utils.getTag(basElem).matches("v..pd...ps.*]");
			if (mods == null || mods.getLength() < 1)
				return noModXPredToUD(Utils.asOrderedList(children), sent, xType);

			ArrayList<Node> ordChildren = Utils.asOrderedList(children);
			LinkedList<Node> buffer = new LinkedList<>();
			buffer.push(ordChildren.get(ordChildren.size()-1));
			Node latestRoot = null;
			for (int i = ordChildren.size() - 2; i >= -1; i--)
			{
				String role = XPathEngine.get().evaluate("./role", ordChildren.get(i));
				if (!LvtbRoles.AUXVERB.equals(role) || i == -1)
				{
					Node newRoot = buffer.peek();
					if (buffer.size() > 1)
						newRoot = noModXPredToUD(buffer, sent, xType);
					Token newR = sent.pmlaToConll.get(Utils.getId(newRoot));
					Token oldR = sent.pmlaToConll.get(Utils.getId(latestRoot));
					oldR.head = newR.idBegin;
					oldR.deprel = URelations.XCOMP;
					latestRoot = newRoot;
				}
				if (i >= 0) buffer.push(ordChildren.get(i));
			}
		}

		System.err.printf("Sentence \"%s\" has unrecognized \"%s\".\n",
				sent.id, xType);
		return missingTransform(xNode, sent);
	}

	/**
	 * Specific helper function, split out from coordToUD(): do the
	 * transformation, assuming that resulting structure has one root and
	 * everything else is directly depending on that one root.
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException
	 */
	protected static Node coordPartsChildListToUD(
			List<Node> sortedNodes, Sentence sent, String coordType)
	throws XPathExpressionException
	{
		// Find the structure root.
		Node newRoot = null;
		for (Node n : sortedNodes)
			if (LvtbRoles.CRDPART.equals(XPathEngine.get().evaluate("./role", n)))
			{
				newRoot = n;
				break;
			}
		if (newRoot == null)
		{
			System.err.printf("Sentence \"%s\" has no \"%s\" in \"%s\".\n",
					sent.id, LvtbRoles.CRDPART, coordType);
			newRoot = sortedNodes.get(0);
		}
		if (newRoot == null)
			throw new IllegalArgumentException(
					"\"" + coordType +"\" in entence \"" + sent.id + "\" seems to be empty.\n");

		// Create dependency structure in conll table.
		Generic.allAsDependents(sent, newRoot, sortedNodes, coordType, null);
		return newRoot;
	}

	/**
	 * Specific helper function: implementation of aux/auxpass/cop logic, split
	 * out from xPred processing. Useful for processing either
	 * active/passive/nominal predicates or for parts of modal predicates.
	 * Neutral word order assumed.
	 * @param sortedNodes
	 * @param sent
	 * @param xType
	 * @return	PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException
	 */
	protected static Node noModXPredToUD(
			List<Node> sortedNodes, Sentence sent, String xType)
	throws XPathExpressionException
	{
		Node lastAux = null;
		Node lastBasElem = null;
		for (Node n : sortedNodes)
		{
			String role = XPathEngine.get().evaluate("./role", n);
			if (LvtbRoles.AUXVERB.equals(role)) lastAux = n;
			else lastBasElem = n;
		}
		String auxLemma = Utils.getLemma(lastAux);
		String auxTag = Utils.getTag(lastAux);

		boolean nominal = auxTag.matches("[napx].*|v..pd...[ap]p.*]") ||
				auxTag.matches("v..pd...ps.*]") && auxLemma.matches("(ne)?(tikt|tapt|būt)"); // Some nominal are missed to passive or active.
		boolean passive = auxTag.matches("v..pd...ps.*]") && !auxLemma.matches("(ne)?(tikt|tapt|būt)"); // Some here actually could be nominal.

		Node newRoot = lastBasElem;
		if (nominal && !auxLemma.matches("(ne)?būt"))
			newRoot = lastAux;
		Generic.allAsDependents(sent, newRoot, sortedNodes, xType, null);
		if (passive)
		{
			Token lastAuxTok = sent.pmlaToConll.get(Utils.getId(lastAux));
			lastAuxTok.deprel = URelations.AUXPASS;
		}
		if (nominal && auxLemma.matches("(ne)?būt"))
		{
			Token lastAuxTok = sent.pmlaToConll.get(Utils.getId(lastAux));
			lastAuxTok.deprel = URelations.COP;
		}
		return newRoot;
	}

	/**
	 * Helper functions for often used UD subtree construction routines.
	 */
	public static class Generic
	{
		/**
		 * Make a list of given nodes children of the designated parent. Set UD
		 * deprel for each child. If designated parent is included in child list
		 * node, circular dependency is not made, role is not set.
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
		public static void allAsDependents(
				Sentence sent, Node newRoot, NodeList children, String phraseType,
				URelations childDeprel)
		throws XPathExpressionException
		{
			allAsDependents(sent, newRoot, Utils.asList(children), phraseType, childDeprel);
		}

		/**
		 * Make a list of given nodes children of the designated parent. Set UD
		 * deprel for each child. If designated parent is included in child list
		 * node, circular dependency is not made, role is not set.
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
		public static void allAsDependents(
				Sentence sent, Node newRoot, List<Node> children, String phraseType,
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
		 * For the given node find first children of the given type and make all
		 * other children depend on it. Set UD deprel for each child.
		 * @param sent				sentence data
		 * @param phraseNode		node whose children must be processed
		 * @param phraseType    	phrase type from PML data, used for obtaining
		 *                      	correct UD role for children
		 * @param newRootType		rubroot for new UD structure will be searched
		 *                          between PML nodes with this type/role
		 * @param childDeprel		value to sent for DEPREL field for child nodes,
		 *                          or null, if DepRelLogic.getUDepFromPhrasePart()
		 *                          should be used to obtain DEPREL for child nodes
		 * @param warnMoreThanOne	whether to warn if more than one potential root
		 *                          is found
		 * @return root of the corresponding dependency structure
		 * @throws XPathExpressionException
		 */
		public static Node allUnderFirst(
				Sentence sent, Node phraseNode, String phraseType,
				String newRootType, URelations childDeprel, boolean warnMoreThanOne)
		throws XPathExpressionException
		{
			NodeList children = (NodeList)XPathEngine.get().evaluate(
					"./children/*", phraseNode, XPathConstants.NODESET);
			NodeList potentialRoots = (NodeList)XPathEngine.get().evaluate(
					"./children/node[role='" + newRootType +"']", phraseNode, XPathConstants.NODESET);
			if (warnMoreThanOne && potentialRoots != null && potentialRoots.getLength() > 1)
				System.err.printf("\"%s\" in sentence \"%s\" has more than one \"%s\".\n",
						phraseType, sent.id, newRootType);
			Node newRoot = Utils.getFirstByOrd(potentialRoots);
			if (newRoot == null)
			{
				System.err.printf("\"%s\" in sentence \"%s\" has no \"%s\".\n",
						phraseType, sent.id, newRootType);
				newRoot = Utils.getFirstByOrd(children);
			}
			if (newRoot == null)
				throw new IllegalArgumentException(
						"\"" + phraseType +"\" in sentence \"" + sent.id + "\" seems to be empty.\n");
			allAsDependents(sent, newRoot, children, phraseType, childDeprel);
			return newRoot;
		}

		/**
		 * For the given node find first children of the given type and make all
		 * other children depend on it. Set UD deprel for each child.
		 * @param sent				sentence data
		 * @param phraseNode		node whose children must be processed
		 * @param phraseType    	phrase type from PML data, used for obtaining
		 *                      	correct UD role for children
		 * @param newRootType		rubroot for new UD structure will be searched
		 *                          between PML nodes with this type/role
		 * @param childDeprel		value to sent for DEPREL field for child nodes,
		 *                          or null, if DepRelLogic.getUDepFromPhrasePart()
		 *                          should be used to obtain DEPREL for child nodes
		 * @param warnMoreThanOne	whether to warn if more than one potential root
		 *                          is found
		 * @return root of the corresponding dependency structure
		 * @throws XPathExpressionException
		 */
		public static Node allUnderLast(
				Sentence sent, Node phraseNode, String phraseType,
				String newRootType, URelations childDeprel, boolean warnMoreThanOne)
		throws XPathExpressionException
		{
			NodeList children = (NodeList)XPathEngine.get().evaluate(
					"./children/*", phraseNode, XPathConstants.NODESET);
			NodeList potentialRoots = (NodeList)XPathEngine.get().evaluate(
					"./children/node[role='" + newRootType +"']", phraseNode, XPathConstants.NODESET);
			Node newRoot = Utils.getLastByOrd(potentialRoots);
			if (warnMoreThanOne && potentialRoots != null && potentialRoots.getLength() > 1)
				System.err.printf("\"%s\" in sentence \"%s\" has more than one \"%s\".\n",
						phraseType, sent.id, newRoot);
			if (newRoot == null)
			{
				System.err.printf("\"%s\" in sentence \"%s\" has no \"%s\".\n",
						phraseType, sent.id, newRoot);
				newRoot = Utils.getLastByOrd(children);
			}
			if (newRoot == null)
				throw new IllegalArgumentException(
						"\"" + phraseType +"\" in sentence \"" + sent.id + "\" seems to be empty.\n");
			allAsDependents(sent, newRoot, children, phraseType, childDeprel);
			return newRoot;
		}
	}
}
