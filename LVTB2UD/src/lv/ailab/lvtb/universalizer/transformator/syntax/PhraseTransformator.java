package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
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
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public Node anyPhraseToUD(Node phraseNode)
	throws XPathExpressionException
	{
		String phraseType = Utils.getPhraseType(phraseNode);

		//======= PMC ==========================================================

		if (phraseType.equals(LvtbPmcTypes.SENT) ||
				phraseType.equals(LvtbPmcTypes.DIRSPPMC) ||
				phraseType.equals(LvtbPmcTypes.INSPMC))
			return sentencyToUD(phraseNode, phraseType);
		if (phraseType.equals(LvtbPmcTypes.UTTER))
			return utterToUD(phraseNode, phraseType);
		if (phraseType.equals(LvtbPmcTypes.SUBRCL) ||
				phraseType.equals(LvtbPmcTypes.MAINCL))
			return s.allUnderFirst(phraseNode, phraseType, LvtbRoles.PRED, null, true);
		if (phraseType.equals(LvtbPmcTypes.SPCPMC) ||
				phraseType.equals(LvtbPmcTypes.QUOT) ||
				phraseType.equals(LvtbPmcTypes.ADDRESS))
			return s.allUnderFirst(phraseNode, phraseType, LvtbRoles.BASELEM, null, true);
		if (phraseType.equals(LvtbPmcTypes.INTERJ) ||
				phraseType.equals(LvtbPmcTypes.PARTICLE))
			return s.allUnderFirst(phraseNode, phraseType, LvtbRoles.BASELEM, null, false);

		//======= COORD ========================================================

		if (phraseType.equals(LvtbCoordTypes.CRDPARTS))
			return crdPartsToUD(phraseNode, phraseType);
		if (phraseType.equals(LvtbCoordTypes.CRDCLAUSES))
			return crdClausesToUD(phraseNode, phraseType);

		//======= X-WORD =======================================================

		// Multiple basElem, root is the last.
		if (phraseType.equals(LvtbXTypes.XAPP) ||
				phraseType.equals(LvtbXTypes.XNUM))
			return s.allUnderLast(phraseNode, phraseType, LvtbRoles.BASELEM, null,null, false);

		// Multiple basElem, root is the first.
		if (phraseType.equals(LvtbXTypes.PHRASELEM) ||
				phraseType.equals(LvtbXTypes.NAMEDENT) ||
				phraseType.equals(LvtbXTypes.COORDANAL))
			return s.allUnderFirst(phraseNode, phraseType, LvtbRoles.BASELEM, null, false);

		// Only one basElem
		if (phraseType.equals(LvtbXTypes.XPARTICLE))
			return s.allUnderLast(phraseNode, phraseType, LvtbRoles.BASELEM, null,null, true);
		if (phraseType.equals(LvtbXTypes.XPREP))
			return s.allUnderLast(phraseNode, phraseType, LvtbRoles.BASELEM, LvtbRoles.PREP, null, true);
		if (phraseType.equals(LvtbXTypes.XSIMILE))
			// If relinking (for "vairāk nekā" constructions) will be needed, it
			// will be done when processing the parent node.
			return s.allUnderLast(phraseNode, phraseType, LvtbRoles.BASELEM, null,null, true);

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
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public Node missingTransform(Node phraseNode)
	throws XPathExpressionException
	{
		NodeList children = Utils.getAllPMLChildren(phraseNode);
		String phraseType = Utils.getPhraseType(phraseNode);
		Node newRoot = Utils.getFirstByDescOrd(children);
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
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	protected Node sentencyToUD(Node pmcNode, String pmcType)
	throws XPathExpressionException
	{
		NodeList children = Utils.getAllPMLChildren(pmcNode);

		// Find the structure root.
		NodeList preds = (NodeList)XPathEngine.get().evaluate(
				"./children/node[role='" + LvtbRoles.PRED +"']", pmcNode, XPathConstants.NODESET);
		Node newRoot = null;
		if (preds != null && preds.getLength() > 1)
			System.err.printf("Sentence \"%s\" has more than one \"%s\" in \"%s\".\n",
					s.id, LvtbRoles.PRED, pmcType);
		if (preds != null && preds.getLength() > 0) newRoot = Utils.getFirstByDescOrd(preds);
		else
		{
			preds = (NodeList)XPathEngine.get().evaluate(
					"./children/node[role='" + LvtbRoles.BASELEM +"']", pmcNode, XPathConstants.NODESET);
			newRoot = Utils.getFirstByDescOrd(preds);
		}
		if (newRoot == null)
		{
			System.err.printf("Sentence \"%s\" has no \"%s\", \"%s\" in \"%s\".\n",
					s.id, LvtbRoles.PRED, LvtbRoles.BASELEM, pmcType);
			newRoot = Utils.getFirstByDescOrd(children);
		}
		if (newRoot == null)
			throw new IllegalArgumentException("Sentence \"" + s.id + "\" seems to be empty.\n");

		// Create dependency structure in conll table.
		s.allAsDependents(newRoot, children, pmcType, null);

		return newRoot;
	}

	/**
	 * Transformation for PMC that can have either basElem or pred - all
	 * children goes below first pred, r below forst basElem, if there is no
	 * pred.
	 * @param pmcNode
	 * @param pmcType
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	protected Node utterToUD(Node pmcNode, String pmcType)
	throws XPathExpressionException
	{
		NodeList children = Utils.getAllPMLChildren(pmcNode);

		// Find the structure root.
		NodeList basElems = (NodeList)XPathEngine.get().evaluate(
				"./children/node[role='" + LvtbRoles.BASELEM +"']", pmcNode, XPathConstants.NODESET);
		Node newRoot = null;
		if (basElems != null && basElems.getLength() > 0) newRoot = Utils.getFirstByDescOrd(basElems);
		if (newRoot == null)
		{
			System.err.printf("Sentence \"%s\" has no \"%s\" in \"%s\".\n",
					s.id, LvtbRoles.BASELEM, pmcType);
			newRoot = Utils.getFirstByDescOrd(children);
		}
		if (newRoot == null)
			throw new IllegalArgumentException("Sentence \"" + s.id + "\" seems to be empty.\n");

		if (basElems!= null && basElems.getLength() > 1 && children.getLength() > basElems.getLength())
		{
			ArrayList<Node> sortedChildren = Utils.asOrderedList(children);
			ArrayList<Node> rootChildren = new ArrayList<>();
			// If utter starts with punct, they are going to be root children.
			while (sortedChildren.size() > 0)
			{
				String role = Utils.getRole(sortedChildren.get(0));
				if (role.equals(LvtbRoles.PUNCT))
					rootChildren.add(sortedChildren.remove(0));
				else break;
			}
			// First "clause" until punctuation is going to be root children.
			while (sortedChildren.size() > 0)
			{
				String role = Utils.getRole(sortedChildren.get(0));
				if (!role.equals(LvtbRoles.PUNCT))
					rootChildren.add(sortedChildren.remove(0));
				else break;
			}
			// Last punctuation aslo is going to be root children.
			LinkedList<Node> lastPunct = new LinkedList<>();
			while (sortedChildren.size() > 0)
			{
				String role = Utils.getRole(sortedChildren.get(sortedChildren.size()-1));
				if (role.equals(LvtbRoles.PUNCT))
					lastPunct.push(sortedChildren.remove(sortedChildren.size()-1));
				else break;
			}
			rootChildren.addAll(lastPunct);
			s.allAsDependents(newRoot, rootChildren, pmcType, null);

			// now let's process what is left
			Token rootTok = s.pmlaToConll.get(Utils.getId(newRoot));
			while (sortedChildren.size() > 0)
			{
				ArrayList<Node> nextPart = new ArrayList<>();
				Node subroot = null;

				// find next stop
				while (sortedChildren.size() > 0)
				{
					String role = Utils.getRole(sortedChildren.get(0));
					if (role.equals(LvtbRoles.PUNCT) && nextPart.size() > 0)
						break;
					else if (role.equals(LvtbRoles.BASELEM) && subroot == null)
						subroot = sortedChildren.get(0);
					nextPart.add(sortedChildren.remove(0));
				}

				// process found part
				s.allAsDependents(subroot, nextPart, pmcType, null);
				Token subrootTok = s.pmlaToConll.get(Utils.getId(subroot));
				subrootTok.deprel = UDv2Relations.PARATAXIS;
				subrootTok.head = rootTok.idBegin;
			}
		}
		else s.allAsDependents(newRoot, children, pmcType, null);

		return newRoot;
	}


	/**
	 * Transformation for coordinated clauses - first coordinated part is used
	 * as root.
	 * @param coordNode
	 * @param coordType
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public Node crdPartsToUD(Node coordNode, String coordType)
	throws XPathExpressionException
	{
		NodeList children = Utils.getAllPMLChildren(coordNode);
		return coordPartsChildListToUD(Utils.asOrderedList(children), coordType);
	}

	/**
	 * Transformation for coordinated clauses - part after semicolon is
	 * parataxis, otherwise the same as coordinated parts.
	 * @param coordNode
	 * @param coordType
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public Node crdClausesToUD (Node coordNode, String coordType)
	throws XPathExpressionException
	{
		// Get all the children.
		NodeList children = Utils.getAllPMLChildren(coordNode);
		// Check if there are any semicolons.
		NodeList semicolons = (NodeList)XPathEngine.get().evaluate(
				"./children/node[m.rf/lemma=';']", coordNode, XPathConstants.NODESET);
		// No semicolons => process as ordinary coordination.
		if (semicolons == null || semicolons.getLength() < 1)
			return coordPartsChildListToUD(Utils.asOrderedList(children), coordType);

		// If semicolon(s) is (are) present, split on semicolon and then process
		// each part as ordinary coordination.
		ArrayList<Node> sortedSemicolons = Utils.asOrderedList(semicolons);
		ArrayList<Node> sortedChildren = Utils.asOrderedList(children);
		int semicOrd = Utils.getOrd(sortedSemicolons.get(0));
		Node newRoot = coordPartsChildListToUD(
				Utils.ordSplice(sortedChildren, 0, semicOrd), coordType);
		Token newRootToken = s.pmlaToConll.get(Utils.getId(newRoot));
		for (int i = 1; i < sortedSemicolons.size(); i++)
		{
			int nextSemicOrd = Utils.getOrd(sortedSemicolons.get(i));
			Node newSubroot = coordPartsChildListToUD(
					Utils.ordSplice(sortedChildren, semicOrd, nextSemicOrd), coordType);
			Token subrootToken = s.pmlaToConll.get(Utils.getId(newSubroot));
			subrootToken.deprel = UDv2Relations.PARATAXIS;
			subrootToken.head = newRootToken.idBegin;
			semicOrd = nextSemicOrd;
		}
		// last
		Node newSubroot = coordPartsChildListToUD(
				Utils.ordSplice(sortedChildren, semicOrd, Integer.MAX_VALUE), coordType);
		Token subrootToken = s.pmlaToConll.get(Utils.getId(newSubroot));
		subrootToken.deprel = UDv2Relations.PARATAXIS;
		subrootToken.head = newRootToken.idBegin;

		return newRoot;
	}

	/**
	 * Specific helper function, split out from coordination processing: do the
	 * transformation, assuming that the nodes provided as input node must be
	 * ordered as standard coordination structure, i.e., first crdPart is the
	 * root, all other crdPart-s are directly under it, all conj and punct are
	 * under the following crdPart.
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	protected Node coordPartsChildListToUD(
			List<Node> sortedNodes, String coordType)
	throws XPathExpressionException
	{
		// Find the structure root.
		Node newRoot = null;
		Node lastSubroot = null;
		ArrayList<Node> postponed = new ArrayList<>();
		// First process all nodes that are followed by a crdPart node.
		for (Node n : sortedNodes)
		{
			if (LvtbRoles.CRDPART.equals(XPathEngine.get().evaluate("./role", n)))
			{
				s.allAsDependents(n, postponed, coordType, null);
				lastSubroot = n;
				if (newRoot == null)
					newRoot = n;
				else
					s.addAsDependent(newRoot, n, coordType, null);
				postponed = new ArrayList<>();
			} else postponed.add(n);
		}
		// Then process what is left.
		if (!postponed.isEmpty())
		{
			if (lastSubroot != null)
				s.allAsDependents(lastSubroot, postponed, coordType, null);
			else
			{
				System.err.printf("Sentence \"%s\" has no \"%s\" in \"%s\".\n",
						s.id, LvtbRoles.CRDPART, coordType);
				if (sortedNodes.get(0) != null )
				{
					newRoot = sortedNodes.get(0);
					s.allAsDependents(newRoot, sortedNodes, coordType, null);
				}
				else throw new IllegalArgumentException(
						"\"" + coordType +"\" in entence \"" + s.id + "\" seems to be empty.\n");
			}
		}
		return newRoot;
	}

	/**
	 * Transformation for unstruct x-word - if all parts are tagged as xf,
	 * DEPREL is foreign, else mwe.
	 * @param xNode
	 * @param xType
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public Node unstructToUd(Node xNode, String xType)
	throws XPathExpressionException
	{
		NodeList children = Utils.getAllPMLChildren(xNode);
		NodeList foreigns = (NodeList)XPathEngine.get().evaluate(
				"./children/node[m.rf/tag='xf']", xNode, XPathConstants.NODESET);
		NodeList punct = (NodeList)XPathEngine.get().evaluate(
				"./children/node[starts-with(m.rf/tag,'z')]", xNode, XPathConstants.NODESET);

		if (foreigns != null && (children.getLength() == foreigns.getLength()
			|| punct != null && foreigns.getLength() > 0
				&& children.getLength() == foreigns.getLength() + punct.getLength()))
			return s.allUnderFirst(xNode, xType, LvtbRoles.BASELEM, UDv2Relations.FLAT_FOREIGN, false);
		else return s.allUnderFirst(xNode, xType, LvtbRoles.BASELEM, null, false);
	}

	/**
	 * Transformation for subrAnal
	 * - special treatment for
	 *   -- "vairāk kā/nekā X" constructions with xSimile: conj from xSimile is
	 *      rearanged under "vairāk"
	 *   -- "tāds kā X" construction with xSimile: xSimile's basElem is root.
	 * - otherwise just make first element root.
	 * @param xNode
	 * @param xType
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public Node subrAnalToUD(Node xNode, String xType)
	throws XPathExpressionException
	{
		NodeList children = Utils.getAllPMLChildren(xNode);

		Node first = Utils.getFirstByDescOrd(children);
		Node last = Utils.getLastByDescOrd(children);
		Node lastPhrase = Utils.getPhraseNode(last);
		if (children != null && children.getLength() == 2  &&
				LvtbXTypes.XSIMILE.equals(Utils.getAnyLabel(lastPhrase)))
		{
			Node firstPhrase = Utils.getPhraseNode(first);
			if (firstPhrase != null)
			{
				NodeList firstChildren = Utils.getAllPMLChildren(firstPhrase);
				//Node firstOfFirst = Utils.getFirstByDescOrd(firstChildren);
				Node lastOfFirst = Utils.getLastByDescOrd(firstChildren);
				// "Ne vairāk kā x"
				if (firstChildren != null && firstChildren.getLength() == 2  &&
						LvtbXTypes.XPARTICLE.equals(Utils.getAnyLabel(firstPhrase)) &&
						("vairāk".equals(XPathEngine.get().evaluate("./m.rf/form", lastOfFirst)) ||
						"Vairāk".equals(XPathEngine.get().evaluate("./m.rf/form", lastOfFirst))))
				{
					NodeList simileConjs = (NodeList) XPathEngine.get().evaluate(
							"./children/node[role='" + LvtbRoles.CONJ + "']", lastPhrase, XPathConstants.NODESET);
					Token newRootToken = s.pmlaToConll.get(Utils.getId(last));
					Token vToken = s.pmlaToConll.get(Utils.getId(lastOfFirst));
					vToken.head = newRootToken.idBegin;
					vToken.deprel = UDv2Relations.ADVMOD;
					if (simileConjs != null) for (int i = 0; i < simileConjs.getLength(); i++)
					{
						Token conjToken = s.pmlaToConll.get(Utils.getId(simileConjs.item(i)));
						//conjToken.deprel = UDv2Relations.FIXED;
						conjToken.head = vToken.idBegin;
					}
					return last;
				}
			}
			// "vairāk kā x"
			else if ("vairāk".equals(XPathEngine.get().evaluate("./m.rf/form", first)) ||
					"Vairāk".equals(XPathEngine.get().evaluate("./m.rf/form", first)))
			{
				// Tricky part, where subordinated xSimile structure also must be
				// rearanged.
				NodeList simileConjs = (NodeList) XPathEngine.get().evaluate(
						"./children/node[role='" + LvtbRoles.CONJ + "']", lastPhrase, XPathConstants.NODESET);
				Token newRootToken = s.pmlaToConll.get(Utils.getId(last));
				Token vToken = s.pmlaToConll.get(Utils.getId(first));
				vToken.head = newRootToken.idBegin;
				vToken.deprel = UDv2Relations.ADVMOD;
				if (simileConjs != null) for (int i = 0; i < simileConjs.getLength(); i++)
				{
					Token conjToken = s.pmlaToConll.get(Utils.getId(simileConjs.item(i)));
					//conjToken.deprel = UDv2Relations.FIXED;
					conjToken.head = vToken.idBegin;
				}
				return last;
			}
			// "tāds kā x"
			else if ("tāds".equals(Utils.getLemma(first)) || "tāda".equals(Utils.getLemma(first)))
			{
				Token newRootToken = s.pmlaToConll.get(Utils.getId(last));
				Token tToken = s.pmlaToConll.get(Utils.getId(first));
				tToken.head = newRootToken.idBegin;
				tToken.deprel = UDv2Relations.DET;
				return last;
			}
		}

		return s.allUnderFirst(xNode, xType, LvtbRoles.BASELEM, null, false);
	}

	/**
	 * Transformation for complex predicates. Predicates are splitted in parts,
	 * each part ending right after mod or basElem. Each part is processed as
	 * non-modal predicate, then parts are linked in a chain with xcomp links.
	 * @param xNode
	 * @param xType
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public Node xPredToUD(Node xNode, String xType)
	throws XPathExpressionException
	{
		NodeList children = Utils.getAllPMLChildren(xNode);
		String xTag = Utils.getTag(xNode);
		if (children.getLength() == 1) return children.item(0);
		NodeList mods = (NodeList) XPathEngine.get().evaluate(
				"./children/node[role='" + LvtbRoles.MOD +"']", xNode, XPathConstants.NODESET);
		NodeList basElems = (NodeList) XPathEngine.get().evaluate(
				"./children/node[role='" + LvtbRoles.BASELEM +"']", xNode, XPathConstants.NODESET);
		Node basElem = Utils.getLastByDescOrd(basElems);
		if (basElem == null)
			throw new IllegalArgumentException(
					"\"" + xType +"\" in entence \"" + s.id + "\" has no basElem.\n");
		if (mods == null || mods.getLength() < 1)
			return noModXPredToUD(Utils.asOrderedList(children), xType, xTag);

		ArrayList<Node> ordChildren = Utils.asOrderedList(children);
		LinkedList<Node> buffer = new LinkedList<>();
		buffer.push(ordChildren.get(ordChildren.size()-1));
		Node latestRoot = null;
		for (int i = ordChildren.size() - 2; i >= -1; i--)
		{
			String role = null;
			if (i > -1)	role = XPathEngine.get().evaluate("./role", ordChildren.get(i));
			if (!LvtbRoles.AUXVERB.equals(role))
			{
				Node newRoot = buffer.peek();
				if (buffer.size() > 1)
					newRoot = noModXPredToUD(buffer, xType, xTag);
				Token newR = s.pmlaToConll.get(Utils.getId(newRoot));
				if (latestRoot != null) // Nothing to add to last xcomp
				{
					Token oldR = s.pmlaToConll.get(Utils.getId(latestRoot));
					oldR.head = newR.idBegin;
					oldR.deprel = UDv2Relations.XCOMP;
				}
				latestRoot = newRoot;
				buffer = new LinkedList<>();
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
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	protected Node noModXPredToUD(
			List<Node> sortedNodes, String xType, String xTag)
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
		String basElemTag = Utils.getTag(lastBasElem);

		boolean nominal = false;
		boolean passive = false;
		if (xTag != null && xTag.contains("["))
		{
			String subtag = xTag.substring(xTag.indexOf("[" + 1));
			passive = subtag.startsWith("pas");
			nominal = subtag.startsWith("subst") || subtag.startsWith("adj") || subtag.startsWith("pronom");
		}
		else if (basElemTag != null)
		{
			nominal = basElemTag.matches("[napxm].*|v..pd...[ap]p.*]") ||
					basElemTag.matches("v..pd...ps.*]") && auxLemma.matches("(ne)?(tikt|tapt|būt)"); // Some nominal are missed to passive or active.
			passive = basElemTag.matches("v..pd...ps.*]") && !auxLemma.matches("(ne)?(tikt|tapt|būt)"); // Some here actually could be nominal.
		}

		Node newRoot = lastBasElem;
		if (nominal && lastAux != null && !auxLemma.matches("(ne)?būt"))
			newRoot = lastAux;
		s.allAsDependents(newRoot, sortedNodes, xType, null);
		if (passive && lastAux != null)
		{
			Token lastAuxTok = s.pmlaToConll.get(Utils.getId(lastAux));
			lastAuxTok.deprel = UDv2Relations.AUX_PASS;
		}
		if (nominal && lastAux!= null && auxLemma.matches("(ne)?būt"))
		{
			Token lastAuxTok = s.pmlaToConll.get(Utils.getId(lastAux));
			lastAuxTok.deprel = UDv2Relations.COP;
		}
		return newRoot;
	}

}
