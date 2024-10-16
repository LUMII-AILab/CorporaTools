package lv.ailab.lvtb.universalizer.conllu;

/**
 * Enumeration for Universal Dependencies' POS tags.
 * Created on 2016-04-17.
 *
 * @author Lauma
 */
public enum UDv2PosTag
{
	/**
	 * Adjective.
	 */
	ADJ("ADJ"),
	/**
	 * Adposition.
	 */
	ADP("ADP"),
	/**
	 * Adverb.
	 */
	ADV("ADV"),
	/**
	 * Auxiliary verb.
	 */
	AUX("AUX"),
	/**
	 * Coordinating conjunction.
	 */
	CCONJ("CCONJ"),
	/**
	 * Determiner.
	 */
	DET("DET"),
	/**
	 * Interjection.
	 */
	INTJ("INTJ"),
	/**
	 * Noun.
	 */
	NOUN("NOUN"),
	/**
	 * Numeral.
	 */
	NUM("NUM"),
	/**
	 * Particle.
	 */
	PART("PART"),
	/**
	 * Pronoun.
	 */
	PRON("PRON"),
	/**
	 * Proper noun.
	 */
	PROPN("PROPN"),
	/**
	 * Punctuation.
	 */
	PUNCT("PUNCT"),
	/**
	 * Subordinating conjunction.
	 */
	SCONJ("SCONJ"),
	/**
	 * Symbol.
	 */
	SYM("SYM"),
	/**
	 * Verb.
	 */
	VERB("VERB"),
	/**
	 * Other.
	 */
	X("X");

	final String strRep;

	UDv2PosTag(String strRep)
	{
		this.strRep = strRep;
	}
	public String toString()
	{
		return strRep;
	}
}
