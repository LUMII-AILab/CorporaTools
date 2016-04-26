package lv.ailab.lvtb.universalizer.transformator;

import lv.ailab.lvtb.universalizer.conllu.URelations;
import lv.ailab.lvtb.universalizer.pml.*;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

/**
 * Relations between roles used in LVTB and UD.
 * Created on 2016-04-20.
 *
 * @author Lauma
 */
public class DepRelLogic
{

	public static URelations getUDepFromDep(Node aNode)
	throws XPathExpressionException
	{
		String nodeId = Utils.getId(aNode);
		Node pmlParent = (Node)XPathEngine.get().evaluate("../..", aNode, XPathConstants.NODE);
		String parentTag = Utils.getTag(pmlParent);
		String parentType = XPathEngine.get().evaluate("./role|./pmctype|./coortype|./xtype", pmlParent);

		String lvtbRole = XPathEngine.get().evaluate("./role", aNode);
		String tag = Utils.getTag(aNode);

		// Simple dependencies.

		if (lvtbRole.equals(LvtbRoles.SUBJ))
		{
			// Nominal subject
			if (tag.matches("[nampx].*|v..pd.*"))
			{
				// Parent is simple predicate
				if (parentType.equals(LvtbRoles.PRED))
				{
					if (parentTag.matches("v..[^p].....a.*|v..n.*")) return URelations.NSUBJ;
					if (parentTag.matches("v..[^p].....p.*")) return URelations.NSUBJPASS;
				}
				// Parent is complex predicate
				if (parentType.equals(LvtbXTypes.XPRED))
				{
					if (parentTag.matches("v..[^p].....p.*|v[^\\[]*\\[pas.*")) return URelations.NSUBJPASS;
					if (parentTag.matches("v.*")) return URelations.NSUBJ;
				}
			}
			System.err.printf("Role \"%s\" for node %s was not transformed.\n", lvtbRole, nodeId);
		}
		if (lvtbRole.equals(LvtbRoles.OBJ))
		{
			if (tag.matches("[na]...a.*|[pm]....a.*|v..p...a.*")) return URelations.DOBJ;
			if (tag.matches("[na]...n.*|[pm]....n.*|v..p...n.*") && parentTag.matches("v..d.*"))
				return URelations.DOBJ;
			return URelations.IOBJ;
		}
		if (lvtbRole.equals(LvtbRoles.SPC))
		{
			// Infinitive SPC
			if (tag.matches("v..n.*"))
			{
				if ((parentType.equals(LvtbRoles.PRED) ||
						parentType.equals(LvtbXTypes.XPRED)) &&
						parentTag.matches("v..[^p]...[123].*"))
					return URelations.CCOMP; // It is impposible safely to distinguish xcomp for now.
				if (parentTag.matches("[nampx].*|v..pd.*")) return URelations.ACL;
			}
			// Simple nominal SPC
			if (tag.matches("[na]...[adnl].*|[pm]....[adnl].*|v..p...[adnl].*|x.*"))
				return URelations.ACL;
			String xType = XPathEngine.get().evaluate("./children/xinfo/xtype", aNode);
			// SPC with comparison
			if (xType != null && xType.equals(LvtbXTypes.XSIMILE)) return URelations.ADVCL;
			// prepositional SPC
			if (xType != null && xType.equals(LvtbXTypes.XPREP))
			{
				NodeList preps = (NodeList)XPathEngine.get().evaluate(
						"./children/xinfo/children/node[role='" + LvtbRoles.PREP + "']", aNode, XPathConstants.NODESET);
				NodeList basElems = (NodeList)XPathEngine.get().evaluate(
						"./children/xinfo/children/node[role='" + LvtbRoles.BASELEM + "']", aNode, XPathConstants.NODESET);
				if (preps.getLength() > 1)
					System.err.printf("\"%s\" has multiple \"%s\"", xType, LvtbRoles.PREP);
				if (basElems.getLength() > 1)
					System.err.printf("\"%s\" has multiple \"%s\"", xType, LvtbRoles.BASELEM);
				String baseElemTag = Utils.getTag(basElems.item(0));
				if ("par".equals(Utils.getLemma(preps.item(0)))
						&& baseElemTag != null && baseElemTag.matches("[nampx].*"))
					return URelations.XCOMP;
			}
			// Participal SPC
			if (tag.matches("v..p[pu].*")) return URelations.ADVCL;

			// SPC with punctuation.
			String pmcType = XPathEngine.get().evaluate("./children/pmcinfo/pmctype", aNode);
			if (pmcType != null && pmcType.equals(LvtbPmcTypes.SPCPMC))
			{
				NodeList basElems = (NodeList)XPathEngine.get().evaluate(
						"./children/pminfo/children/node[role='" + LvtbRoles.BASELEM + "']", aNode, XPathConstants.NODESET);
				if (basElems.getLength() > 1)
					System.err.printf("\"%s\" has multiple \"%s\"", pmcType, LvtbRoles.BASELEM);
				String basElemTag = Utils.getTag(basElems.item(0));
				String basElemXType = XPathEngine.get().evaluate("./childen/xinfo/xtype", basElems.item(0));

				// SPC with comparison
				if (LvtbXTypes.XSIMILE.equals(basElemXType)) return URelations.ADVCL;
				// Participal SPC
				if (basElemTag.matches("v..p[pu].*")) return URelations.ADVCL;
				// Nominal SPC
				if (basElemTag.matches("n.*")) return URelations.APPOS;
				// Adjective SPC
				if (basElemTag.matches("a.*|v..d.*")) return URelations.ACL;
			}
			System.err.printf("Role \"%s\" for node %s was not transformed.\n", lvtbRole, nodeId);
		}
		if (lvtbRole.equals(LvtbRoles.ATTR))
		{
			if (tag.matches("n.*")) return URelations.NMOD;
			if (tag.matches("m[cf].*|xn.*")) return URelations.NUMMOD;
			if (tag.matches("mo.*|xo.*|v..p.*")) return URelations.AMOD;
			if (tag.matches("p.*")) return URelations.DET;
			if (tag.matches("a.*"))
			{
				String lemma = Utils.getLemma(aNode);
				if (lemma != null && lemma.matches("(man|mūs|tav|jūs|viņ|sav)ēj(ais|ā)|(daudz|vairāk|daž)(i|as)"))
					return URelations.DET;
				return URelations.AMOD;
			}
			System.err.printf("Role \"%s\" for node %s was not transformed.\n", lvtbRole, nodeId);
		}

		if (lvtbRole.equals(LvtbRoles.ADV) || lvtbRole.equals(LvtbRoles.SIT))
		{
			if (tag.matches("n.*")) return URelations.NMOD;
			if (tag.matches("r.*")) return URelations.ADVMOD;
			System.err.printf("Role \"%s\" for node %s was not transformed.\n", lvtbRole, nodeId);
		}
		if (lvtbRole.equals(LvtbRoles.DET)) return URelations.NMOD;

		if (lvtbRole.equals(LvtbRoles.NO))
		{
			String subPmcType = XPathEngine.get().evaluate("./children/pmcinfo/pmctype", aNode);
			if (LvtbPmcTypes.ADRESS.equals(subPmcType)) return URelations.VOCATIVE;
			if (LvtbPmcTypes.INTERJ.equals(subPmcType) || LvtbPmcTypes.PARTICLE.equals(subPmcType))
				return URelations.DISCOURSE;
			if (tag != null && tag.matches("q.*")) return URelations.DISCOURSE;
			System.err.printf("Role \"%s\" for node %s was not transformed.\n", lvtbRole, nodeId);
		}

		// Clausal dependencies.

		if (lvtbRole.equals(LvtbRoles.PREDCL))
		{
			// Parent is simple predicate
			if (parentType.equals(LvtbRoles.PRED)) return URelations.CCOMP;
			// Parent is complex predicate
			if (parentType.equals(LvtbXTypes.XPRED)) return URelations.ACL;
			System.err.printf("Role \"%s\" for node %s was not transformed.\n", lvtbRole, nodeId);
		}
		if (lvtbRole.equals(LvtbRoles.SUBJCL))
		{
			// Parent is simple predicate
			if (parentType.equals(LvtbRoles.PRED))
			{
				if (parentTag.matches("v..[^p].....a.*|v..n.*")) return URelations.NSUBJ;
				if (parentTag.matches("v..[^p].....p.*")) return URelations.NSUBJPASS;
			}
			// Parent is complex predicate
			if (parentType.equals(LvtbXTypes.XPRED))
			{
				if (parentTag.matches("v..[^p].....p.*|v.*?\\[pas.*")) return URelations.NSUBJPASS;
				if (parentTag.matches("v.*")) return URelations.NSUBJ;
			}
			System.err.printf("Role \"%s\" for node %s was not transformed.\n", lvtbRole, nodeId);
		}
		if (lvtbRole.equals(LvtbRoles.OBJCL)) return URelations.CCOMP;
		if (lvtbRole.equals(LvtbRoles.ATTRCL)) return URelations.ACL;
		if (lvtbRole.equals(LvtbRoles.PLACECL) ||
				lvtbRole.equals(LvtbRoles.TIMECL) ||
				lvtbRole.equals(LvtbRoles.MANCL) ||
				lvtbRole.equals(LvtbRoles.DEGCL) ||
				lvtbRole.equals(LvtbRoles.CAUSCL) ||
				lvtbRole.equals(LvtbRoles.PURPCL) ||
				lvtbRole.equals(LvtbRoles.CONDCL) ||
				lvtbRole.equals(LvtbRoles.CNSECCL) ||
				lvtbRole.equals(LvtbRoles.CNCESCL) ||
				lvtbRole.equals(LvtbRoles.MOTIVCL) ||
				lvtbRole.equals(LvtbRoles.COMPCL) ||
				lvtbRole.equals(LvtbRoles.QUASICL)) return URelations.ADVCL;

		// Semi-clausal dependencies.
		if (lvtbRole.equals(LvtbRoles.INS))
		{

			NodeList basElems = (NodeList)XPathEngine.get().evaluate(
					"./children/pminfo/children/node[role='" + LvtbRoles.PRED + "']", aNode, XPathConstants.NODESET);
			if (basElems!= null && basElems.getLength() > 1)
				System.err.printf("\"%s\" has multiple \"%s\"", LvtbPmcTypes.INSPMC, LvtbRoles.PRED);
			if (basElems != null) return URelations.PARATAXIS;
			return URelations.DISCOURSE; // Washington (CNN) is left unidentified.
		}
		if (lvtbRole.equals(LvtbRoles.DIRSP)) return URelations.PARATAXIS;

		return URelations.DEP;
	}

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
	public static URelations getUDepFromPhrasePart(Node aNode, String phraseType)
	throws XPathExpressionException
	{
		String nodeId = XPathEngine.get().evaluate("./@id", aNode);
		String lvtbRole = XPathEngine.get().evaluate("./role", aNode);

		if ((phraseType.equals(LvtbPmcTypes.SENT) || phraseType.equals(LvtbPmcTypes.UTER)
				|| phraseType.equals(LvtbPmcTypes.SUBRCL)) || phraseType.equals(LvtbPmcTypes.MAINCL))
			if (lvtbRole.equals(LvtbRoles.NO))
			{
				String subPmcType = XPathEngine.get().evaluate("./children/pmcinfo/pmctype", aNode);
				if (LvtbPmcTypes.ADRESS.equals(subPmcType)) return URelations.VOCATIVE;
				if (LvtbPmcTypes.INTERJ.equals(subPmcType) || LvtbPmcTypes.PARTICLE.equals(subPmcType))
					return URelations.DISCOURSE;
				String tag = Utils.getTag(aNode);
				if (tag != null && tag.matches("q.*")) return URelations.DISCOURSE;
			}

		if ((phraseType.equals(LvtbPmcTypes.SENT) || phraseType.equals(LvtbPmcTypes.UTER)
				|| phraseType.equals(LvtbPmcTypes.SUBRCL)) || phraseType.equals(LvtbPmcTypes.INSPMC)
				|| phraseType.equals(LvtbPmcTypes.SPCPMC) || phraseType.equals(LvtbPmcTypes.PARTICLE)
				|| phraseType.equals(LvtbPmcTypes.DIRSPPMC) || phraseType.equals(LvtbPmcTypes.QUOT)
				|| phraseType.equals(LvtbPmcTypes.ADRESS) || phraseType.equals(LvtbPmcTypes.INTERJ))
			if (lvtbRole.equals(LvtbRoles.PUNCT)) return URelations.PUNCT;

		if (phraseType.equals(LvtbPmcTypes.SENT) || phraseType.equals(LvtbPmcTypes.UTER))
			if (lvtbRole.equals(LvtbRoles.CONJ)) return URelations.DISCOURSE;

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
				phraseType.equals(LvtbXTypes.COORDANAL) ||
				phraseType.equals(LvtbXTypes.SUBRANAL)) &&
				lvtbRole.equals(LvtbRoles.BASELEM)) return URelations.COMPOUND;
		if ((phraseType.equals(LvtbXTypes.PHRASELEM) ||
				phraseType.equals(LvtbXTypes.UNSTRUCT)) &&
				lvtbRole.equals(LvtbRoles.BASELEM)) return URelations.MWE;
		if (phraseType.equals(LvtbXTypes.NAMEDENT) &&
				lvtbRole.equals(LvtbRoles.BASELEM)) return URelations.NAME;

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
			Node parent = (Node)XPathEngine.get().evaluate(
					"../..", aNode, XPathConstants.NODE); // node/xinfo/pmcinfo/phraseinfo
			NodeList vSiblings = (NodeList)XPathEngine.get().evaluate(
					"./children/node[m.rf/lemma='vairāk']", parent, XPathConstants.NODESET);
			String parentRole = XPathEngine.get().evaluate("./role", parent);
			String parentType = XPathEngine.get().evaluate("./xinfo/xtype|./pmcinfo/pmctype", parent);
			if (LvtbRoles.SPC.equals(parentRole) ||
					LvtbPmcTypes.SPCPMC.equals(parentType) ||
					LvtbPmcTypes.INSPMC.equals(parentType)) return URelations.MARK;
			if (LvtbXTypes.XPRED.equals(parentType)) return URelations.DISCOURSE;
			if (LvtbXTypes.SUBRANAL.equals(parentType) && vSiblings != null &&
					vSiblings.getLength() > 0) return URelations.MWE;
		}

		if (phraseType.equals(LvtbXTypes.XPRED))
		{
				if (lvtbRole.equals(LvtbRoles.AUXVERB)) return URelations.AUX;
				if (lvtbRole.equals(LvtbRoles.BASELEM) ||
						lvtbRole.equals(LvtbRoles.MOD)) return URelations.XCOMP;
		}

		System.err.printf("%s in %s phrase has no UD label.", nodeId, phraseType);
		return URelations.DEP;
	}
}
