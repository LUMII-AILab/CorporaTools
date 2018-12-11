package lv.ailab.lvtb.universalizer.transformator.morpho;

import lv.ailab.lvtb.universalizer.transformator.StandardLogger;

import lv.semti.morphology.analyzer.Analyzer;
import lv.semti.morphology.analyzer.Word;
import lv.semti.morphology.analyzer.Wordform;
import lv.semti.morphology.attributes.AttributeNames;

public class AnalyzerWrapper
{
	protected static Analyzer morphoEngineSing;

	protected static Analyzer getMorpho() throws Exception
	{
		if (morphoEngineSing == null) morphoEngineSing = new Analyzer();
		morphoEngineSing.enableGuessing = true;
		morphoEngineSing.enableAllGuesses = true;
		return morphoEngineSing;
	}

	/**
	 * Get attribute-value pairs for given wordform disambiguated by morphotag.
	 */
	public static Wordform getAVPairs(String form, String postag)
	{
		try
		{
			Word analysis = getMorpho().analyze(form);
			String tag = postag.contains("_") ? postag.substring(0, postag.indexOf('_')) : postag;
			//TODO: Kad Pēteris partaisīs iespēju izvadīt complain uz citu plūsmu, ieslēgt atpakaļ.
			return analysis.getMatchingWordform(tag, false);
		} catch (Exception e)
		{
			StandardLogger.l.warnForAnalyzerException(e);
			return null;
		}

	}

	/**
	 * Get lemma for given wordform disambiguated by morphotag.
	 */
	public static String getLemma(String form, String postag)
	{
		try
		{
			Word w = getMorpho().analyze(form);
			Wordform wf = w.getMatchingWordform(postag, false);
			//TODO: Kad Pēteris partaisīs iespēju izvadīt complain uz citu plūsmu, ieslēgt atpakaļ.
			return wf.getValue(AttributeNames.i_Lemma);
		} catch (Exception e)
		{
			StandardLogger.l.warnForAnalyzerException(e);
			return null;
		}
	}
}
