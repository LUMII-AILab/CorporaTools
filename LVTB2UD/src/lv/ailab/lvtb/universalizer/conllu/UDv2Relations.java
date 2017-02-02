package lv.ailab.lvtb.universalizer.conllu;

/**
 * Enumeration for Universal Dependencies's v2 syntax relations.
 * Created on 2017-02-02.
 *
 * @author Lauma
 */
public enum UDv2Relations
{

	/**
	 * acl: clausal modifier of noun (adjectival clause)
	 */
	ACL ("acl"),
	/**
	 * advcl: adverbial clause modifier
	 */
	ADVCL ("advcl"),
	/**
	 * advmod: adverbial modifier
	 */
	ADVMOD ("advmod"),
	/**
	 * amod: adjectival modifier
	 */
	AMOD ("amod"),
	/**
	 * appos: appositional modifier
	 */
	APPOS("appos"),
	/**
	 * aux: auxiliary
	 */
	AUX ("aux"),
	/**
	 * case: case marking
	 */
    CASE ("case"),
	/**
	 * cc: coordinating conjunction
	 */
	CC ("cc"),
	/**
	 * ccomp: clausal complement
	 */
	CCOMP ("ccomp"),
	/**
	 * clf: classifier
	 */
	CLF ("clf"),
	/**
	 * compound: compound
	 */
	COMPOUND ("compound"),
	/**
	 * conj: conjunct
	 */
	CONJ ("conj"),
	/**
	 * cop: copula
	 */
	COP ("cop"),
	/**
	 * csubj: clausal subject
	 */
	CSUBJ ("csubj"),
	/**
	 * dep: unspecified dependency
	 */
	DEP ("dep"),
	/**
	 * det: determiner
	 */
	DET ("det"),
	/**
	 * discourse: discourse element
	 */
	DISCOURSE ("discourse"),
	/**
	 * dislocated: dislocated elements
	 */
	DISLOCATED ("dislocated"),
	/**
	 * expl: expletive
	 */
	EXPL ("expl"),
	/**
	 * fixed: fixed multiword expression
	 */
	FIXED ("fixed"),
	/**
	 * flat: flat multiword expression
	 */
	FLAT ("flat"),
	/**
	 * goeswith: goes with
	 */
	GOESWITH ("goeswith"),
	/**
	 * iobj: indirect object
	 */
	IOBJ ("iobj"),
	/**
	 * list: list
	 */
	LIST ("list"),
	/**
	 * mark: marker
	 */
	MARK ("mark"),
	/**
	 * nmod: nominal modifier
	 */
	NMOD ("nmod"),
	/**
	 * nsubj: nominal subject
	 */
	NSUBJ ("nsubj"),
	/**
	 * nummod: numeric modifier
	 */
	NUMMOD ("nummod"),
	/**
	 * 	obj: object
	 */
	OBJ ("OBJ"),
	/**
	 * obl: oblique nominal
	 */
	OBL ("OBL"),
	/**
	 * orphan: orphan
	 */
	ORPHAN ("orphan"),
	/**
	 * parataxis: parataxis
	 */
	PARATAXIS ("parataxis"),
	/**
	 * punct: punctuation
	 */
	PUNCT ("punct"),
	/**
	 * reparandum: overridden disfluency
	 */
	REPARANDUM ("overridden disfluency"),
	/**
	 * root: root
	 */
	ROOT ("root"),
	/**
	 * vocative: vocative
	 */
	VOCATIVE ("vocative"),
	/**
	 * xcomp: open clausal complement
	 */
	XCOMP ("xcomp");

	final String strRep;

	UDv2Relations(String strRep)
	{
		this.strRep = strRep;
	}
	public String toString()
	{
		return strRep;
	}
}
