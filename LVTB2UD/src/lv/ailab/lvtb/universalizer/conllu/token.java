package lv.ailab.lvtb.universalizer.conllu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

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
	 * range for tokens with multiple words;  may be a decimal number for empty
	 * nodes.
	 * First element of the range.
	 */
	public int idBegin = -1;
	/**
	 * 1st column.
	 * ID: Word index, integer starting at 1 for each new sentence; may be a
	 * range for tokens with multiple words; may be a decimal number for empty
	 * nodes.
	 * First element of the range.
	 */
	public int idSub = -1;
	/**
	 * 1st column.
	 * ID: Word index, integer starting at 1 for each new sentence; may be a
	 * range for tokens with multiple words; may be a decimal number for empty
	 * nodes.
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
	public UDv2PosTag upostag = null;
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
	public ArrayList<UDv2Feat> feats = new ArrayList<>();
	/**
	 * 7th column.
	 * HEAD: Head of the current token, which is either a value of ID or zero (0).
	 */
	public String head = null;
	/**
	 * 8th column.
	 * DEPREL: Universal Stanford dependency relation to the HEAD
	 * (root iff HEAD = 0) or a defined language-specific subtype of one.
	 */
	public UDv2Relations deprel = null;
	/**
	 * 9th column.
	 * DEPS: List of secondary dependencies (head-deprel pairs).
	 */
	public ArrayList<EnhencedDep> deps = new ArrayList<>();
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
		idSub = 0;
		idEnd = id;
		this.form = form;
		this.lemma = lemma;
		this.xpostag = xpostag;
	}

	/**
	 * Concatenates the three inner integers to appropriate string ID. Assumes
	 * that ID can be either decimal or interval, but not both
	 * @return ID string representation
	 */
	public String getFirstColumn()
	{
		StringBuilder res = new StringBuilder();
		res.append(idBegin);
		if (idBegin != idEnd && idSub > 0)
			throw new IllegalArgumentException(
					"A token has invalid ID: " + idBegin + "." + idSub + "-" + idEnd);
		if (idSub > 0)
		{
			res.append(".");
			res.append(idSub);
		}
		if (idBegin != idEnd)
		{
			res.append("-");
			res.append(idEnd);
		}

		return res.toString();
	}

/*	public void setSimpleHead(Token token, UDv2Relations role)
	{
		head = token.getFirstColumn();
		deprel = role;
	}
	public void setEnhencedHead(Token token, UDv2Relations role)
	{
		deps.add(new EnhencedDep(token, role));
	}
	public void setBothHeads(Token token, UDv2Relations role)
	{
		setSimpleHead(token, role);
		setEnhencedHead(token, role);
	}

	public void setSimpleHeadRoot()
	{
		head = "0";
		deprel = UDv2Relations.ROOT;
	}
	public void setEnhencedHeadRoot()
	{
		deps.add(EnhencedDep.root());
	}
	public void setBothHeadsRoot()
	{
		setSimpleHeadRoot();
		setEnhencedHeadRoot();
	}*/
	/**
	 * Transforms token to a CoNLL-U format line. Newline is added.
	 */
	public String toConllU()
	{

		StringBuilder res = new StringBuilder();
		// 1
		// ID - single integer or range, or decimal.
		res.append(getFirstColumn());
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
		else
		{
			HashMap<String, HashSet<String>> compact = UDv2Feat.toMap(feats);
			res.append(compact.keySet().stream()
					.map(k -> k + "=" + compact.get(k).stream().sorted(String.CASE_INSENSITIVE_ORDER).reduce((v1, v2) -> v1 + "," + v2).orElse(""))
					.sorted(String.CASE_INSENSITIVE_ORDER)
					.reduce((a, b) -> a + "|" + b).orElse("_"));
		}
		// 7
		res.append("\t");
		if (head == null || head.isEmpty()) res.append("_");
		else res.append(head);
		// 8
		res.append("\t");
		if (deprel == null) res.append("_");
		else res.append(deprel);
		// 9
		res.append("\t");
		if (deps == null || deps.size() < 1) res.append("_");
		else {

			res.append(deps.stream().sorted(Comparator.comparingDouble(d -> d.sortValue))
					.map(EnhencedDep::toConllU)	.reduce((s1, s2) -> s1 + "|" + s2)
					.orElse("_"));
		}
		// 10
		res.append("\t");
		if (misc == null|| misc.length() < 1) res.append("_");
		else res.append(misc);
		res.append("\n");

		return res.toString();
	}

}
