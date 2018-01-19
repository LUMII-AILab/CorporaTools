package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.*;
import lv.ailab.lvtb.universalizer.pml.utils.NodeFieldUtils;
import lv.ailab.lvtb.universalizer.pml.utils.NodeListUtils;
import lv.ailab.lvtb.universalizer.pml.utils.NodeUtils;
import lv.ailab.lvtb.universalizer.utils.Logger;
import lv.ailab.lvtb.universalizer.transformator.Sentence;
import lv.ailab.lvtb.universalizer.utils.Tuple;
import lv.ailab.lvtb.universalizer.utils.XPathEngine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
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
	protected Logger logger;

	public PhraseTransformator(Sentence sent, Logger logger)
	{
		s = sent;
		this.logger = logger;
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
		String phraseType = NodeFieldUtils.getPhraseType(phraseNode);
		String phraseTag = NodeFieldUtils.getTag(phraseNode);

		//======= PMC ==========================================================

		if (phraseType.equals(LvtbPmcTypes.SENT) ||
				phraseType.equals(LvtbPmcTypes.DIRSPPMC) ||
				phraseType.equals(LvtbPmcTypes.INSPMC))
			return sentencyToUD(phraseNode, phraseType);
		if (phraseType.equals(LvtbPmcTypes.UTTER))
			return utterToUD(phraseNode, phraseType);
		if (phraseType.equals(LvtbPmcTypes.SUBRCL) ||
				phraseType.equals(LvtbPmcTypes.MAINCL))
			return s.allUnderFirst(phraseNode, phraseType, null, LvtbRoles.PRED, null, true, logger);
		if (phraseType.equals(LvtbPmcTypes.SPCPMC) ||
				phraseType.equals(LvtbPmcTypes.QUOT) ||
				phraseType.equals(LvtbPmcTypes.ADDRESS))
			return s.allUnderFirst(phraseNode, phraseType, null, LvtbRoles.BASELEM, null, true, logger);
		if (phraseType.equals(LvtbPmcTypes.INTERJ) ||
				phraseType.equals(LvtbPmcTypes.PARTICLE))
			return s.allUnderFirst(phraseNode, phraseType, null, LvtbRoles.BASELEM, null, false, logger);

		//======= COORD ========================================================

		if (phraseType.equals(LvtbCoordTypes.CRDPARTS))
			return crdPartsToUD(phraseNode, phraseType, phraseTag);
		if (phraseType.equals(LvtbCoordTypes.CRDCLAUSES))
			return crdClausesToUD(phraseNode, phraseType, phraseTag);

		//======= X-WORD =======================================================

		// Multiple basElem, root is the last.
		if (phraseType.equals(LvtbXTypes.XAPP) ||
				phraseType.equals(LvtbXTypes.XNUM))
			return s.allUnderLast(phraseNode, phraseType, phraseTag, LvtbRoles.BASELEM, null,null, false, logger);

		// Multiple basElem, root is the first.
		if (phraseType.equals(LvtbXTypes.PHRASELEM) ||
				phraseType.equals(LvtbXTypes.NAMEDENT) ||
				phraseType.equals(LvtbXTypes.COORDANAL))
			return s.allUnderFirst(phraseNode, phraseType, phraseTag, LvtbRoles.BASELEM, null, false, logger);

		// Only one basElem
		if (phraseType.equals(LvtbXTypes.XPARTICLE))
			return s.allUnderLast(phraseNode, phraseType, phraseTag, LvtbRoles.BASELEM, null,null, true, logger);
		if (phraseType.equals(LvtbXTypes.XPREP))
			return s.allUnderLast(phraseNode, phraseType, phraseTag, LvtbRoles.BASELEM, LvtbRoles.PREP, null, true, logger);
		if (phraseType.equals(LvtbXTypes.XSIMILE))
			return xSimileToUD(phraseNode, phraseType, phraseTag);

		// Specific.
		if (phraseType.equals(LvtbXTypes.UNSTRUCT))
			return unstructToUd(phraseNode, phraseType, phraseTag);
		if (phraseType.equals(LvtbXTypes.SUBRANAL))
			return subrAnalToUD(phraseNode, phraseType, phraseTag);
		if (phraseType.equals(LvtbXTypes.XPRED))
			return xPredToUD(phraseNode, phraseType, phraseTag);

		logger.doInsentenceWarning(String.format(
				"Sentence \"%s\" has unrecognized \"%s\".", s.id, phraseType));
		//warnOut.printf("Sentence \"%s\" has unrecognized \"%s\".\n", s.id, phraseType);
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
		NodeList children = NodeUtils.getAllPMLChildren(phraseNode);
		String phraseType = NodeFieldUtils.getPhraseType(phraseNode);
		String phraseTag = NodeFieldUtils.getTag(phraseNode);
		Node newRoot = NodeListUtils.getFirstByDescOrd(children);
		s.allAsDependents(newRoot, children, phraseType, phraseTag, null, logger);
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
		NodeList children = NodeUtils.getAllPMLChildren(pmcNode);

		// Find the structure root.
		NodeList preds = (NodeList)XPathEngine.get().evaluate(
				"./children/node[role='" + LvtbRoles.PRED +"']",
				pmcNode, XPathConstants.NODESET);
		Node newRoot = null;
		if (preds != null && preds.getLength() > 1)
			logger.doInsentenceWarning(String.format(
					"Sentence \"%s\" has more than one \"%s\" in \"%s\".",
					s.id, LvtbRoles.PRED, pmcType));
			//warnOut.printf("Sentence \"%s\" has more than one \"%s\" in \"%s\".\n", s.id, LvtbRoles.PRED, pmcType);
		if (preds != null && preds.getLength() > 0) newRoot = NodeListUtils.getFirstByDescOrd(preds);
		else
		{
			preds = (NodeList)XPathEngine.get().evaluate(
					"./children/node[role='" + LvtbRoles.BASELEM +"']",
					pmcNode, XPathConstants.NODESET);
			newRoot = NodeListUtils.getFirstByDescOrd(preds);
		}
		if (newRoot == null)
		{
			logger.doInsentenceWarning(String.format(
					"Sentence \"%s\" has no \"%s\", \"%s\" in \"%s\".",
					s.id, LvtbRoles.PRED, LvtbRoles.BASELEM, pmcType));
			//warnOut.printf("Sentence \"%s\" has no \"%s\", \"%s\" in \"%s\".\n", s.id, LvtbRoles.PRED, LvtbRoles.BASELEM, pmcType);
			newRoot = NodeListUtils.getFirstByDescOrd(children);
		}
		if (newRoot == null)
			throw new IllegalArgumentException(String.format(
					"Sentence \"%s\" seems to be empty", s.id));

		// Create dependency structure in conll table.
		s.allAsDependents(newRoot, children, pmcType, null, null, logger);

		return newRoot;
	}

	/**
	 * Transformation for PMC that can have either basElem or pred - all
	 * children goes below first pred, r below forst basElem, if there is no
	 * pred.
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	protected Node utterToUD(Node pmcNode, String pmcType)
	throws XPathExpressionException
	{
		NodeList children = NodeUtils.getAllPMLChildren(pmcNode);

		// Find the structure root.
		NodeList basElems = (NodeList)XPathEngine.get().evaluate(
				"./children/node[role='" + LvtbRoles.BASELEM +"']",
				pmcNode, XPathConstants.NODESET);
		Node newRoot = null;
		if (basElems != null && basElems.getLength() > 0) newRoot = NodeListUtils.getFirstByDescOrd(basElems);
		if (newRoot == null)
		{
			logger.doInsentenceWarning(String.format(
					"Sentence \"%s\" has no \"%s\" in \"%s\".",
					s.id, LvtbRoles.BASELEM, pmcType));
			//warnOut.printf("Sentence \"%s\" has no \"%s\" in \"%s\".\n", s.id, LvtbRoles.BASELEM, pmcType);
			newRoot = NodeListUtils.getFirstByDescOrd(children);
		}
		if (newRoot == null)
			throw new IllegalArgumentException(String.format(
					"Sentence \"%s\" seems to be empty", s.id));

		if (basElems!= null && basElems.getLength() > 1 && children.getLength() > basElems.getLength())
		{
			ArrayList<Node> sortedChildren = NodeListUtils.asOrderedList(children);
			ArrayList<Node> rootChildren = new ArrayList<>();
			// If utter starts with punct, they are going to be root children.
			while (sortedChildren.size() > 0)
			{
				String role = NodeFieldUtils.getRole(sortedChildren.get(0));
				if (role.equals(LvtbRoles.PUNCT))
					rootChildren.add(sortedChildren.remove(0));
				else break;
			}
			// First "clause" until punctuation is going to be root children.
			while (sortedChildren.size() > 0)
			{
				String role = NodeFieldUtils.getRole(sortedChildren.get(0));
				if (!role.equals(LvtbRoles.PUNCT))
					rootChildren.add(sortedChildren.remove(0));
				else break;
			}
			// Last punctuation aslo is going to be root children.
			LinkedList<Node> lastPunct = new LinkedList<>();
			while (sortedChildren.size() > 0)
			{
				String role = NodeFieldUtils.getRole(sortedChildren.get(sortedChildren.size()-1));
				if (role.equals(LvtbRoles.PUNCT))
					lastPunct.push(sortedChildren.remove(sortedChildren.size()-1));
				else break;
			}
			rootChildren.addAll(lastPunct);
			s.allAsDependents(newRoot, rootChildren, pmcType, null, null, logger);

			// now let's process what is left
			Token rootTok = s.pmlaToConll.get(NodeFieldUtils.getId(newRoot));
			while (sortedChildren.size() > 0)
			{
				ArrayList<Node> nextPart = new ArrayList<>();
				Node subroot = null;

				// find next stop
				while (sortedChildren.size() > 0)
				{
					String role = NodeFieldUtils.getRole(sortedChildren.get(0));
					if (role.equals(LvtbRoles.PUNCT) && nextPart.size() > 0)
						break;
					else if (role.equals(LvtbRoles.BASELEM) && subroot == null)
						subroot = sortedChildren.get(0);
					nextPart.add(sortedChildren.remove(0));
				}

				// process found part
				s.allAsDependents(subroot, nextPart, pmcType, null, null, logger);
				s.setLink(newRoot, subroot, UDv2Relations.PARATAXIS,
						Tuple.of(UDv2Relations.PARATAXIS, null), true, true);
			}
		}
		else s.allAsDependents(newRoot, children, pmcType, null, null, logger);

		return newRoot;
	}


	/**
	 * Transformation for coordinated clauses - first coordinated part is used
	 * as root.
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public Node crdPartsToUD(Node coordNode, String coordType, String coordTag)
	throws XPathExpressionException
	{
		NodeList children = NodeUtils.getAllPMLChildren(coordNode);
		return coordPartsChildListToUD(NodeListUtils.asOrderedList(children), coordType, coordTag, logger);
	}

	/**
	 * Transformation for coordinated clauses - part after semicolon is
	 * parataxis, otherwise the same as coordinated parts.
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public Node crdClausesToUD (Node coordNode, String coordType, String coordTag)
	throws XPathExpressionException
	{
		// Get all the children.
		NodeList children = NodeUtils.getAllPMLChildren(coordNode);
		// Check if there are any semicolons.
		NodeList semicolons = (NodeList)XPathEngine.get().evaluate(
				"./children/node[m.rf/lemma=';']", coordNode, XPathConstants.NODESET);
		// No semicolons => process as ordinary coordination.
		if (semicolons == null || semicolons.getLength() < 1)
			return coordPartsChildListToUD(NodeListUtils.asOrderedList(children), coordType, coordTag, logger);

		// If semicolon(s) is (are) present, split on semicolon and then process
		// each part as ordinary coordination.
		ArrayList<Node> sortedSemicolons = NodeListUtils.asOrderedList(semicolons);
		ArrayList<Node> sortedChildren = NodeListUtils.asOrderedList(children);
		int semicOrd = NodeFieldUtils.getOrd(sortedSemicolons.get(0));
		Node newRoot = coordPartsChildListToUD(
				NodeListUtils.ordSplice(sortedChildren, 0, semicOrd), coordType, coordTag, logger);
		for (int i = 1; i < sortedSemicolons.size(); i++)
		{
			int nextSemicOrd = NodeFieldUtils.getOrd(sortedSemicolons.get(i));
			Node newSubroot = coordPartsChildListToUD(
					NodeListUtils.ordSplice(sortedChildren, semicOrd, nextSemicOrd), coordType, coordTag, logger);
			s.setLink(newRoot, newSubroot, UDv2Relations.PARATAXIS,
					Tuple.of(UDv2Relations.PARATAXIS, null), true, true);
			semicOrd = nextSemicOrd;
		}
		// last
		Node newSubroot = coordPartsChildListToUD(
				NodeListUtils.ordSplice(sortedChildren, semicOrd, Integer.MAX_VALUE), coordType, coordTag, logger);
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
			List<Node> sortedNodes, String coordType, String coordTag, Logger logger)
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
				s.allAsDependents(n, postponed, coordType, coordTag, null, logger);
				lastSubroot = n;
				if (newRoot == null)
					newRoot = n;
				else
					s.addAsDependent(newRoot, n, coordType, coordTag, null, logger);
				postponed = new ArrayList<>();
			} else postponed.add(n);
		}
		// Then process what is left.
		if (!postponed.isEmpty())
		{
			if (lastSubroot != null)
				s.allAsDependents(lastSubroot, postponed, coordType, coordTag, null, logger);
			else
			{
				logger.doInsentenceWarning(String.format(
						"Sentence \"%s\" has no \"%s\" in \"%s\".",
						s.id, LvtbRoles.CRDPART, coordType));
				//warnOut.printf("Sentence \"%s\" has no \"%s\" in \"%s\".\n", s.id, LvtbRoles.CRDPART, coordType);
				if (sortedNodes.get(0) != null )
				{
					newRoot = sortedNodes.get(0);
					s.allAsDependents(newRoot, sortedNodes, coordType, coordTag, null, logger);
				}
				else throw new IllegalArgumentException(String.format(
						"\"%s\" in sentence \"%s\" seems to be empty",
						coordType, s.id));
			}
		}
		return newRoot;
	}

	/**
	 * Transformation for unstruct x-word - if all parts are tagged as xf,
	 * DEPREL is foreign, else mwe.
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public Node unstructToUd(Node xNode, String xType, String xTag)
	throws XPathExpressionException
	{
		NodeList children = NodeUtils.getAllPMLChildren(xNode);
		NodeList foreigns = (NodeList)XPathEngine.get().evaluate(
				"./children/node[m.rf/tag='xf']", xNode, XPathConstants.NODESET);
		NodeList punct = (NodeList)XPathEngine.get().evaluate(
				"./children/node[starts-with(m.rf/tag,'z')]", xNode, XPathConstants.NODESET);

		if (foreigns != null && (children.getLength() == foreigns.getLength()
			|| punct != null && foreigns.getLength() > 0
				&& children.getLength() == foreigns.getLength() + punct.getLength()))
			return s.allUnderFirst(xNode, xType, xTag, LvtbRoles.BASELEM,
					Tuple.of(UDv2Relations.FLAT_FOREIGN, null), false, logger);
		else return s.allUnderFirst(xNode, xType, xTag, LvtbRoles.BASELEM,
				null, false, logger);
	}

	/**
	 * Transformation for subrAnal, based on subtag.
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public Node subrAnalToUD(Node xNode, String xType, String xTag)
	throws XPathExpressionException
	{
		NodeList children = NodeUtils.getAllPMLChildren(xNode);
		if (xTag == null || xTag.isEmpty())
		{
			logger.doInsentenceWarning(String.format(
					"Sentence \"%s\" has \"%s\" with incomplete xTag \"%s\".",
					s.id, xType, xTag));
			//warnOut.printf("Sentence \"%s\" has \"%s\" with incomplete xTag \"%s\".\n", s.id, xType, xTag);
			return missingTransform(xNode);
		}
		Matcher subTypeMatcher = Pattern.compile("[^\\[]*\\[(vv|ipv|skv|set|sal|part).*")
				.matcher(xTag);
		if (!subTypeMatcher.matches())
		{
			logger.doInsentenceWarning(String.format(
					"Sentence \"%s\" has \"%s\" with incomplete xTag \"%s\".",
					s.id, xType, xTag));
			//warnOut.printf("Sentence \"%s\" has \"%s\" with incomplete xTag \"%s\".\n", s.id, xType, xTag);
			return missingTransform(xNode);
		}

		String subType = subTypeMatcher.group(1);
		switch (subType)
		{
			// TODO maybe this role choice should be moved to PhrasePartDepLogic.phrasePartRoleToUD()
			case "vv" : return s.allUnderFirst(
					xNode, xType, xTag, LvtbRoles.BASELEM, null, false, logger);
			case "part" : return s.allUnderFirst(
					xNode, xType, xTag, LvtbRoles.BASELEM, null, false, logger);
			case "ipv" :
			{
				NodeList basElems = (NodeList)XPathEngine.get().evaluate(
						"./children/node[role='" + LvtbRoles.BASELEM + "']",
						xNode, XPathConstants.NODESET);
				ArrayList<Node> adjs = new ArrayList<>();
				for (int i = 0; i < basElems.getLength(); i++)
				{
					Node current = basElems.item(i);
					String tag = NodeFieldUtils.getTag(current);
					if (tag.matches("(a|ya).*")) adjs.add(current);
				}
				if (adjs.size() < 1)
				{
					logger.doInsentenceWarning(String.format(
							"\"%s\" in sentence \"%s\" has no adjective \"%s\".",
							xType, s.id, LvtbRoles.BASELEM));
					//warnOut.printf("\"%s\" in sentence \"%s\" has no adjective \"%s\".\n", xType, s.id, LvtbRoles.BASELEM);
					adjs = NodeListUtils.asList(children);
				}
				else if (adjs.size() > 1)
					logger.doInsentenceWarning(String.format(
							"\"%s\" in sentence \"%s\" has more than one adjective \"%s\".",
							xType, s.id, LvtbRoles.BASELEM));
					//warnOut.printf("\"%s\" in sentence \"%s\" has more than one adjective \"%s\".\n", xType, s.id, LvtbRoles.BASELEM);
				Node newRoot = NodeListUtils.getLastByOrd(adjs);
				s.allAsDependents(newRoot, children, xType, xTag, null, logger);
				return newRoot;
			}
			case "skv" :
			{
				NodeList basElems = (NodeList)XPathEngine.get().evaluate(
						"./children/node[role='" + LvtbRoles.BASELEM + "']",
						xNode, XPathConstants.NODESET);
				ArrayList<Node> prons = new ArrayList<>();
				for (int i = 0; i < basElems.getLength(); i++)
				{
					Node current = basElems.item(i);
					String tag = NodeFieldUtils.getTag(current);
					if (tag.matches("p.*")) prons.add(current);
				}
				if (prons.size() < 1)
				{
					logger.doInsentenceWarning(String.format(
							"\"%s\" in sentence \"%s\" has no pronominal \"%s\".",
							xType, s.id, LvtbRoles.BASELEM));
					//warnOut.printf("\"%s\" in sentence \"%s\" has no pronominal \"%s\".\n", xType, s.id, LvtbRoles.BASELEM);
					prons = NodeListUtils.asList(children);
				}
				else if (prons.size() > 1)
					logger.doInsentenceWarning(String.format(
							"\"%s\" in sentence \"%s\" has more than one pronominal \"%s\".",
							xType, s.id, LvtbRoles.BASELEM));
					//warnOut.printf("\"%s\" in sentence \"%s\" has more than one pronominal \"%s\".\n", xType, s.id, LvtbRoles.BASELEM);
				Node newRoot = NodeListUtils.getFirstByOrd(prons);
				s.allAsDependents(newRoot, children, xType, xTag,null, logger);
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
					logger.doInsentenceWarning(String.format(
							"\"%s\" in sentence \"%s\" has no \"%s\" without \"%s\".",
							xType, s.id, LvtbRoles.BASELEM, LvtbXTypes.XPREP));
					//warnOut.printf("\"%s\" in sentence \"%s\" has no \"%s\" without \"%s\".\n", xType, s.id, LvtbRoles.BASELEM, LvtbXTypes.XPREP);
					noPrepBases = children;
				}
				else if (noPrepBases.getLength() > 1)
					logger.doInsentenceWarning(String.format(
							"\"%s\" in sentence \"%s\" has more than one \"%s\" without \"%s\".",
							xType, s.id, LvtbRoles.BASELEM, LvtbXTypes.XPREP));
					//warnOut.printf("\"%s\" in sentence \"%s\" has more than one \"%s\" without \"%s\".\n", xType, s.id, LvtbRoles.BASELEM, LvtbXTypes.XPREP);
				Node newRoot = NodeListUtils.getLastByOrd(noPrepBases);
				s.allAsDependents(newRoot, children, xType, xTag, null, logger);
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
					logger.doInsentenceWarning(String.format(
							"\"%s\" in sentence \"%s\" has no \"%s\" without \"%s\".",
							xType, s.id, LvtbRoles.BASELEM, LvtbXTypes.XSIMILE));
					//warnOut.printf("\"%s\" in sentence \"%s\" has no \"%s\" without \"%s\".\n", xType, s.id, LvtbRoles.BASELEM, LvtbXTypes.XSIMILE);
					noSimBases = children;
				}
				else if (noSimBases.getLength() > 1)
					logger.doInsentenceWarning(String.format(
							"\"%s\" in sentence \"%s\" has more than one \"%s\" without \"%s\".",
							xType, s.id, LvtbRoles.BASELEM, LvtbXTypes.XSIMILE));
					//warnOut.printf("\"%s\" in sentence \"%s\" has more than one \"%s\" without \"%s\".\n", xType, s.id, LvtbRoles.BASELEM, LvtbXTypes.XSIMILE);
				Node newRoot = NodeListUtils.getLastByOrd(noSimBases);
				s.allAsDependents(newRoot, children, xType, xTag, null, logger);
				return newRoot;
			}
		}
		logger.doInsentenceWarning(String.format(
				"Sentence \"%s\" has \"%s\" with incomplete xTag \"%s\".",
				s.id, xType, xTag));
		//warnOut.printf("Sentence \"%s\" has \"%s\" with incomplete xTag \"%s\".\n", s.id, xType, xTag);
		return missingTransform(xNode);
	}

	/**
	 * Transformation for xSimile construction. Grammaticalization feature in
	 * xTag is required for successful transformation.
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public Node xSimileToUD(Node xNode, String xType, String xTag)
	throws XPathExpressionException
	{
		if (xTag == null || xTag.isEmpty() || !xTag.matches("[^\\[]*\\[(sim|comp)[yn].*"))
		{
			logger.doInsentenceWarning(String.format(
					"Sentence \"%s\" has \"%s\" with incomplete xTag \"%s\".",
					s.id, xType, xTag));
			//warnOut.printf("Sentence \"%s\" has \"%s\" with incomplete xTag \"%s\".\n", s.id, xType, xTag);
			return missingTransform(xNode);
		}
		boolean gramzed = xTag.matches("[^\\[]*\\[(sim|comp)y.*");
		if (gramzed)
		{
			NodeList children = NodeUtils.getAllPMLChildren(xNode);
			Node newRoot = NodeListUtils.getFirstByDescOrd(children);
			// TODO maybe this role choice should be moved to PhrasePartDepLogic.phrasePartRoleToUD()
			s.allAsDependents(newRoot, children, xType, xTag,
					Tuple.of(UDv2Relations.FIXED, null), logger);
			return newRoot;
		}
		return s.allUnderLast(xNode, xType, xTag, LvtbRoles.BASELEM,
				null,null, true, logger);
	}

	/**
	 * Transformation for complex predicates. Predicates are expected to have
	 * only one basElem and either one mod or some aux'es. In case of mod,
	 * baseElem is attached as xcomp to it. Otherwise, noModXPredUD() are used.
	 * @return PML A-level node: root of the corresponding UD structure.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public Node xPredToUD(Node xNode, String xType, String xTag)
	throws XPathExpressionException
	{
		NodeList children = NodeUtils.getAllPMLChildren(xNode);
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
			logger.doInsentenceWarning(String.format(
					"xPred \"%s\" has a problematic tag \"%s\".",
					NodeFieldUtils.getId(NodeUtils.getPMLParent(xNode)), xTag));
			//warnOut.printf("xPred \"%s\" has a problematic tag \"%s\".\n", NodeFieldUtils.getId(NodeUtils.getPMLParent(xNode)), xTag);
		// Just put basElem under mod.
		return s.allUnderLast(xNode, xType, xTag,
				LvtbRoles.MOD, LvtbRoles.BASELEM, null, true, logger);
	}

	/**
	 * Specific helper function: implementation of aux/auxpass/cop logic, split
	 * out from xPred processing.
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
		Node basElem = NodeListUtils.getLastByDescOrd(basElems);
		if (basElem == null)
			throw new IllegalArgumentException(String.format(
					"\"%s\" in sentence \"%s\" has no \"basElem\"",
					xType, s.id));
		NodeList auxes = (NodeList) XPathEngine.get().evaluate(
				"./children/node[role='" + LvtbRoles.AUXVERB +"']", xNode, XPathConstants.NODESET);
		Node lastAux = NodeListUtils.getLastByDescOrd(auxes);
		if (lastAux == null)
			throw new IllegalArgumentException(String.format(
					"\"%s\" in sentence \"%s\" has neither \"auxVerb\" nor \"mod\"",
					xType, s.id));
		if (auxes.getLength() > 1) for (int i = 0; i < auxes.getLength(); i++)
		{
			String auxLemma = NodeFieldUtils.getLemma(lastAux);
			String auxRedLemma = NodeFieldUtils.getReductionLemma(lastAux, logger);
			if (auxRedLemma == null) auxRedLemma = ""; // So regexp matching would not fail.
			if (!auxLemma.matches("(ne)?(būt|tikt|tapt|kļūt)") &&
					!auxRedLemma.matches("(ne)?(būt|tikt|tapt|kļūt)"))
				logger.doInsentenceWarning(String.format(
						"xPred \"%s\" has multiple auxVerb one of which has lemma \"%s\".",
						NodeFieldUtils.getId(NodeUtils.getPMLParent(xNode)), auxLemma));
				//warnOut.printf("xPred \"%s\" has multiple auxVerb one of which has lemma \"%s\".\n", NodeFieldUtils.getId(NodeUtils.getPMLParent(xNode)), auxLemma);
		}

		String auxLemma = NodeFieldUtils.getLemma(lastAux);
		String auxRedLemma = NodeFieldUtils.getReductionLemma(lastAux, logger);
		if (auxRedLemma == null) auxRedLemma = ""; // So regexp matching would not fail.
		boolean ultimateAux = auxLemma.matches("(ne)?(būt|kļūt|tikt|tapt)") ||
				auxRedLemma.matches("(ne)?(būt|kļūt|tikt|tapt)");
		String basElemTag = NodeFieldUtils.getTag(basElem);

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
				logger.doInsentenceWarning(String.format(
						"xPred \"%s\" has a problematic tag \"%s\".",
						NodeFieldUtils.getId(NodeUtils.getPMLParent(xNode)), xTag));
				//warnOut.printf("xPred \"%s\" has a problematic tag \"%s\".\n", NodeFieldUtils.getId(NodeUtils.getPMLParent(xNode)), xTag);
		}
		else if (basElemTag != null)
			logger.doInsentenceWarning(String.format(
					"xPred \"%s\" has a problematic tag \"%s\".",
					NodeFieldUtils.getId(NodeUtils.getPMLParent(xNode)), xTag));
			//warnOut.printf("xPred \"%s\" has a problematic tag \"%s\".\n", NodeFieldUtils.getId(NodeUtils.getPMLParent(xNode)), xTag);

		Node newRoot = basElem;
		if (!ultimateAux) newRoot = lastAux;
		NodeList children = NodeUtils.getPMLNodeChildren(xNode);
		s.allAsDependents(newRoot, children, xType, xTag, null, logger);
		if (passive && ultimateAux)
			s.setLink(newRoot, lastAux, UDv2Relations.AUX_PASS,
					Tuple.of(UDv2Relations.AUX_PASS, null), true, true);
		if (nominal && ultimateAux)
			s.setLink(newRoot, lastAux, UDv2Relations.COP,
					Tuple.of(UDv2Relations.COP, null), true, true);
		return newRoot;
	}
}
