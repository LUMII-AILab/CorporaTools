package lv.ailab.lvtb.universalizer.conllu;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enumeration for Universal Dependencies's mophological FEATs.
 * Only used pairs represented.
 * Created on 2016-04-17.
 *
 * @author Lauma
 */
public enum UDv2Feat
{
	ABBR_YES("Abbr", "Yes"),
	//ANIMACY("Animacy"),
    ASPECT_IMP("Aspect", "Imp"),
    ASPECT_PERF("Aspect", "Perf"),
    CASE_NOM("Case", "Nom"),
    CASE_ACC("Case", "Acc"),
    CASE_DAT("Case", "Dat"),
    CASE_GEN("Case", "Gen"),
    CASE_LOC("Case", "Loc"),
    CASE_VOC("Case", "Voc"),
    DEFINITE_IND("Definite", "Ind"),
    DEFINITE_SPEC("Definite", "Spec"),
    DEFINITE_DEF("Definite", "Def"),
    DEGREE_POS("Degree", "Pos"),
    DEGREE_CMP("Degree", "Cmp"),
    DEGREE_SUP("Degree", "Sup"),
    EVIDENT_FH("Evident", "Fh"),
    EVIDENT_NFH("Evident", "Nfh"),
    FOREIGN_YES("Foreign", "Yes"),
    GENDER_MASC("Gender", "Masc"),
    GENDER_FEM("Gender", "Fem"),
	MOOD_IND("Mood", "Ind"),
	MOOD_IMP("Mood", "Imp"),
	MOOD_CND("Mood", "Cnd"),
	MOOD_QOT("Mood", "Qot"),
	MOOD_NEC("Mood", "Nec"),
    NUMTYPE_CARD("NumType", "Card"),
    NUMTYPE_ORD("NumType", "Ord"),
    NUMTYPE_MULT("NumType", "Mult"),
    NUMTYPE_FRAC("NumType", "Frac"),
    NUMBER_SING("Number", "Sing"),
    NUMBER_PLUR("Number", "Plur"),
    NUMBER_PTAN("Number", "Ptan"),
    NUMBER_COLL("Number", "Coll"),
    PERSON_1("Person", "1"),
    PERSON_2("Person", "2"),
    PERSON_3("Person", "3"),
	POLARITY_POS("Polarity", "Pos"),
	POLARITY_NEG("Polarity", "Neg"),
    POSS_YES("Poss", "Yes"),
    PRONTYPE_PRS("PronType", "Prs"),
    PRONTYPE_RCP("PronType", "Rcp"),
    PRONTYPE_INT("PronType", "Int"),
    PRONTYPE_REL("PronType", "Rel"),
    PRONTYPE_DEM("PronType", "Dem"),
    PRONTYPE_TOT("PronType", "Tot"),
    PRONTYPE_NEG("PronType", "Neg"),
    PRONTYPE_IND("PronType", "Ind"),
    REFLEX_YES("Reflex", "Yes"),
    TENSE_PAST("Tense", "Past"),
    TENSE_PRES("Tense", "Pres"),
    TENSE_FUT("Tense", "Fut"),
    VERBFORM_FIN("VerbForm", "Fin"),
    VERBFORM_INF("VerbForm", "Inf"),
    VERBFORM_PART("VerbForm", "Part"),
    VERBFORM_CONV("VerbForm", "Conv"),
    VERBFORM_VNOUN("VerbForm", "Vnoun"),
    VOICE_ACT("Voice", "Act"),
    VOICE_PASS("Voice", "Pass"),

    TYPO_YES("Typo", "Yes"),
	;

	public final String key;
	public final String value;

	UDv2Feat(String key, String value)
	{
		this.key = key;
		this.value = value;
	}
	public String toString()
	{
		return key + "=" + value;
	}

	public static HashMap<String, HashSet<String>> toMap (List<UDv2Feat> feats)
	{
		if (feats == null) return null;
		HashMap<String, HashSet<String>> res = new HashMap<>();
		for (UDv2Feat f : feats)
		{
			HashSet<String> val = res.get(f.key);
			if (val == null) val = new HashSet<String>(){{add(f.value);}};
			else val.add(f.value);
			res.put(f.key, val);
		}
		return res;
	}

	public static String tagToCaseString(String tag)
	{
		if (tag == null) return null;
		Matcher m = Pattern.compile("([na]...|[mp]....|v..p...)(.).*").matcher(tag);
		String caseLetter = null;
		if (m.matches()) caseLetter = m.group(2);
		return UDv2Feat.caseLetterToLCString(caseLetter);
	}

	public static String caseLetterToLCString(String ch)
	{
		if (ch == null) return null;
		switch (ch)
		{
			case "n": return UDv2Feat.CASE_NOM.value.toLowerCase();
			case "g": return UDv2Feat.CASE_GEN.value.toLowerCase();
			case "d": return UDv2Feat.CASE_DAT.value.toLowerCase();
			case "a": return UDv2Feat.CASE_ACC.value.toLowerCase();
			case "l": return UDv2Feat.CASE_LOC.value.toLowerCase();
			case "v": return UDv2Feat.CASE_VOC.value.toLowerCase();
			default: return null;
		}
	}
}
