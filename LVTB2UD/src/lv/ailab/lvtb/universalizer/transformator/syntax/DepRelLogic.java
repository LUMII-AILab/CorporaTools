package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.URelations;
import lv.ailab.lvtb.universalizer.pml.*;
import lv.ailab.lvtb.universalizer.transformator.XPathEngine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

/**
 * Relations between dependency labeling used in LVTB and UD.
 * Created on 2016-04-20.
 *
 * @author Lauma
 */
public class DepRelLogic
{
	/**
	 * Generic relation between LVTB dependency roles and UD DEPREL.
	 * @param aNode	node for which UD DEPREL should be obtained
	 * @return	UD DEPREL
	 * @throws XPathExpressionException
	 */
	public static URelations depToUD(Node aNode)
	throws XPathExpressionException
	{
		String lvtbRole = Utils.getRole(aNode);

		// Simple dependencies.
		if (lvtbRole.equals(LvtbRoles.SUBJ))
			return subjToUD(aNode);
		if (lvtbRole.equals(LvtbRoles.OBJ))
			return objToUD(aNode);
		if (lvtbRole.equals(LvtbRoles.SPC))
			return spcToUD(aNode);
		if (lvtbRole.equals(LvtbRoles.ATTR))
			return attrToUD(aNode);
		if (lvtbRole.equals(LvtbRoles.ADV) ||
				lvtbRole.equals(LvtbRoles.SIT))
			return advSitToUD(aNode);
		if (lvtbRole.equals(LvtbRoles.DET))
			return URelations.NMOD;
		if (lvtbRole.equals(LvtbRoles.NO))
			return noToUD(aNode);

		// Clausal dependencies.
		if (lvtbRole.equals(LvtbRoles.PREDCL))
			return predClToUD(aNode);
		if (lvtbRole.equals(LvtbRoles.SUBJCL))
			return subjClToUD(aNode);
		if (lvtbRole.equals(LvtbRoles.OBJCL))
			return URelations.CCOMP;
		if (lvtbRole.equals(LvtbRoles.ATTRCL))
			return URelations.ACL;
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
				lvtbRole.equals(LvtbRoles.QUASICL))
			return URelations.ADVCL;

		// Semi-clausal dependencies.
		if (lvtbRole.equals(LvtbRoles.INS))
			return insToUD(aNode);
		if (lvtbRole.equals(LvtbRoles.DIRSP))
			return URelations.PARATAXIS;

		warn(aNode);
		return URelations.DEP;
	}

	public static URelations subjToUD(Node aNode)
	throws XPathExpressionException
	{
		String tag = Utils.getTag(aNode);
		// Nominal++ subject
		// This procesing is somewhat tricky: it is allowed for nsubj and
		// nsubjpas to be [rci].*, but it is not allowed for nmod.
		if (tag.matches("[nampxy].*|v..pd.*|[rci].*"))
		{
			Node pmlParent = Utils.getPMLParent(aNode);
			String parentTag = Utils.getTag(pmlParent);
			String parentType = Utils.getAnyLabel(pmlParent);

			// Parent is predicate
			if (parentType.equals(LvtbRoles.PRED))
			{
				Node xChild = Utils.getPhraseNode(pmlParent);
				// Parent is complex predicate
				if (LvtbXTypes.XPRED.equals(Utils.getPhraseType(xChild)))
				{
					if (parentTag.matches("v..[^p].....p.*|v[^\\[]*\\[pas.*")) return URelations.NSUBJPASS;
					if (parentTag.matches("v.*")) return URelations.NSUBJ;
				}
				// Parent is simple predicate
				else
				{
					// TODO: check the data if participles is realy appropriate here.
					if (parentTag.matches("v..[^p].....a.*|v..pd...a.*|v..pu.*|v..n.*"))
					//if (parentTag.matches("v..[^p].....a.*"))
						return URelations.NSUBJ;
					if (parentTag.matches("v..[^p].....p.*|v..pd...p.*"))
					//if (parentTag.matches("v..[^p].....p.*"))
						return URelations.NSUBJPASS;
					if (parentTag.matches("z.*"))
					{
						String reduction = XPathEngine.get().evaluate(
								"./reduction", pmlParent);
						if (reduction.matches("v..[^p].....a.*|v..pd...a.*|v..pu.*|v..n.*"))
							return URelations.NSUBJ;
						if (reduction.matches("v..[^p].....p.*||v..pd...p.*"))
							return URelations.NSUBJPASS;
						//if (reduction.matches("v..n.*"))
						//	return  URelations.NMOD;
					}
				}
			}

			// SPC subject.
			else if (parentType.equals(LvtbRoles.SPC) && !tag.matches("[rci].*]"))
				return URelations.NMOD;

			// Parent is basElem of some phrase
			else if (parentType.equals(LvtbRoles.BASELEM))
			{
				Node xChild = Utils.getPhraseNode(pmlParent);
				// Parent is complex predicate
				if (LvtbXTypes.XPRED.equals(Utils.getPhraseType(xChild)))
				{
					if (parentTag.matches("v..[^pn].....p.*|v[^\\[]+\\[pas.*")) return URelations.NSUBJPASS;
					if (parentTag.matches("v..[^pn].....a.*|v[^\\[]+\\[(act|subst|ad[jv]|pronom).*")) return URelations.NSUBJ;
				}
				else if (parentTag.matches("v..[^pn].....a.*"))
						return URelations.NSUBJ;
				else if (parentTag.matches("v..[^pn].....p.*"))
						return URelations.NSUBJPASS;
				// Infinitive subjects
				else if (parentTag.matches("v..[np].*") && !tag.matches("[rci].*]"))
						return URelations.NMOD;
			}

		}
		// Infinitive
		if (tag.matches("v..n.*"))
			return URelations.CCOMP;

		warn(aNode);
		return URelations.DEP;
	}

	public static URelations objToUD(Node aNode)
	throws XPathExpressionException
	{
		String tag = Utils.getTag(aNode);
		String parentTag = Utils.getTag(Utils.getPMLParent(aNode));

		if (tag.matches("[na]...a.*|[pm]....a.*|v..p...a.*")) return URelations.DOBJ;
		if (tag.matches("[na]...n.*|[pm]....n.*|v..p...n.*") && parentTag.matches("v..d.*"))
			return URelations.DOBJ;
		return URelations.IOBJ;
	}

	public static URelations spcToUD(Node aNode)
	throws XPathExpressionException
	{
		String tag = Utils.getTag(aNode);
		String parentTag = Utils.getTag(Utils.getPMLParent(aNode));
		// Infinitive SPC
		if (tag.matches("v..n.*"))
		{
			Node pmlEfParent = Utils.getEffectiveAncestor(aNode);
			String effParentType = Utils.getAnyLabel(pmlEfParent);
			if ((effParentType.equals(LvtbRoles.PRED) ||
					(effParentType.equals(LvtbRoles.BASELEM) &&
					LvtbXTypes.XPRED.equals(Utils.getEffectiveLabel(Utils.getPMLParent(pmlEfParent))))) &&
					parentTag.matches("v..[^p]...[123].*"))
				return URelations.CCOMP; // It is impposible safely to distinguish xcomp for now.
			if (parentTag.matches("[nampxy].*|v..pd.*")) return URelations.ACL;
		}
		// Simple nominal SPC
		if (tag.matches("[na]...[g].*|[pm]....[g].*|v..p...[g].*") ||
			tag.matches("x.*|y.*") && parentTag.matches("v..p....ps.*"))
			return URelations.NMOD;
		if (tag.matches("[na]...[adnl].*|[pm]....[adnl].*|v..p...[adnl].*|x.*|y.*"))
			return URelations.ACL;

		String xType = XPathEngine.get().evaluate("./children/xinfo/xtype", aNode);
		// SPC with comparison
		if (xType != null && xType.equals(LvtbXTypes.XSIMILE)) return URelations.ADVCL;
		// prepositional SPC
		if (xType != null && xType.equals(LvtbXTypes.XPREP))
		{
			NodeList preps = (NodeList)XPathEngine.get().evaluate(
					"./children/xinfo/children/node[role='" + LvtbRoles.PREP + "']",
					aNode, XPathConstants.NODESET);
			NodeList basElems = (NodeList)XPathEngine.get().evaluate(
					"./children/xinfo/children/node[role='" + LvtbRoles.BASELEM + "']",
					aNode, XPathConstants.NODESET);
			if (preps.getLength() > 1)
				System.err.printf("\"%s\" has multiple \"%s\"", xType, LvtbRoles.PREP);
			if (basElems.getLength() > 1)
				System.err.printf("\"%s\" has multiple \"%s\"", xType, LvtbRoles.BASELEM);
			String baseElemTag = Utils.getTag(basElems.item(0));
			if ("par".equals(Utils.getLemma(preps.item(0)))
					&& baseElemTag != null && baseElemTag.matches("[nampxy].*"))
				return URelations.XCOMP;
			else if (tag.matches("[nampxy].*|v..pd.*"))
				return URelations.NMOD;
		}
		// Participal SPC
		if (tag.matches("v..p[pu].*")) return URelations.ADVCL;

		// SPC with punctuation.
		String pmcType = XPathEngine.get().evaluate("./children/pmcinfo/pmctype", aNode);
		if (pmcType != null && pmcType.equals(LvtbPmcTypes.SPCPMC))
		{
			NodeList basElems = (NodeList)XPathEngine.get().evaluate(
					"./children/pmcinfo/children/node[role='" + LvtbRoles.BASELEM + "']",
					aNode, XPathConstants.NODESET);
			if (basElems.getLength() > 1)
				System.err.printf("\"%s\" has multiple \"%s\"", pmcType, LvtbRoles.BASELEM);
			String basElemTag = Utils.getTag(basElems.item(0));
			String basElemXType = Utils.getPhraseType(basElems.item(0));

			// SPC with comparison
			if (LvtbXTypes.XSIMILE.equals(basElemXType)) return URelations.ADVCL;
			// Participal SPC
			if (basElemTag.matches("v..p[pu].*")) return URelations.ADVCL;
			// Nominal SPC
			if (basElemTag.matches("n.*") ||
					basElemTag.matches("y.*") &&
					Utils.getLemma(basElems.item(0)).matches("\\p{Lu}+"))
				return URelations.APPOS;
			// Adjective SPC
			if (basElemTag.matches("a.*|v..d.*")) return URelations.ACL;
		}

		warn(aNode);
		return URelations.DEP;
	}

	public static URelations attrToUD(Node aNode)
	throws XPathExpressionException
	{
		String tag = Utils.getTag(aNode);
		String lemma = Utils.getLemma(aNode);

		if (tag.matches("n.*|y.*") || lemma.equals("%")) return URelations.NMOD;
		if (tag.matches("r.*")) return URelations.ADVMOD;
		if (tag.matches("m[cf].*|xn.*")) return URelations.NUMMOD;
		if (tag.matches("mo.*|xo.*|v..p.*")) return URelations.AMOD;
		if (tag.matches("p.*")) return URelations.DET;
		if (tag.matches("a.*"))
		{
			if (lemma != null && lemma.matches("(man|mūs|tav|jūs|viņ|sav)ēj(ais|ā)|(daudz|vairāk|daž)(i|as)"))
				return URelations.DET;
			return URelations.AMOD;
		}
		// Both cases can provide mistakes, but there is no way to solve this
		// now.
		if (tag.matches("xf.*")) return URelations.NMOD;
		if (tag.matches("xx.*")) return URelations.AMOD;
		
		/*if (tag.matches("y.*"))
		{
			String lemma = Utils.getLemma(aNode);
			if (lemma.matches("\\p{Lu}+"))
				return URelations.NMOD;
		}*/

		warn(aNode);
		return URelations.DEP;
	}

	public static URelations advSitToUD(Node aNode)
	throws XPathExpressionException
	{
		String tag = Utils.getTag(aNode);

		if (tag.matches("n.*|xn.*|p.*|.*\\[(pre|post|rel).*|mc.*|y.*"))
			return URelations.NMOD;

		String lemma = Utils.getLemma(aNode);

		if (tag.matches("r.*") || lemma.equals("%")) return URelations.ADVMOD;

		if (tag.matches("q.*"))
		{
			if ("ne".equals(lemma)) return URelations.NEG;
			return URelations.DISCOURSE;
		}

		warn(aNode);
		return URelations.DEP;
	}

	public static URelations noToUD(Node aNode) throws XPathExpressionException
	{
		String tag = Utils.getTag(aNode);
		String lemma = Utils.getLemma(aNode);
		String subPmcType = XPathEngine.get().evaluate("./children/pmcinfo/pmctype", aNode);
		if (LvtbPmcTypes.ADRESS.equals(subPmcType)) return URelations.VOCATIVE;
		if (LvtbPmcTypes.INTERJ.equals(subPmcType) || LvtbPmcTypes.PARTICLE.equals(subPmcType))
			return URelations.DISCOURSE;
		if (tag != null && tag.matches("[qi].*")) return URelations.DISCOURSE;

		if (lemma.matches("utt\\.|u\\.t\\.jpr\\.|u\\.c\\.|u\\.tml\\.|v\\.tml\\."))
			return URelations.CONJ;


		warn(aNode);
		return URelations.DEP;
	}

	public static URelations predClToUD(Node aNode)
	throws XPathExpressionException
	{
		String parentType = Utils.getAnyLabel(Utils.getPMLParent(aNode));

		// Parent is simple predicate
		if (parentType.equals(LvtbRoles.PRED)) return URelations.CCOMP;
		// Parent is complex predicate
		String grandPatentType = Utils.getAnyLabel(Utils.getPMLGrandParent(aNode));
		if (grandPatentType.equals(LvtbXTypes.XPRED)) return URelations.ACL;

		warn(aNode);
		return URelations.DEP;
	}

	public static URelations subjClToUD(Node aNode)
	throws XPathExpressionException
	{
		Node pmlParent = Utils.getPMLParent(aNode);
		String parentTag = Utils.getTag(pmlParent);
		String parentType = Utils.getAnyLabel(pmlParent);

		// Parent is predicate
		if (parentType.equals(LvtbRoles.PRED))
		{
			Node xChild = Utils.getPhraseNode(pmlParent);
			// Parent is complex predicate
			if (LvtbXTypes.XPRED.equals(Utils.getPhraseType(xChild)))
			{
				if (parentTag.matches("v..[^p].....p.*|v.*?\\[pas.*")) return URelations.NSUBJPASS;
				if (parentTag.matches("v.*")) return URelations.NSUBJ;
			}
			// Parent is simple predicate
			else
			{
				if (parentTag.matches("v..[^p].....a.*|v..n.*"))
					return URelations.NSUBJ;
				if (parentTag.matches("v..[^p].....p.*"))
					return URelations.NSUBJPASS;
			}
		}
		if (parentType.equals(LvtbRoles.SUBJ))
			return URelations.ACL;

		warn(aNode);
		return URelations.DEP;
	}

	public static URelations insToUD(Node aNode)
	throws XPathExpressionException
	{
		NodeList basElems = (NodeList)XPathEngine.get().evaluate(
				"./children/pminfo/children/node[role='" + LvtbRoles.PRED + "']",
				aNode, XPathConstants.NODESET);
		if (basElems!= null && basElems.getLength() > 1)
			System.err.printf("\"%s\" has multiple \"%s\"", LvtbPmcTypes.INSPMC, LvtbRoles.PRED);
		if (basElems != null) return URelations.PARATAXIS;
		return URelations.DISCOURSE; // Washington (CNN) is left unidentified.
	}

	/**
	 * Print out the warning that role was not tranformed.
	 * @param node	node about which to warn
	 * @throws XPathExpressionException
	 */
	protected static void warn(Node node) throws XPathExpressionException
	{
		String nodeId = Utils.getId(node);
		String role = XPathEngine.get().evaluate("./role", node);
		System.err.printf("Role \"%s\" for node %s was not transformed.\n", role, nodeId);
	}

}