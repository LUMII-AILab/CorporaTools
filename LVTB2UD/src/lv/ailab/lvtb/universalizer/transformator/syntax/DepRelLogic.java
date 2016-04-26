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
		String lvtbRole = XPathEngine.get().evaluate("./role", aNode);

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
		Node pmlParent = Utils.getPMLParent(aNode);
		String parentTag = Utils.getTag(pmlParent);
		String parentType = Utils.getAnyLabel(pmlParent);

		// Nominal subject
		if (tag.matches("[nampx].*|v..pd.*") ||
				(tag.matches("y.*") && Utils.getLemma(aNode).matches("\\p{Lu}+")))
		{
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

					// TODO recheck zd.*
					if (parentTag.matches("v..[^p].....a.*|v..pd...a.*|v..n.*"))
						return URelations.NSUBJ;
					if (parentTag.matches("v..[^p].....p.*|v..pd...p.*"))
						return URelations.NSUBJPASS;
					if (parentTag.matches("z.*"))
					{
						String reduction = XPathEngine.get().evaluate(
								"./reduction", pmlParent);
						if (reduction.matches("v..[^p].....a.*|v..n.*"))
							return URelations.NSUBJ;
						if (reduction.matches("v..[^p].....p.*"))
							return URelations.NSUBJPASS;
					}
				}
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
		Node pmlParent = Utils.getPMLParent(aNode);
		String parentTag = Utils.getTag(pmlParent);
		String parentType = Utils.getAnyLabel(pmlParent);

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
			if (basElemTag.matches("n.*")) return URelations.APPOS;
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

		if (tag.matches("n.*")) return URelations.NMOD;
		if (tag.matches("r.*")) return URelations.ADVMOD;
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
		if (tag.matches("y.*"))
		{
			String lemma = Utils.getLemma(aNode);
			if (lemma.matches("\\p{Lu}+"))
				return URelations.NMOD;
		}

		warn(aNode);
		return URelations.DEP;
	}

	public static URelations advSitToUD(Node aNode)
	throws XPathExpressionException
	{
		String tag = Utils.getTag(aNode);

		if (tag.matches("n.*|xn.*|p.*|.*\\[(pre|post|rel).*")) return URelations.NMOD;
		if (tag.matches("r.*")) return URelations.ADVMOD;
		if (tag.matches("q.*"))
		{
			String lemma = Utils.getLemma(aNode);
			if ("ne".equals(lemma)) return URelations.NEG;
			return URelations.DISCOURSE;
		}

		warn(aNode);
		return URelations.DEP;
	}

	public static URelations noToUD(Node aNode) throws XPathExpressionException
	{
		String tag = Utils.getTag(aNode);
		String subPmcType = XPathEngine.get().evaluate("./children/pmcinfo/pmctype", aNode);
		if (LvtbPmcTypes.ADRESS.equals(subPmcType)) return URelations.VOCATIVE;
		if (LvtbPmcTypes.INTERJ.equals(subPmcType) || LvtbPmcTypes.PARTICLE.equals(subPmcType))
			return URelations.DISCOURSE;
		if (tag != null && tag.matches("q.*")) return URelations.DISCOURSE;

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
