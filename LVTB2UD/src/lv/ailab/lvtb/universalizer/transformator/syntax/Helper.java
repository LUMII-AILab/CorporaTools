package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.pml.*;
import lv.ailab.lvtb.universalizer.pml.utils.PmlANodeListUtils;
import lv.ailab.lvtb.universalizer.transformator.StandardLogger;

import java.util.List;

public class Helper
{
	//TODO is there any problem to use this for xParticle?
	/** Get conj lemma from xSimile or prepLemma from xPrep.
	 * @param phrase		phrase where to search conj/prep
	 * @param prepRole		what to look for - LvtbRoles.PREP or LvtbRoles.CONJ
	 * @return	lemma or null, if nothing found.
	 */
	public static String getXSimileConjOrXPrepPrepLemma(
			PmlANode phrase, String prepRole)
	{
		if (phrase == null) return null;
		String phraseNodeId = phrase.getParent().getId();
		String xType = phrase.getPhraseType();
		List<PmlANode> preps = phrase.getChildren(prepRole);
		if (preps.size() > 1)
			StandardLogger.l.doInsentenceWarning(String.format(
					"\"%s\" with ID \"%s\" has multiple \"%s\".",
					xType, phraseNodeId, prepRole));
		String prepLemma = null;
		if (preps.size() > 0) // One weird case of reduced conjunction has no conjs in xSimile.
		{
			PmlANode prep = preps.get(0);
			PmlMNode morpho = prep.getM();
			String combinedPrepLemma = null;
			// If there is no morphology, but there is an xFunctor or coordination,
			// use the lemma of the first basElem or crdPart.
			PmlANode phraseConj = prep.getPhraseNode();
			String phraseType = phraseConj == null ? null : phraseConj.getPhraseType();
			while (morpho == null && combinedPrepLemma == null && phraseConj != null &&
					(phraseType.equals(LvtbXTypes.XFUNCTOR) || phraseType.equals(LvtbCoordTypes.CRDPARTS)))
			{
				if ((phraseType.equals(LvtbXTypes.XFUNCTOR)))
				{
					combinedPrepLemma = "";
					for (PmlANode n : PmlANodeListUtils.asOrderedList(phraseConj.getChildren()))
						if (n.getM() != null)
							combinedPrepLemma = combinedPrepLemma + "_" +n.getM().getLemma();
					combinedPrepLemma = combinedPrepLemma.replaceAll("^_+", "");
					combinedPrepLemma = combinedPrepLemma.replaceAll("_+$", "");
				}
				else if (phraseType.equals(LvtbCoordTypes.CRDPARTS))
				{
					PmlANode firstXBase = PmlANodeListUtils.getFirstByDescOrd(
							phraseConj.getChildren(LvtbRoles.BASELEM));
					PmlANode firstCrdPart = PmlANodeListUtils.getFirstByDescOrd(
							phraseConj.getChildren(LvtbRoles.CRDPART));
					PmlANode resultPrep = null;
					if (firstXBase != null) resultPrep = firstXBase;
					else if (firstCrdPart != null) resultPrep = firstCrdPart;
					if (resultPrep != null)
					{
						phraseConj = resultPrep.getPhraseNode();
						phraseType = phraseConj == null ? null : phraseConj.getPhraseType();
						morpho = resultPrep.getM();
					}
				}
			}
			if (combinedPrepLemma != null && !combinedPrepLemma.isEmpty())
				prepLemma = combinedPrepLemma;
			else if (morpho != null) prepLemma = morpho.getLemma();

			String prepRed = prep.getReduction();
			if (prepRed != null && !prepRed.isEmpty()) prepLemma = null;
		}
		else StandardLogger.l.doInsentenceWarning(String.format(
				"\"%s\" with ID \"%s\" has no \"%s\".",
				xType, phraseNodeId, prepRole));
		return prepLemma;
	}
}
