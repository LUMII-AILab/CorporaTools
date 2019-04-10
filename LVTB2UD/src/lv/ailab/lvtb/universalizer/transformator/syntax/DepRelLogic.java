package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.UDv2Feat;
import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.*;
import lv.ailab.lvtb.universalizer.transformator.StandardLogger;
import lv.ailab.lvtb.universalizer.utils.Tuple;

import java.util.List;
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
	 * Generic relation between LVTB dependency roles and UD DEPREL.
	 * @param node		node for which UD DEPREL should be obtained (use this
	 *                  node's placement, role, tag and lemma)
	 * @param isEligibleForOrphan	is this node to become a child for a core
	 *                              argument or adjunct elevated instead of
	 *                              ellipted predicate? (basically true if
	 *                              EllipsisLogic.newParent() said so)
	 * @return	UD DEPREL (including orphan, if parent is reduction and node is
	 * 			representing a core argument).
	 */
	public static UDv2Relations depToUDBase(PmlANode node, boolean isEligibleForOrphan)
	{
		PmlANode pmlParent = node.getParent();
		String lvtbRole = node.getRole();
		UDv2Relations prelaminaryRole = depToUDLogic(node, pmlParent, lvtbRole).first;
		if (prelaminaryRole == UDv2Relations.DEP)
			warnOnRole(node, pmlParent, lvtbRole, false);
		if (isEligibleForOrphan && canBecomeOrphan(prelaminaryRole))
			return UDv2Relations.ORPHAN;
		else return prelaminaryRole;

		/*PmlANode pmlEffParent = node.getEffectiveAncestor();
		if ((pmlParent.isPureReductionNode() || pmlEffParent.isPureReductionNode())
				&& (prelaminaryRole.equals(UDv2Relations.NSUBJ)
					|| prelaminaryRole.equals(UDv2Relations.NSUBJ_PASS)
					|| prelaminaryRole.equals(UDv2Relations.OBJ)
					|| prelaminaryRole.equals(UDv2Relations.IOBJ)
					|| prelaminaryRole.equals(UDv2Relations.CSUBJ)
					|| prelaminaryRole.equals(UDv2Relations.CSUBJ_PASS)
					|| prelaminaryRole.equals(UDv2Relations.CCOMP)
					|| prelaminaryRole.equals(UDv2Relations.XCOMP)))
			return UDv2Relations.ORPHAN;
		return prelaminaryRole;*/
	}

	/**
	 * Enhanced relation between LVTB dependency roles and UD enhanced dependency
	 * role. Orphan roles are not assigned, warnings on DEP roles are given.
	 * @param node		node for which UD relation should be obtained (use this
	 *                  node's placement, role, tag and lemma)
	 * @return	UD dependency role and enhanced depency role postfix, if such is
	 * 			needed.
	 */
	public static Tuple<UDv2Relations, String> depToUDEnhanced(PmlANode node)
	{
		return depToUDEnhanced(
				node, node.getParent(), node.getRole());
	}

	/**
	 * Enhanced relation between LVTB dependency roles and UD enhanced dependency
	 * role. Orphan roles are not assigned, warnings on DEP roles are given.
	 * @param node		node for which UD dependency should be obtained (use
	 *             		this node's lemma, morphology, etc.)
	 * @param parent	node which represents UD or enhanced UD parent for the
	 *                  node to be labeled
	 * @return	UD dependency role and enhanced depency role postfix, if such is
	 * 			needed.
	 */
	public static Tuple<UDv2Relations, String> depToUDEnhanced(
			PmlANode node, PmlANode parent, String lvtbRole)
	{
		Tuple<UDv2Relations, String> res = depToUDLogic(node, parent, lvtbRole);
		if (res.first == null || UDv2Relations.DEP.equals(res.first))
			warnOnRole(node, parent, lvtbRole,true);
		//System.out.println("Enh. child: " + node.getId() + ", enh. parent: " + parent.getId() + " -> " + res.first);
		return res;
	}

	/**
	 * Generic relation between LVTB dependency roles and UD role. Orphan roles
	 * are not assigned, warnings on DEP roles are not given.
	 * @param node		node for which UD dependency should be obtained (use
	 *             		this node's lemma, morphology, etc.)
	 * @param parent	node which represents UD or enhanced UD parent for the
	 *                  node to be labeled
	 * @return	UD dependency role and enhanced depency role postfix, if such is
	 * 			needed.
	 */
	public static Tuple<UDv2Relations, String> depToUDLogic(
			PmlANode node, PmlANode parent, String lvtbRole)
	{
		// Simple dependencies.

		switch (lvtbRole)
		{
			case LvtbRoles.SUBJ : return subjToUD(node, parent);
			case LvtbRoles.OBJ : return objToUD(node, parent);
			case LvtbRoles.SPC : return spcToUD(node, parent);
			case LvtbRoles.ATTR : return attrToUD(node, parent);
			case LvtbRoles.ADV :
			case LvtbRoles.SIT :
				return advSitToUD(node, parent);
			case LvtbRoles.DET : return detToUD(node, parent);
			case LvtbRoles.NO: return noToUD(node, parent);

			// Clausal dependencies.
			case LvtbRoles.PREDCL : return predClToUD(node, parent);
			case LvtbRoles.SUBJCL : return subjClToUD(node, parent);
			case LvtbRoles.OBJCL : return Tuple.of(UDv2Relations.CCOMP, null);
			case LvtbRoles.ATTRCL :
			case LvtbRoles.APPCL : return Tuple.of(UDv2Relations.ACL, null);
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
			case LvtbRoles.INS : return insToUD(node, parent);
			case LvtbRoles.DIRSP : return Tuple.of(UDv2Relations.PARATAXIS, null);

			// Other
			// TODO wait for answer in https://github.com/UniversalDependencies/docs/issues/594
			case LvtbRoles.REPEAT : return Tuple.of(UDv2Relations.REPARANDUM, null);
			case LvtbRoles.ELLIPSIS_TOKEN: return ellipsisTokToUD(node, parent);
			default : return Tuple.of(UDv2Relations.DEP, null);
		}
	}

	public static Tuple<UDv2Relations, String> subjToUD(PmlANode node, PmlANode parent)
	{
		String tag = node.getAnyTag();
		UDv2Relations resRoleActive;
		UDv2Relations resRolePasive;

		// First: is this nominal or clausal subject?
		if (tag.matches("[nampxy].*|v..pd.*|[rci].*|y[npa].*]"))
		{
			resRoleActive = UDv2Relations.NSUBJ;
			resRolePasive = UDv2Relations.NSUBJ_PASS;
		}
		else if (tag.matches("v..n.*"))
		{
			resRoleActive = UDv2Relations.CSUBJ;
			resRolePasive = UDv2Relations.CSUBJ_PASS;
		}
		else return Tuple.of(UDv2Relations.DEP, null);

		// Second: is parent active or passive?

		// Here we get nodes that will tell us if parent is pred or xPred
		String parentTag = parent.getAnyTag();
		String parentEffType = parent.getEffectiveLabel();
		PmlANode parentXChild = parent.getPhraseNode();
		String parentXChildType = parentXChild == null ? null : parentXChild.getPhraseType();

		PmlANode pmlEffAncestor = parent.getThisOrEffectiveAncestor();
		String effAncestorType = pmlEffAncestor.getEffectiveLabel();
		PmlANode ancXChild = pmlEffAncestor.getPhraseNode();
		String ancXChildType = ancXChild == null ? null : ancXChild.getPhraseType();

		// But if parent is basElem, we need to know, if grandparent is spc
		PmlANode pmlEffAncestor2 = pmlEffAncestor.getThisOrEffectiveAncestor();
		String effAncestorType2 = pmlEffAncestor2.getEffectiveLabel();

		boolean isParentRealVerbal = effAncestorType.equals(LvtbRoles.PRED)
				|| effAncestorType.equals(LvtbRoles.SPC)
				|| (effAncestorType.equals(LvtbRoles.BASELEM)
				&& effAncestorType2.equals(LvtbRoles.SPC));
		boolean isParentOtherBasElem = parentEffType.equals(LvtbRoles.BASELEM);

		if (!isParentRealVerbal && !isParentOtherBasElem)
			return Tuple.of(UDv2Relations.DEP, null);

		// Parent is complex predicate something
		if (LvtbXTypes.XPRED.equals(parentXChildType) ||
				LvtbXTypes.XPRED.equals(ancXChildType))
		{
			if (parentTag.matches("v..[^p].....p.*|v..pd...p.*|v[^\\[]*\\[pas.*"))
				return Tuple.of(resRolePasive, null);
			if (parentTag.matches("v.*") && isParentRealVerbal ||
					parentTag.matches("v..[^pn].....a.*|v[^\\[]+\\[(act|subst|ad[jv]|pronom).*"))
				return Tuple.of(resRoleActive, null);

			String ancestorTag = pmlEffAncestor.getAnyTag();
			if (ancestorTag.matches("v..[^p].....p.*|v..pd...p.*|v[^\\[]*\\[pas.*"))
				return Tuple.of(resRolePasive, null);
			if (ancestorTag.matches("v.*") && isParentRealVerbal ||
					parentTag.matches("v..[^pn].....a.*|v[^\\[]+\\[(act|subst|ad[jv]|pronom).*"))
				return Tuple.of(resRoleActive, null);
		}

		// Parent is simple predicate/spc/basElem
		else
		{
			if (parentTag.matches("v..[^p].....p.*|v..pd...p.*"))
				return Tuple.of(resRolePasive, null);
			//if (parentTag.matches("v..[^p].....a.*|v..pd...a.*|v..pu.*|v..n.*"))
			if (parentTag.matches("v.*"))
				return Tuple.of(resRoleActive, null);

			String reduction = parent.getReduction();
			if (reduction != null && !reduction.isEmpty())
			{
				if (reduction.matches("v..[^p].....p.*|v..pd...p.*"))
					return Tuple.of(resRolePasive, null);
				if (reduction.matches("v.*"))
					return Tuple.of(resRoleActive, null);
			}
		}

		return Tuple.of(UDv2Relations.DEP, null);
	}

	public static Tuple<UDv2Relations, String> objToUD(PmlANode node, PmlANode parent)
	{
		String tag = node.getAnyTag();
		String parentTag = parent.getAnyTag();
		PmlANode phraseChild = node.getPhraseNode();
		if (phraseChild != null)
		{
			String constLabel = phraseChild.getAnyLabel();
			if (LvtbXTypes.XPREP.matches(constLabel)) return Tuple.of(UDv2Relations.IOBJ, null);
		}
		if (tag.matches(".*?\\[(pre|post).*]")) return Tuple.of(UDv2Relations.IOBJ, null);
		if (tag.matches("[na]...a.*|[pm]....a.*|v..p...a.*")) return Tuple.of(UDv2Relations.OBJ, null);
		if (tag.matches("[na]...n.*|[pm]....n.*|v..p...n.*") && parentTag.matches("v..d.*"))
			return Tuple.of(UDv2Relations.OBJ, null);
		return Tuple.of(UDv2Relations.IOBJ, null);
	}

	public static Tuple<UDv2Relations, String> spcToUD(PmlANode node, PmlANode parent)
	{
		String tag = node.getAnyTag();
		String parentTag = parent.getAnyTag();

		// If parent is something reduced to punctuation mark, use reduction
		// tag instead.
		if (parentTag.matches("z.*"))
		{
			String parentRed = parent.getReduction();
			if (parentRed != null && parentRed.length() > 0)
				parentTag = parentRed;
		}

		String parentEffRole = parent.getEffectiveLabel();
		// Infinitive SPC
		if (tag.matches("v..n.*"))
		{
			PmlANode pmlEfParent = parent.getThisOrEffectiveAncestor();
			String effParentType = pmlEfParent.getAnyLabel();
			if (parentTag.matches("v..([^p]|p[^d]).*") || LvtbXTypes.XPRED.equals(effParentType))
				return Tuple.of(UDv2Relations.CCOMP, null); // It is impposible safely to distinguish xcomp for now.
			if (parentTag.matches("v..pd.*")) return Tuple.of(UDv2Relations.XCOMP, null);
			if (parentTag.matches("[nampx].*|y[npa].*")) return Tuple.of(UDv2Relations.ACL, null);
		}

		PmlANode phrase = node.getPhraseNode();
		//String xType = XPathEngine.get().evaluate("./children/xinfo/xtype", node);
		String xType = phrase == null || phrase.getNodeType() != PmlANode.Type.X
				? null
				: phrase.getPhraseType();
		// prepositional SPC
		if (xType != null && xType.equals(LvtbXTypes.XPREP))
		{
			List<PmlANode> preps = phrase.getChildren(LvtbRoles.PREP);
			List<PmlANode> basElems = phrase.getChildren(LvtbRoles.BASELEM);

			// NB! Secība ir svarīga. Nevar pirms šī likt parastos nomenus!
			if (preps.size() > 1)
				StandardLogger.l.doInsentenceWarning(String.format(
						"\"%s\" with ID \"%s\" has multiple \"%s\".",
						xType, node.getId(), LvtbRoles.PREP));
			if (preps.isEmpty())
			{
				StandardLogger.l.doInsentenceWarning(String.format(
						"\"%s\" with ID \"%s\" has no \"%s\".",
						xType, node.getId(), LvtbRoles.PREP));
				return Tuple.of(UDv2Relations.DEP, null);
			}
			if (basElems.size() > 1)
				StandardLogger.l.doInsentenceWarning(String.format(
						"\"%s\" with ID \"%s\" has multiple \"%s\".",
						xType, node.getId(), LvtbRoles.BASELEM));
			if (basElems.isEmpty())
			{
				StandardLogger.l.doInsentenceWarning(String.format(
						"\"%s\" with ID \"%s\" has no \"%s\".",
						xType, node.getId(), LvtbRoles.BASELEM));
				return Tuple.of(UDv2Relations.DEP, null);
			}
			String baseElemTag = basElems.get(0).getAnyTag();
			PmlMNode prepM = preps.get(0).getM();
			String prepRed = preps.get(0).getReduction();
			// prepM is null in the rare cases when prep is coordinated.
			String prepLemma = prepM == null ? null : prepM.getLemma();
			if (prepRed != null && !prepRed.isEmpty()) prepLemma = null;
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
			List<PmlANode> conjs = phrase.getChildren(LvtbRoles.CONJ);
			if (conjs.size() > 1)
				StandardLogger.l.doInsentenceWarning(String.format(
						"\"%s\" with ID \"%s\" has multiple \"%s\".",
						xType, node.getId(), LvtbRoles.CONJ));
			String conjLemma = null;
			if (conjs.size() > 0) // One weird case of reduced conjunction has no conjs.
			{
				conjLemma = conjs.get(0).getM().getLemma();
				String conjRed = conjs.get(0).getReduction();
				if (conjRed != null && !conjRed.isEmpty()) conjLemma = null;
			}
			else StandardLogger.l.doInsentenceWarning(String.format(
					"\"%s\" with ID \"%s\" has no \"%s\".",
					xType, node.getId(), LvtbRoles.CONJ));
			if (parentTag.matches("n.*|y[np].*") && tag.matches("[nampx].*|y[npa].*|v..pd.*"))
				return Tuple.of(UDv2Relations.NMOD, conjLemma);
			return Tuple.of(UDv2Relations.OBL, conjLemma);
		}
		// Simple nominal SPC
		if (tag.matches("[na]...[g].*|[pm]....[g].*|v..p...[g].*"))
			return Tuple.of(UDv2Relations.OBL, UDv2Feat.CASE_GEN.value.toLowerCase());
		if (tag.matches("x.*|y[npa].*") && parentTag.matches("v..p....ps.*"))
			return Tuple.of(UDv2Relations.OBL, null);
		if (tag.matches("[na]...[adnl].*|[pm]....[adnl].*|v..pd..[adnl].*|x.*|y[npa].*"))
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
		//String pmcType = XPathEngine.get().evaluate("./children/pmcinfo/pmctype", node);
		String pmcType = phrase == null || phrase.getNodeType() != PmlANode.Type.PMC
				? null
				: phrase.getPhraseType();
		if (pmcType != null && pmcType.equals(LvtbPmcTypes.SPCPMC))
		{
			List<PmlANode> basElems = phrase.getChildren(LvtbRoles.BASELEM);
			if (basElems.size() > 1)
				StandardLogger.l.doInsentenceWarning(String.format(
						"\"%s\" has multiple \"%s\".", pmcType, LvtbRoles.BASELEM));
			String basElemTag = basElems.get(0).getAnyTag();
			// TODO test this bugfix
			PmlANode basElemPhrase = basElems.get(0).getPhraseNode();
			String basElemXType = basElemPhrase == null
					? null
					: basElems.get(0).getPhraseType();

			// SPC with comparison
			if (basElemPhrase != null && basElemPhrase.getNodeType() == PmlANode.Type.X
					&& LvtbXTypes.XSIMILE.equals(basElemXType))
			{
				List<PmlANode> conjs = basElemPhrase.getChildren(LvtbRoles.CONJ);
				if (conjs.size() > 1)
					StandardLogger.l.doInsentenceWarning(String.format(
							"\"%s\" with ID \"%s\" has multiple \"%s\".",
							xType, basElems.get(0).getId(), LvtbRoles.CONJ));
				String conjLemma = conjs.get(0).getM().getLemma();
				String conjRed = conjs.get(0).getReduction();
				if (conjRed != null && !conjRed.isEmpty()) conjLemma = null;
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

	public static Tuple<UDv2Relations, String> attrToUD(PmlANode node, PmlANode parent)
	{
		String tag = node.getAnyTag();
		PmlMNode mNode = node.getM();
		String lemma = mNode == null ? null : mNode.getLemma();

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
		if (tag.matches("y[np].*") || lemma != null && lemma.equals("%"))
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

	public static Tuple<UDv2Relations, String> advSitToUD(PmlANode node, PmlANode parent)
	{
		String tag = node.getAnyTag();
		if (tag.matches("mc.*|xn.*"))
			return Tuple.of(UDv2Relations.NUMMOD, null);

		// NB! Secība ir svarīga. Nevar pirms šī likt parastos nomenus!
		PmlANode phrase = node.getPhraseNode();
		//String xType = XPathEngine.get().evaluate("./children/xinfo/xtype", node);
		String xType = phrase == null ? null : phrase.getPhraseType();
		if (xType != null && phrase.getNodeType() == PmlANode.Type.X &&
				xType.equals(LvtbXTypes.XPREP))
		{
			List<PmlANode> preps = phrase.getChildren(LvtbRoles.PREP);
			if (preps.size() > 1)
				StandardLogger.l.doInsentenceWarning(String.format(
						"\"%s\" with ID \"%s\" has multiple \"%s\".",
						xType, node.getId(), LvtbRoles.PREP));
			String prepLemma = preps.get(0).getM().getLemma();
			String prepRed = preps.get(0).getReduction();
			if (prepRed != null && !prepRed.isEmpty()) prepLemma = null;
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

		PmlMNode mNode = node.getM();
		String lemma = mNode == null ? null : mNode.getLemma();

		if (tag.matches("r.*|yr.*") || lemma!= null && lemma.equals("%"))
			return Tuple.of(UDv2Relations.ADVMOD, null);
		if (tag.matches("q.*|yd.*"))
			return Tuple.of(UDv2Relations.DISCOURSE, null);

		return Tuple.of(UDv2Relations.DEP, null);
	}

	public static Tuple<UDv2Relations, String> detToUD(PmlANode node, PmlANode parent)
	{
		String tag = node.getAnyTag();
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

	public static Tuple<UDv2Relations, String> noToUD(PmlANode node, PmlANode parent)
	{
		String tag = node.getAnyTag();
		PmlMNode mNode = node.getM();
		String lemma = mNode == null ? "" : mNode.getLemma();
		PmlANode phrase = node.getPhraseNode();
		String subPmcType = phrase == null || phrase.getNodeType() != PmlANode.Type.PMC
				? null
				: phrase.getPhraseType();
		//String subPmcType = XPathEngine.get().evaluate("./children/pmcinfo/pmctype", node);
		if (LvtbPmcTypes.ADDRESS.equals(subPmcType))
			return Tuple.of(UDv2Relations.VOCATIVE, null);
		if (LvtbPmcTypes.INTERJ.equals(subPmcType) || LvtbPmcTypes.PARTICLE.equals(subPmcType))
			return Tuple.of(UDv2Relations.DISCOURSE, null);
		if (lemma.matches("utt\\.|u\\.t\\.jpr\\.|u\\.c\\.|u\\.tml\\.|v\\.tml\\."))
			return Tuple.of(UDv2Relations.CONJ, null);
		if (tag != null && tag.matches("[qi].*|yd.*|xx.*"))
			return Tuple.of(UDv2Relations.DISCOURSE, null);

		return Tuple.of(UDv2Relations.DEP, null);
	}

	public static Tuple<UDv2Relations, String> predClToUD(PmlANode node, PmlANode parent)
	{
		String parentType = parent.getAnyLabel();

		// Parent is simple predicate
		if (parentType.equals(LvtbRoles.PRED))
			return Tuple.of(UDv2Relations.CCOMP, null);
		// Parent is complex predicate
		String grandPatentType = parent.getParent().getAnyLabel();
		if (grandPatentType.equals(LvtbXTypes.XPRED))
			return Tuple.of(UDv2Relations.ACL, null);

		return Tuple.of(UDv2Relations.DEP, null);
	}

	public static Tuple<UDv2Relations, String> subjClToUD(PmlANode node, PmlANode parent)
	{
		// Effective ancestor is predicate
		if (LvtbRoles.PRED.equals(parent.getEffectiveLabel()))
		{
			String parentTag = parent.getAnyTag();
			PmlANode pmlEffAncestor = parent.getThisOrEffectiveAncestor();
			// Hopefully either parent or effective ancestor is tagged as verb
			// or xPred.
			PmlANode parentXChild = parent.getPhraseNode();
			PmlANode ancXChild = pmlEffAncestor.getPhraseNode();
			// Parent is complex predicate
			if (parentXChild != null && LvtbXTypes.XPRED.equals(parentXChild.getPhraseType()) ||
					ancXChild != null && LvtbXTypes.XPRED.equals(ancXChild.getPhraseType()))
			{
				if (parentTag.matches("v..[^p].....p.*|v.*?\\[pas.*"))
					return Tuple.of(UDv2Relations.CSUBJ_PASS, null);
				if (parentTag.matches("v.*"))
					return Tuple.of(UDv2Relations.CSUBJ, null);
				String ancestorTag = pmlEffAncestor.getAnyTag();
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
		} else if (LvtbRoles.SUBJ.equals(parent.getEffectiveLabel()))
			return Tuple.of(UDv2Relations.ACL, null);

		return Tuple.of(UDv2Relations.DEP, null);
	}

	public static Tuple<UDv2Relations, String> insToUD(PmlANode node, PmlANode parent)
	{
		PmlANode phrase = node.getPhraseNode();
		if (phrase == null || phrase.getNodeType() != PmlANode.Type.PMC)
			return Tuple.of(UDv2Relations.DISCOURSE, null);

		List<PmlANode> preds = phrase.getChildren(LvtbRoles.PRED);
		if (preds!= null && preds.size() > 1)
			StandardLogger.l.doInsentenceWarning(String.format(
					"\"%s\" has multiple \"%s\".", LvtbPmcTypes.INSPMC, LvtbRoles.PRED));
		if (preds != null && !preds.isEmpty()) return Tuple.of(UDv2Relations.PARATAXIS, null);
		return Tuple.of(UDv2Relations.DISCOURSE, null); // Washington (CNN) is left unidentified.
	}

	public static Tuple<UDv2Relations, String> ellipsisTokToUD(PmlANode node, PmlANode parent)
	{
		String tag = node.getAnyTag();
		//System.out.println("Node " + node.getId() + ": " + tag + "; parent " + parent.getId() + ": " + parent.getAnyTag());
		if (tag.matches("z.*")) return Tuple.of(UDv2Relations.PUNCT, null);
		return Tuple.of(UDv2Relations.DEP, null);
	}

	/**
	 * Print out the warning that role was not tranformed.
	 * @param node		node for which UD dependency should be obtained (use
	 *             		this node's lemma, morphology, etc.)
	 * @param parent	node which represents UD or enhanced UD parent for the
	 *                  node to be labeled
	 * @param enhanced  true, if role for enhanced dependency tree is being made
	 */
	protected static void warnOnRole(
			PmlANode node, PmlANode parent, String lvtbRole, boolean enhanced)
	{
		String prefix = enhanced ? "Enhanced role" : "Role";
		String warning = String.format(
				"%s \"%s\" for node \"%s\" with respect to parent \"%s\" was not transformed.",
				prefix, lvtbRole, node.getId(), parent.getId());
		StandardLogger.l.doInsentenceWarning(warning);
	}

	/**
	 * Relation betwen LVTB roles and UD deprel customised for processing
	 * controlled and raised subject links
	 * @param node		node for which UD dependency should be obtained (use
	 *             		this node's lemma, morphology, etc.)
	 * @param parent	node which represents UD or enhanced UD parent for the
	 *                  node to be labeled
	 * @param isClausal	clausal subject (true) or ordinary (false)
	 * @return	UD dependency role and enhanced depency role postfix, if such is
	 * 			needed.
	 */
	// TODO:: customise better!
	public static Tuple<UDv2Relations, String> cRSubjToUD(
			PmlANode node, PmlANode parent, boolean isClausal)
	{
		String tag = node.getAnyTag();
		UDv2Relations resRoleActive = isClausal
				? UDv2Relations.CSUBJ : UDv2Relations.NSUBJ;
		UDv2Relations resRolePasive = isClausal
				? UDv2Relations.CSUBJ_PASS : UDv2Relations.NSUBJ_PASS;
		// Nominal++ subject
		if (tag.matches("[nampxy].*|v..pd.*|[rci].*|y[npa].*]"))
		{
			String parentTag = parent.getAnyTag();
			// Hopefully either parent or effective ancestor is tagged as verb
			// or xPred.
			PmlANode parentXChild = parent.getPhraseNode();
			String parentXChildType = parentXChild == null ? null : parentXChild.getPhraseType();

			// As this is called only for enhanced subject links, we can safely
			// assume that parent is something predicative

			// Parent is complex predicate
			if (LvtbXTypes.XPRED.equals(parentXChildType))// || LvtbXTypes.XPRED.equals(ancXChildType))
			{
				if (parentTag.matches("v..[^p].....p.*|v[^\\[]*\\[pas.*|v..pd...p.*"))
					return Tuple.of(resRolePasive, null);
				if (parentTag.matches("v.*"))
					return Tuple.of(resRoleActive, null);
			}
			// Parent is simple predicate or simple part of something like predicate
			else
			{
				if (parentTag.matches("v..[^p].....p.*|v..pd...p.*"))
					return Tuple.of(resRolePasive, null);
				if (parentTag.matches("v.*"))
					return Tuple.of(resRoleActive, null);

				String reduction = parent.getReduction();
				if (reduction != null && !reduction.isEmpty())
				{
					if (reduction.matches("v..[^p].....p.*|v..pd...p.*"))
						return Tuple.of(resRolePasive, null);
					if (reduction.matches("v.*"))
						return Tuple.of(resRoleActive, null);
				}
				// TODO nominal parent?
			}
		}
		// TODO infinite subject?
		return Tuple.of(UDv2Relations.DEP, null);
	}

	/**
	 * Quoted https://universaldependencies.org/u/overview/specific-syntax.html#ellipsis
	 * "If the elided element is a predicate and the promoted element is one of
	 * its arguments or adjuncts, we use the orphan relation when attaching
	 * other non-functional dependents to the promoted head."
	 * Our interpretation is that non-functional means roles from blocks
	 * Nominals, Clauses, Modifier words in https://universaldependencies.org/u/dep/index.html
	 * @param role	role to check
	 * @return	wheather it should be orphan when becomes dependant of
	 * 			something lifted instead of predicate
	 */
	public static boolean canBecomeOrphan(UDv2Relations role)
	{
		return
				// Nominals: core arguments
				role == UDv2Relations.NSUBJ || role == UDv2Relations.NSUBJ_PASS
				|| role == UDv2Relations.OBJ || role == UDv2Relations.IOBJ
				// Clauses: core arguments
				|| role == UDv2Relations.CSUBJ || role == UDv2Relations.CSUBJ_PASS
				|| role == UDv2Relations.CCOMP || role == UDv2Relations.XCOMP
				// Nominals: non-core dependents
				|| role == UDv2Relations.OBL || role == UDv2Relations.VOCATIVE
				|| role == UDv2Relations.EXPL || role == UDv2Relations.DISLOCATED
				// Clauses: non-core dependents
				|| role == UDv2Relations.ADVCL
				// Modifier words: non-core dependents
				|| role == UDv2Relations.ADVMOD || role == UDv2Relations.DISCOURSE
				// Nominals: nominal dependents
				|| role == UDv2Relations.NMOD || role == UDv2Relations.APPOS
				|| role == UDv2Relations.NUMMOD
				// Clauses: nominal dependents
				|| role == UDv2Relations.ACL
				// Modifier words: nominal dependents
				|| role == UDv2Relations.AMOD;
	}
}
