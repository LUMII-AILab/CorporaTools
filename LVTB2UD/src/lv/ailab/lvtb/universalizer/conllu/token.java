package lv.ailab.lvtb.universalizer.conllu;

import java.util.ArrayList;

/**
 * Format definition: http://universaldependencies.org/format.html
 * Created on 2016-04-17.
 *
 * @author Lauma
 */
public class Token
{
	/**
	 * 1st column.
	 * ID: Word index, integer starting at 1 for each new sentence; may be a
	 * range for tokens with multiple words.
	 * First element of the range.
	 */
	public int idBegin = -1;
	/**
	 * 1st column.
	 * ID: Word index, integer starting at 1 for each new sentence; may be a
	 * range for tokens with multiple words.
	 * Last element of the range.
	 */
	public int idEnd = -1;
	/**
	 * 2nd column.
	 * FORM: Word form or punctuation symbol.
	 */
	public String form = null;
	/**
	 * 3rd column.
	 * LEMMA: Lemma or stem of word form.
	 */
	public String lemma = null;
	/**
	 * 4th column.
	 * UPOSTAG: Universal part-of-speech tag drawn from our revised version of
	 * the Google universal POS tags.
	 */
	public UPosTag upostag = null;
	/**
	 * 5th column.
	 * XPOSTAG: Language-specific part-of-speech tag; underscore if not
	 * available.
	 */
	public String xpostag = null;
	/**
	 * 6th column.
	 * FEATS: List of morphological features from the universal feature
	 * inventory or from a defined language-specific extension; underscore if
	 * not available.
	 */
	public ArrayList<UFeat> feats = null;
	/**
	 * 7th column.
	 * HEAD: Head of the current token, which is either a value of ID or zero (0).
	 */
	public Integer head = null;
	/**
	 * 8th column.
	 * DEPREL: Universal Stanford dependency relation to the HEAD
	 * (root iff HEAD = 0) or a defined language-specific subtype of one.
	 */
	public URelations deprel = null;
	/**
	 * 9th column.
	 * DEPS: List of secondary dependencies (head-deprel pairs).
	 */
	public String deps = null;
	/**
	 * 10th column.
	 * MISC: Any other annotation.
	 * SpaceAfter=No
	 */
	public String misc = null;

	public Token() { }

	public Token (int id, String form, String lemma, String xpostag)
	{
		idBegin = id;
		idEnd = id;
		this.form = form;
		this.lemma = lemma;
		this.xpostag = xpostag;
	}

	/**
	 * Transforms token to a CoNLL-U format line. No newline added.
	 */
	public String toConllU()
	{
		StringBuilder res = new StringBuilder();
		// 1
		// ID - single integer or range.
		res.append(idBegin);
		if (idBegin != idEnd)
		{
			res.append("-");
			res.append(idEnd);
		}
		// 2
		res.append("\t");
		res.append(form);
		// 3
		res.append("\t");
		res.append(lemma);
		// 4
		res.append("\t");
		res.append(upostag.toString());
		// 5
		res.append("\t");
		if (xpostag == null || xpostag.length() < 1) res.append("_");
		else res.append(xpostag);
		// 6
		res.append("\t");
		if (feats == null || feats.size() < 1) res.append("_");
		else res.append(feats.stream().map(UFeat::toString).reduce((a, b) -> a + "|" + b).orElse("_"));
		// 7
		res.append("\t");
		if (head == null || head < 0) res.append("_");
		else res.append(head);
		// 8
		res.append("\t");
		if (deprel == null) res.append("_");
		else res.append(deprel);
		// 9
		res.append("\t");
		if (deps == null || deps.length() < 1) res.append("_");
		else res.append(deps);
		// 10
		res.append("\t");
		if (misc == null|| misc.length() < 1) res.append("_");
		else res.append(misc);

		return res.toString();
	}

}
