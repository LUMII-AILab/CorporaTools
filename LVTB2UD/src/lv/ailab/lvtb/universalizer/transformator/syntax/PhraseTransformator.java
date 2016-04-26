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
	 * In this sentence all the transformations are carried out.
	 */
	public Sentence s;

	public PhraseTransformator(Sentence sent)
	{
		s = sent;
	}

	/**
	 * Transform phrase to the UD structure.
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException
	 */
	public Node anyPhraseToUD(Node phraseNode)
	throws XPathExpressionException
	{
		String phraseType = Utils.getPhraseType(phraseNode);

		//======= PMC ==========================================================

		if (phraseType.equals(LvtbPmcTypes.SENT) ||
				phraseType.equals(LvtbPmcTypes.UTTER) ||
				phraseType.equals(LvtbPmcTypes.DIRSPPMC) ||
				phraseType.equals(LvtbPmcTypes.INSPMC))
			return sentencyToUD(phraseNode, phraseType);
		if (phraseType.equals(LvtbPmcTypes.SUBRCL) ||
				phraseType.equals(LvtbPmcTypes.MAINCL))
			return s.allUnderFirst(phraseNode, phraseType, LvtbRoles.PRED, null, true);
		if (phraseType.equals(LvtbPmcTypes.SPCPMC) ||
				phraseType.equals(LvtbPmcTypes.PARTICLE) ||
				phraseType.equals(LvtbPmcTypes.QUOT) ||
				phraseType.equals(LvtbPmcTypes.ADRESS) ||
				phraseType.equals(LvtbPmcTypes.INTERJ))
			return s.allUnderFirst(phraseNode, phraseType, LvtbRoles.BASELEM, null, true);

		//======= COORD ========================================================

		if (phraseType.equals(LvtbCoordTypes.CRDPARTS))
			return crdPartsToUD(phraseNode, phraseType);
		if (phraseType.equals(LvtbCoordTypes.CRDCLAUSES))
			crdClausesToUD(phraseNode, phraseType);

		//======= X-WORD =======================================================

		// Multiple basElem, root is the last.
		if (phraseType.equals(LvtbXTypes.XAPP) ||
				phraseType.equals(LvtbXTypes.XNUM))
			return s.allUnderLast(phraseNode, phraseType, LvtbRoles.BASELEM, null, false);

		// Multiple basElem, root is the first.
		if (phraseType.equals(LvtbXTypes.PHRASELEM) ||
				phraseType.equals(LvtbXTypes.NAMEDENT) ||
				phraseType.equals(LvtbXTypes.COORDANAL))
			return s.allUnderFirst(phraseNode, phraseType, LvtbRoles.BASELEM, null, false);

		// Only one basElem
		if (phraseType.equals(LvtbXTypes.XPREP) ||
				phraseType.equals(LvtbXTypes.XPARTICLE))
			return s.allUnderLast(phraseNode, phraseType, LvtbRoles.BASELEM, null, true);
		if (phraseType.equals(LvtbXTypes.XSIMILE))
			// If relinking (for "vairāk nekā" constructions) will be needed, it
			// will be done when processing the parent node.
			return s.allUnderLast(phraseNode, phraseType, LvtbRoles.BASELEM, null, true);

		// Specific.
		if (phraseType.equals(LvtbXTypes.UNSTRUCT))
			return unstructToUd(phraseNode, phraseType);
		if (phraseType.equals(LvtbXTypes.SUBRANAL))
			return subrAnalToUD(phraseNode, phraseType);
		if (phraseType.equals(LvtbXTypes.XPRED))
			return xPredToUD(phraseNode, phraseType);

			System.err.printf("Sentence \"%s\" has unrecognized \"%s\".\n",
				s.id, phraseType);
		return missingTransform(phraseNode);
	}

	/**
	 * Default phrase transformation: used when no phrase transformation rule
	 * is defined.
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException
	 */
	public Node missingTransform(Node phraseNode)
	throws XPathExpressionException
	{
		NodeList children = Utils.getPMLChildren(phraseNode);
		String phraseType = Utils.getPhraseType(phraseNode);
		Node newRoot = Utils.getFirstByOrd(children);
		s.allAsDependents(newRoot, children, phraseType, null);
		return newRoot;
	}

	/**
	 * Transformation for PMC that can have either basElem or pred - all
	 * children goes below first pred, r below forst basElem, if there is no
	 * pred.
	 * @param pmcNode
	 * @param pmcType
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException
	 */
	protected Node sentencyToUD(Node pmcNode, String pmcType)
	throws XPathExpressionException
	{
		NodeList children = Utils.getPMLChildren(pmcNode);

		// Find the structure root.
		NodeList preds = (NodeList)XPathEngine.get().evaluate(
				"./children/node[role='" + LvtbRoles.PRED +"']", pmcNode, XPathConstants.NODESET);
		Node newRoot = null;
		if (preds != null && preds.getLength() > 1)
			System.err.printf("Sentence \"%s\" has more than one \"%s\" in \"%s\".\n",
					s.id, LvtbRoles.PRED, pmcType);
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
					s.id, LvtbRoles.PRED, LvtbRoles.BASELEM, pmcType);
			newRoot = Utils.getFirstByOrd(children);
		}
		if (newRoot == null)
			throw new IllegalArgumentException("Sentence \"" + s.id + "\" seems to be empty.\n");

		// Create dependency structure in conll table.
		s.allAsDependents(newRoot, children, pmcType, null);

		return newRoot;
	}

	/**
	 * Transformation for coordinated clauses - first coordinated part is used
	 * as root.
	 * @param coordNode
	 * @param coordType
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException
	 */
	public Node crdPartsToUD(Node coordNode, String coordType)
	throws XPathExpressionException
	{
		NodeList children = Utils.getPMLChildren(coordNode);
		return coordPartsChildListToUD(Utils.asOrderedList(children), coordType);
	}

	/**
	 * Transformation for coordinated clauses - part after semicolon is
	 * parataxis, otherwise the same as coordinated parts.
	 * @param coordNode
	 * @param coordType
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException
	 */
	public Node crdClausesToUD (Node coordNode, String coordType)
	throws XPathExpressionException
	{
		NodeList children = Utils.getPMLChildren(coordNode);

		NodeList semicolons = (NodeList)XPathEngine.get().evaluate(
				"./children/node[m.rf/lemma=';']", coordNode, XPathConstants.NODESET);
		if (semicolons == null || semicolons.getLength() < 1)
			return coordPartsChildListToUD(Utils.asOrderedList(children), coordType);
		ArrayList<Node> sortedSemicolons = Utils.asOrderedList(semicolons);
		ArrayList<Node> sortedChildren = Utils.asOrderedList(children);
		int semicOrd = Utils.getOrd(sortedSemicolons.get(0));
		Node newRoot = coordPartsChildListToUD(
				Utils.ordSplice(sortedChildren, 0, semicOrd), coordType);
		Token newRootToken = s.pmlaToConll.get(Utils.getId(newRoot));
		for (int i  = 1; i < sortedSemicolons.size(); i++)
		{
			int nextSemicOrd = Utils.getOrd(sortedSemicolons.get(i));
			Node newSubroot = coordPartsChildListToUD(
					Utils.ordSplice(sortedChildren, semicOrd, nextSemicOrd), coordType);
			Token subrootToken = s.pmlaToConll.get(Utils.getId(newSubroot));
			subrootToken.deprel = URelations.PARATAXIS;
			subrootToken.head = newRootToken.idBegin;
		}

		return newRoot;
	}

	/**
	 * Specific helper function, split out from coordination processing: do the
	 * transformation, assuming that resulting structure has one root and
	 * everything else is directly depending on that one root.
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException
	 */
	protected Node coordPartsChildListToUD(
			List<Node> sortedNodes, String coordType)
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
					s.id, LvtbRoles.CRDPART, coordType);
			newRoot = sortedNodes.get(0);
		}
		if (newRoot == null)
			throw new IllegalArgumentException(
					"\"" + coordType +"\" in entence \"" + s.id + "\" seems to be empty.\n");

		// Create dependency structure in conll table.
		s.allAsDependents(newRoot, sortedNodes, coordType, null);
		return newRoot;
	}

	/**
	 * Transformation for unstruct x-word - if all parts are tagged as xf,
	 * DEPREL is foreign, else mwe.
	 * @param xNode
	 * @param xType
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException
	 */
	public Node unstructToUd(Node xNode, String xType)
	throws XPathExpressionException
	{
		NodeList children = Utils.getPMLChildren(xNode);
		NodeList foreigns = (NodeList)XPathEngine.get().evaluate(
				"./children/node[m.rf/tag='xf']", xNode, XPathConstants.NODESET);

		if (foreigns != null && children.getLength() == foreigns.getLength())
			return s.allUnderFirst(xNode, xType, LvtbRoles.BASELEM, URelations.FOREIGN, false);
		else return s.allUnderFirst(xNode, xType, LvtbRoles.BASELEM, null, false);
	}

	/**
	 * Transformation for subrAnal - special treatment for "vairāk kā/nekā X"
	 * constructions with xSimile (conj from xSimile is rearanged under
	 * "vairāk"), otherwise just make first element root.
	 * @param xNode
	 * @param xType
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException
	 */
	public Node subrAnalToUD(Node xNode, String xType)
	throws XPathExpressionException
	{
		NodeList children = Utils.getPMLChildren(xNode);

		Node first = Utils.getFirstByOrd(children);
		Node last = Utils.getLastByOrd(children);
		if (children != null && children.getLength() == 2 &&
				"vairāk".equals(Utils.getLemma(first)) &&
				LvtbXTypes.XSIMILE.equals(XPathEngine.get().evaluate("./children/xinfo/xtype", last)))
		{
			// Tricky part, where subordinated xSimile structure also must be
			// rearanged.
			NodeList simileConjs = (NodeList) XPathEngine.get().evaluate(
					"./children/node[role='" + LvtbRoles.CONJ + "']", last, XPathConstants.NODESET);
			Token newRootToken = s.pmlaToConll.get(Utils.getId(last));
			Token vToken = s.pmlaToConll.get(Utils.getId(first));
			vToken.head = newRootToken.idBegin;
			vToken.deprel = URelations.ADVMOD;
			if (simileConjs != null) for (int i = 0; i < simileConjs.getLength(); i++)
			{
				Token conjToken = s.pmlaToConll.get(Utils.getId(simileConjs.item(i)));
				conjToken.deprel = URelations.MWE;
				conjToken.head = vToken.idBegin;
			}
			return last;
		}
		else return s.allUnderFirst(xNode, xType, LvtbRoles.BASELEM, null, false);
	}

	/**
	 * Transformation for complex predicates. Predicates are splitted in parts,
	 * each part ending right after mod or basElem. Each part is processed as
	 * non-modal predicate, then parts are linked in a chain with xcomp links.
	 * @param xNode
	 * @param xType
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException
	 */
	public Node xPredToUD(Node xNode, String xType)
	throws XPathExpressionException
	{
		NodeList children = Utils.getPMLChildren(xNode);
		NodeList mods = (NodeList) XPathEngine.get().evaluate(
				"./children/node[role='" + LvtbRoles.MOD +"']", xNode, XPathConstants.NODESET);
		NodeList basElems = (NodeList) XPathEngine.get().evaluate(
				"./children/node[role='" + LvtbRoles.BASELEM +"']", xNode, XPathConstants.NODESET);
		Node basElem = Utils.getLastByOrd(basElems);
		if (basElem == null)
			throw new IllegalArgumentException(
					"\"" + xType +"\" in entence \"" + s.id + "\" has no basElem.\n");
		if (mods == null || mods.getLength() < 1)
			return noModXPredToUD(Utils.asOrderedList(children), xType);

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
					newRoot = noModXPredToUD(buffer, xType);
				Token newR = s.pmlaToConll.get(Utils.getId(newRoot));
				Token oldR = s.pmlaToConll.get(Utils.getId(latestRoot));
				oldR.head = newR.idBegin;
				oldR.deprel = URelations.XCOMP;
				latestRoot = newRoot;
			}
			if (i >= 0) buffer.push(ordChildren.get(i));
		}
		return latestRoot;
	}

	/**
	 * Specific helper function: implementation of aux/auxpass/cop logic, split
	 * out from xPred processing. Useful for processing either
	 * active/passive/nominal predicates or for parts of modal predicates.
	 * Neutral word order assumed.
	 * @param sortedNodes
	 * @param xType
	 * @return	PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException
	 */
	protected Node noModXPredToUD(
			List<Node> sortedNodes, String xType)
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
		s.allAsDependents(newRoot, sortedNodes, xType, null);
		if (passive)
		{
			Token lastAuxTok = s.pmlaToConll.get(Utils.getId(lastAux));
			lastAuxTok.deprel = URelations.AUXPASS;
		}
		if (nominal && auxLemma.matches("(ne)?būt"))
		{
			Token lastAuxTok = s.pmlaToConll.get(Utils.getId(lastAux));
			lastAuxTok.deprel = URelations.COP;
		}
		return newRoot;
	}

}
