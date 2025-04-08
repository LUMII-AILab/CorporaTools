package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.UDv2Feat;
import lv.ailab.lvtb.universalizer.conllu.UDv2PosTag;
import lv.ailab.lvtb.universalizer.pml.LvtbXTypes;
import lv.ailab.lvtb.universalizer.transformator.morpho.UPosLogic;

import java.util.ArrayList;

/**
 * Logic for obtaining Universal Dependency feature information based on LVTB
 * syntax information.
 *
 * Created on 2025-01-22.
 * @author Lauma
 */
public class FeatsLogic
{
	public static ArrayList<UDv2Feat> getUFeatsFromPhraseNode(
			String phraseType, String phraseTag)
	{
		ArrayList<UDv2Feat> res = new ArrayList<>();
		if (phraseTag == null) phraseTag = ""; // To avoid null pointer exceptions.

		if (phraseType.equals(LvtbXTypes.XSIMILE))
		{
			if (phraseTag.matches("[^\\[]*\\[compy.*")) res.add(UDv2Feat.EXTPOS_ADV);
			else if (phraseTag.matches("^p[^\\[]*\\[simy.*")) res.add(UDv2Feat.EXTPOS_DET);
			else if (phraseTag.matches("^r[^\\[]*\\[simy.*")) res.add(UDv2Feat.EXTPOS_ADV);
		}
		else if (phraseType.equals(LvtbXTypes.XFUNCTOR))
		{
			UDv2PosTag upos = UPosLogic.getUPosTag("", "", phraseTag);
			res.add(UDv2Feat.uposToExtPos(upos));
		}
		return res;
	}
}
