package lv.ailab.lvtb.universalizer.transformator.morpho;

import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.UDv2Feat;
import lv.ailab.lvtb.universalizer.pml.LvtbRoles;
import lv.ailab.lvtb.universalizer.pml.LvtbXTypes;
import lv.ailab.lvtb.universalizer.pml.PmlANode;
import lv.ailab.lvtb.universalizer.pml.PmlMNode;
import lv.ailab.lvtb.universalizer.utils.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2016-04-20.
 *
 * @author Lauma
 */
public class FeatsLogic
{
	/**
	 * Use this to obtain FEATS, if syntactic information is available.
	 * @param logger	Logger object used to collect warnings; if null,
	 *                  System.out is used
	 */
	public static ArrayList<UDv2Feat> getUFeats(
			String form, String lemma, String xpostag, PmlANode aNode, Logger logger)
	{
		ArrayList<UDv2Feat> res = getUFeats(form, lemma, xpostag, logger);
		String comprLemma = lemma;
		if (comprLemma == null) comprLemma = ""; // To avoid null pointer exceptions.

		// Lexical features
		if (xpostag.matches("n.*") && comprLemma.equals("kuriene") &&
				LvtbXTypes.XPREP.equals(aNode.getParent().getPhraseType()))
				//LvtbXTypes.XPREP.equals(XPathEngine.get().evaluate("../../xtype", aNode)))
			res.add(UDv2Feat.PRONTYPE_INT);
		if (xpostag.matches("n.*") && comprLemma.equals("t(ur|ej)iene") &&
				LvtbXTypes.XPREP.equals(aNode.getParent().getPhraseType()))
				//LvtbXTypes.XPREP.equals(XPathEngine.get().evaluate("../../xtype", aNode)))
			res.add(UDv2Feat.PRONTYPE_DEM);
		if (xpostag.matches("n.*") && comprLemma.equals("vis(ur|ad)iene") &&
				LvtbXTypes.XPREP.equals(aNode.getParent().getPhraseType()))
				//LvtbXTypes.XPREP.equals(XPathEngine.get().evaluate("../../xtype", aNode)))
			res.add(UDv2Feat.PRONTYPE_TOT);
		if (xpostag.matches("n.*") && comprLemma.equals("nek(ur|ad)iene") &&
				LvtbXTypes.XPREP.equals(aNode.getParent().getPhraseType()))
				//LvtbXTypes.XPREP.equals(XPathEngine.get().evaluate("../../xtype", aNode)))
			res.add(UDv2Feat.PRONTYPE_NEG);
		if (xpostag.matches("r0.*") &&
				LvtbXTypes.XPARTICLE.equals(aNode.getParent().getPhraseType()))
				//LvtbXTypes.XPARTICLE.equals(XPathEngine.get().evaluate("../../xtype", aNode)))
		{
			List<PmlANode> siblings = aNode.getParent().getChildren();
			boolean particleSib = false;
			for (PmlANode sib : siblings)
			{
				PmlMNode sibM = sib.getM();
				String sibLemma = sibM == null ? "" : sibM.getLemma();
				String sibTag = sibM == null ? "" : sibM.getTag();
				if (sibTag.startsWith("q") &&
						sibLemma.matches("kaut|diez(in)?|nez(in)?"))
				{
					particleSib = true;
					break;
				}
			}
			if (particleSib)
			{
				res.add(UDv2Feat.PRONTYPE_IND);
				res.remove(UDv2Feat.PRONTYPE_INT);
			}
		}
		if (xpostag.matches("n.*") && comprLemma.equals("kuriene") &&
				LvtbXTypes.XPARTICLE.equals(aNode.getParent().getPhraseType()))
				//LvtbXTypes.XPARTICLE.equals(XPathEngine.get().evaluate("../../xtype", aNode)))
		{
			List<PmlANode> siblings = aNode.getParent().getChildren();
			boolean particleSib = false;
			for (PmlANode sib : siblings)
			{
				PmlMNode sibM = sib.getM();
				String sibLemma = sibM == null ? "" : sibM.getLemma();
				String sibTag = sibM == null ? "" : sibM.getTag();
				if (sibTag.startsWith("q") &&
						sibLemma.matches("kaut|diez(in)?|nez(in)?"))
				{
					particleSib = true;
					break;
				}
			}
			PmlANode parent = aNode.getParent();
			PmlANode grandParent = parent == null ? null : parent.getParent();
			String gpRole = grandParent == null ? null : grandParent.getRole();
			PmlANode greatGrandParent = grandParent == null ? null : grandParent.getParent();
			String ggpPhraseType = greatGrandParent == null ? null : greatGrandParent.getPhraseType();
			PmlANode.Type ggpNodeType = greatGrandParent == null ? null : greatGrandParent.getNodeType();
			if (particleSib && LvtbRoles.BASELEM.equals(gpRole)
					&& PmlANode.Type.X == ggpNodeType
					&& LvtbXTypes.XPREP.equals(ggpPhraseType))
					//LvtbRoles.BASELEM.equals(XPathEngine.get().evaluate("../../../../role", aNode)) &&
					//LvtbXTypes.XPREP.equals(XPathEngine.get().evaluate("../../../../../../xtype", aNode)))
				res.add(UDv2Feat.PRONTYPE_IND);
		}

		return res;
	}

	/**
	 * Use this to obtain FEATS, if no syntactic information is available.
	 * @param logger	Logger object used to collect warnings; if null,
	 *                  System.out is used
	 */
	public static ArrayList<UDv2Feat> getUFeats(
			String form, String lemma, String xpostag, Logger logger)
	{
		ArrayList<UDv2Feat> res = new ArrayList<>();
		if (lemma == null) lemma = ""; // To avoid null pointer exceptions.

		// Inflectional features: nominal
		if (xpostag.matches("[na].m.*|v..p.m.*|[pm]..m.*")) res.add(UDv2Feat.GENDER_MASC);
		if (xpostag.matches("[na].f.*|v..p.f.*|[pm]..f.*")) res.add(UDv2Feat.GENDER_FEM);

		if (xpostag.matches("[na]..s.*|v..[^p]....s.*|v..p..s.*|[pm]...s.*")) res.add(UDv2Feat.NUMBER_SING);
		if (xpostag.matches("[na]..p.*|v..[^p]....p.*|v..p..p.*|[pm]...p.*")) res.add(UDv2Feat.NUMBER_PLUR);
		if (xpostag.matches("n..d.*")) res.add(UDv2Feat.NUMBER_PTAN); // Fuzzy borders.
		if (xpostag.matches("n..v.*")) res.add(UDv2Feat.NUMBER_COLL); // Fuzzy borders.

		if (xpostag.matches("[na]...n.*|v..p...n.*|[pm]....n.*")) res.add(UDv2Feat.CASE_NOM);
		if (xpostag.matches("[na]...a.*|v..p...a.*|[pm]....a.*")) res.add(UDv2Feat.CASE_ACC);
		if (xpostag.matches("[na]...d.*|v..p...d.*|[pm]....d.*")) res.add(UDv2Feat.CASE_DAT);
		if (xpostag.matches("[na]...g.*|v..p...g.*|[pm]....g.*")) res.add(UDv2Feat.CASE_GEN);
		if (xpostag.matches("[na]...l.*|v..p...l.*|[pm]....l.*")) res.add(UDv2Feat.CASE_LOC);
		if (xpostag.matches("[na]...v.*|v..p...v.*")) res.add(UDv2Feat.CASE_VOC);

		if (xpostag.matches("a.....n.*|v..p......n.*")) res.add(UDv2Feat.DEFINITE_IND);
		if (xpostag.matches("mo.*") && lemma.matches("(treš|ceturt|piekt|sest|septīt|astot|devīt)[sa]")) res.add(UDv2Feat.DEFINITE_SPEC);
		if (xpostag.matches("a.....y.*|v..p......y.*")) res.add(UDv2Feat.DEFINITE_DEF);
		if (xpostag.matches("mo.*") && !lemma.matches("(treš|ceturt|piekt|sest|septīt|astot|devīt)[sa]")) res.add(UDv2Feat.DEFINITE_DEF);

		//if (xpostag.matches("a.....p.*|rp.*|v.ypd.*")) res.add(UDv2Feat.DEGREE_POS);
		if (xpostag.matches("a.....p.*|v..pd......p.*|rp.*|mo.*")) res.add(UDv2Feat.DEGREE_POS);
		if (xpostag.matches("a.....c.*|v..pd......c.*|rc.*")) res.add(UDv2Feat.DEGREE_CMP);
		if (xpostag.matches("a.....s.*|v..pd......s.*|rs.*")) res.add(UDv2Feat.DEGREE_SUP);
		// Patalogical cases like "pirmākais un vispirmākais" are not represented.

		// Inflectional features: verbal

		//if (xpostag.matches("v..[^p]....[123].*")) res.add(UDv2Feat.VERBFORM_FIN); // According to local understanding
		if (xpostag.matches("v..[^pn].*")) res.add(UDv2Feat.VERBFORM_FIN); // According to UD rule of thumb.
		if (xpostag.matches("v..n.*")) res.add(UDv2Feat.VERBFORM_INF);
		if (xpostag.matches("v..pd.*")) res.add(UDv2Feat.VERBFORM_PART);
		if (xpostag.matches("a.*") && lemma.matches(".*?oš[sa]")) res.add(UDv2Feat.VERBFORM_PART); // Some deverbal adjectives slip unmarked.
		if (xpostag.matches("v..p[pu].*")) res.add(UDv2Feat.VERBFORM_CONV);
		if (xpostag.matches("n.....4.*") && lemma.endsWith("šana")) res.add(UDv2Feat.VERBFORM_VNOUN);
		if (xpostag.matches("n.....r.*") && lemma.endsWith("šanās")) res.add(UDv2Feat.VERBFORM_VNOUN);

		if (xpostag.matches("v..i.*")) res.add(UDv2Feat.MOOD_IND);
		if (xpostag.matches("v..m.*")) res.add(UDv2Feat.MOOD_IMP);
		if (xpostag.matches("v..c.*")) res.add(UDv2Feat.MOOD_CND);
		if (xpostag.matches("v..r.*")) res.add(UDv2Feat.MOOD_QOT);
		if (xpostag.matches("v..d.*")) res.add(UDv2Feat.MOOD_NEC);

		if (xpostag.matches("v..[^p]s.*|v..pd....s.*")) res.add(UDv2Feat.TENSE_PAST);
		if (xpostag.matches("v..[^p]p.*|v..pd....p.*")) res.add(UDv2Feat.TENSE_PRES);
		if (xpostag.matches("v..[^p]f.*")) res.add(UDv2Feat.TENSE_FUT);

		if (xpostag.matches("v..pd...ap.*")) res.add(UDv2Feat.ASPECT_IMP);
		if (xpostag.matches("v..pd....s.*")) res.add(UDv2Feat.ASPECT_PERF);

		if (xpostag.matches("v..[^p].....a.*|v..p.....a.*")) res.add(UDv2Feat.VOICE_ACT);
		if (xpostag.matches("a.*") && lemma.matches(".*?oš[sa]")) res.add(UDv2Feat.VOICE_ACT); // Some deverbal adjectives slip unmarked.
		if (xpostag.matches("v..[^p].....p.*|v..p.....p.*")) res.add(UDv2Feat.VOICE_PASS); // Some deverbal adjectives slip unmarked.

		if (xpostag.matches("v..i.*")) res.add(UDv2Feat.EVIDENT_FH);
		if (xpostag.matches("v..r.*")) res.add(UDv2Feat.EVIDENT_NFH);

		if (xpostag.matches("p.1.*|v..[^p]...1.*")) res.add(UDv2Feat.PERSON_1);
		if (xpostag.matches("a.*") && lemma.matches("(man|mūs)ēj(ais|ā)")) res.add(UDv2Feat.PERSON_1);
		if (xpostag.matches("p.2.*|v..[^p]...2.*")) res.add(UDv2Feat.PERSON_2);
		if (xpostag.matches("a.*") && lemma.matches("(tav|jūs)ēj(ais|ā)")) res.add(UDv2Feat.PERSON_2);
		if (xpostag.matches("p.3.*|v..[^p]...3.*")) res.add(UDv2Feat.PERSON_3);
		if (xpostag.matches("a.*") && lemma.matches("viņēj(ais|ā)")) res.add(UDv2Feat.PERSON_3);

		// Minimal annotations, for nomens manual labor is needed.
		if (xpostag.matches("v..[^p]......n.*")) res.add(UDv2Feat.POLARITY_POS);
		if (xpostag.matches("is.*") && lemma.matches("jā")) res.add(UDv2Feat.POLARITY_POS);
		if (xpostag.matches("v..[^p]......y.*")) res.add(UDv2Feat.POLARITY_NEG);
		if (xpostag.matches("qs.*") && lemma.matches("n[eē]")) res.add(UDv2Feat.POLARITY_NEG);
		if (xpostag.matches("is.*") && lemma.matches("n[eē]")) res.add(UDv2Feat.POLARITY_NEG);

		// Lexical features
		if (xpostag.matches("p[ps].*")) res.add(UDv2Feat.PRONTYPE_PRS);
		if (xpostag.matches("a.*") && lemma.matches("(man|mūs|tav|jūs|viņ|sav)ēj(ais|ā)"))
			res.add(UDv2Feat.PRONTYPE_PRS);
		if (xpostag.matches("px.*")) res.add(UDv2Feat.PRONTYPE_RCP);
		if (xpostag.matches("pq.*")) res.add(UDv2Feat.PRONTYPE_INT);
		if (xpostag.matches("r0.*") && lemma.matches("(ne)?(cik|kad|kā|kurp?|kāpēc|kādēļ|kālab(ad)?)"))
			res.add(UDv2Feat.PRONTYPE_INT);

		if (xpostag.matches("pr.*")) res.add(UDv2Feat.PRONTYPE_REL);
		if (xpostag.matches("pd.*")) res.add(UDv2Feat.PRONTYPE_DEM);
		if (xpostag.matches("r0.*") && lemma.matches("(ne)?(te|tur|šeit|tad|tagad|tik|tā)"))
			res.add(UDv2Feat.PRONTYPE_DEM);

		if (xpostag.matches("pg.*")) res.add(UDv2Feat.PRONTYPE_TOT);
		if (xpostag.matches("r0.*") && lemma.matches("vienmēr|visur|visad(iņ)?"))
			res.add(UDv2Feat.PRONTYPE_TOT);

		if (xpostag.matches("p.....y.*")) res.add(UDv2Feat.PRONTYPE_NEG);
		if (xpostag.matches("r0.*") && lemma.matches("ne.*"))
			res.add(UDv2Feat.PRONTYPE_NEG);

		if (xpostag.matches("pi.*")) res.add(UDv2Feat.PRONTYPE_IND);

		if (xpostag.matches("mc.*|xn.*")) res.add(UDv2Feat.NUMTYPE_CARD); // Nouns like "simts", "desmits" are not marked.
		if (xpostag.matches("mo.*|xo.*")) res.add(UDv2Feat.NUMTYPE_ORD);
		if (xpostag.matches("r0.*") && lemma.matches("(vien|div|trīs|četr|piec|seš|septiņ|astoņ|deviņ|desmit|pusotr)reiz"))
			res.add(UDv2Feat.NUMTYPE_MULT); // Incomplete list.
		if (xpostag.matches("mf.*")) res.add(UDv2Feat.NUMTYPE_FRAC); // Nouns like "desmitdaļa" are not marked.

		if (xpostag.matches("ps.*")) res.add(UDv2Feat.POSS_YES);
		if (xpostag.matches("a.*") && lemma.matches("(man|mūs|tav|jūs|viņ|sav)ēj(ais|ā)"))
			res.add(UDv2Feat.POSS_YES);

		if (xpostag.matches("xf.*")) res.add(UDv2Feat.FOREIGN_YES);
		if (xpostag.matches("y.*")) res.add(UDv2Feat.ABBR_YES);
		if (xpostag.matches("px.*|v.y.*")) res.add(UDv2Feat.REFLEX_YES); // Currently it is impossible to split out "reflexive particle" of each verb.

		return res;
	}
}
