package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.UDv2Feat;
import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.*;
import lv.ailab.lvtb.universalizer.util.Tuple;
import lv.ailab.lvtb.universalizer.util.XPathEngine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Relations between dependency labeling used in LVTB and UD.
 * Created on 2016-04-20.
 *
 * @author Lauma
 */
public class DepRelLogic
{
	/**
	 * To avoid repetitive messages, any message once printed are remembered.
	 * Set this to null to avoid this.
	 */
	public HashSet<String> warnRegister;

	protected static DepRelLogic singleton = null;
	protected DepRelLogic() { warnRegister = new HashSet<>(); }

	public static DepRelLogic getSingleton()
	{
		if (singleton == null) singleton = new DepRelLogic();
		return singleton;
	}

	/*
	 * Generic relation between LVTB dependency roles and UD DEPREL.
	 * @param aNode		node for which UD DEPREL should be obtained
	 * @param enhanced  true, if role for enhanced dependency tree is needed
	 * @param warnOut	where all warnings goes
	 * @return	UD DEPREL (including orphan, if parent is reduction and node is
	 * 			representing a core argument).
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	/*public UDv2Relations depToUD(Node aNode, boolean enhanced, PrintWriter warnOut)
			throws XPathExpressionException
	{
		return depToUD(aNode, aNode, enhanced, warnOut);
	}//*/

	/**
	 * Generic relation between LVTB dependency roles and UD DEPREL.
	 * @param node		node for which UD DEPREL should be obtained (use this
	 *                  node's placement, role, tag and lemma)
	 * @param warnOut	where all warnings goes
	 * @return	UD DEPREL (including orphan, if parent is reduction and node is
	 * 			representing a core argument).
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public UDv2Relations depToUDBase(Node node, PrintWriter warnOut)
	throws XPathExpressionException
	{
		Node pmlParent = Utils.getPMLParent(node);
		String lvtbRole = Utils.getRole(node);
		UDv2Relations prelaminaryRole = depToUDLogic(node, pmlParent, lvtbRole, warnOut).first;
		if (prelaminaryRole == UDv2Relations.DEP)
			warnOnRole(node, pmlParent, lvtbRole, false, warnOut);

		Node pmlEffParent = Utils.getEffectiveAncestor(node);
		if ((Utils.isReductionNode(pmlParent) || Utils.isReductionNode(pmlEffParent))
				&& (prelaminaryRole.equals(UDv2Relations.NSUBJ)
					|| prelaminaryRole.equals(UDv2Relations.NSUBJ_PASS)
					|| prelaminaryRole.equals(UDv2Relations.OBJ)
					|| prelaminaryRole.equals(UDv2Relations.IOBJ)
					|| prelaminaryRole.equals(UDv2Relations.CSUBJ)
					|| prelaminaryRole.equals(UDv2Relations.CSUBJ_PASS)
					|| prelaminaryRole.equals(UDv2Relations.CCOMP)
					|| prelaminaryRole.equals(UDv2Relations.XCOMP)))
			return UDv2Relations.ORPHAN;
		return prelaminaryRole;
	}

	/**
	 * Enhanced relation between LVTB dependency roles and UD enhanced dependency
	 * role. Orphan roles are not assigned, warnings on DEP roles are given.
	 * @param node		node for which UD relation should be obtained (use this
	 *                  node's placement, role, tag and lemma)
	 * @param warnOut	where all warnings goes
	 * @return	UD dependency role and enhanced depency role postfix, if such is
	 * 			needed.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public Tuple<UDv2Relations, String> depToUDEnhanced(
			Node node,  PrintWriter warnOut)
			throws XPathExpressionException
	{
		return depToUDEnhanced(node, Utils.getPMLParent(node), Utils.getRole(node), warnOut);
	}

	/**
	 * Enhanced relation between LVTB dependency roles and UD enhanced dependency
	 * role. Orphan roles are not assigned, warnings on DEP roles are given.
	 * @param node		node for which UD dependency should be obtained (use
	 *             		this node's lemma, morphology, etc.)
	 * @param parent	node which represents UD or enhanced UD parent for the
	 *                  node to be labeled
	 * @param warnOut	where all warnings goes
	 * @return	UD dependency role and enhanced depency role postfix, if such is
	 * 			needed.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public Tuple<UDv2Relations, String> depToUDEnhanced(
			Node node, Node parent, String lvtbRole, PrintWriter warnOut)
			throws XPathExpressionException
	{
		Tuple<UDv2Relations, String> res = depToUDLogic(node, parent, lvtbRole, warnOut);
		if (res.first == null || UDv2Relations.DEP.equals(res.first))
			warnOnRole(node, parent, lvtbRole,true, warnOut);
		return res;
	}

	/**
	 * Generic relation between LVTB dependency roles and UD role. Orphan roles
	 * are not assigned, warnings on DEP roles are not given.
	 * @param node		node for which UD dependency should be obtained (use
	 *             		this node's lemma, morphology, etc.)
	 * @param parent	node which represents UD or enhanced UD parent for the
	 *                  node to be labeled
	 * @param warnOut	where all warnings goes
	 * @return	UD dependency role and enhanced depency role postfix, if such is
	 * 			needed.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public Tuple<UDv2Relations, String> depToUDLogic(
			Node node, Node parent, String lvtbRole, PrintWriter warnOut)
	throws XPathExpressionException
	{
		// Simple dependencies.
		switch (lvtbRole)
		{
			case LvtbRoles.SUBJ : return subjToUD(node, parent);
			case LvtbRoles.OBJ : return objToUD(node, parent);
			case LvtbRoles.SPC : return spcToUD(node, parent, warnOut);
			case LvtbRoles.ATTR : return attrToUD(node, parent);
			case LvtbRoles.ADV :
			case LvtbRoles.SIT :
				return advSitToUD(node, parent, warnOut);
			case LvtbRoles.DET : return detToUD(node, parent);
			case LvtbRoles.NO: return noToUD(node, parent);

			// Clausal dependencies.
			case LvtbRoles.PREDCL : return predClToUD(node, parent);
			case LvtbRoles.SUBJCL : return subjClToUD(node, parent);
			case LvtbRoles.OBJCL : return Tuple.of(UDv2Relations.CCOMP, null);
			case LvtbRoles.ATTRCL : return Tuple.of(UDv2Relations.ACL, null);
			case LvtbRoles.PLACECL :
			case LvtbRoles.TIMECL :
			case LvtbRoles.MANCL :
			case LvtbRoles.DEGCL :
			case LvtbRoles.CAUSCL :
			case LvtbRoles.PURPCL :
			case LvtbRoles.CONDCL :
			case LvtbRoles.CNSECCL :
			case LvtbRoles.CNCESCL :
			case LvtbRoles.MOTIVCL :
			case LvtbRoles.COMPCL :
			case LvtbRoles.QUASICL :
				return Tuple.of(UDv2Relations.ADVCL, null);

			// Semi-clausal dependencies.
			case LvtbRoles.INS : return insToUD(node, parent, warnOut);
			case LvtbRoles.DIRSP : return Tuple.of(UDv2Relations.PARATAXIS, null);
			default : return Tuple.of(UDv2Relations.DEP, null);
		}
	}

	public Tuple<UDv2Relations, String> subjToUD(Node node, Node parent)
	throws XPathExpressionException
	{
		String tag = Utils.getTag(node);
		// Nominal++ subject
		// This procesing is somewhat tricky: it is allowed for nsubj and
		// nsubjpas to be [rci].*, but it is not allowed for nmod.
		if (tag.matches("[nampxy].*|v..pd.*|[rci].*|y[npa].*]"))
		{
			String parentTag = Utils.getTag(parent);
			String parentEffType = Utils.getEffectiveLabel(parent);
			Node pmlEffAncestor = Utils.getThisOrEffectiveAncestor(parent);
			// Hopefully either parent or effective ancestor is tagged as verb
			// or xPred.
			Node parentXChild = Utils.getPhraseNode(parent);
			Node ancXChild = Utils.getPhraseNode(pmlEffAncestor);

			// Parent is predicate
			if (parentEffType.equals(LvtbRoles.PRED))
			{
				// Parent is complex predicate
				if (LvtbXTypes.XPRED.equals(Utils.getPhraseType(parentXChild)) ||
						LvtbXTypes.XPRED.equals(Utils.getPhraseType(ancXChild)))
				{
					if (parentTag.matches("v..[^p].....p.*|v[^\\[]*\\[pas.*")) return Tuple.of(UDv2Relations.NSUBJ_PASS, null);
					if (parentTag.matches("v.*")) return Tuple.of(UDv2Relations.NSUBJ, null);
					String ancestorTag = Utils.getTag(pmlEffAncestor);
					if (ancestorTag.matches("v..[^p].....p.*|v[^\\[]*\\[pas.*")) return Tuple.of(UDv2Relations.NSUBJ_PASS, null);
					if (ancestorTag.matches("v.*")) return Tuple.of(UDv2Relations.NSUBJ, null);

				}
				// Parent is simple predicate
				else
				{
					// TODO: check the data if participles is realy appropriate here.
					if (parentTag.matches("v..[^p].....a.*|v..pd...a.*|v..pu.*|v..n.*"))
					//if (parentTag.matches("v..[^p].....a.*"))
						return Tuple.of(UDv2Relations.NSUBJ, null);
					if (parentTag.matches("v..[^p].....p.*|v..pd...p.*"))
					//if (parentTag.matches("v..[^p].....p.*"))
						return Tuple.of(UDv2Relations.NSUBJ_PASS, null);
					String reduction = XPathEngine.get().evaluate(
							"./reduction", parent);
					//if (parentTag.matches("z.*"))
					if (reduction != null && !reduction.isEmpty())
					{
						if (reduction.matches("v..[^pn].....[a0].*|v..pd...[a0].*|v..pu.*|v..n.*"))
							return Tuple.of(UDv2Relations.NSUBJ, null);
						if (reduction.matches("v..[^p].....p.*|v..pd...p.*"))
							return Tuple.of(UDv2Relations.NSUBJ_PASS, null);
						//if (reduction.matches("v..n.*"))
						//	return  URelations.NMOD;
					}
				}
			}

			// SPC subject, subject subject ("vienam cīnīties ir grūtāk")
			else if ((LvtbRoles.SPC.equals(parentEffType) || LvtbRoles.SUBJ.equals(parentEffType))
					&& !tag.matches("[rci].*|yr.*]"))
			{
				Matcher m = Pattern.compile("([na]...|[mp]....|v..pd..)(.).*").matcher(tag);
				if (m.matches())
				{
					String caseLetter = m.group(2);
					String caseString = UDv2Feat.caseLetterToLCString(caseLetter);
					if (caseString != null || caseLetter.equals("0") || caseLetter.equals("_"))
						return Tuple.of(UDv2Relations.OBL, caseString);
				}
				if (tag.matches("[x].*"))
					return Tuple.of(UDv2Relations.OBL, null);
			}

			// Parent is basElem of some phrase
			else if (parentEffType.equals(LvtbRoles.BASELEM))
			{
				// Parent is complex predicate
				if (LvtbXTypes.XPRED.equals(Utils.getPhraseType(parentXChild)) ||
						LvtbXTypes.XPRED.equals(Utils.getPhraseType(ancXChild)))
				{
					if (parentTag.matches("v..[^pn].....p.*|v[^\\[]+\\[pas.*")) return Tuple.of(UDv2Relations.NSUBJ_PASS, null);
					if (parentTag.matches("v..[^pn].....a.*|v[^\\[]+\\[(act|subst|ad[jv]|pronom).*")) return Tuple.of(UDv2Relations.NSUBJ, null);
					String ancestorTag = Utils.getTag(pmlEffAncestor);
					if (ancestorTag.matches("v..[^pn].....p.*|v[^\\[]+\\[pas.*")) return Tuple.of(UDv2Relations.NSUBJ_PASS, null);
					if (ancestorTag.matches("v..[^pn].....a.*|v[^\\[]+\\[(act|subst|ad[jv]|pronom).*")) return Tuple.of(UDv2Relations.NSUBJ, null);
				}
				else if (parentTag.matches("v..[^pn].....a.*"))
						return Tuple.of(UDv2Relations.NSUBJ, null);
				else if (parentTag.matches("v..[^pn].....p.*"))
						return Tuple.of(UDv2Relations.NSUBJ_PASS, null);
				// Infinitive subjects
				else if (parentTag.matches("v..[np].*") && !tag.matches("(yr|[rci]).*]"))
				{
					Matcher m = Pattern.compile("([na]...|[mp]....|v..pd..)(.).*").matcher(tag);
					if (m.matches())
					{
						String caseLetter = m.group(2);
						String caseString = UDv2Feat.caseLetterToLCString(caseLetter);
						if (caseString != null || caseLetter.equals("0") || caseLetter.equals("_"))
							return Tuple.of(UDv2Relations.OBL, caseString);
					}
					if (tag.matches("(x|y[npa]).*"))
						return Tuple.of(UDv2Relations.OBL, null);
				}
			}
		}
		// Infinitive
		if (tag.matches("v..n.*"))
			return Tuple.of(UDv2Relations.CCOMP, null);

		return Tuple.of(UDv2Relations.DEP, null);
	}

	public Tuple<UDv2Relations, String> objToUD(Node node, Node parent)
	throws XPathExpressionException
	{
		String tag = Utils.getTag(node);
		String parentTag = Utils.getTag(parent);
		Node phraseChild = Utils.getPhraseNode(node);
		if (phraseChild != null)
		{
			String constLabel = Utils.getAnyLabel(phraseChild);
			if (LvtbXTypes.XPREP.matches(constLabel)) return Tuple.of(UDv2Relations.IOBJ, null);
		}
		if (tag.matches(".*?\\[(pre|post).*]")) return Tuple.of(UDv2Relations.IOBJ, null);
		if (tag.matches("[na]...a.*|[pm]....a.*|v..p...a.*")) return Tuple.of(UDv2Relations.OBJ, null);
		if (tag.matches("[na]...n.*|[pm]....n.*|v..p...n.*") && parentTag.matches("v..d.*"))
			return Tuple.of(UDv2Relations.OBJ, null);
		return Tuple.of(UDv2Relations.IOBJ, null);
	}

	public Tuple<UDv2Relations, String> spcToUD(Node node, Node parent, PrintWriter warnOut)
	throws XPathExpressionException
	{
		String tag = Utils.getTag(node);
		String parentTag = Utils.getTag(parent);

		// If parent is something reduced to punctuation mark, use reduction
		// tag instead.
		if (parentTag.matches("z.*"))
		{
			String parentRed = Utils.getReduction(parent);
			if (parentRed != null && parentRed.length() > 0)
				parentTag = parentRed;
		}

		String parentEffRole = Utils.getEffectiveLabel(parent);
		// Infinitive SPC
		if (tag.matches("v..n.*"))
		{
			Node pmlEfParent = Utils.getThisOrEffectiveAncestor(parent);
			String effParentType = Utils.getAnyLabel(pmlEfParent);
			if (parentTag.matches("v..([^p]|p[^d]).*") || LvtbXTypes.XPRED.equals(effParentType))
				return Tuple.of(UDv2Relations.CCOMP, null); // It is impposible safely to distinguish xcomp for now.
			if (parentTag.matches("v..pd.*")) return Tuple.of(UDv2Relations.XCOMP, null);
			if (parentTag.matches("[nampx].*|y[npa].*")) return Tuple.of(UDv2Relations.ACL, null);
		}
		String xType = XPathEngine.get().evaluate("./children/xinfo/xtype", node);
		// prepositional SPC
		if (xType != null && xType.equals(LvtbXTypes.XPREP))
		{
			NodeList preps = (NodeList)XPathEngine.get().evaluate(
					"./children/xinfo/children/node[role='" + LvtbRoles.PREP + "']",
					node, XPathConstants.NODESET);
			NodeList basElems = (NodeList)XPathEngine.get().evaluate(
					"./children/xinfo/children/node[role='" + LvtbRoles.BASELEM + "']",
					node, XPathConstants.NODESET);

			// NB! Secība ir svarīga. Nevar pirms šī likt parastos nomenus!
			if (preps.getLength() > 1)
				warn(String.format("\"%s\" with ID \"%s\" has multiple \"%s\".",
						xType, Utils.getId(node), LvtbRoles.PREP), warnOut);
			if (basElems.getLength() > 1)
				warn(String.format("\"%s\" with ID \"%s\" has multiple \"%s\".",
						xType, Utils.getId(node), LvtbRoles.BASELEM), warnOut);
			String baseElemTag = Utils.getTag(basElems.item(0));
			String prepLemma = Utils.getLemma(preps.item(0));
			if ("par".equals(prepLemma)
					&& baseElemTag != null && baseElemTag.matches("[nampx].*|y[npa].*")
					&& (parentTag.matches("v.*") || LvtbRoles.PRED.equals(parentEffRole)))
				return Tuple.of(UDv2Relations.XCOMP, null);
			else if (parentTag.matches("[nampx].*|y[npa].*|v..pd.*"))
				//return Tuple.of(UDv2Relations.NMOD, prepLemma == null ? null : prepLemma.toLowerCase());
				return Tuple.of(UDv2Relations.NMOD, prepLemma);
		}

		// SPC with comparison
		if (xType != null && xType.equals(LvtbXTypes.XSIMILE))
		{
			NodeList conjs = (NodeList)XPathEngine.get().evaluate(
					"./children/xinfo/children/node[role='" + LvtbRoles.CONJ + "']",
					node, XPathConstants.NODESET);
			if (conjs.getLength() > 1)
				warn(String.format("\"%s\" with ID \"%s\" has multiple \"%s\".",
						xType, Utils.getId(node), LvtbRoles.CONJ), warnOut);
			String conjLemma = Utils.getLemma(conjs.item(0));
			if (parentTag.matches("n.*|y[np].*") && tag.matches("[nampx].*|y[npa].*|v..pd.*"))
				return Tuple.of(UDv2Relations.NMOD, conjLemma);
			return Tuple.of(UDv2Relations.OBL, conjLemma);
		}
		// Simple nominal SPC
		if (tag.matches("[na]...[g].*|[pm]....[g].*|v..p...[g].*"))
			return Tuple.of(UDv2Relations.OBL, UDv2Feat.CASE_GEN.value.toLowerCase());
		if (tag.matches("x.*|y[npa].*") && parentTag.matches("v..p....ps.*"))
			return Tuple.of(UDv2Relations.OBL, null);
		if (tag.matches("[na]...[adnl].*|[pm]....[adnl].*|v..p...[adnl].*|x.*|y[npa].*"))
		{
			// TODO Optimize to a single match
			Matcher m = Pattern.compile("([na]...|[mp]....|v..p...)(.).*").matcher(tag);
			if (m.matches())
			{
				String caseLetter = m.group(2);
				String caseString = UDv2Feat.caseLetterToLCString(caseLetter);
				if (caseString != null || caseLetter.equals("0") || caseLetter.equals("_"))
					return Tuple.of(UDv2Relations.ACL, caseString);
			}
			if (tag.matches("[xy].*"))
				return Tuple.of(UDv2Relations.ACL, null);
		}

		// Participal SPC
		if (tag.matches("v..p[pu].*")) return Tuple.of(UDv2Relations.ADVCL, null);

		// SPC with punctuation.
		String pmcType = XPathEngine.get().evaluate("./children/pmcinfo/pmctype", node);
		if (pmcType != null && pmcType.equals(LvtbPmcTypes.SPCPMC))
		{
			NodeList basElems = (NodeList)XPathEngine.get().evaluate(
					"./children/pmcinfo/children/node[role='" + LvtbRoles.BASELEM + "']",
					node, XPathConstants.NODESET);
			if (basElems.getLength() > 1)
				warn(String.format("\"%s\" has multiple \"%s\".", pmcType, LvtbRoles.BASELEM), warnOut);
			String basElemTag = Utils.getTag(basElems.item(0));
			String basElemXType = Utils.getPhraseType(basElems.item(0));

			// SPC with comparison
			if (LvtbXTypes.XSIMILE.equals(basElemXType))
			{
				NodeList conjs = (NodeList)XPathEngine.get().evaluate(
						"./children/xinfo/children/node[role='" + LvtbRoles.CONJ + "']",
						basElems.item(0), XPathConstants.NODESET);
				if (conjs.getLength() > 1)
					warn(String.format("\"%s\" with ID \"%s\" has multiple \"%s\".",
							xType, Utils.getId(basElems.item(0)), LvtbRoles.CONJ), warnOut);
				String conjLemma = Utils.getLemma(conjs.item(0));
				return Tuple.of(UDv2Relations.ADVCL, conjLemma);
			}
			// Participal SPC, adverbs in commas
			if (basElemTag.matches("v..p[pu].*|r.*|yr.*"))
				return Tuple.of(UDv2Relations.ADVCL, null);
			// Nominal SPC
			if (basElemTag.matches("n.*") || 	basElemTag.matches("y[np].*"))
				return Tuple.of(UDv2Relations.APPOS, null);
			// Adjective SPC
			if (basElemTag.matches("a.*|v..d.*|ya.*"))
				return Tuple.of(UDv2Relations.ACL, null);
		}

		return Tuple.of(UDv2Relations.DEP, null);
	}

	public Tuple<UDv2Relations, String> attrToUD(Node node, Node parent)
	throws XPathExpressionException
	{
		String tag = Utils.getTag(node);
		String lemma = Utils.getLemma(node);

		if (tag.matches("n.*"))
		{
			Matcher m = Pattern.compile("n...(.).*").matcher(tag);
			if (m.matches())
			{
				String caseLetter = m.group(1);
				String caseString = UDv2Feat.caseLetterToLCString(caseLetter);
				if (caseString != null || caseLetter.equals("0") || caseLetter.equals("_"))
					return Tuple.of(UDv2Relations.NMOD, caseString);
			}
		}
		if (tag.matches("y[np].*") || lemma.equals("%"))
			return Tuple.of(UDv2Relations.NMOD, null);
		if (tag.matches("r.*|yr.*"))
			return Tuple.of(UDv2Relations.ADVMOD, null);
		if (tag.matches("m[cf].*|xn.*"))
			return Tuple.of(UDv2Relations.NUMMOD, null);
		if (tag.matches("mo.*|xo.*|v..p.*|ya.*"))
			return Tuple.of(UDv2Relations.AMOD, null);
		if (tag.matches("p.*"))
			return Tuple.of(UDv2Relations.DET, null);
		if (tag.matches("a.*"))
		{
			if (lemma != null && lemma.matches("(man|mūs|tav|jūs|viņ|sav)ēj(ais|ā)|(daudz|vairāk|daž)(i|as)"))
				return Tuple.of(UDv2Relations.DET, null);
			return Tuple.of(UDv2Relations.AMOD, null);
		}
		// Both cases can provide mistakes, but there is no way to solve this
		// now.
		if (tag.matches("x[fu].*")) return Tuple.of(UDv2Relations.NMOD, null);
		if (tag.matches("xx.*")) return Tuple.of(UDv2Relations.AMOD, null);

		return Tuple.of(UDv2Relations.DEP, null);
	}

	public Tuple<UDv2Relations, String> advSitToUD(Node node, Node parent, PrintWriter warnOut)
	throws XPathExpressionException
	{
		String tag = Utils.getTag(node);
		if (tag.matches("mc.*|xn.*"))
			return Tuple.of(UDv2Relations.NUMMOD, null);

		// NB! Secība ir svarīga. Nevar pirms šī likt parastos nomenus!
		String xType = XPathEngine.get().evaluate("./children/xinfo/xtype", node);
		if (xType != null && xType.equals(LvtbXTypes.XPREP))
		{
			NodeList preps = (NodeList)XPathEngine.get().evaluate(
					"./children/xinfo/children/node[role='" + LvtbRoles.PREP + "']",
					node, XPathConstants.NODESET);
			if (preps.getLength() > 1)
				warn(String.format("\"%s\" with ID \"%s\" has multiple \"%s\".",
						xType, Utils.getId(node), LvtbRoles.PREP), warnOut);
			String prepLemma = Utils.getLemma(preps.item(0));
				return Tuple.of(UDv2Relations.OBL, prepLemma);
		}
		if (tag.matches("n.*|p.*|mo.*"))
		{
			Matcher m = Pattern.compile("(n...|p....|mo...)(.).*").matcher(tag);
			if (m.matches())
			{
				String caseLetter = m.group(2);
				String caseString = UDv2Feat.caseLetterToLCString(caseLetter);
				if (caseString != null || caseLetter.equals("0") || caseLetter.equals("_"))
					return Tuple.of(UDv2Relations.OBL, caseString);
			}
		}
		if (tag.matches("x[fo].*|y[npa].*"))
			return Tuple.of(UDv2Relations.OBL, null);

		String lemma = Utils.getLemma(node);

		if (tag.matches("r.*|yr.*") || lemma.equals("%"))
			return Tuple.of(UDv2Relations.ADVMOD, null);
		if (tag.matches("q.*|yd.*"))
			return Tuple.of(UDv2Relations.DISCOURSE, null);

		return Tuple.of(UDv2Relations.DEP, null);
	}

	public Tuple<UDv2Relations, String> detToUD(Node node, Node parent)
			throws XPathExpressionException
	{
		String tag = Utils.getTag(node);
		Matcher m = Pattern.compile("([na]...|[mp]....|v..pd..)(.).*").matcher(tag);
		if (m.matches())
		{
			String caseLetter = m.group(2);
			String caseString = UDv2Feat.caseLetterToLCString(caseLetter);
			if (caseString != null || caseLetter.equals("0") || caseLetter.equals("_"))
				return Tuple.of(UDv2Relations.OBL, caseString);
		}
		if (tag.matches("x.*|y[npa].*"))
			return Tuple.of(UDv2Relations.OBL, null);
		return Tuple.of(UDv2Relations.DEP, null);
	}

	public Tuple<UDv2Relations, String> noToUD(Node node, Node parent)
	throws XPathExpressionException
	{
		String tag = Utils.getTag(node);
		String lemma = Utils.getLemma(node);
		String subPmcType = XPathEngine.get().evaluate("./children/pmcinfo/pmctype", node);
		if (LvtbPmcTypes.ADDRESS.equals(subPmcType))
			return Tuple.of(UDv2Relations.VOCATIVE, null);
		if (LvtbPmcTypes.INTERJ.equals(subPmcType) || LvtbPmcTypes.PARTICLE.equals(subPmcType))
			return Tuple.of(UDv2Relations.DISCOURSE, null);
		if (lemma.matches("utt\\.|u\\.t\\.jpr\\.|u\\.c\\.|u\\.tml\\.|v\\.tml\\."))
			return Tuple.of(UDv2Relations.CONJ, null);
		if (tag != null && tag.matches("[qi].*|yd.*"))
			return Tuple.of(UDv2Relations.DISCOURSE, null);

		return Tuple.of(UDv2Relations.DEP, null);
	}

	public Tuple<UDv2Relations, String> predClToUD(Node node, Node parent)
	throws XPathExpressionException
	{
		String parentType = Utils.getAnyLabel(parent);

		// Parent is simple predicate
		if (parentType.equals(LvtbRoles.PRED))
			return Tuple.of(UDv2Relations.CCOMP, null);
		// Parent is complex predicate
		String grandPatentType = Utils.getAnyLabel(Utils.getPMLParent(parent));
		if (grandPatentType.equals(LvtbXTypes.XPRED))
			return Tuple.of(UDv2Relations.ACL, null);

		return Tuple.of(UDv2Relations.DEP, null);
	}

	public Tuple<UDv2Relations, String> subjClToUD(Node node, Node parent)
	throws XPathExpressionException
	{
		// Effective ancestor is predicate
		if (LvtbRoles.PRED.equals(Utils.getEffectiveLabel(parent)))
		{
			String parentTag = Utils.getTag(parent);
			Node pmlEffAncestor = Utils.getThisOrEffectiveAncestor(parent);
			// Hopefully either parent or effective ancestor is tagged as verb
			// or xPred.
			Node parentXChild = Utils.getPhraseNode(parent);
			Node ancXChild = Utils.getPhraseNode(pmlEffAncestor);
			// Parent is complex predicate
			if (LvtbXTypes.XPRED.equals(Utils.getPhraseType(parentXChild)) ||
					LvtbXTypes.XPRED.equals(Utils.getPhraseType(ancXChild)))
			{
				if (parentTag.matches("v..[^p].....p.*|v.*?\\[pas.*"))
					return Tuple.of(UDv2Relations.CSUBJ_PASS, null);
				if (parentTag.matches("v.*"))
					return Tuple.of(UDv2Relations.CSUBJ, null);
				String ancestorTag = Utils.getTag(pmlEffAncestor);
				if (ancestorTag.matches("v..[^p].....p.*|v.*?\\[pas.*"))
					return Tuple.of(UDv2Relations.CSUBJ_PASS, null);
				if (ancestorTag.matches("v.*"))
					return Tuple.of(UDv2Relations.CSUBJ, null);
			}
			// Parent is simple predicate
			else
			{
				if (parentTag.matches("v..[^p].....a.*|v..n.*"))
					return Tuple.of(UDv2Relations.CSUBJ, null);
				if (parentTag.matches("v..[^p].....p.*"))
					return Tuple.of(UDv2Relations.CSUBJ_PASS, null);
			}
		} else if (LvtbRoles.SUBJ.equals(Utils.getEffectiveLabel(parent)))
			return Tuple.of(UDv2Relations.ACL, null);

		return Tuple.of(UDv2Relations.DEP, null);
	}

	public Tuple<UDv2Relations, String> insToUD(Node node, Node parent, PrintWriter warnOut)
	throws XPathExpressionException
	{
		NodeList basElems = (NodeList)XPathEngine.get().evaluate(
				"./children/pminfo/children/node[role='" + LvtbRoles.PRED + "']",
				node, XPathConstants.NODESET);
		if (basElems!= null && basElems.getLength() > 1)
			warn (String.format("\"%s\" has multiple \"%s\".", LvtbPmcTypes.INSPMC, LvtbRoles.PRED),
					warnOut);
		if (basElems != null) return Tuple.of(UDv2Relations.PARATAXIS, null);
		return Tuple.of(UDv2Relations.DISCOURSE, null); // Washington (CNN) is left unidentified.
	}

	/**
	 * Print out the warning that role was not tranformed.
	 * @param node		node for which UD dependency should be obtained (use
	 *             		this node's lemma, morphology, etc.)
	 * @param parent	node which represents UD or enhanced UD parent for the
	 *                  node to be labeled
	 * @param enhanced  true, if role for enhanced dependency tree is being made
	 * @param warnOut	stream where to warn
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	protected void warnOnRole(Node node, Node parent, String lvtbRole,
							  boolean enhanced, PrintWriter warnOut)
	throws XPathExpressionException
	{
		String prefix = enhanced ? "Enhanced role" : "Role";
		String warning = String.format(
				"%s \"%s\" for node \"%s\" with respect to parent \"%s\" was not transformed.",
				prefix, lvtbRole, Utils.getId(node), Utils.getId(parent));
		warn(warning, warnOut);
	}

	/**
	 * TODO: move to sentence or sentence transformation engine
	 * Print out the given warning and add it to the warning register.
	 * @param warning	warning to print
	 * @param warnOut	stream where to print
	 */
	protected void warn(String warning, PrintWriter warnOut)
	{
		if (warnRegister!= null && !warnRegister.contains(warning))
		{
			warnOut.println(warning);
			warnRegister.add(warning);
		}
	}

}
