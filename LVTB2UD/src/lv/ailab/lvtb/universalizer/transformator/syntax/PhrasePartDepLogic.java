package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.*;
import lv.ailab.lvtb.universalizer.pml.utils.NodeFieldUtils;
import lv.ailab.lvtb.universalizer.pml.utils.NodeUtils;
import lv.ailab.lvtb.universalizer.transformator.Logger;
import lv.ailab.lvtb.universalizer.utils.Tuple;
import lv.ailab.lvtb.universalizer.utils.XPathEngine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

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
	 * @param phraseType	type of phrase in relation to which DEPREL must be
	 *                      chosen
	 * @param phraseTag		tag of phrase in relation to which DEPREL must be
	 *                      chosen (can/should be null for pmc nodes)
	 * @param logger 		where all the warnings goes
	 * @return	UD dependency role and enhanced depency role postfix, if such is
	 * 			needed.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static Tuple<UDv2Relations, String> phrasePartRoleToUD(
			Node aNode, String phraseType, String phraseTag, Logger logger)
	throws XPathExpressionException
	{
		String nodeId = NodeFieldUtils.getId(aNode);
		String lvtbRole = NodeFieldUtils.getRole(aNode);
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
				String subPmcType = XPathEngine.get().evaluate("./children/pmcinfo/pmctype", aNode);
				if (LvtbPmcTypes.ADDRESS.equals(subPmcType))
					return Tuple.of(UDv2Relations.VOCATIVE, null);
				if (LvtbPmcTypes.INTERJ.equals(subPmcType) || LvtbPmcTypes.PARTICLE.equals(subPmcType))
					return Tuple.of(UDv2Relations.DISCOURSE, null);
				String tag = NodeFieldUtils.getTag(aNode);
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
				String tag = NodeFieldUtils.getTag(aNode);
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
		if ((phraseType.equals(LvtbXTypes.PHRASELEM) ||
				phraseType.equals(LvtbXTypes.UNSTRUCT) ||
				phraseType.equals(LvtbPmcTypes.INTERJ) ||
				phraseType.equals(LvtbPmcTypes.PARTICLE)) &&
				lvtbRole.equals(LvtbRoles.BASELEM))
			return Tuple.of(UDv2Relations.FLAT, null);
		if (phraseType.equals(LvtbXTypes.NAMEDENT) &&
				lvtbRole.equals(LvtbRoles.BASELEM))
			return Tuple.of(UDv2Relations.FLAT_NAME, null);

		if (phraseType.equals(LvtbXTypes.SUBRANAL) &&
				lvtbRole.equals(LvtbRoles.BASELEM))
		{

			String subXType = XPathEngine.get().evaluate("./children/xinfo/xtype", aNode);
			String tag = NodeFieldUtils.getTag(aNode);

			if (LvtbXTypes.XPREP.equals(subXType) && subTag.startsWith("set"))
			{
				if (tag.matches("[np].*"))
				{
					NodeList preps = (NodeList)XPathEngine.get().evaluate(
							"./children/xinfo/children/node[role='" + LvtbRoles.PREP + "']",
							aNode, XPathConstants.NODESET);
					if (preps.getLength() > 1)
						logger.doInsentenceWarning(String.format(
								"\"%s\" with ID \"%s\" has multiple \"%s\".",
								subXType, NodeFieldUtils.getId(aNode), LvtbRoles.PREP));
						//warnOut.printf("\"%s\" with ID \"%s\" has multiple \"%s\"\n.", subXType, NodeFieldUtils.getId(aNode), LvtbRoles.PREP);
					String prepLemma = NodeFieldUtils.getLemma(preps.item(0));
					return Tuple.of(UDv2Relations.NMOD, prepLemma);
				}
				if (tag.matches("(mc|xn).*")) return Tuple.of(UDv2Relations.NUMMOD, null);
				if (tag.matches("(a|ya|xo|mo).*")) return Tuple.of(UDv2Relations.AMOD, null);
			}

			else if (LvtbXTypes.XSIMILE.equals(subXType) && subTag.matches("(ipv|sal).*"))
				return Tuple.of(UDv2Relations.DET, null);

			else if (tag.matches("p.*") && subTag.startsWith("vv"))
				return Tuple.of(UDv2Relations.COMPOUND, null);
			else if (tag.matches("p.*") && subTag.startsWith("ipv"))
				return Tuple.of(UDv2Relations.DET, null);
			else if (tag.matches("(mc|xn).*") && subTag.startsWith("skv"))
				return Tuple.of(UDv2Relations.NUMMOD, null);
			else if (tag.matches("q.*") && subTag.startsWith("part"))
				return Tuple.of(UDv2Relations.FLAT, null);
		}

		if (phraseType.equals(LvtbXTypes.XPREP) &&
				lvtbRole.equals(LvtbRoles.PREP))
			return Tuple.of(UDv2Relations.CASE, null);
		if (phraseType.equals(LvtbXTypes.XPARTICLE) &&
				lvtbRole.equals(LvtbRoles.NO))
			return Tuple.of(UDv2Relations.DISCOURSE, null);

		if (phraseType.equals(LvtbXTypes.XSIMILE) &&
				lvtbRole.equals(LvtbRoles.CONJ))
		{
			// For now let us assume, that conjunction can't be coordinated.
			// Then parent in this situation is the xSimile itself.
			Node firstAncestor = NodeUtils.getEffectiveAncestor(NodeUtils.getPMLParent(aNode)); // node/xinfo/pmcinfo/phraseinfo
			Node secondAncestor = NodeUtils.getEffectiveAncestor(firstAncestor); // node/xinfo/pmcinfo/phraseinfo
			String firstAncType = NodeFieldUtils.getAnyLabel(firstAncestor);
			String secondAncType = NodeFieldUtils.getAnyLabel(secondAncestor);

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
			
			Node effAncestor = secondAncestor;
			if (LvtbXTypes.XPARTICLE.equals(NodeFieldUtils.getAnyLabel(effAncestor)))
				effAncestor = NodeUtils.getEffectiveAncestor(effAncestor);
			String effAncLabel = NodeFieldUtils.getAnyLabel(effAncestor);

			// What to do ith this? Do we even need it?
			//if (LvtbRoles.SPC.equals(effAncLabel))
			if (LvtbPmcTypes.SPCPMC.equals(effAncLabel)
					|| LvtbPmcTypes.SPCPMC.equals(effAncLabel))
				return Tuple.of(UDv2Relations.MARK, null);

			// NO adv + xSimile instances in data! Is this old?
			//if (LvtbRoles.ADV.equals(effAncLabel))
			//	return Tuple.of(UDv2Relations.DISCOURSE, null);
		}

		if (phraseType.equals(LvtbXTypes.XPRED))
		{
			if (lvtbRole.equals(LvtbRoles.AUXVERB))
				return Tuple.of(UDv2Relations.AUX, null);
			if (lvtbRole.equals(LvtbRoles.BASELEM) ||
					lvtbRole.equals(LvtbRoles.MOD))
				return Tuple.of(UDv2Relations.XCOMP, null);
		}

		logger.doInsentenceWarning(String.format(
				"\"%s\" (%s) in \"%s\" has no UD label.",
				lvtbRole, nodeId, phraseType));
		//warnOut.printf("\"%s\" (%s) in \"%s\" has no UD label.\n", lvtbRole, nodeId, phraseType);
		return Tuple.of(UDv2Relations.DEP, null);
	}
}
