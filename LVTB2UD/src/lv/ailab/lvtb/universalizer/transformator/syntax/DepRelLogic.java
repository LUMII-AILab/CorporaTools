package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.UDv2Feat;
import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.*;
import lv.ailab.lvtb.universalizer.utils.Logger;
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
	 * Where all warnings goes.
	 */
	protected Logger logger;

	public DepRelLogic(Logger logger)
	{
		this.logger = logger;
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
	 * @return	UD DEPREL (including orphan, if parent is reduction and node is
	 * 			representing a core argument).
	 */
	public UDv2Relations depToUDBase(PmlANode node)
	{
		PmlANode pmlParent = node.getParent();
		String lvtbRole = node.getRole();
		UDv2Relations prelaminaryRole = depToUDLogic(node, pmlParent, lvtbRole).first;
		if (prelaminaryRole == UDv2Relations.DEP)
			warnOnRole(node, pmlParent, lvtbRole, false);

		PmlANode pmlEffParent = node.getEffectiveAncestor();
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
		return prelaminaryRole;
	}

	/**
	 * Enhanced relation between LVTB dependency roles and UD enhanced dependency
	 * role. Orphan roles are not assigned, warnings on DEP roles are given.
	 * @param node		node for which UD relation should be obtained (use this
	 *                  node's placement, role, tag and lemma)
	 * @return	UD dependency role and enhanced depency role postfix, if such is
	 * 			needed.
	 */
	public Tuple<UDv2Relations, String> depToUDEnhanced(PmlANode node)
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
	public Tuple<UDv2Relations, String> depToUDEnhanced(
			PmlANode node, PmlANode parent, String lvtbRole)
	{
		Tuple<UDv2Relations, String> res = depToUDLogic(node, parent, lvtbRole);
		if (res.first == null || UDv2Relations.DEP.equals(res.first))
			warnOnRole(node, parent, lvtbRole,true);
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
	public Tuple<UDv2Relations, String> depToUDLogic(
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
			default : return Tuple.of(UDv2Relations.DEP, null);
		}
	}

	public Tuple<UDv2Relations, String> subjToUD(PmlANode node, PmlANode parent)
	{
		String tag = node.getAnyTag();
		// Nominal++ subject
		// This procesing is somewhat tricky: it is allowed for nsubj and
		// nsubjpas to be [rci].*, but it is not allowed for nmod.
		if (tag.matches("[nampxy].*|v..pd.*|[rci].*|y[npa].*]"))
		{
			String parentTag = parent.getAnyTag();
			String parentEffType = parent.getEffectiveLabel();
			PmlANode pmlEffAncestor = parent.getThisOrEffectiveAncestor();
			// Hopefully either parent or effective ancestor is tagged as verb
			// or xPred.
			PmlANode parentXChild = parent.getPhraseNode();
			String parentXChildType = parentXChild == null ? null : parentXChild.getPhraseType();
			PmlANode ancXChild = pmlEffAncestor.getPhraseNode();
			String ancXChildType = ancXChild == null ? null : ancXChild.getPhraseType();

			// Parent is predicate
			if (parentEffType.equals(LvtbRoles.PRED))
			{
				// Parent is complex predicate
				if (LvtbXTypes.XPRED.equals(parentXChildType) ||
						LvtbXTypes.XPRED.equals(ancXChildType))
				{
					if (parentTag.matches("v..[^p].....p.*|v[^\\[]*\\[pas.*")) return Tuple.of(UDv2Relations.NSUBJ_PASS, null);
					if (parentTag.matches("v.*")) return Tuple.of(UDv2Relations.NSUBJ, null);
					String ancestorTag = pmlEffAncestor.getAnyTag();
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
					String reduction = parent.getReduction();
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
				if (LvtbXTypes.XPRED.equals(parentXChildType) ||
						LvtbXTypes.XPRED.equals(ancXChildType))
				{
					if (parentTag.matches("v..[^pn].....p.*|v[^\\[]+\\[pas.*")) return Tuple.of(UDv2Relations.NSUBJ_PASS, null);
					if (parentTag.matches("v..[^pn].....a.*|v[^\\[]+\\[(act|subst|ad[jv]|pronom).*")) return Tuple.of(UDv2Relations.NSUBJ, null);
					String ancestorTag = pmlEffAncestor.getAnyTag();
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

	public Tuple<UDv2Relations, String> objToUD(PmlANode node, PmlANode parent)
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

	public Tuple<UDv2Relations, String> spcToUD(PmlANode node, PmlANode parent)
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
				logger.doInsentenceWarning(String.format(
						"\"%s\" with ID \"%s\" has multiple \"%s\".",
						xType, node.getId(), LvtbRoles.PREP));
			if (preps.isEmpty())
			{
				logger.doInsentenceWarning(String.format(
						"\"%s\" with ID \"%s\" has no \"%s\".",
						xType, node.getId(), LvtbRoles.PREP));
				return Tuple.of(UDv2Relations.DEP, null);
			}
			if (basElems.size() > 1)
				logger.doInsentenceWarning(String.format(
						"\"%s\" with ID \"%s\" has multiple \"%s\".",
						xType, node.getId(), LvtbRoles.BASELEM));
			if (basElems.isEmpty())
			{
				logger.doInsentenceWarning(String.format(
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
				logger.doInsentenceWarning(String.format(
						"\"%s\" with ID \"%s\" has multiple \"%s\".",
						xType, node.getId(), LvtbRoles.CONJ));
			String conjLemma = conjs.get(0).getM().getLemma();
			String conjRed = conjs.get(0).getReduction();
			if (conjRed != null && !conjRed.isEmpty()) conjLemma = null;
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
		//String pmcType = XPathEngine.get().evaluate("./children/pmcinfo/pmctype", node);
		String pmcType = phrase == null || phrase.getNodeType() != PmlANode.Type.PMC
				? null
				: phrase.getPhraseType();
		if (pmcType != null && pmcType.equals(LvtbPmcTypes.SPCPMC))
		{
			List<PmlANode> basElems = phrase.getChildren(LvtbRoles.BASELEM);
			if (basElems.size() > 1)
				logger.doInsentenceWarning(String.format(
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
					logger.doInsentenceWarning(String.format(
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

	public Tuple<UDv2Relations, String> attrToUD(PmlANode node, PmlANode parent)
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

	public Tuple<UDv2Relations, String> advSitToUD(PmlANode node, PmlANode parent)
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
				logger.doInsentenceWarning(String.format(
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

	public Tuple<UDv2Relations, String> detToUD(PmlANode node, PmlANode parent)
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

	public Tuple<UDv2Relations, String> noToUD(PmlANode node, PmlANode parent)
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

	public Tuple<UDv2Relations, String> predClToUD(PmlANode node, PmlANode parent)
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

	public Tuple<UDv2Relations, String> subjClToUD(PmlANode node, PmlANode parent)
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

	public Tuple<UDv2Relations, String> insToUD(PmlANode node, PmlANode parent)
	{
		PmlANode phrase = node.getPhraseNode();
		if (phrase == null || phrase.getNodeType() != PmlANode.Type.PMC)
			return Tuple.of(UDv2Relations.DISCOURSE, null);

		List<PmlANode> preds = phrase.getChildren(LvtbRoles.PRED);
		if (preds!= null && preds.size() > 1)
			logger.doInsentenceWarning(String.format(
					"\"%s\" has multiple \"%s\".", LvtbPmcTypes.INSPMC, LvtbRoles.PRED));
		if (preds != null) return Tuple.of(UDv2Relations.PARATAXIS, null);
		return Tuple.of(UDv2Relations.DISCOURSE, null); // Washington (CNN) is left unidentified.
	}

	/**
	 * Print out the warning that role was not tranformed.
	 * @param node		node for which UD dependency should be obtained (use
	 *             		this node's lemma, morphology, etc.)
	 * @param parent	node which represents UD or enhanced UD parent for the
	 *                  node to be labeled
	 * @param enhanced  true, if role for enhanced dependency tree is being made
	 */
	protected void warnOnRole(
			PmlANode node, PmlANode parent, String lvtbRole, boolean enhanced)
	{
		String prefix = enhanced ? "Enhanced role" : "Role";
		String warning = String.format(
				"%s \"%s\" for node \"%s\" with respect to parent \"%s\" was not transformed.",
				prefix, lvtbRole, node.getId(), parent.getId());
		logger.doInsentenceWarning(warning);
	}


}
