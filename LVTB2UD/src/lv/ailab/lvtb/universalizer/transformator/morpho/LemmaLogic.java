package lv.ailab.lvtb.universalizer.transformator.morpho;

import lv.ailab.lvtb.universalizer.utils.Logger;

/**
 * Logic on obtaining Universal lemmas from Latvian Treebank lemmas.
 * Created on 2018-10-16.
 * @author Lauma
 */
public class LemmaLogic
{
	public static String getULemma(String lemma, String xpostag, Logger logger)
	{
		if (xpostag == null || xpostag.isEmpty() || xpostag.equals("N/A")
			|| lemma == null || lemma.isEmpty())
			return lemma;
		if (xpostag.matches("^v..([^p].{6}|p.{8})y.*"))
		{
			if (lemma.startsWith("ne"))
				lemma = lemma.substring(2);
			else
			{
				String errorMsg = String.format(
						"Can't remove negative prefix from lemma \"%s\" with XPOSTAG \"%s\".", lemma, xpostag);
				if (logger != null)
					logger.doInsentenceWarning(errorMsg);
			}
		}
		if (xpostag.matches("^(s|rr).*"))
		{
			String lcLemma = lemma.toLowerCase();
			if (!lcLemma.equals(lemma))
			{
				String errorMsg = String.format(
						"Had to lower-case lemma \"%s\" for XPOSTAG \"%s\".", lemma, xpostag);
				if (logger != null)
					logger.doInsentenceWarning(errorMsg);
				lemma = lcLemma;
			}
		}
		return lemma;
	}
}
