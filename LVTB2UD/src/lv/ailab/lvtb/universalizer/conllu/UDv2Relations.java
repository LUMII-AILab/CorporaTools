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
	AUX_PASS ("aux:pass"),
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
	CSUBJ_PASS ("csubj:pass"),
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
	FLAT_FOREIGN ("flat:foreign"),
	FLAT_NAME ("flat:name"),
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
	NSUBJ_PASS ("nsubj:pass"),
	/**
	 * nummod: numeric modifier
	 */
	NUMMOD ("nummod"),
	/**
	 * 	obj: object
	 */
	OBJ ("obj"),
	/**
	 * obl: oblique nominal
	 */
	OBL ("obl"),
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
	REPARANDUM ("reparandum"),
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

	/**
	 * Checks, if the given role (UD relation type) can be added to enhanced
	 * dependency liks obtained during conjunct propagation.
	 * @param role	role to check
	 * @return	true, if the propagated link with such role can be made
	 */
	public static boolean canPropagateAftercheck(UDv2Relations role)
	{
		return role != ROOT &&
				role != PUNCT &&
				role != CC;
	}

	/**
	 * Checks, if it should be tried to propagate UD dependency link with the
	 * given role (UD relation type) during conjunct propagation for enhanced
	 * dependencies. Can be used to speed up conjunct propagation.
	 * @param role	role to check
	 * @return	true, if conjunct propagation can give valid links
	 * 			(canPropagateAftercheck() must be done afterwards)
	 */
	public static boolean canPropagatePrecheck(UDv2Relations role)
	{
		return role != ROOT &&
				role != PUNCT &&
				role != CC &&
				role != CONJ;
	}
}
