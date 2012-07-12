package lv.morphology.corpora.tests;

import lv.morphology.corpora.util.MorphoEntry;
import lv.semti.morphology.analyzer.*;
import lv.semti.morphology.attributes.AttributeNames;
import lv.semti.morphology.attributes.AttributeValues;

/**
 * Tests verifying single tokens.
 * N.B. Compatibility with UTF16 is not tested.
 */
public class SingleTokenTests
{
	
	Analyzer anal;
	
	/**
	 * Initiate test enviroment.
	 */
	public SingleTokenTests(Analyzer a)
	{
		anal = a;
	}
	
	/**
	 * Item is clasified as false residual, if analyzer recognise it as
	 * nonresidual, but it is taged as residual.
	 */
	public boolean falseResidual(MorphoEntry m)
	{
		if (m.attributes == null) return false;
		
		// Exception list.
		//if (m.token.equals("ES")) return true;
		
		if (m.attributes.isMatchingStrong(
			AttributeNames.i_PartOfSpeech, AttributeNames.v_Residual))
		{
			Word w = anal.analyze(m.token);
			if (w.isRecognized())
			{
				for(Wordform wf : w.wordforms)
				{
					if (!wf.isMatchingStrong(
							AttributeNames.i_PartOfSpeech,
							AttributeNames.v_Residual))
						return true;
				}
			} 
		}
		return false;
	}
	
	/**
	 * Catches tokens like "» Ziepes",
	 */
	public static boolean tokenizationError(String token)
	{
		// Punctuation marks.
		if (!containsLetter(token) && !containsSpace(token)) return false;
		if (!containsLetter(token) && containsSpace(token)) return true;
		
		// Token must start with letter.
		if (!Character.isLetter(token.codePointAt(0))) return true;
		
		// Token may end with '.', but not with other nonletter characters.
		int lastInd = token.length() - 1;
		if (!Character.isLetter(token.codePointAt(lastInd)))
			return !token.endsWith(".");
			
		// Seems good.
		return false;
	}
	
	/**
	 * Catches tokens like "» Ziepes",
	 */
	public static boolean tokenizationError(MorphoEntry m)
	{
		return tokenizationError(m.token);
	}
	
	/**
	 * Validate lemma, based on wordform
	 */
	public static boolean validLemma(MorphoEntry m)
	{
		if (m.lemma == null) return false;
		
		// Can't check lemma. Null atributes are checked by other functions.
		if (m.attributes == null) return true;
		
		AttributeValues attr = m.attributes;
		//String t = m.token;
		//String l = attr.getValue(AttributeNames.i_Lemma);
		String l = m.lemma;
		
		
		// Nouns.
		if (attr.isMatchingStrong(
			AttributeNames.i_PartOfSpeech, AttributeNames.v_Noun))
		{
			if(attr.isMatchingStrong(AttributeNames.i_Declension, "1") &&
					(l.endsWith("s") || l.endsWith("\u0161")) || l.endsWith("i"))	//sh
				return true;
			else if((attr.isMatchingStrong(AttributeNames.i_Declension, "2")) &&
					(l.endsWith("is") || l.endsWith("i")))
				return true;
			else if((attr.isMatchingStrong(AttributeNames.i_Declension, "3")) &&
					(l.endsWith("us") || l.endsWith("i")))
				return true;
			else if(attr.isMatchingStrong(AttributeNames.i_Declension, "4") &&
					(l.endsWith("a") || l.endsWith("as")))
				return true;
			else if(attr.isMatchingStrong(AttributeNames.i_Declension, "5") &&
					(l.endsWith("e") || l.endsWith("es")))
				return true;
			else if(attr.isMatchingStrong(AttributeNames.i_Declension, "6") &&
					(l.endsWith("s") || l.endsWith("is")))
				return true;
			else if(attr.isMatchingStrong(AttributeNames.i_Declension, 
						AttributeNames.v_Reflexive) &&
					(l.endsWith("\u0161an\u0101s"))) //shanaas
				return true;
				
			else if(attr.isMatchingStrong(AttributeNames.i_Declension,
					AttributeNames.v_NA) &&
					(l.endsWith("o") || l.endsWith("s")) && l.equals(m.token))
				return true;
			else return false;
		}
	
		// Verbs.
		if (attr.isMatchingStrong(
			AttributeNames.i_PartOfSpeech, AttributeNames.v_Verb))
		{
			if (l.endsWith("t") || l.endsWith("ties"))
				return true;
			else return false;
		}
		
		// Adjectives.
		if (attr.isMatchingStrong(
			AttributeNames.i_PartOfSpeech, AttributeNames.v_Adjective))
		{
			if (l.endsWith("s") || l.endsWith("\u0161") || l.endsWith("a"))	//sh
				return true;
			else return false;
		}
	
		// Adverbs.
		if (attr.isMatchingStrong(
			AttributeNames.i_PartOfSpeech, AttributeNames.v_Adverb))
		{
			if (attr.isMatchingStrong(
					AttributeNames.i_Degree, AttributeNames.v_Positive))		
			{		
				if (l.equalsIgnoreCase(m.token)
					&& (l.endsWith("i") || l.endsWith("z"))) return true;
				else return false;
			} else if(attr.isMatchingStrong(
					AttributeNames.i_Degree, AttributeNames.v_Comparative) ||
				attr.isMatchingStrong(
					AttributeNames.i_Degree, AttributeNames.v_Superlative))
			{
				if (l.endsWith("i") || l.endsWith("z")) return true;
				else return false;
				
			} else 
			{
				if (l.equalsIgnoreCase(m.token)) return true;
				else return false;
			}
		}
		
		// Numerals.
		if (attr.isMatchingStrong(
			AttributeNames.i_PartOfSpeech, AttributeNames.v_Numeral))
		{
			if (attr.isMatchingStrong(
					AttributeNames.i_SkaitljaTips, AttributeNames.v_PamataSv))
			{
				if (l.endsWith("i") || l.endsWith("s") || l.endsWith("t"))
					return true;
				else return false;
				
			} else if (attr.isMatchingStrong(
					AttributeNames.i_SkaitljaTips, AttributeNames.v_Kaartas))
			{
				if (l.endsWith("ais"))
					return true;
				else return false;
				
			} else return true;
			
		}
		
		// Pronouns.
		if (attr.isMatchingStrong(
				AttributeNames.i_PartOfSpeech, AttributeNames.v_Pronoun))
		{
			if (attr.isMatchingStrong(AttributeNames.i_VvTips,
				AttributeNames.v_Personu))
			{
				if (l.equals("es") || l.equals("tu")
					|| l.equals("vi\u0146\u0161") || l.equals("vi\u0146a")
					|| l.equals("m\u0113s") || l.equals("j\u016Bs")
					|| l.equals("vi\u0146i") || l.equals("vi\u0146as"))
					return true;
				else return false;	
			}
			else if (attr.isMatchingStrong(AttributeNames.i_VvTips,
					AttributeNames.v_Noraadaamie) &&
				(l.equals("\u0161\u012B") || l.equals("t\u0101"))) //shii, taa
				return true;
			else if (l.endsWith("s") || l.endsWith("\u0161") || l.endsWith("a")) // sh
				return true;
			return false;
		}	
	
		// Prepositions, conjunctions, interjections, particles.
		// Residuals.
		if (attr.isMatchingStrong(
				AttributeNames.i_PartOfSpeech, AttributeNames.v_Preposition) ||
			attr.isMatchingStrong(
				AttributeNames.i_PartOfSpeech, AttributeNames.v_Conjunction) ||
			attr.isMatchingStrong(
				AttributeNames.i_PartOfSpeech, AttributeNames.v_Interjection) ||
			attr.isMatchingStrong(
				AttributeNames.i_PartOfSpeech, AttributeNames.v_Particle) ||
			attr.isMatchingStrong(
				AttributeNames.i_PartOfSpeech, AttributeNames.v_Residual) ||
			attr.isMatchingStrong(
				AttributeNames.i_PartOfSpeech, AttributeNames.v_Punctuation))
		{
			if (l.equalsIgnoreCase(m.token)) return true;
			else return false;
		}
	
		else return false;
		
	}
	
	
	private static boolean containsLetter(String token)
	{
		for (int i = 0; i < token.length(); i++)
		{
			if (Character.isLetter(token.codePointAt(i)))
				return true;
		}
		return false;
	}
	
	private static boolean containsSpace(String token)
	{
		for (int i = 0; i < token.length(); i++)
		{
			if (Character.isSpaceChar(token.codePointAt(i)))
				return true;
		}
		return false;
		
	}
	
	private static boolean containsOnlyLetters(String token)
	{
		for (int i = 0; i < token.length(); i++)
		{
			if (!Character.isLetter(token.codePointAt(i)))
				return false;
		}
		return true;
	}
	
	private static boolean containsOnlyLettersSpaces(String token)
	{
		// May not work for UTF16
		for (int i = 0; i < token.length(); i++)
		{
			if (!Character.isLetter(token.codePointAt(i)) &&
				!Character.isSpaceChar(token.codePointAt(i)))
				return false;
		}
		return true;
	}
	
}