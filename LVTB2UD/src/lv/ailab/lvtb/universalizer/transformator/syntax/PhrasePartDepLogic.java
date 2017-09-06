package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.*;
import lv.ailab.lvtb.universalizer.util.XPathEngine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.PrintWriter;

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
	 * All specific cases with xPred are handled in PhraseTransform class.
	 * @param aNode			node for which the DEPREL must be obtained
	 * @param phraseType	type of phrase in relation to which DEPREL must be
	 *                      chosen
	 * @param warnOut 		where all the warnings goes
	 * @return contents for corresponding DEPREL field
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static UDv2Relations phrasePartRoleToUD(
			Node aNode, String phraseType, PrintWriter warnOut)
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
				if (LvtbPmcTypes.ADDRESS.equals(subPmcType)) return UDv2Relations.VOCATIVE;
				if (LvtbPmcTypes.INTERJ.equals(subPmcType) || LvtbPmcTypes.PARTICLE.equals(subPmcType))
					return UDv2Relations.DISCOURSE;
				String tag = Utils.getTag(aNode);
				if (tag != null && tag.matches("[qi].*")) return UDv2Relations.DISCOURSE;
				if (tag != null && tag.matches("n...v.*")) return UDv2Relations.VOCATIVE;
			}

		if (phraseType.equals(LvtbPmcTypes.SENT) || phraseType.equals(LvtbPmcTypes.UTTER)
				|| phraseType.equals(LvtbPmcTypes.SUBRCL) || phraseType.equals(LvtbPmcTypes.MAINCL)
				|| phraseType.equals(LvtbPmcTypes.INSPMC) || phraseType.equals(LvtbPmcTypes.SPCPMC)
				|| phraseType.equals(LvtbPmcTypes.DIRSPPMC) || phraseType.equals(LvtbPmcTypes.QUOT)
				|| phraseType.equals(LvtbPmcTypes.ADDRESS) || phraseType.equals(LvtbPmcTypes.INTERJ)
				|| phraseType.equals(LvtbPmcTypes.PARTICLE))
			if (lvtbRole.equals(LvtbRoles.PUNCT)) return UDv2Relations.PUNCT;

		if (phraseType.equals(LvtbPmcTypes.SENT) ||
				phraseType.equals(LvtbPmcTypes.UTTER) ||
				phraseType.equals(LvtbPmcTypes.MAINCL) ||
				phraseType.equals(LvtbPmcTypes.INSPMC) ||
				phraseType.equals(LvtbPmcTypes.DIRSPPMC))
			if (lvtbRole.equals(LvtbRoles.CONJ))
			{
				String tag = Utils.getTag(aNode);
				if (tag.matches("cc.*"))
					return UDv2Relations.CC;
				if (tag.matches("cs.*"))
					return UDv2Relations.MARK;
			}

		if (phraseType.equals(LvtbPmcTypes.SUBRCL))
			if (lvtbRole.equals(LvtbRoles.CONJ)) return UDv2Relations.MARK;


		if (phraseType.equals(LvtbCoordTypes.CRDPARTS) || phraseType.equals(LvtbCoordTypes.CRDCLAUSES))
		{
			if (lvtbRole.equals(LvtbRoles.CRDPART)) return UDv2Relations.CONJ; // Parataxis role is given in PhraseTransform class.
			if (lvtbRole.equals(LvtbRoles.CONJ)) return UDv2Relations.CC;
			if (lvtbRole.equals(LvtbRoles.PUNCT)) return UDv2Relations.PUNCT;
		}

		if (phraseType.equals(LvtbXTypes.XAPP) &&
				lvtbRole.equals(LvtbRoles.BASELEM)) return UDv2Relations.NMOD;
		if ((phraseType.equals(LvtbXTypes.XNUM) ||
				phraseType.equals(LvtbXTypes.COORDANAL)) &&
				lvtbRole.equals(LvtbRoles.BASELEM)) return UDv2Relations.COMPOUND;
		if ((phraseType.equals(LvtbXTypes.PHRASELEM) ||
				phraseType.equals(LvtbXTypes.UNSTRUCT) ||
				phraseType.equals(LvtbPmcTypes.INTERJ) ||
				phraseType.equals(LvtbPmcTypes.PARTICLE)) &&
				lvtbRole.equals(LvtbRoles.BASELEM)) return UDv2Relations.FLAT;
		if (phraseType.equals(LvtbXTypes.NAMEDENT) &&
				lvtbRole.equals(LvtbRoles.BASELEM)) return UDv2Relations.FLAT_NAME;

		if (phraseType.equals(LvtbXTypes.SUBRANAL) &&
				lvtbRole.equals(LvtbRoles.BASELEM))
		{
			// NB: "vairāk kā/nekā X" and "tāds kā X" roles for "vairāk" and
			//     "tāds" are asigned in phrase transformator.
			return UDv2Relations.COMPOUND;
		}

		if (phraseType.equals(LvtbXTypes.XPREP) &&
				lvtbRole.equals(LvtbRoles.PREP)) return UDv2Relations.CASE;
		if (phraseType.equals(LvtbXTypes.XPARTICLE) &&
				lvtbRole.equals(LvtbRoles.NO))
			return UDv2Relations.DISCOURSE;

		if (phraseType.equals(LvtbXTypes.XSIMILE) &&
				lvtbRole.equals(LvtbRoles.CONJ))
		{
			// For now lets assume, that conjunction can't be coordinated.
			// Then parent in this situation is the xSimile itself.
			Node firstAncestor = Utils.getEffectiveAncestor(Utils.getPMLParent(aNode)); // node/xinfo/pmcinfo/phraseinfo
			Node secondAncestor = Utils.getEffectiveAncestor(firstAncestor); // node/xinfo/pmcinfo/phraseinfo
			String firstAncType = Utils.getAnyLabel(firstAncestor);
			String secondAncType = Utils.getAnyLabel(secondAncestor);

			// "vairāk kā trīs"
			NodeList vSiblings = (NodeList)XPathEngine.get().evaluate(
					"./children/node[m.rf/form='vairāk' or m.rf/form='Vairāk']",
					secondAncestor, XPathConstants.NODESET);
			// "ne vairāk kā trīs"
			NodeList vSiblings2 = (NodeList)XPathEngine.get().evaluate(
					"./children/node[role='basElem']/children/xinfo[xtype='xParticle']/children/node[m.rf/form='vairāk' or m.rf/form='Vairāk']",
					secondAncestor, XPathConstants.NODESET);
			// "tāds kā dumjš"
			NodeList tSiblings = (NodeList)XPathEngine.get().evaluate(
					"./children/node[m.rf/lemma='tāds' or m.rf/lemma='tāda']",
					secondAncestor, XPathConstants.NODESET);
			// "tāda paraduma kā nagu graušana
			//NodeList tSiblings2 = (NodeList)XPathEngine.get().evaluate(
			//		"./children/node[role='basElem']/children/node[role='attr' and (m.rf/lemma='tāds' or m.rf/lemma='tāda')]",
			//		secongAncestor, XPathConstants.NODESET);

			// Check the specific roles
			if (LvtbRoles.BASELEM.equals(firstAncType))
			{
				if (LvtbPmcTypes.SPCPMC.equals(secondAncType) ||
						LvtbPmcTypes.INSPMC.equals(secondAncType))
					return UDv2Relations.MARK;
				if (LvtbXTypes.XPRED.equals(secondAncType) || LvtbPmcTypes.UTTER.equals(secondAncType) ||
						LvtbXTypes.SUBRANAL.equals(secondAncType) && tSiblings != null &&
						tSiblings.getLength() > 0)
					return UDv2Relations.DISCOURSE;
				if (LvtbXTypes.SUBRANAL.equals(secondAncType) && (vSiblings != null &&
						vSiblings.getLength() > 0 || vSiblings2 != null && vSiblings2.getLength() > 0))
					return UDv2Relations.FIXED;
			}
			// In generic SPC case use mark, in generic ADV we use discourse.
			if (LvtbRoles.SPC.equals(firstAncType))
				return UDv2Relations.MARK;
			if (LvtbRoles.ADV.equals(firstAncType))
				return UDv2Relations.DISCOURSE;
			
			Node effAncestor = secondAncestor;
			if (LvtbXTypes.XPARTICLE.equals(Utils.getAnyLabel(effAncestor)))
				effAncestor = Utils.getEffectiveAncestor(effAncestor);
			String effAncLabel = Utils.getAnyLabel(effAncestor);

			if (LvtbRoles.SPC.equals(effAncLabel) || LvtbPmcTypes.SPCPMC.equals(effAncLabel)
					|| LvtbPmcTypes.SPCPMC.equals(effAncLabel))
				return UDv2Relations.MARK;
			if (LvtbRoles.ADV.equals(effAncLabel))
				return UDv2Relations.DISCOURSE;
		}

		if (phraseType.equals(LvtbXTypes.XPRED))
		{
			if (lvtbRole.equals(LvtbRoles.AUXVERB)) return UDv2Relations.AUX;
			if (lvtbRole.equals(LvtbRoles.BASELEM) ||
					lvtbRole.equals(LvtbRoles.MOD)) return UDv2Relations.XCOMP;
		}

		warnOut.printf("\"%s\" (%s) in \"%s\" has no UD label.\n",
				lvtbRole, nodeId, phraseType);
		return UDv2Relations.DEP;
	}
}
