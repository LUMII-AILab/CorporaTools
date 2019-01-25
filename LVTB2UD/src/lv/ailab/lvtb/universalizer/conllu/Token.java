package lv.ailab.lvtb.universalizer.conllu;

import lv.ailab.lvtb.universalizer.utils.Tuple;

import java.util.*;

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
	 * String representation and actual head token or null for Root.
	 */
	public Tuple<String, Token> head = null;
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
	public HashSet<EnhencedDep> deps = new HashSet<>();

	public EnhencedDep depsBackbone = null;
	/**
	 * 10th column.
	 * MISC: Any other annotation.
	 * SpaceAfter=No
	 * NewPar=Yes
	 * LvtbNodeId=...
	 */
	public HashMap<MiscKeys, HashSet<String>> misc = new HashMap<>();

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

	@Override
	public boolean equals (Object o)
	{
		if (o == null) return false;
		if (this.getClass() != o.getClass()) return false;
		if (this == o) return true;
		Token other = (Token) o;
		return (idBegin == other.idBegin && idSub == other.idSub && idEnd == other.idEnd &&
				(form == other.form || form != null && form.equals(other.form)) &&
				(lemma == other.lemma || lemma != null && lemma.equals(other.lemma)) &&
				upostag == other.upostag &&
				(xpostag == other.xpostag || xpostag != null && xpostag.equals(other.xpostag)) &&
				(feats == other.feats || feats != null && feats.equals(other.feats)) &&
				(head == other.head || head != null && head.equals(other.head)) &&
				deprel == other.deprel &&
				(deps == other.deps || deps != null && deps.equals(other.deps)) &&
				(depsBackbone == other.depsBackbone || depsBackbone != null && depsBackbone.equals(other.depsBackbone)) &&
				(misc == other.misc || misc != null && misc.equals(other.misc)));
	}
	@Override
	public int hashCode()
	{
		return 7477 * Integer.hashCode(idBegin) +
				6011 * Integer.hashCode(idSub) +
				5737 * Integer.hashCode(idEnd) +
				4001 * (form == null ? 1 : form.hashCode()) +
				3991 * (lemma == null ? 1 : lemma.hashCode()) +
				3301 * (upostag == null ? 1 : upostag.hashCode()) +
				2621 * (xpostag == null ? 1 : xpostag.hashCode()) +
				2141 * (feats == null ? 1 : feats.hashCode()) +
				1721 * (head == null ? 1 : head.hashCode()) +
				1171 * (deprel == null ? 1 : deprel.hashCode()) +
				677 * (deps == null ? 1 : deps.hashCode()) +
				17 * (depsBackbone == null ? 1 : depsBackbone.hashCode()) +
				3 * (misc == null ? 1 : misc.hashCode());
	}

	/**
	 * Method to add new attribute-value pari for MISC field. Handles all the
	 * nulls for you.
	 */
	public void addMisc (MiscKeys key, String value)
	{
		if (misc == null) misc = new HashMap<>();
		HashSet<String> values = misc.get(key);
		if (values == null) values = new HashSet<>();
		values.add(value);
		misc.put(key, values);
	}

	public boolean checkMisc(MiscKeys key)
	{
		return misc != null && misc.containsKey(key);
	}
	public boolean checkMisc(MiscKeys key, String value)
	{
		return checkMisc(key) && misc.get(key) != null
				&& misc.get(key).contains(value);

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

	/**
	 * Set base and, optionally, enhanced dependency links between two tokens,
	 * but do not set circular dependencies.
	 * @param parent		token to make this token's parent
	 * @param baseDep		label to be used for base dependency
	 * @param enhancedDep	label to be used for enhanced dependency or null,
	 *                      if enhanced should not be set
	 * @param setBackbone	if enhanced dependency is made, should it be set as
	 *                      backbone for child node
	 * @param cleanOldDeps	whether previous contents from deps field should be
	 *                      removed
	 * @param forbidHeadDuplicates	should multiple links with the same head be
	 *                              forbidden
	 */
	public void setHead(
			Token parent, UDv2Relations baseDep, Tuple<UDv2Relations, String> enhancedDep,
			boolean setBackbone, boolean cleanOldDeps, boolean forbidHeadDuplicates)
	{
		// Set dependencies, but avoid circular dependencies.
		if (!equals(parent))
		{
			// Set base dependency.
			head = Tuple.of(parent.getFirstColumn(), parent);
			deprel = baseDep;
			// Set enhanced dependencie.
			if (enhancedDep != null)
			{
				setEnhancedHead(parent, enhancedDep, setBackbone, cleanOldDeps, forbidHeadDuplicates);
				// This was here before refactoring setEnhancedHead out. Seems wrong.
				//if (setBackbone) parent.depsBackbone = newDep;
			}
		}
	}

	/**
	 * Set base and enhanced dependency links between two tokens, but do not set
	 * circular dependencies. Assumes base dependency label is the same as
	 * enhanced. Assumes enhanced label does not need subtype.
	 * Shortcut method, for more parameters use the other setHead().
	 * @param parent		token to make this token's parent
	 * @param dep			label to be used both for base and enhanced
	 *                      dependency
	 * @param setBackbone	if enhanced dependency is made, should it be set as
	 *                      backbone for child node
	 * @param cleanOldDeps	whether previous contents from deps field should be
	 *                      removed
	 * @param forbidHeadDuplicates	should multiple links with the same head be
	 *                              forbidden
	 */
	public void setHead(
			Token parent, UDv2Relations dep, boolean setBackbone,
			boolean cleanOldDeps, boolean forbidHeadDuplicates)
	{
		setHead(parent, dep, Tuple.of(dep, null), setBackbone, cleanOldDeps,
				forbidHeadDuplicates);
	}

	/**
	 * Sets enhanced dependency, does not set circular dependencies. Can check
	 * and avoid setting multiple dependencies with the same head.
	 * @param parent			token to be enhanced dependency head
	 * @param role			UD role for enhanced dependency
	 * @param rolePostfix	role postfix (preposition or case), can be null
	 * @param setBackbone    		if enhanced dependency is made, should it
	 *                              be set as backbone for child node
	 * @param cleanOldDeps    		whether previous contents from deps field
	 *                              should be removed
	 * @param forbidHeadDuplicates	should multiple links with the same head be
	 *                              forbidden
	 */
	public void setEnhancedHead(
			Token parent, UDv2Relations role, String rolePostfix,
			boolean setBackbone, boolean cleanOldDeps, boolean forbidHeadDuplicates)
	{
		if (equals(parent)) return; // No circulars.

		EnhencedDep dep = new EnhencedDep(parent, role, rolePostfix);
		if (cleanOldDeps) deps.clear();

		if (!forbidHeadDuplicates)
		{
			deps.add(dep);
			if (setBackbone) depsBackbone = dep;
			return;
		}
		EnhencedDep[] previous = deps.stream()
				.filter(a -> a.headID.equals(dep.headID)).toArray(EnhencedDep[]::new);
		if (previous.length == 0)
		{
			deps.add(dep);
			if (setBackbone) depsBackbone = dep;
		}
		else if (UDv2Relations.DEP != dep.role)
		{
			deps.removeAll(Arrays.asList(previous));
			deps.add(dep);
			if (setBackbone) depsBackbone = dep;

		}
		//else if (previous.length > 0 && UDv2Relations.DEP == dep.role);
	}

	/**
	 * Sets enhanced dependency, does not set circular dependencies. Can check
	 * and avoid setting multiple dependencies with the same head.
	 * @param parent				token to be enhanced dependency head
	 * @param enhancedDep			UD role for enhanced dependency
	 * @param setBackbone    		if enhanced dependency is made, should it
	 *                              be set as backbone for child node
	 * @param cleanOldDeps    		whether previous contents from deps field
	 *                              should be removed
	 * @param forbidHeadDuplicates	should multiple links with the same head be
	 *                              forbidden
	 */
	public void setEnhancedHead(
			Token parent, Tuple<UDv2Relations, String> enhancedDep,
			boolean setBackbone, boolean cleanOldDeps, boolean forbidHeadDuplicates)
	{
		setEnhancedHead(parent, enhancedDep.first, enhancedDep.second,
				setBackbone, cleanOldDeps, forbidHeadDuplicates);
	}


	/**
	 * Sets enhanced dependency, does not set circular dependencies. Can check
	 * and avoid setting multiple dependencies with the same head.
	 * @param setBackbone    		if enhanced dependency is made, should it
	 *                              be set as backbone for child node
	 * @param cleanOldDeps    		whether previous contents from deps field
	 *                              should be removed
	 //* @param forbidHeadDuplicates	should multiple links with the same head be
	 *                              forbidden
	 */
	public void setEnhancedHeadRoot(
			boolean setBackbone, boolean cleanOldDeps)
	{
		boolean forbidHeadDuplicates = false; // TODO make this parameter
		if (cleanOldDeps) deps.clear();
		EnhencedDep dep = EnhencedDep.root();
		if (!forbidHeadDuplicates)
		{
			deps.add(dep);
			if (setBackbone) depsBackbone = dep;
			return;
		}
		EnhencedDep[] previous = deps.stream()
				.filter(a -> a.headID.equals(dep.headID)).toArray(EnhencedDep[]::new);
		if (previous.length > 0)
			deps.removeAll(Arrays.asList(previous)); //if (UDv2Relations.DEP != dep.role) - // LOL, this is root.
		deps.add(dep);
		if (setBackbone) depsBackbone = dep;
		// TODO is there realy something that tries to set dependency other than root to headID 0?
	}


/*	public void setSimpleHead(Token token, UDv2Relations role)
	{
		head = token.getFirstColumn();
		deprel = role;
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
		if (form == null || form.length() < 1) res.append("_");
		else res.append(form);
		// 3
		res.append("\t");
		if (lemma == null || lemma.length() < 1) res.append("_");
		else res.append(lemma);
		// 4
		res.append("\t");
		if (upostag == null) res.append("_");
		else res.append(upostag.toString());
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
		if (head == null || head.first.isEmpty()) res.append("_");
		else res.append(head.first);
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
		if (misc == null|| misc.size() < 1) res.append("_");
		else
		{
			//res.append(misc);
			res.append(misc.keySet().stream()
					.sorted((k1, k2) -> k1.toString().compareToIgnoreCase(k2.toString()))
					.filter(k -> misc.get(k) != null && !misc.get(k).isEmpty())
					.map(k -> k + "=" + misc.get(k).stream()
							.sorted(String.CASE_INSENSITIVE_ORDER)
							.reduce((v1, v2) -> v1 + "," + v2).orElse("_"))
					.reduce((p1, p2) -> p1 + "|" + p2).orElse("_")
			);
			//res.append(misc.stream()
			//		.sorted(String.CASE_INSENSITIVE_ORDER)
			//		.reduce((a, b) -> a + "|" + b).orElse("_"));
		}
		res.append("\n");

		return res.toString();
	}

}
