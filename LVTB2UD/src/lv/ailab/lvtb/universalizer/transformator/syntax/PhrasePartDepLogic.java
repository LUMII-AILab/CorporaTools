package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.*;
import lv.ailab.lvtb.universalizer.transformator.StandardLogger;
import lv.ailab.lvtb.universalizer.utils.Tuple;

/**
 * Relation between phrase part names used in LVTB and dependency labeling used
 * in UD (regular only; irregularity processing and for phrase root labeling is
 * done in PhraseTransform).
 * Created on 2016-04-26.
 *
 * @author Lauma
 */
public class PhrasePartDepLogic
{

	/**
	 * Generic relation between phrase part roles and UD DEPREL and/or enhanced
	 * dependencies.
	 * Only for nodes that are not roots or subroots.
	 * NB! Case when a part of crdClauses maps to parataxis is handled in
	 * PhraseTransform class.
	 * Case when a part of unstruct basElem maps to foreign is handled in
	 * PhraseTransform class.
	 * All specific cases with xPred are handled in PhraseTransform class.
	 * @param aNode			node for which the DEPREL must be obtained
	 * @param phraseNode	phrase in relation to which DEPREL must be
	 *                      chosen (also used, if information about phrase's
	 *                      ancestors is needed)
	 * @return	UD dependency role and enhanced depency role postfix, if such is
	 * 			needed.
	 */
	public static Tuple<UDv2Relations, String> phrasePartRoleToUD(
			PmlANode aNode, PmlANode phraseNode)
	{
		String nodeId = aNode.getId();
		String lvtbRole = aNode.getRole();
		String phraseType = phraseNode.getPhraseType();
		String phraseTag = phraseNode.getPhraseTag();
		String subTag = phraseTag != null && phraseTag.contains("[")
				? phraseTag.substring(phraseTag.indexOf("[") + 1)
				: "";

		if ((phraseType.equals(LvtbPmcTypes.SENT) ||
				phraseType.equals(LvtbPmcTypes.UTTER) ||
				phraseType.equals(LvtbPmcTypes.SUBRCL)) ||
				phraseType.equals(LvtbPmcTypes.MAINCL) ||
				phraseType.equals(LvtbPmcTypes.INSPMC) ||
				phraseType.equals(LvtbPmcTypes.DIRSPPMC))
			if (lvtbRole.equals(LvtbRoles.NO))
			{
				//String subPmcType = XPathEngine.get().evaluate("./children/pmcinfo/pmctype", aNode);
				PmlANode subPhrase = aNode.getPhraseNode();
				String subPmcType =  subPhrase == null ? null
						: aNode.getPhraseNode().getAnyLabel();
				if (LvtbPmcTypes.ADDRESS.equals(subPmcType))
					return Tuple.of(UDv2Relations.VOCATIVE, null);
				if (LvtbPmcTypes.INTERJ.equals(subPmcType) || LvtbPmcTypes.PARTICLE.equals(subPmcType))
					return Tuple.of(UDv2Relations.DISCOURSE, null);
				String tag = aNode.getAnyTag();
				if (tag != null && tag.matches("[qi].*"))
					return Tuple.of(UDv2Relations.DISCOURSE, null);
				if (tag != null && tag.matches("n...v.*"))
					return Tuple.of(UDv2Relations.VOCATIVE, null);
			}

		if (phraseType.equals(LvtbPmcTypes.SENT) || phraseType.equals(LvtbPmcTypes.UTTER)
				|| phraseType.equals(LvtbPmcTypes.SUBRCL) || phraseType.equals(LvtbPmcTypes.MAINCL)
				|| phraseType.equals(LvtbPmcTypes.INSPMC) || phraseType.equals(LvtbPmcTypes.SPCPMC)
				|| phraseType.equals(LvtbPmcTypes.DIRSPPMC) || phraseType.equals(LvtbPmcTypes.QUOT)
				|| phraseType.equals(LvtbPmcTypes.ADDRESS) || phraseType.equals(LvtbPmcTypes.INTERJ)
				|| phraseType.equals(LvtbPmcTypes.PARTICLE))
			if (lvtbRole.equals(LvtbRoles.PUNCT))
				return Tuple.of(UDv2Relations.PUNCT, null);

		if (phraseType.equals(LvtbPmcTypes.SENT) ||
				phraseType.equals(LvtbPmcTypes.UTTER) ||
				phraseType.equals(LvtbPmcTypes.MAINCL) ||
				phraseType.equals(LvtbPmcTypes.INSPMC) ||
				phraseType.equals(LvtbPmcTypes.DIRSPPMC))
			if (lvtbRole.equals(LvtbRoles.CONJ))
			{
				String tag = aNode.getAnyTag();
				if (tag.matches("cc.*"))
					return Tuple.of(UDv2Relations.CC, null);
				if (tag.matches("cs.*"))
					return Tuple.of(UDv2Relations.MARK, null);
			}

		if (phraseType.equals(LvtbPmcTypes.SUBRCL))
			if (lvtbRole.equals(LvtbRoles.CONJ))
				return Tuple.of(UDv2Relations.MARK, null);


		if (phraseType.equals(LvtbCoordTypes.CRDPARTS) || phraseType.equals(LvtbCoordTypes.CRDCLAUSES))
		{
			if (lvtbRole.equals(LvtbRoles.CRDPART))
				return Tuple.of(UDv2Relations.CONJ, null); // Parataxis role is given in PhraseTransform class.
			if (lvtbRole.equals(LvtbRoles.CONJ))
				return Tuple.of(UDv2Relations.CC, null);
			if (lvtbRole.equals(LvtbRoles.PUNCT))
				return Tuple.of(UDv2Relations.PUNCT, null);
		}

		if (phraseType.equals(LvtbXTypes.XAPP) &&
				lvtbRole.equals(LvtbRoles.BASELEM))
			return Tuple.of(UDv2Relations.NMOD, null);
		if ((phraseType.equals(LvtbXTypes.XNUM) ||
				phraseType.equals(LvtbXTypes.COORDANAL)) &&
				lvtbRole.equals(LvtbRoles.BASELEM))
			return Tuple.of(UDv2Relations.COMPOUND, null);
		if (phraseType.equals(LvtbXTypes.XFUNCTOR) &&
				lvtbRole.equals(LvtbRoles.BASELEM))
			return Tuple.of(UDv2Relations.FIXED, null);
		if ((phraseType.equals(LvtbXTypes.PHRASELEM) ||
				phraseType.equals(LvtbPmcTypes.INTERJ) ||
				phraseType.equals(LvtbPmcTypes.PARTICLE)) &&
				lvtbRole.equals(LvtbRoles.BASELEM))
			return Tuple.of(UDv2Relations.FLAT, null);

		if (phraseType.equals(LvtbXTypes.UNSTRUCT) &&
				lvtbRole.equals(LvtbRoles.BASELEM))
		{
			if (aNode.getAnyTag().matches("z.*"))
				return Tuple.of(UDv2Relations.PUNCT, null);
			if (phraseTag != null && phraseTag.matches("xf.*"))
				return Tuple.of(UDv2Relations.FLAT_FOREIGN, null);
			else return Tuple.of(UDv2Relations.FLAT, null);
		}
		if (phraseType.equals(LvtbXTypes.NAMEDENT) &&
				lvtbRole.equals(LvtbRoles.BASELEM))
		{
			if (aNode.getAnyTag().matches("z.*"))
				return Tuple.of(UDv2Relations.PUNCT, null);
			return Tuple.of(UDv2Relations.FLAT_NAME, null);
		}

		if (phraseType.equals(LvtbXTypes.SUBRANAL) &&
				lvtbRole.equals(LvtbRoles.BASELEM))
		{

			PmlANode subPhrase = aNode.getPhraseNode();
			String subXType =  subPhrase == null ? null
					: aNode.getPhraseNode().getAnyLabel();
			String tag = aNode.getAnyTag();

			if (LvtbXTypes.XPREP.equals(subXType) && subTag.startsWith("set"))
			{
				if (tag.matches("[np].*"))
				{
					/*List<? extends  PmlANode> preps =
							subPhrase.getChildren(LvtbRoles.PREP);
					if (preps.size() > 1)
						StandardLogger.l.doInsentenceWarning(String.format(
								"\"%s\" with ID \"%s\" has multiple \"%s\".",
								subXType, aNode.getId(), LvtbRoles.PREP));
					String prepLemma = preps.get(0).getM().getLemma();*/
					String prepLemma = Helper.getXSimileConjOrXPrepPrepLemma(subPhrase, LvtbRoles.PREP);
					return Tuple.of(UDv2Relations.NMOD, prepLemma);
				}
				if (tag.matches("(mc|xn).*")) return Tuple.of(UDv2Relations.NUMMOD, null);
				if (tag.matches("(a|ya|xo|mo|v..pd).*")) return Tuple.of(UDv2Relations.AMOD, null);
			}

			else if (LvtbXTypes.XSIMILE.equals(subXType) && subTag.matches("(ipv|sal).*"))
				return Tuple.of(UDv2Relations.DET, null);

			else if (tag.matches("p.*") && subTag.startsWith("vv"))
				return Tuple.of(UDv2Relations.COMPOUND, null);
			else if (tag.matches("p.*") && subTag.startsWith("ipv"))
				return Tuple.of(UDv2Relations.DET, null);
			else if (tag.matches("p.*") && subTag.startsWith("skv"))
				return Tuple.of(UDv2Relations.DET, null);
			else if (tag.matches("(mc|xn).*") && subTag.startsWith("skv"))
				return Tuple.of(UDv2Relations.NUMMOD, null);
			//else if (tag.matches("q.*") && subTag.startsWith("part"))
			//	return Tuple.of(UDv2Relations.FLAT, null);
		}

		if (phraseType.equals(LvtbXTypes.XPREP) &&
				lvtbRole.equals(LvtbRoles.PREP))
			return Tuple.of(UDv2Relations.CASE, null);
		if (phraseType.equals(LvtbXTypes.XPARTICLE) &&
				lvtbRole.equals(LvtbRoles.NO))
			return Tuple.of(UDv2Relations.DISCOURSE, null);

		if (phraseType.equals(LvtbXTypes.XSIMILE) &&
				subTag.matches("(sim|comp)y.*"))
			return Tuple.of(UDv2Relations.FIXED, null);

		if (phraseType.equals(LvtbXTypes.XSIMILE) &&
				lvtbRole.equals(LvtbRoles.CONJ))
		{
			// For now let us assume, that conjunction can't be coordinated.
			// Then parent in this situation is the xSimile itself.
			// TODO FIXME what happens if coordination somewhre?
			PmlANode firstAncestor = phraseNode.getEffectiveAncestor(); // node/xinfo/pmcinfo/phraseinfo
			PmlANode secondAncestor = firstAncestor.getEffectiveAncestor(); // node/xinfo/pmcinfo/phraseinfo
			String firstAncType = firstAncestor.getAnyLabel();
			String secondAncType = secondAncestor.getAnyLabel();

			// Check the specific roles
			if (LvtbRoles.BASELEM.equals(firstAncType))
			{
				if (LvtbPmcTypes.SPCPMC.equals(secondAncType) ||
						LvtbPmcTypes.INSPMC.equals(secondAncType))
					return Tuple.of(UDv2Relations.MARK, null);
				if (LvtbXTypes.XPRED.equals(secondAncType) || LvtbPmcTypes.UTTER.equals(secondAncType))
					return Tuple.of(UDv2Relations.DISCOURSE, null);
			}
			// In generic SPC (without PMC) case use case.
			if (LvtbRoles.SPC.equals(firstAncType))
				return Tuple.of(UDv2Relations.CASE, null);

			// NO adv + xSimile instances in data! Is this old?
			//if (LvtbRoles.ADV.equals(firstAncType))
			//	return Tuple.of(UDv2Relations.DISCOURSE, null);
			
			PmlANode effAncestor = secondAncestor;
			if (LvtbXTypes.XPARTICLE.equals(effAncestor.getAnyLabel()))
				effAncestor = effAncestor.getEffectiveAncestor();
			String effAncLabel = effAncestor.getAnyLabel();

			// What to do ith this? Do we even need it?
			//if (LvtbRoles.SPC.equals(effAncLabel))
			if (LvtbPmcTypes.SPCPMC.equals(effAncLabel))
				return Tuple.of(UDv2Relations.MARK, null);

			// NO adv + xSimile instances in data! Is this old?
			//if (LvtbRoles.ADV.equals(effAncLabel))
			//	return Tuple.of(UDv2Relations.DISCOURSE, null);
		}

		if (phraseType.equals(LvtbXTypes.XPRED))
		{
			if (lvtbRole.equals(LvtbRoles.AUXVERB))
			{
				// Determine properties of this AUXVERB: is it standard aux or no,
				// is in pasive construction and is it in nominal construction.
				PmlMNode morfo = aNode.getM();
				String lemma = morfo == null ? null : morfo.getLemma();
				String redLemma = aNode.getReductionLemma();
				boolean ultimateAux = lemma != null && lemma.matches("(ne)?(būt|kļūt|tikt|tapt)") ||
						redLemma != null && redLemma.matches("(ne)?(būt|kļūt|tikt|tapt)");
				boolean nominal = false;
				boolean passive = false;
				if (subTag.startsWith("pass"))
					passive = true;
				else if (subTag.startsWith("subst") || subTag.startsWith("adj") ||
						subTag.startsWith("pronom") || subTag.startsWith("adv") ||
						subTag.startsWith("inf") || subTag.startsWith("num"))
						nominal = true;
				else if (!subTag.startsWith("act"))
					StandardLogger.l.doInsentenceWarning(String.format(
							"xPred \"%s\" has a problematic tag \"%s\".",
							phraseNode.getParent().getId(), phraseTag));

				if (passive && ultimateAux)
					return Tuple.of(UDv2Relations.AUX_PASS, null);
				else if (nominal && ultimateAux)
					return Tuple.of(UDv2Relations.COP, null);
				else if (ultimateAux) return Tuple.of(UDv2Relations.AUX, null);
			}

			if (lvtbRole.equals(LvtbRoles.BASELEM)) //|| lvtbRole.equals(LvtbRoles.MOD))
				return Tuple.of(UDv2Relations.XCOMP, null);
		}

		if (lvtbRole.equals(LvtbRoles.ELLIPSIS_TOKEN))
		{
			String tag = aNode.getAnyTag();
			if (tag.matches("z.*")) return Tuple.of(UDv2Relations.PUNCT, null);
		}

		StandardLogger.l.doInsentenceWarning(String.format(
				"\"%s\" (%s) in \"%s\" has no UD label.",
				lvtbRole, nodeId, phraseType));
		//warnOut.printf("\"%s\" (%s) in \"%s\" has no UD label.\n", lvtbRole, nodeId, phraseType);
		return Tuple.of(UDv2Relations.DEP, null);
	}
}
