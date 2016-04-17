package lv.ailab.lvtb.universalizer.conllu;

/**
 * Enumeration for Universal Dependencies's mophological FEATs.
 * Only used pairs represented.
 * Created on 2016-04-17.
 *
 * @author Lauma
 */
public enum UFeat
{
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
    DEFINITE_DEF("Definite", "Def"),
    DEGREE_POS("Degree", "Pos"),
    DEGREE_CMP("Degree", "Cmp"),
    DEGREE_SUP("Degree", "Sup"),
    GENDER_MASC("Gender", "Masc"),
    GENDER_FEM("Gender", "Fem"),
	MOOD_IND("Mood", "Ind"),
	MOOD_IMP("Mood", "Imp"),
	MOOD_CND("Mood", "Cnd"),
	MOOD_QOT("Mood", "Qot"),
	MOOD_NEC("Mood", "Nec"),
	NEGATIVE_POS("Negative", "Pos"),
	NEGATIVE_NEG("Negative", "Neg"),
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
    VERBFORM_TRANS("VerbForm", "Trans"),
    VOICE_ACT("Voice", "Act"),
    VOICE_PASS("Voice", "Pass"),
	;

	final String key;
	final String value;

	UFeat(String key, String value)
	{
		this.key = key;
		this.value = value;
	}
	public String toString()
	{
		return key + "=" + value;
	}
}
