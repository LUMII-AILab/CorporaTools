package lv.ailab.morphology.corpora.tests;

import java.util.ArrayList;
import lv.semti.morphology.analyzer.*;
import lv.semti.morphology.attributes.AttributeNames;

import lv.ailab.morphology.corpora.util.MorphoEntry;

public class ContinousTests
{
	Analyzer anal;
	
	/**
	 * Initialize test enviroment.
	 */
	public ContinousTests(Analyzer a)
	{
		anal = a;
	}
	
	/**
	 * Process given string applying all tests.
	 */
	public String concatFirst(ArrayList<MorphoEntry> string)
	{
		int initLength = string.size();
		if (concatWithAnalyzer(string).size() < initLength)
			return "Concatenized by Lexicon";
		
		// Concatenizing reziduals is not good idea when there are much false
		// residuals.
		//if (concatByPos(string).size() < initLength)
		//	return "Concatenized by POS Tags";
		return null;
	}
	
	/**
	 * Checks if first tokens in the list can be concatenized as one multi-token
	 * word. Decision is made based on lexicon of the morphological analyzer.
	 *
	 * @return	pointer to the data structure recieved as parameter. If
	 *			appropriate concatination has been found, this structure is
	 *			alternated!
	 */
	public ArrayList<MorphoEntry> concatWithAnalyzer(
		ArrayList<MorphoEntry> string)
	{
		// Calculate words to try
		String[] guesses = new String[string.size()];
		guesses[0] = string.get(0).token;
		for (int i = 1; i < string.size(); i++)
		{
			// TODO use spacing from w file?
			guesses[i] = (guesses[i-1] + " " + string.get(i).token).trim();
		}
		
		int last = string.size();
		boolean found = false;
		Word w = null;
		while (last > 0 && !found)
		{
			last--;
			//w = null;
			w = anal.analyze(guesses[last]);
			if (w.isRecognized())
			{
				found = true;
/*				if (last > 0)
				{
					//System.out.println(anal.enableGuessing);
					System.out.println("pom-pom!" + guesses[last]);
				}//*/
			}
		}
		
		// If nothing was found.
		if (last == 0) return string;
		
		// Replace concatenated morphoentries with the new one.
		MorphoEntry newME = new MorphoEntry (
			guesses[last], w.wordforms.get(0),
			w.wordforms.get(0).getValue(AttributeNames.i_Lemma),
			string.get(0).XML);
		
		return replaceFirst(string, newME, last + 1);
		
	}
		
	/**
	 * Checks if first tokens in the list can be concatenized as one multi-token
	 * word. Decision is made based on heuristics about POS.
	 *
	 */
	public ArrayList<MorphoEntry> concatByPos(ArrayList<MorphoEntry> string)
	{
		// Multiple reziduals can be concatinated.
		// Last rezidual in continuous rezidual stream.
		int lastRez = -1;
		for (int i = 0; i < string.size(); i++)
		{
			if (string.get(i).attributes == null ||
				!string.get(i).attributes.isMatchingStrong(
					AttributeNames.i_PartOfSpeech,
					AttributeNames.v_Residual))
			{
				break;
			} else lastRez = i;
		}
		if (lastRez > 0)
		{
			String newToken = "";
			String newLemma = "";
			
			for (int i = 0; i <= lastRez; i++)
			{
				// TODO use space from w file?
				newToken = newToken + " " + string.get(i).token;
			}
			newToken = newToken.trim();
			
			MorphoEntry newME = new MorphoEntry(
				newToken, MarkupConverter.fromKamolsMarkup("x"), newToken,
				string.get(0).XML);
			//newME.content = string.get(0).content;
			
			replaceFirst(string, newME, lastRez + 1);
			
			
		}
		return string;
	}
	
	/**
	 * Replace first n elements with the given new element and create
	 * appropriate w references for the new element.
	 */
	private ArrayList<MorphoEntry> replaceFirst(
		ArrayList<MorphoEntry> string, MorphoEntry newFirst, int n)
	{
		newFirst.wRefs = new ArrayList<String>();
		
		if (newFirst.XML) newFirst.content = string.get(0).content;
		
		for (int i = 0; i < n; i++)
		{
			if (newFirst.XML) newFirst.wRefs.addAll(string.get(0).wRefs);
			string.remove(0);
		}
		
		string.add(0, newFirst);
		return string;
	}

}