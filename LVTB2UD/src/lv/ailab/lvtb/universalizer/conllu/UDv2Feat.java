package lv.ailab.lvtb.universalizer.conllu;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enumeration for Universal Dependencies' morphological FEATs.
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

	// This has to be changed, if more UPOS are introduced
    EXTPOS_ADJ("ExtPos",UDv2PosTag.ADJ.strRep),
    EXTPOS_ADP("ExtPos",UDv2PosTag.ADP.strRep),
    EXTPOS_ADV("ExtPos",UDv2PosTag.ADV.strRep),
    EXTPOS_AUX("ExtPos",UDv2PosTag.AUX.strRep),
    EXTPOS_CCONJ("ExtPos", UDv2PosTag.CCONJ.strRep),
    EXTPOS_DET("ExtPos", UDv2PosTag.DET.strRep),
    EXTPOS_INTJ("ExtPos", UDv2PosTag.INTJ.strRep),
    EXTPOS_NOUN("ExtPos", UDv2PosTag.NOUN.strRep),
    EXTPOS_NUM("ExtPos", UDv2PosTag.NUM.strRep),
    EXTPOS_PART("ExtPos", UDv2PosTag.PART.strRep),
    EXTPOS_PRON("ExtPos", UDv2PosTag.PRON.strRep),
    EXTPOS_PROPN("ExtPos", UDv2PosTag.PROPN.strRep),
    EXTPOS_PUNCT("ExtPos", UDv2PosTag.PUNCT.strRep),
    EXTPOS_SCOJ("ExtPos", UDv2PosTag.SCONJ.strRep),
    EXTPOS_SYM("ExtPos", UDv2PosTag.SYM.strRep),
    EXTPOS_VERB("ExtPos", UDv2PosTag.VERB.strRep),
    EXTPOS_X("ExtPos", UDv2PosTag.X.strRep),
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
			if (val == null) val = new HashSet<>(){{add(f.value);}};
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

	public static UDv2Feat uposToExtPos (UDv2PosTag upos)
	{
		if (upos == null) return null;
		return switch (upos) {
			case ADJ -> UDv2Feat.EXTPOS_ADJ;
			case ADP -> UDv2Feat.EXTPOS_ADP;
			case ADV -> UDv2Feat.EXTPOS_ADV;
			case AUX -> UDv2Feat.EXTPOS_AUX;
			case CCONJ -> UDv2Feat.EXTPOS_CCONJ;
			case DET -> UDv2Feat.EXTPOS_DET;
			case INTJ -> UDv2Feat.EXTPOS_INTJ;
			case NOUN -> UDv2Feat.EXTPOS_NOUN;
			case NUM -> UDv2Feat.EXTPOS_NUM;
			case PART -> UDv2Feat.EXTPOS_PART;
			case PRON -> UDv2Feat.EXTPOS_PRON;
			case PROPN -> UDv2Feat.EXTPOS_PROPN;
			case PUNCT -> UDv2Feat.EXTPOS_PUNCT;
			case SCONJ -> UDv2Feat.EXTPOS_SCOJ;
			case SYM -> UDv2Feat.EXTPOS_SYM;
			case VERB -> UDv2Feat.EXTPOS_VERB;
			case X -> UDv2Feat.EXTPOS_X;
			default -> throw new UnsupportedOperationException(
					"Missing ExtPos feat for UPOS value " + upos +
							"! Probably you need to add missing value to UDv2Feat enum.");
		};
	}

	public static String caseLetterToLCString(String ch)
	{
		if (ch == null) return null;
		return switch (ch) {
			case "n" -> UDv2Feat.CASE_NOM.value.toLowerCase();
			case "g" -> UDv2Feat.CASE_GEN.value.toLowerCase();
			case "d" -> UDv2Feat.CASE_DAT.value.toLowerCase();
			case "a" -> UDv2Feat.CASE_ACC.value.toLowerCase();
			case "l" -> UDv2Feat.CASE_LOC.value.toLowerCase();
			case "v" -> UDv2Feat.CASE_VOC.value.toLowerCase();
			default -> null;
		};
	}
}
