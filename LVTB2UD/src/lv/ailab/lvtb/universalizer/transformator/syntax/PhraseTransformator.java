package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.*;
import lv.ailab.lvtb.universalizer.transformator.Sentence;
import lv.ailab.lvtb.universalizer.util.Tuple;
import lv.ailab.lvtb.universalizer.util.XPathEngine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	protected PrintWriter warnOut;

	public PhraseTransformator(Sentence sent, PrintWriter warnOut)
	{
		s = sent;
		this.warnOut = warnOut;
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
			return s.allUnderFirst(phraseNode, phraseType, LvtbRoles.PRED, null, true, warnOut);
		if (phraseType.equals(LvtbPmcTypes.SPCPMC) ||
				phraseType.equals(LvtbPmcTypes.QUOT) ||
				phraseType.equals(LvtbPmcTypes.ADDRESS))
			return s.allUnderFirst(phraseNode, phraseType, LvtbRoles.BASELEM, null, true, warnOut);
		if (phraseType.equals(LvtbPmcTypes.INTERJ) ||
				phraseType.equals(LvtbPmcTypes.PARTICLE))
			return s.allUnderFirst(phraseNode, phraseType, LvtbRoles.BASELEM, null, false, warnOut);

		//======= COORD ========================================================

		if (phraseType.equals(LvtbCoordTypes.CRDPARTS))
			return crdPartsToUD(phraseNode, phraseType);
		if (phraseType.equals(LvtbCoordTypes.CRDCLAUSES))
			return crdClausesToUD(phraseNode, phraseType);

		//======= X-WORD =======================================================

		// Multiple basElem, root is the last.
		if (phraseType.equals(LvtbXTypes.XAPP) ||
				phraseType.equals(LvtbXTypes.XNUM))
			return s.allUnderLast(phraseNode, phraseType, LvtbRoles.BASELEM, null,null, false, warnOut);

		// Multiple basElem, root is the first.
		if (phraseType.equals(LvtbXTypes.PHRASELEM) ||
				phraseType.equals(LvtbXTypes.NAMEDENT) ||
				phraseType.equals(LvtbXTypes.COORDANAL))
			return s.allUnderFirst(phraseNode, phraseType, LvtbRoles.BASELEM, null, false, warnOut);

		// Only one basElem
		if (phraseType.equals(LvtbXTypes.XPARTICLE))
			return s.allUnderLast(phraseNode, phraseType, LvtbRoles.BASELEM, null,null, true, warnOut);
		if (phraseType.equals(LvtbXTypes.XPREP))
			return s.allUnderLast(phraseNode, phraseType, LvtbRoles.BASELEM, LvtbRoles.PREP, null, true, warnOut);
		if (phraseType.equals(LvtbXTypes.XSIMILE))
			return xSimileToUD(phraseNode, phraseType);

		// Specific.
		if (phraseType.equals(LvtbXTypes.UNSTRUCT))
			return unstructToUd(phraseNode, phraseType);
		if (phraseType.equals(LvtbXTypes.SUBRANAL))
			return subrAnalToUD(phraseNode, phraseType);
		if (phraseType.equals(LvtbXTypes.XPRED))
			return xPredToUD(phraseNode, phraseType);

			warnOut.printf("Sentence \"%s\" has unrecognized \"%s\".\n",
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
		s.allAsDependents(newRoot, children, phraseType, null, warnOut);
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
				"./children/node[role='" + LvtbRoles.PRED +"']",
				pmcNode, XPathConstants.NODESET);
		Node newRoot = null;
		if (preds != null && preds.getLength() > 1)
			warnOut.printf("Sentence \"%s\" has more than one \"%s\" in \"%s\".\n",
					s.id, LvtbRoles.PRED, pmcType);
		if (preds != null && preds.getLength() > 0) newRoot = Utils.getFirstByDescOrd(preds);
		else
		{
			preds = (NodeList)XPathEngine.get().evaluate(
					"./children/node[role='" + LvtbRoles.BASELEM +"']",
					pmcNode, XPathConstants.NODESET);
			newRoot = Utils.getFirstByDescOrd(preds);
		}
		if (newRoot == null)
		{
			warnOut.printf("Sentence \"%s\" has no \"%s\", \"%s\" in \"%s\".\n",
					s.id, LvtbRoles.PRED, LvtbRoles.BASELEM, pmcType);
			newRoot = Utils.getFirstByDescOrd(children);
		}
		if (newRoot == null)
			throw new IllegalArgumentException("Sentence \"" + s.id + "\" seems to be empty.\n");

		// Create dependency structure in conll table.
		s.allAsDependents(newRoot, children, pmcType, null, warnOut);

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
				"./children/node[role='" + LvtbRoles.BASELEM +"']",
				pmcNode, XPathConstants.NODESET);
		Node newRoot = null;
		if (basElems != null && basElems.getLength() > 0) newRoot = Utils.getFirstByDescOrd(basElems);
		if (newRoot == null)
		{
			warnOut.printf("Sentence \"%s\" has no \"%s\" in \"%s\".\n",
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
			s.allAsDependents(newRoot, rootChildren, pmcType, null, warnOut);

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
				s.allAsDependents(subroot, nextPart, pmcType, null, warnOut);
				s.setLink(newRoot, subroot, UDv2Relations.PARATAXIS,
						Tuple.of(UDv2Relations.PARATAXIS, null), true, true);
			}
		}
		else s.allAsDependents(newRoot, children, pmcType, null, warnOut);

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
		return coordPartsChildListToUD(Utils.asOrderedList(children), coordType, warnOut);
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
			return coordPartsChildListToUD(Utils.asOrderedList(children), coordType, warnOut);

		// If semicolon(s) is (are) present, split on semicolon and then process
		// each part as ordinary coordination.
		ArrayList<Node> sortedSemicolons = Utils.asOrderedList(semicolons);
		ArrayList<Node> sortedChildren = Utils.asOrderedList(children);
		int semicOrd = Utils.getOrd(sortedSemicolons.get(0));
		Node newRoot = coordPartsChildListToUD(
				Utils.ordSplice(sortedChildren, 0, semicOrd), coordType, warnOut);
		for (int i = 1; i < sortedSemicolons.size(); i++)
		{
			int nextSemicOrd = Utils.getOrd(sortedSemicolons.get(i));
			Node newSubroot = coordPartsChildListToUD(
					Utils.ordSplice(sortedChildren, semicOrd, nextSemicOrd), coordType, warnOut);
			s.setLink(newRoot, newSubroot, UDv2Relations.PARATAXIS,
					Tuple.of(UDv2Relations.PARATAXIS, null), true, true);
			semicOrd = nextSemicOrd;
		}
		// last
		Node newSubroot = coordPartsChildListToUD(
				Utils.ordSplice(sortedChildren, semicOrd, Integer.MAX_VALUE), coordType, warnOut);
		s.setLink(newRoot, newSubroot, UDv2Relations.PARATAXIS,
				Tuple.of(UDv2Relations.PARATAXIS, null), true, true);
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
			List<Node> sortedNodes, String coordType, PrintWriter warnOut)
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
				s.allAsDependents(n, postponed, coordType, null, warnOut);
				lastSubroot = n;
				if (newRoot == null)
					newRoot = n;
				else
					s.addAsDependent(newRoot, n, coordType, null, warnOut);
				postponed = new ArrayList<>();
			} else postponed.add(n);
		}
		// Then process what is left.
		if (!postponed.isEmpty())
		{
			if (lastSubroot != null)
				s.allAsDependents(lastSubroot, postponed, coordType, null, warnOut);
			else
			{
				warnOut.printf("Sentence \"%s\" has no \"%s\" in \"%s\".\n",
						s.id, LvtbRoles.CRDPART, coordType);
				if (sortedNodes.get(0) != null )
				{
					newRoot = sortedNodes.get(0);
					s.allAsDependents(newRoot, sortedNodes, coordType, null, warnOut);
				}
				else throw new IllegalArgumentException(
						"\"" + coordType +"\" in sentence \"" + s.id + "\" seems to be empty.\n");
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
			return s.allUnderFirst(xNode, xType, LvtbRoles.BASELEM,
					Tuple.of(UDv2Relations.FLAT_FOREIGN, null), false, warnOut);
		else return s.allUnderFirst(xNode, xType, LvtbRoles.BASELEM,
				null, false, warnOut);
	}

	/**
	 * Transformation for subrAnal, based on subtag.
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
		String xTag = Utils.getTag(xNode);
		if (xTag == null || xTag.isEmpty())
		{
			warnOut.printf("Sentence \"%s\" has \"%s\" with incomplete xTag \"%s\".\n",
					s.id, xType, xTag);
			return missingTransform(xNode);
		}
		Matcher subTypeMatcher = Pattern.compile("[^\\[]*\\[(vv|ipv|skv|set|sal|part).*")
				.matcher(xTag);
		if (!subTypeMatcher.matches())
		{
			warnOut.printf("Sentence \"%s\" has \"%s\" with incomplete xTag \"%s\".\n",
					s.id, xType, xTag);
			return missingTransform(xNode);
		}

		String subType = subTypeMatcher.group(1);
		switch (subType)
		{
			// TODO maybe this role choice should be moved to PhrasePartDepLogic.phrasePartRoleToUD()
			case "vv" : return s.allUnderFirst(
					xNode, xType, LvtbRoles.BASELEM, null, false, warnOut);
			case "part" : return s.allUnderFirst(
					xNode, xType, LvtbRoles.BASELEM, null, false, warnOut);
			case "ipv" :
			{
				NodeList adjs = (NodeList)XPathEngine.get().evaluate(
						"./children/node[role='" + LvtbRoles.BASELEM +
								"' and (starts-with(m.rf/tag,'a') or starts-with(m.rf/tag,'ya') or starts-with(xinfo/tag,'a') or starts-with(xinfo/tag,'ya'))]",
						xNode, XPathConstants.NODESET);
				if (adjs.getLength() < 1)
				{
					warnOut.printf(
							"\"%s\" in sentence \"%s\" has no adjective \"%s\".\n",
							xType, s.id, LvtbRoles.BASELEM);
					adjs = children;
				}
				else if (adjs.getLength() > 1) warnOut.printf(
						"\"%s\" in sentence \"%s\" has more than one adjective \"%s\".\n",
						xType, s.id, LvtbRoles.BASELEM);
				Node newRoot = Utils.getLastByOrd(adjs);
				s.allAsDependents(newRoot, children, xType, null, warnOut);
				return newRoot;
			}
			case "skv" :
			{
				NodeList nums = (NodeList)XPathEngine.get().evaluate(
						"./children/node[role='" + LvtbRoles.BASELEM +
								"' and (starts-with(m.rf/tag,'mc') or starts-with(m.rf/tag,'xn') or starts-with(xinfo/tag,'mc') or starts-with(xinfo/tag,'xn'))]",
						xNode, XPathConstants.NODESET);
				if (nums.getLength() < 1)
				{
					warnOut.printf(
							"\"%s\" in sentence \"%s\" has no numeral \"%s\".\n",
							xType, s.id, LvtbRoles.BASELEM);
					nums = children;
				}
				else if (nums.getLength() > 1) warnOut.printf(
						"\"%s\" in sentence \"%s\" has more than one numeral \"%s\".\n",
						xTag, s.id, LvtbRoles.BASELEM);
				Node newRoot = Utils.getLastByOrd(nums);
				s.allAsDependents(newRoot, children, xType, null, warnOut);
				return newRoot;
			}
			case "set" :
			{
				NodeList noPrepBases = (NodeList)XPathEngine.get().evaluate(
						"./children/node[role='" + LvtbRoles.BASELEM +
								"' and not(children/xinfo/xtype='" + LvtbXTypes.XPREP + "')]",
						xNode, XPathConstants.NODESET);
				if (noPrepBases.getLength() < 1)
				{
					warnOut.printf(
							"\"%s\" in sentence \"%s\" has no \"%s\" without \"%s\".\n",
							xType, s.id, LvtbRoles.BASELEM, LvtbXTypes.XPREP);
					noPrepBases = children;
				}
				else if (noPrepBases.getLength() > 1) warnOut.printf(
						"\"%s\" in sentence \"%s\" has more than one \"%s\" without \"%s\".\n",
						xType, s.id, LvtbRoles.BASELEM, LvtbXTypes.XPREP);
				Node newRoot = Utils.getLastByOrd(noPrepBases);
				s.allAsDependents(newRoot, children, xType, null, warnOut);
				return newRoot;
			}
			case "sal" :
			{
				NodeList noSimBases = (NodeList)XPathEngine.get().evaluate(
						"./children/node[role='" + LvtbRoles.BASELEM +
								"' and not(children/xinfo/xtype='" + LvtbXTypes.XSIMILE + "')]",
						xNode, XPathConstants.NODESET);
				if (noSimBases.getLength() < 1)
				{
					warnOut.printf(
							"\"%s\" in sentence \"%s\" has no \"%s\" without \"%s\".\n",
							xType, s.id, LvtbRoles.BASELEM, LvtbXTypes.XSIMILE);
					noSimBases = children;
				}
				else if (noSimBases.getLength() > 1) warnOut.printf(
						"\"%s\" in sentence \"%s\" has more than one \"%s\" without \"%s\".\n",
						xType, s.id, LvtbRoles.BASELEM, LvtbXTypes.XSIMILE);
				Node newRoot = Utils.getLastByOrd(noSimBases);
				s.allAsDependents(newRoot, children, xType, null, warnOut);
				return newRoot;
			}
		}
		warnOut.printf("Sentence \"%s\" has \"%s\" with incomplete xTag \"%s\".\n",
				s.id, xType, xTag);
		return missingTransform(xNode);
	}

	/**
	 * Transformation for xSimile construction. Grammaticalization feature in
	 * xTag is required for successful transformation.
	 * @param xNode
	 * @param xType
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public Node xSimileToUD(Node xNode, String xType)
	throws XPathExpressionException
	{
		String xTag = Utils.getTag(xNode);
		if (xTag == null || xTag.isEmpty() || !xTag.matches("[^\\[]*\\[(sim|comp)[yn].*"))
		{
			warnOut.printf("Sentence \"%s\" has \"%s\" with incomplete xTag \"%s\".\n",
					s.id, xType, xTag);
			return missingTransform(xNode);
		}
		boolean gramzed = xTag.matches("[^\\[]*\\[(sim|comp)y.*");
		if (gramzed)
		{
			NodeList children = Utils.getAllPMLChildren(xNode);
			Node newRoot = Utils.getFirstByDescOrd(children);
			// TODO maybe this role choice should be moved to PhrasePartDepLogic.phrasePartRoleToUD()
			s.allAsDependents(newRoot, children, null,
					Tuple.of(UDv2Relations.FIXED, null), warnOut);
			return newRoot;
		}
		return s.allUnderLast(xNode, xType, LvtbRoles.BASELEM, null,null, true, warnOut);
	}

	/**
	 * Transformation for complex predicates. Predicates are expected to have
	 * only one basElem and either one mod or some aux'es. In case of mod,
	 * baseElem is attached as xcomp to it. Otherwise, noModXPredUD() are used.
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
		if (mods == null || mods.getLength() < 1)
			return noModXPredToUD(xNode, xType, xTag);
		else return modXPredToUD(xNode, xType, xTag);
	}

	/**
	 * Specific helper function: implementation of modal predication logic,
	 * split out from xPred processing.
	 * @param xNode
	 * @param xType
	 * @return	PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */	protected Node modXPredToUD(
			Node xNode, String xType, String xTag)
	throws XPathExpressionException
	{
		// Check if the tag is appropriate.
		String subtag = (xTag != null && xTag.contains("[") ?
				xTag.substring(xTag.indexOf("[") + 1) : "");
		if (!subtag.startsWith("modal") && !subtag.startsWith("expr")
				&& !subtag.startsWith("phase"))
			warnOut.printf("xPred \"%s\" has a problematic tag \"%s\".\n",
					Utils.getId(Utils.getPMLParent(xNode)), xTag);
		// Just put basElem under mod.
		return s.allUnderLast(xNode, xType,
				LvtbRoles.MOD, LvtbRoles.BASELEM, null, true, warnOut);
	}

	/**
	 * Specific helper function: implementation of aux/auxpass/cop logic, split
	 * out from xPred processing.
	 * @param xNode
	 * @param xType
	 * @return	PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	protected Node noModXPredToUD(
			Node xNode, String xType, String xTag)
	throws XPathExpressionException
	{
		// Get basElems and warn if there is none.
		NodeList basElems = (NodeList) XPathEngine.get().evaluate(
				"./children/node[role='" + LvtbRoles.BASELEM +"']", xNode, XPathConstants.NODESET);
		Node basElem = Utils.getLastByDescOrd(basElems);
		if (basElem == null)
			throw new IllegalArgumentException(
					"\"" + xType +"\" in sentence \"" + s.id + "\" has no basElem.\n");
		NodeList auxes = (NodeList) XPathEngine.get().evaluate(
				"./children/node[role='" + LvtbRoles.AUXVERB +"']", xNode, XPathConstants.NODESET);
		Node lastAux = Utils.getLastByDescOrd(auxes);
		if (lastAux == null)
			throw new IllegalArgumentException(
					"\"" + xType +"\" in sentence \"" + s.id + "\" has neither auxVerb nor mod.\n");
		if (auxes.getLength() > 1) for (int i = 0; i < auxes.getLength(); i++)
		{
			String auxLemma = Utils.getLemma(lastAux);
			if (!auxLemma.matches("(ne)?(būt|tikt|tapt|kļūt)"))
				warnOut.printf("xPred \"%s\" has multiple auxVerb one of which has lemma \"%s\".\n",
						Utils.getId(Utils.getPMLParent(xNode)), auxLemma);
		}

		String auxLemma = Utils.getLemma(lastAux);
		boolean ultimateAux = auxLemma.matches("(ne)?(būt|kļūt|tikt|tapt)");
		String basElemTag = Utils.getTag(basElem);

		boolean nominal = false;
		boolean passive = false;
		if (xTag != null && xTag.contains("["))
		{
			String subtag = xTag.substring(xTag.indexOf("[") + 1);
			if (subtag.startsWith("pass"))
				passive = true;
			else if (subtag.startsWith("subst") || subtag.startsWith("adj") ||
					subtag.startsWith("pronom") || subtag.startsWith("adv") ||
					subtag.startsWith("inf") || subtag.startsWith("num"))
				nominal = true;
			else if (!subtag.startsWith("act"))
				warnOut.printf("xPred \"%s\" has a problematic tag \"%s\".\n",
						Utils.getId(Utils.getPMLParent(xNode)), xTag);
		}
		else if (basElemTag != null)
			warnOut.printf("xPred \"%s\" has a problematic tag \"%s\".\n",
					Utils.getId(Utils.getPMLParent(xNode)), xTag);

		Node newRoot = basElem;
		if (!ultimateAux) newRoot = lastAux;
		NodeList children = Utils.getPMLNodeChildren(xNode);
		s.allAsDependents(newRoot, children, xType, null, warnOut);
		if (passive && ultimateAux)
			s.setLink(newRoot, lastAux, UDv2Relations.AUX_PASS,
					Tuple.of(UDv2Relations.AUX_PASS, null), true, true);
		if (nominal && ultimateAux)
			s.setLink(newRoot, lastAux, UDv2Relations.COP,
					Tuple.of(UDv2Relations.COP, null), true, true);
		return newRoot;
	}
}
