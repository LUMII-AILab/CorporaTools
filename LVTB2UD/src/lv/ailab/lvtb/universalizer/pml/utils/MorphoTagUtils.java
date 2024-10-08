package lv.ailab.lvtb.universalizer.pml.utils;

public class MorphoTagUtils {
	/**
	 * For given tag substitutes certain positions with underscores to reduce tag sparsity
	 * @param tag tag to be processed
	 * @return the normalised result
	 */
	public static String reduceTagSparsity(String tag)
	{
		if (tag == null || tag.isEmpty()) return tag;

		//// Wordtags and first parts of phrase tags
		// Nouns: POS+, Type+, Gender-, Number-, Case+, Declension-
		tag = tag.replaceFirst("^(n.)..(.).(.*)$", "$1__$2_$3");
		// Verbs: POS+, Type+, Reflexive-, Mood+, Tense+, Transitivity-, Conjugation-, Person-, Number-, Voice+, Negation-
		tag = tag.replaceFirst("^(v.).([^p].)....(.).(.*)$", "$1_$2____$3_$4");
		// Participles: POS+, Type+, Reflexive-, Mood+, Declinability+, Gender-, Number-, Case+, Voice+, Tense+, Definitness-, Degree-, Negation-
		tag = tag.replaceFirst("^(v.).(p.)..(...)...(.*)$", "$1_$2__$3___$4");
		// Adjectives: POS+, Type-, Gender-, Number-, Case+, Definitness-, Degree-
		tag = tag.replaceFirst("^(a)...(.)..(.*)$", "$1___$2__$3");
		// Numerals: POS+, Type+, Compositon-, Gender-, Number-, Case+
		tag = tag.replaceFirst("^(m.)...(.*)$", "$1___$2");
		// Pronouns: POS+, Type+, Person, Gender, Number, Case, Negation-
		tag = tag.replaceFirst("^(p.)...(.).(.*)$", "$1___$2_$3");
		// Adverbials: POS+, Degree-, Prepositional adverb+
		tag = tag.replaceFirst("^(r).(.*)$", "$1_$2");
		// Adpositions: POS+, Position-, Governed number-, Governed case-
		tag = tag.replaceFirst("^(s)...(.*)$", "$1___$2");
		// Conjunctions: POS+, Syntactic function+
		// tag = tag.replaceFirst("^(c.*)$", "$1"); // Currently an exception where we need everything
		// Interjections: POS+
		// tag = tag.replaceFirst("^(i.*)$", "$1"); // Currently an exception where we need everything
		// Particles: POS+
		// tag = tag.replaceFirst("^(q.*)$", "$1"); // Currently an exception where we need everything
		// Punctuation: POS+, Type+
		// tag = tag.replaceFirst("^(z.*)$", "$1"); // Currently an exception where we need everything
		// Abbreviations: POS+, Type+
		// tag = tag.replaceFirst("^(y.*)$", "$1"); // Currently an exception where we need everything
		// Residuals: POS+, Type+
		// tag = tag.replaceFirst("^(x.*)$", "$1"); // Currently an exception where we need everything

		//// Second parts of phrase tags
		// xPred: xType+, Tense+, Gender-, Case+
		tag = tag.replaceFirst("^([^\\[]+\\[(?:act|pass|subst|adj|pronom|modal|phase|expr|adv|inf|num).).(.\\].*)$", "$1_$2");
		// xApp: xType+
		// tag = tag.replaceFirst("^([^\\[]+\\[(?:agr|non)\\].*)$", "$1"); // Currently an exception where we need everything
		// xSimile: xType+, Grammaticalization+
		// tag = tag.replaceFirst("^([^\\[]+\\[(?:sim|comp).\\].*)$", "$1"); // Currently an exception where we need everything
		// xPrep: xType+
		// tag = tag = tag.replaceFirst("^([^\\[]+\\[(?:pre|post|rel)\\].*)$", "$1"); // Currently an exception where we need everything
		// xParticle: xType+
		// tag = tag.replaceFirst("^([^\\[]+\\[(?:aff|neg)\\].*)$", "$1"); // Currently an exception where we need everything
		// subrAnal: xType+
		// tag = tag.replaceFirst("^([^\\[]+\\[(?:vv|ipv|skv|set)\\].*)$", "$1"); // Currently an exception where we need everything

		return tag;
	}
}
