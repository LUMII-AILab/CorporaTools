package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.URelations;
import lv.ailab.lvtb.universalizer.pml.*;
import lv.ailab.lvtb.universalizer.transformator.XPathEngine;
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
	 * Generic relation between phrase part roles and UD DEPREL.
	 * Only for nodes that are not roots or subroots.
	 * NB! Case when a part of crdClauses maps to parataxis is handled in
	 * PhraseTransform class.
	 * Case when a part of unstruct basElem maps to foreign is handled in
	 * PhraseTransform class.
	 * Case when subrAnal is "vairāk" + xSimile is handled in
	 * PhraseTransform class.
	 * All specific cases with xPred are handled in PhraseTransform class.
	 * @param aNode			node for which the DEPREL must be obtained
	 * @param phraseType	type of phrase in relation to which DEPREL must be
	 *                      chosen
	 * @return contents for corresponding DEPREL field
	 * @throws XPathExpressionException
	 */
	public static URelations phrasePartRoleToUD(Node aNode, String phraseType)
	throws XPathExpressionException
	{
		String nodeId = Utils.getId(aNode);
		String lvtbRole = Utils.getRole(aNode);

		if ((phraseType.equals(LvtbPmcTypes.SENT) ||
				phraseType.equals(LvtbPmcTypes.UTTER) ||
				phraseType.equals(LvtbPmcTypes.SUBRCL)) ||
				phraseType.equals(LvtbPmcTypes.MAINCL) ||
				phraseType.equals(LvtbPmcTypes.INSPMC) ||
				phraseType.equals(LvtbPmcTypes.DIRSPPMC))
			if (lvtbRole.equals(LvtbRoles.NO))
			{
				String subPmcType = XPathEngine.get().evaluate("./children/pmcinfo/pmctype", aNode);
				if (LvtbPmcTypes.ADDRESS.equals(subPmcType)) return URelations.VOCATIVE;
				if (LvtbPmcTypes.INTERJ.equals(subPmcType) || LvtbPmcTypes.PARTICLE.equals(subPmcType))
					return URelations.DISCOURSE;
				String tag = Utils.getTag(aNode);
				if (tag != null && tag.matches("[qi].*")) return URelations.DISCOURSE;
			}

		if ((phraseType.equals(LvtbPmcTypes.SENT) || phraseType.equals(LvtbPmcTypes.UTTER)
				|| phraseType.equals(LvtbPmcTypes.SUBRCL)) || phraseType.equals(LvtbPmcTypes.INSPMC)
				|| phraseType.equals(LvtbPmcTypes.SPCPMC) || phraseType.equals(LvtbPmcTypes.PARTICLE)
				|| phraseType.equals(LvtbPmcTypes.DIRSPPMC) || phraseType.equals(LvtbPmcTypes.QUOT)
				|| phraseType.equals(LvtbPmcTypes.ADDRESS) || phraseType.equals(LvtbPmcTypes.INTERJ))
			if (lvtbRole.equals(LvtbRoles.PUNCT)) return URelations.PUNCT;

		if (phraseType.equals(LvtbPmcTypes.SENT) ||
				phraseType.equals(LvtbPmcTypes.UTTER) ||
				phraseType.equals(LvtbPmcTypes.MAINCL) ||
				phraseType.equals(LvtbPmcTypes.INSPMC) ||
				phraseType.equals(LvtbPmcTypes.DIRSPPMC))
			if (lvtbRole.equals(LvtbRoles.CONJ))
			{
				String tag = Utils.getTag(aNode);
				if (tag.matches("cc.*"))
					return URelations.CC;
				if (tag.matches("cs.*"));
					return URelations.MARK;
			}

		if (phraseType.equals(LvtbPmcTypes.SUBRCL))
			if (lvtbRole.equals(LvtbRoles.CONJ)) return URelations.MARK;


		if (phraseType.equals(LvtbCoordTypes.CRDPARTS) || phraseType.equals(LvtbCoordTypes.CRDCLAUSES))
		{
			if (lvtbRole.equals(LvtbRoles.CRDPART)) return URelations.CONJ; // Parataxis role is given in PhraseTransform class.
			if (lvtbRole.equals(LvtbRoles.CONJ)) return URelations.CC;
			if (lvtbRole.equals(LvtbRoles.PUNCT)) return URelations.PUNCT;
		}

		if (phraseType.equals(LvtbXTypes.XAPP) &&
				lvtbRole.equals(LvtbRoles.BASELEM)) return URelations.NMOD;
		if ((phraseType.equals(LvtbXTypes.XNUM) ||
				phraseType.equals(LvtbXTypes.COORDANAL)) &&
				lvtbRole.equals(LvtbRoles.BASELEM)) return URelations.COMPOUND;
		if ((phraseType.equals(LvtbXTypes.PHRASELEM) ||
				phraseType.equals(LvtbXTypes.UNSTRUCT)) &&
				lvtbRole.equals(LvtbRoles.BASELEM)) return URelations.MWE;
		if (phraseType.equals(LvtbXTypes.NAMEDENT) &&
				lvtbRole.equals(LvtbRoles.BASELEM)) return URelations.NAME;

		if (phraseType.equals(LvtbXTypes.SUBRANAL) &&
				lvtbRole.equals(LvtbRoles.BASELEM))
		{
			// NB: "vairāk kā/nekā X" and "tāds kā X" roles for "vairāk" and
			//     "tāds" are asigned in phrase transformator.
			return URelations.COMPOUND;
		}

		if (phraseType.equals(LvtbXTypes.XPREP) &&
				lvtbRole.equals(LvtbRoles.PREP)) return URelations.CASE;
		if (phraseType.equals(LvtbXTypes.XPARTICLE) &&
				lvtbRole.equals(LvtbRoles.NO))
		{
			if ("ne".equals(Utils.getLemma(aNode))) return URelations.NEG;
			return URelations.DISCOURSE;
		}

		if (phraseType.equals(LvtbXTypes.XSIMILE) &&
				lvtbRole.equals(LvtbRoles.CONJ))
		{
			// Because parent is the xSimile itself.
			Node firstAncestor = Utils.getPMLGrandParent(aNode); // node/xinfo/pmcinfo/phraseinfo
			Node secongAncestor = Utils.getPMLGreatGrandParent(aNode); // node/xinfo/pmcinfo/phraseinfo
			NodeList vSiblings = (NodeList)XPathEngine.get().evaluate(
					"./children/node[m.rf/form='vairāk']",
					secongAncestor, XPathConstants.NODESET);
			NodeList tSiblings = (NodeList)XPathEngine.get().evaluate(
					"./children/node[m.rf/lemma='tāds' or m.rf/lemma='tāda']",
					secongAncestor, XPathConstants.NODESET);
			String firstAncType = Utils.getAnyLabel(firstAncestor);
			String secondAncType = Utils.getAnyLabel(secongAncestor);

			if (LvtbRoles.SPC.equals(firstAncType))
				return URelations.MARK;
			if (LvtbRoles.BASELEM.equals(firstAncType))
			{
				if (LvtbPmcTypes.SPCPMC.equals(secondAncType) ||
						LvtbPmcTypes.INSPMC.equals(secondAncType))
					return URelations.MARK;
				if (LvtbXTypes.XPRED.equals(secondAncType) ||
						LvtbXTypes.SUBRANAL.equals(secondAncType) && tSiblings != null &&
						tSiblings.getLength() > 0)
					return URelations.DISCOURSE;
				if (LvtbXTypes.SUBRANAL.equals(secondAncType) && vSiblings != null &&
						vSiblings.getLength() > 0)
					return URelations.MWE;
			}
		}

		if (phraseType.equals(LvtbXTypes.XPRED))
		{
			if (lvtbRole.equals(LvtbRoles.AUXVERB)) return URelations.AUX;
			if (lvtbRole.equals(LvtbRoles.BASELEM) ||
					lvtbRole.equals(LvtbRoles.MOD)) return URelations.XCOMP;
		}

		System.err.printf("\"%s\" (%s) in \"%s\" has no UD label.\n",
				lvtbRole, nodeId, phraseType);
		return URelations.DEP;
	}
}
