package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.*;
import lv.ailab.lvtb.universalizer.pml.utils.PmlANodeListUtils;
import lv.ailab.lvtb.universalizer.pml.xmldom.XmlDomANode;
import lv.ailab.lvtb.universalizer.utils.Logger;
import lv.ailab.lvtb.universalizer.transformator.Sentence;
import lv.ailab.lvtb.universalizer.utils.Tuple;

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
	 */
	public PmlANode anyPhraseToUD(PmlANode phraseNode)
	{
		String phraseType = phraseNode.getPhraseType();
		String phraseTag = phraseNode.getAnyTag();

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
	 */
	public PmlANode missingTransform(PmlANode phraseNode)
	{
		//NodeList children = NodeUtils.getAllPMLChildren(phraseNode);
		List<PmlANode> children = phraseNode.getChildren();
		String phraseType = phraseNode.getPhraseType();
		String phraseTag = phraseNode.getAnyTag();
		PmlANode newRoot = PmlANodeListUtils.getFirstByDescOrd(children);
		s.allAsDependents(newRoot, children, phraseType, phraseTag, null, logger);
		return newRoot;
	}

	/**
	 * Transformation for PMC that can have either basElem or pred - all
	 * children goes below first pred, r below forst basElem, if there is no
	 * pred.
	 * @return PML A-level node: root of the corresponding UD structure.
	 */
	protected PmlANode sentencyToUD(PmlANode pmcNode, String pmcType)
	{
		//NodeList children = NodeUtils.getAllPMLChildren(pmcNode);
		List<PmlANode> children = pmcNode.getChildren();

		// Find the structure root.
		List<PmlANode> preds = pmcNode.getChildren(LvtbRoles.PRED);
		PmlANode newRoot = null;
		if (preds != null && preds.size() > 1)
			logger.doInsentenceWarning(String.format(
					"Sentence \"%s\" has more than one \"%s\" in \"%s\".",
					s.id, LvtbRoles.PRED, pmcType));
		if (preds == null || preds.isEmpty())
			preds = pmcNode.getChildren(LvtbRoles.BASELEM);
		newRoot = PmlANodeListUtils.getFirstByDescOrd(preds);

		if (newRoot == null)
		{
			logger.doInsentenceWarning(String.format(
					"Sentence \"%s\" has no \"%s\", \"%s\" in \"%s\".",
					s.id, LvtbRoles.PRED, LvtbRoles.BASELEM, pmcType));
			newRoot = PmlANodeListUtils.getFirstByDescOrd(children);
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
	 */
	protected PmlANode utterToUD(PmlANode pmcNode, String pmcType)
	{
		//NodeList children = NodeUtils.getAllPMLChildren(pmcNode);
		List<PmlANode> children = pmcNode.getChildren();

		// Find the structure root.
		List<PmlANode> basElems = pmcNode.getChildren(LvtbRoles.BASELEM);
		PmlANode newRoot = null;
		if (basElems != null && basElems.size() > 0)
			newRoot = PmlANodeListUtils.getFirstByDescOrd(basElems);
		if (newRoot == null)
		{
			logger.doInsentenceWarning(String.format(
					"Sentence \"%s\" has no \"%s\" in \"%s\".",
					s.id, LvtbRoles.BASELEM, pmcType));
			newRoot = PmlANodeListUtils.getFirstByDescOrd(children);
		}
		if (newRoot == null)
			throw new IllegalArgumentException(String.format(
					"Sentence \"%s\" seems to be empty", s.id));

		if (basElems!= null && basElems.size() > 1 && children.size() > basElems.size())
		{
			ArrayList<PmlANode> sortedChildren = PmlANodeListUtils.asOrderedList(children);
			ArrayList<PmlANode> rootChildren = new ArrayList<>();
			// If utter starts with punct, they are going to be root children.
			while (sortedChildren.size() > 0)
			{
				String role = sortedChildren.get(0).getRole();
				if (role.equals(LvtbRoles.PUNCT))
					rootChildren.add(sortedChildren.remove(0));
				else break;
			}
			// First "clause" until punctuation is going to be root children.
			while (sortedChildren.size() > 0)
			{
				String role = sortedChildren.get(0).getRole();
				if (!role.equals(LvtbRoles.PUNCT))
					rootChildren.add(sortedChildren.remove(0));
				else break;
			}
			// Last punctuation aslo is going to be root children.
			LinkedList<PmlANode> lastPunct = new LinkedList<>();
			while (sortedChildren.size() > 0)
			{
				String role = sortedChildren.get(sortedChildren.size()-1).getRole();
				if (role.equals(LvtbRoles.PUNCT))
					lastPunct.push(sortedChildren.remove(sortedChildren.size()-1));
				else break;
			}
			rootChildren.addAll(lastPunct);
			s.allAsDependents(newRoot, rootChildren, pmcType, null, null, logger);

			// now let's process what is left
			Token rootTok = s.pmlaToConll.get(newRoot.getId());
			while (sortedChildren.size() > 0)
			{
				ArrayList<PmlANode> nextPart = new ArrayList<>();
				PmlANode subroot = null;

				// find next stop
				while (sortedChildren.size() > 0)
				{
					String role = sortedChildren.get(0).getRole();
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
	 */
	public PmlANode crdPartsToUD(PmlANode coordNode, String coordType, String coordTag)
	{
		//NodeList children = NodeUtils.getAllPMLChildren(coordNode);
		List<PmlANode> children = coordNode.getChildren();
		return coordPartsChildListToUD(PmlANodeListUtils.asOrderedList(children), coordType, coordTag, logger);
	}

	/**
	 * Transformation for coordinated clauses - part after semicolon is
	 * parataxis, otherwise the same as coordinated parts.
	 * @return PML A-level node: root of the corresponding UD structure.
	 */
	public PmlANode crdClausesToUD (PmlANode coordNode, String coordType, String coordTag)
	{
		// Get all the children.
		//NodeList children = NodeUtils.getAllPMLChildren(coordNode);
		List<PmlANode> children = coordNode.getChildren();
		// Check if there are any semicolons.
		List<PmlANode> semicolons = new ArrayList<>();
		for (PmlANode child : children)
		{
			PmlMNode mNode = child.getM();
			if (mNode != null && ";".equals(mNode.getLemma()))
				semicolons.add(child);
		}

		// No semicolons => process as ordinary coordination.
		if (semicolons.size() < 1)
			return coordPartsChildListToUD(children, coordType, coordTag, logger);

		// If semicolon(s) is (are) present, split on semicolon and then process
		// each part as ordinary coordination.
		ArrayList<PmlANode> sortedSemicolons = PmlANodeListUtils.asOrderedList(semicolons);
		ArrayList<PmlANode> sortedChildren = PmlANodeListUtils.asOrderedList(children);
		int semicOrd = sortedSemicolons.get(0).getOrd();
		PmlANode newRoot = coordPartsChildListToUD(
				PmlANodeListUtils.ordSplice(sortedChildren, 0, semicOrd), coordType, coordTag, logger);
		for (int i = 1; i < sortedSemicolons.size(); i++)
		{
			int nextSemicOrd = sortedSemicolons.get(i).getOrd();
			PmlANode newSubroot = coordPartsChildListToUD(
					PmlANodeListUtils.ordSplice(sortedChildren, semicOrd, nextSemicOrd), coordType, coordTag, logger);
			s.setLink(newRoot, newSubroot, UDv2Relations.PARATAXIS,
					Tuple.of(UDv2Relations.PARATAXIS, null), true, true);
			semicOrd = nextSemicOrd;
		}
		// last
		PmlANode newSubroot = coordPartsChildListToUD(
				PmlANodeListUtils.ordSplice(sortedChildren, semicOrd, Integer.MAX_VALUE), coordType, coordTag, logger);
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
	 */
	protected PmlANode coordPartsChildListToUD(
			List<PmlANode> sortedNodes, String coordType, String coordTag, Logger logger)
	{
		// Find the structure root.
		PmlANode newRoot = null;
		PmlANode lastSubroot = null;
		ArrayList<PmlANode> postponed = new ArrayList<>();
		// First process all nodes that are followed by a crdPart node.
		for (PmlANode n : sortedNodes)
		{
			if (LvtbRoles.CRDPART.equals(n.getRole()))
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
	 */
	public PmlANode unstructToUd(PmlANode xNode, String xType, String xTag)
	{
		//NodeList children = NodeUtils.getAllPMLChildren(xNode);
		List<PmlANode> children = xNode.getChildren();
		List<PmlANode> foreigns = new ArrayList<>();
		List<PmlANode> punct = new ArrayList<>();
		for (PmlANode child : children)
		{
			PmlMNode morpho = child.getM();
			if (morpho == null) continue;
			String morphotag = morpho.getTag();
			if ("xf".equals(morphotag)) foreigns.add(child);
			else if (morphotag != null && morphotag.startsWith("z")) punct.add(child);
		}

		if (children.size() == foreigns.size()
			|| foreigns.size() > 0 && children.size() == foreigns.size() + punct.size())
			return s.allUnderFirst(xNode, xType, xTag, LvtbRoles.BASELEM,
					Tuple.of(UDv2Relations.FLAT_FOREIGN, null), false, logger);
		else return s.allUnderFirst(xNode, xType, xTag, LvtbRoles.BASELEM,
				null, false, logger);
	}

	/**
	 * Transformation for subrAnal, based on subtag.
	 * @return PML A-level node: root of the corresponding UD structure.
	 */
	public PmlANode subrAnalToUD(PmlANode xNode, String xType, String xTag)
	{
		//NodeList children = NodeUtils.getAllPMLChildren(xNode);
		List<PmlANode> children = xNode.getChildren();
		if (xTag == null || xTag.isEmpty())
		{
			logger.doInsentenceWarning(String.format(
					"Sentence \"%s\" has \"%s\" with incomplete xTag \"%s\".",
					s.id, xType, xTag));
			return missingTransform(xNode);
		}
		Matcher subTypeMatcher = Pattern.compile("[^\\[]*\\[(vv|ipv|skv|set|sal|part).*")
				.matcher(xTag);
		if (!subTypeMatcher.matches())
		{
			logger.doInsentenceWarning(String.format(
					"Sentence \"%s\" has \"%s\" with incomplete xTag \"%s\".",
					s.id, xType, xTag));
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
				List<PmlANode> basElems = xNode.getChildren(LvtbRoles.BASELEM);
				List<PmlANode> adjs = new ArrayList<>();
				for (PmlANode basElem : basElems)
				{
					String tag = basElem.getAnyTag();
					if (tag.matches("(a|ya|v..pd).*")) adjs.add(basElem);
				}
				if (adjs.size() < 1)
				{
					logger.doInsentenceWarning(String.format(
							"\"%s\" in sentence \"%s\" has no adjective \"%s\".",
							xType, s.id, LvtbRoles.BASELEM));
					adjs = children;
				}
				else if (adjs.size() > 1)
					logger.doInsentenceWarning(String.format(
							"\"%s\" in sentence \"%s\" has more than one adjective \"%s\".",
							xType, s.id, LvtbRoles.BASELEM));
					//warnOut.printf("\"%s\" in sentence \"%s\" has more than one adjective \"%s\".\n", xType, s.id, LvtbRoles.BASELEM);
				PmlANode newRoot = PmlANodeListUtils.getLastByOrd(adjs);
				s.allAsDependents(newRoot, children, xType, xTag, null, logger);
				return newRoot;
			}
			case "skv" :
			{
				List<PmlANode> basElems = xNode.getChildren(LvtbRoles.BASELEM);
				List<PmlANode> prons = new ArrayList<>();
				for (PmlANode basElem : basElems)
				{
					String tag = basElem.getAnyTag();
					if (tag.matches("p.*")) prons.add(basElem);
				}
				if (prons.size() < 1)
				{
					logger.doInsentenceWarning(String.format(
							"\"%s\" in sentence \"%s\" has no pronominal \"%s\".",
							xType, s.id, LvtbRoles.BASELEM));
					prons = children;
				}
				else if (prons.size() > 1)
					logger.doInsentenceWarning(String.format(
							"\"%s\" in sentence \"%s\" has more than one pronominal \"%s\".",
							xType, s.id, LvtbRoles.BASELEM));
				PmlANode newRoot = PmlANodeListUtils.getFirstByOrd(prons);
				s.allAsDependents(newRoot, children, xType, xTag,null, logger);
				return newRoot;
			}
			case "set" :
			{
				List<PmlANode> basElems = xNode.getChildren(LvtbRoles.BASELEM);
				List<PmlANode> noPrepBases = new ArrayList<>();
				for (PmlANode basElem : basElems)
				{
					PmlANode phrase = basElem.getPhraseNode();
					if (phrase == null || phrase.getNodeType() != PmlANode.Type.X
							&& !LvtbXTypes.XPREP.equals(phrase.getPhraseType()))
						noPrepBases.add(basElem);
				}

				if (noPrepBases.size() < 1)
				{
					logger.doInsentenceWarning(String.format(
							"\"%s\" in sentence \"%s\" has no \"%s\" without \"%s\".",
							xType, s.id, LvtbRoles.BASELEM, LvtbXTypes.XPREP));
					//warnOut.printf("\"%s\" in sentence \"%s\" has no \"%s\" without \"%s\".\n", xType, s.id, LvtbRoles.BASELEM, LvtbXTypes.XPREP);
					noPrepBases = children;
				}
				else if (noPrepBases.size() > 1)
					logger.doInsentenceWarning(String.format(
							"\"%s\" in sentence \"%s\" has more than one \"%s\" without \"%s\".",
							xType, s.id, LvtbRoles.BASELEM, LvtbXTypes.XPREP));
					//warnOut.printf("\"%s\" in sentence \"%s\" has more than one \"%s\" without \"%s\".\n", xType, s.id, LvtbRoles.BASELEM, LvtbXTypes.XPREP);
				PmlANode newRoot = PmlANodeListUtils.getLastByOrd(noPrepBases);
				s.allAsDependents(newRoot, children, xType, xTag, null, logger);
				return newRoot;
			}
			case "sal" :
			{
				List<PmlANode> basElems = xNode.getChildren(LvtbRoles.BASELEM);
				List<PmlANode> noSimBases = new ArrayList<>();
				for (PmlANode basElem : basElems)
				{
					PmlANode phrase = basElem.getPhraseNode();
					if (phrase == null || phrase.getNodeType() != PmlANode.Type.X
							&& !LvtbXTypes.XSIMILE.equals(phrase.getPhraseType()))
						noSimBases.add(basElem);
				}

				if (noSimBases.size() < 1)
				{
					logger.doInsentenceWarning(String.format(
							"\"%s\" in sentence \"%s\" has no \"%s\" without \"%s\".",
							xType, s.id, LvtbRoles.BASELEM, LvtbXTypes.XSIMILE));
					noSimBases = children;
				}
				else if (noSimBases.size() > 1)
					logger.doInsentenceWarning(String.format(
							"\"%s\" in sentence \"%s\" has more than one \"%s\" without \"%s\".",
							xType, s.id, LvtbRoles.BASELEM, LvtbXTypes.XSIMILE));
				PmlANode newRoot = PmlANodeListUtils.getLastByOrd(noSimBases);
				s.allAsDependents(newRoot, children, xType, xTag, null, logger);
				return newRoot;
			}
		}
		logger.doInsentenceWarning(String.format(
				"Sentence \"%s\" has \"%s\" with incomplete xTag \"%s\".",
				s.id, xType, xTag));
		return missingTransform(xNode);
	}

	/**
	 * Transformation for xSimile construction. Grammaticalization feature in
	 * xTag is required for successful transformation.
	 * @return PML A-level node: root of the corresponding UD structure.
	 */
	public PmlANode xSimileToUD(PmlANode xNode, String xType, String xTag)
	{
		if (xTag == null || xTag.isEmpty() || !xTag.matches("[^\\[]*\\[(sim|comp)[yn].*"))
		{
			logger.doInsentenceWarning(String.format(
					"Sentence \"%s\" has \"%s\" with incomplete xTag \"%s\".",
					s.id, xType, xTag));
			return missingTransform(xNode);
		}
		boolean gramzed = xTag.matches("[^\\[]*\\[(sim|comp)y.*");
		if (gramzed)
		{
			//NodeList children = NodeUtils.getAllPMLChildren(xNode);
			List<PmlANode> children = xNode.getChildren();
			PmlANode newRoot = PmlANodeListUtils.getFirstByDescOrd(children);
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
	 */
	public PmlANode xPredToUD(PmlANode xNode, String xType, String xTag)
	{
		//NodeList children = NodeUtils.getAllPMLChildren(xNode);
		List<PmlANode> children = xNode.getChildren();
		if (children.size() == 1) return children.get(0);
		List<PmlANode> mods = xNode.getChildren(LvtbRoles.MOD);
		if (mods == null || mods.size() < 1)
			return noModXPredToUD(xNode, xType, xTag);
		else return modXPredToUD(xNode, xType, xTag);
	}

	/**
	 * Specific helper function: implementation of modal predication logic,
	 * split out from xPred processing.
	 * @return	PML A-level node: root of the corresponding UD structure.
	 */
	protected PmlANode modXPredToUD(
			PmlANode xNode, String xType, String xTag)
	{
		// Check if the tag is appropriate.
		String subtag = (xTag != null && xTag.contains("[") ?
				xTag.substring(xTag.indexOf("[") + 1) : "");
		if (!subtag.startsWith("modal") && !subtag.startsWith("expr")
				&& !subtag.startsWith("phase"))
			logger.doInsentenceWarning(String.format(
					"xPred \"%s\" has a problematic tag \"%s\".",
					xNode.getParent().getId(), xTag));
		// Just put basElem under mod.
		return s.allUnderLast(xNode, xType, xTag,
				LvtbRoles.MOD, LvtbRoles.BASELEM, null, true, logger);
	}

	/**
	 * Specific helper function: implementation of aux/auxpass/cop logic, split
	 * out from xPred processing.
	 * @return	PML A-level node: root of the corresponding UD structure.
	 */
	protected PmlANode noModXPredToUD(
			PmlANode xNode, String xType, String xTag)
	{
		// Get basElems and warn if there is none.
		List<PmlANode> basElems = xNode.getChildren(LvtbRoles.BASELEM);
		PmlANode basElem = PmlANodeListUtils.getLastByDescOrd(basElems);
		if (basElem == null)
			throw new IllegalArgumentException(String.format(
					"\"%s\" in sentence \"%s\" has no \"basElem\"",
					xType, s.id));
		List<PmlANode> auxes = xNode.getChildren(LvtbRoles.AUXVERB);
		PmlANode lastAux = PmlANodeListUtils.getLastByDescOrd(auxes);
		if (lastAux == null)
			throw new IllegalArgumentException(String.format(
					"\"%s\" in sentence \"%s\" has neither \"auxVerb\" nor \"mod\"",
					xType, s.id));
		if (auxes.size() > 1) for (int i = 0; i < auxes.size(); i++)
		{
			String auxLemma = lastAux.getM().getLemma();
			String auxRedLemma = lastAux.getReductionLemma(logger);
			if (auxRedLemma == null) auxRedLemma = ""; // So regexp matching would not fail.
			if (!auxLemma.matches("(ne)?(būt|tikt|tapt|kļūt)") &&
					!auxRedLemma.matches("(ne)?(būt|tikt|tapt|kļūt)"))
				logger.doInsentenceWarning(String.format(
						"xPred \"%s\" has multiple auxVerb one of which has lemma \"%s\".",
						xNode.getParent().getId(), auxLemma));
		}

		PmlMNode lastAuxM = lastAux.getM();
		String auxLemma = lastAuxM == null ? null : lastAuxM.getLemma();
		String auxRedLemma = lastAux.getReductionLemma(logger);
		if (auxRedLemma == null) auxRedLemma = ""; // So regexp matching would not fail.
		boolean ultimateAux =
				auxLemma != null && auxLemma.matches("(ne)?(būt|kļūt|tikt|tapt)") ||
				auxRedLemma != null && auxRedLemma.matches("(ne)?(būt|kļūt|tikt|tapt)");
		String basElemTag = basElem.getAnyTag();

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
						xNode.getParent().getId(), xTag));
		}
		else if (basElemTag != null)
			logger.doInsentenceWarning(String.format(
					"xPred \"%s\" has a problematic tag \"%s\".",
					xNode.getParent().getId(), xTag));

		PmlANode newRoot = basElem;
		if (!ultimateAux) newRoot = lastAux;
		List<PmlANode> children = xNode.getChildren();
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
