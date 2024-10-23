package lv.ailab.lvtb.universalizer.utils;

import lv.ailab.lvtb.universalizer.conllu.UDv2PosTag;
import lv.ailab.lvtb.universalizer.transformator.StandardLogger;

import lv.semti.morphology.analyzer.Analyzer;
import lv.semti.morphology.analyzer.Word;
import lv.semti.morphology.analyzer.Wordform;
import lv.semti.morphology.attributes.AttributeNames;

import java.util.Arrays;
import java.util.HashSet;

/**
 * TODO: Initialize using either Baltic Latvian or Latgalian analyzer properly.
 * Currently due to missing paradigms in Latgalian analyzer, Baltic Latvian plus
 * wordlist for PRON/DET distinction is used in all cases.
 */
public class MorphoAnalyzerWrapper
{
	protected static Analyzer morphoEngineSing;
	protected static boolean latgalian;
	// This is very nasty, but we don't have this info currently in LTG Tēzaurs, so we need to hardcode it here.
	protected static HashSet<String> ltgDET = new HashSet<>(Arrays.asList(
			"sova", "sovs", "muns", "muna", "tovejs", "toveja", "kurs", "kura", "kaids", "kaida",
			"tei", "tis", "itei", "itys"));
	protected static HashSet<String> ltgPRON = new HashSet<>(Arrays.asList(
			"es", "tu", "jis", "jei", "jī", "kas"));

	public static void init(boolean latgalian) throws Exception
	{
		// TODO: When Latalian analyzer is better, set up to actually use IT
		if (morphoEngineSing == null) morphoEngineSing = new Analyzer();
		MorphoAnalyzerWrapper.latgalian = latgalian;
		morphoEngineSing.enableGuessing = true;
		morphoEngineSing.enableAllGuesses = true;
	}

	public static Analyzer getMorpho() throws Exception
	{
		if (morphoEngineSing == null) throw new Exception
				("You should call MorphoAnalyzerWrapper.init() before MorphoAnalyzerWrapper.getMorpho()");
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
			//String tag = postag.contains("_") ? postag.substring(0, postag.indexOf('_')) : postag;
			//TODO: Turn this back on, when morphonalyzer allows to change the output stream for complaining.
			//return analysis.getMatchingWordform(tag, false);
			return analysis.getMatchingWordform(postag, false);
		} catch (Exception e)
		{
			StandardLogger.l.warnForAnalyzerException(e);
			return null;
		}
	}

	public static String getPredefUpos(String form, String lemma, String postag)
	{
		if (latgalian)
		{
			// TODO: take from LTG Tēzaurs
			if (postag.matches("p.*"))
			{
				if (ltgDET.contains(lemma.toLowerCase())) return UDv2PosTag.DET.toString();
				if (ltgPRON.contains(lemma.toLowerCase())) return UDv2PosTag.PRON.toString();
				return null;
			}
		}
		else
		{
			try
			{

					Word analysis = getMorpho().analyze(form);
					//TODO: Turn this back on, when morphonalyzer allows to change the output stream for complaining.
					return analysis.getMatchingWordform(postag, false).getValue("UD vārdšķira");
			} catch (Exception e)
			{
				StandardLogger.l.warnForAnalyzerException(e);
				return null;
			}
		}
		return null;
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
			//TODO: Turn this back on, when morphonalyzer allows to change the output stream for complaining.
			return wf.getValue(AttributeNames.i_Lemma);
		} catch (Exception e)
		{
			StandardLogger.l.warnForAnalyzerException(e);
			return null;
		}
	}
}
