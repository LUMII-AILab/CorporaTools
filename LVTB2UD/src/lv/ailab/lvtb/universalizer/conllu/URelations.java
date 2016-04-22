package lv.ailab.lvtb.universalizer.conllu;

/**
 * Enumeration for Universal Dependencies's syntax relations.
 * Created on 2016-04-17.
 *
 * @author Lauma
 */
public enum URelations
{

	/**
	 * Clausal modifier of noun (adjectival clause).
	 */
	ACL("acl"),
	/**
	 * Adverbial clause modifier.
	 */
	ADVCL("advcl"),
	/**
	 * Adverbial modifier.
	 */
	ADVMOD("advmod"),
	/**
	 * Adjectival modifier.
	 */
	AMOD("amod"),
	/**
	 * Appositional modifier.
	 */
	APPOS("appos"),
	/**
	 * Auxiliary.
	 */
	AUX("aux"),
	/**
	 * Passive auxiliary.
	 */
	AUXPASS("auxpass"),
	/**
	 * Case marking.
	 */
	CASE("case"),
	/**
	 * Coordinating conjunction.
	 */
	CC("cc"),
	/**
	 * Clausal complement.
	 */
	CCOMP("ccomp"),
	/**
	 * Compound.
	 */
	COMPOUND("compound"),
	/**
	 * Conjunct.
	 */
	CONJ("conj"),
	/**
	 * Copula.
	 */
	COP("cop"),
	/**
	 * Clausal subject.
	 */
	CSUBJ("csubj"),
	/**
	 * Clausal passive subject.
	 */
	CSUBJPASS("csubjpass"),
	/**
	 * Unspecified dependency.
	 */
	DEP("dep"),
	/**
	 * Determiner.
	 */
	DET("det"),
	/**
	 * Discourse element.
	 */
	DISCOURSE("discourse"),
	/**
	 * Dislocated elements.
	 */
	DISLOCATED("dislocated"),
	/**
	 * Direct object.
	 */
	DOBJ("dobj"),
	/**
	 * Expletive.
	 */
	EXPL("expl"),
	/**
	 * Foreign words.
	 */
	FOREIGN("foreign"),
	/**
	 * Goes with.
	 */
	GOESWITH("goeswith"),
	/**
	 * Indirect object.
	 */
	IOBJ("iobj"),
	/**
	 * List.
	 */
	LIST("list"),
	/**
	 * Marker.
	 */
	MARK("mark"),
	/**
	 * Multi-word expression.
	 */
	MWE("mwe"),
	/**
	 * Name.
	 */
	NAME("name"),
	/**
	 * Negation modifier.
	 */
	NEG("neg"),
	/**
	 * Nominal modifier.
	 */
	NMOD("nmod"),
	/**
	 * Nominal subject.
	 */
	NSUBJ("nsubj"),
	/**
	 * Passive nominal subject.
	 */
	NSUBJPASS("nsubjpass"),
	/**
	 * Numeric modifier.
	 */
	NUMMOD("nummod"),
	/**
	 * Parataxis.
	 */
	PARATAXIS("parataxis"),
	/**
	 * Punctuation.
	 */
	PUNCT("punct"),
	/**
	 * Remnant in ellipsis.
	 */
	REMNANT("remnant"),
	/**
	 * Overridden disfluency.
	 */
	REPARANDUM("reparandum"),
	/**
	 * Root.
	 */
	ROOT("root"),
	/**
	 * Vocative.
	 */
	VOCATIVE("vocative"),
	/**
	 * Open clausal complement.
	 */
	XCOMP("xcomp"),
	;

	final String strRep;

	URelations(String strRep)
	{
		this.strRep = strRep;
	}
	public String toString()
	{
		return strRep;
	}
}
