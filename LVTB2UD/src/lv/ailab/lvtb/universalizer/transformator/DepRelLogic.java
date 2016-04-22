package lv.ailab.lvtb.universalizer.transformator;

import lv.ailab.lvtb.universalizer.conllu.URelations;
import lv.ailab.lvtb.universalizer.pml.LvtbPmcTypes;
import lv.ailab.lvtb.universalizer.pml.LvtbRoles;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

/**
 * Created on 2016-04-20.
 *
 * @author Lauma
 */
public class DepRelLogic
{

	public static URelations getUDepFromDep(Node aNode) throws XPathExpressionException
	{
		return URelations.DEP;
		/*Node pmlParent = (Node)xPathEngine.evaluate("../..", aNode, XPathConstants.NODE);
		String lvtbRole = xPathEngine.evaluate("./role", aNode);

		if (lvtbRole.equals("subj"))
		{
			if ("pred".equals(xPathEngine.evaluate("./role", pmlParent)))
			{
				String parentTag = xPathEngine.evaluate("./m.rf/tag", pmlParent);
				if (parentTag.matches("v..[^p].....a.*")) return URelations.NSUBJ;
				if (parentTag.matches("v..[^p].....p.*")) return URelations.NSUBJPASS;
				System.out.printf("\"%s\"")

			}
		}*/

	}

	public static URelations getUDepFromPhrsePart(Node aNode, String pmcType)
	throws XPathExpressionException
	{
		String nodeId = XPathEngine.get().evaluate("./@id", aNode);
		String lvtbRole = XPathEngine.get().evaluate("./role", aNode);

		if ((pmcType.equals(LvtbPmcTypes.SENT) || pmcType.equals(LvtbPmcTypes.UTER)
				|| pmcType.equals(LvtbPmcTypes.SUBRCL)) || pmcType.equals(LvtbPmcTypes.MAINCL))
			if (lvtbRole.equals(LvtbRoles.NO))
			{
				String subPmcType = XPathEngine.get().evaluate("./children/pmcinfo/pmctype", aNode);
				if (LvtbPmcTypes.ADRESS.equals(subPmcType)) return URelations.VOCATIVE;
				if (LvtbPmcTypes.INTERJ.equals(subPmcType) || LvtbPmcTypes.PARTICLE.equals(subPmcType))
					return URelations.DISCOURSE;
				String tag = XPathEngine.get().evaluate("./tag", aNode);
				if (tag != null && tag.matches("q.*")) return URelations.DISCOURSE;
			}

		if ((pmcType.equals(LvtbPmcTypes.SENT) || pmcType.equals(LvtbPmcTypes.UTER)
				|| pmcType.equals(LvtbPmcTypes.SUBRCL)) || pmcType.equals(LvtbPmcTypes.INSPMC)
				|| pmcType.equals(LvtbPmcTypes.SPCPMC) || pmcType.equals(LvtbPmcTypes.PARTICLE)
				|| pmcType.equals(LvtbPmcTypes.DIRSPPMC) || pmcType.equals(LvtbPmcTypes.QUOT)
				|| pmcType.equals(LvtbPmcTypes.ADRESS) || pmcType.equals(LvtbPmcTypes.INTERJ))
			if (lvtbRole.equals(LvtbRoles.PUNCT)) return URelations.PUNCT;

		if (pmcType.equals(LvtbPmcTypes.SENT) || pmcType.equals(LvtbPmcTypes.UTER))
			if (lvtbRole.equals(LvtbRoles.CONJ)) return URelations.DISCOURSE;

		if (pmcType.equals(LvtbPmcTypes.SUBRCL))
			if (lvtbRole.equals(LvtbRoles.CONJ)) return URelations.MARK;

		System.err.printf("%s in %s phrase has no UD label.", nodeId, pmcType);
		return URelations.DEP;
	}
}
