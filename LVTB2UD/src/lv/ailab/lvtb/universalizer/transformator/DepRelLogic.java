package lv.ailab.lvtb.universalizer.transformator;

import lv.ailab.lvtb.universalizer.conllu.URelations;
import lv.ailab.lvtb.universalizer.pml.LvtbCoordTypes;
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

	/**
	 * Generic relation between phrase part roles and UD DEPREL.
	 * Only for nodes that are not roots or subroots.
	 * NB! Case when a part of crdClauses maps to parataxis is handled in
	 * PhraseTransform class.
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
				String tag = XPathEngine.get().evaluate("./tag", aNode);
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
		System.err.printf("%s in %s phrase has no UD label.", nodeId, phraseType);
		return URelations.DEP;
	}
}
