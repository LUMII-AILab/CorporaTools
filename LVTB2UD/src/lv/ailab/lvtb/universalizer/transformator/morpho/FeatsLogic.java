package lv.ailab.lvtb.universalizer.transformator.morpho;

import lv.ailab.lvtb.universalizer.conllu.UFeat;
import lv.ailab.lvtb.universalizer.transformator.XPathEngine;
import lv.semti.morphology.analyzer.Analyzer;
import lv.semti.morphology.analyzer.Word;
import lv.semti.morphology.analyzer.Wordform;
import lv.semti.morphology.attributes.AttributeNames;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;

/**
 * Created on 2016-04-20.
 *
 * @author Lauma
 */
public class FeatsLogic
{
	protected static Analyzer morphoEngineSing;

	protected static Analyzer getMorpho() throws Exception
	{
		if (morphoEngineSing == null) morphoEngineSing = new Analyzer();
		return morphoEngineSing;
	}

	public static ArrayList<UFeat> getUFeats(String form, String lemma, String xpostag, Node aNode)
	throws XPathExpressionException
	{
		ArrayList<UFeat> res = new ArrayList<>();

		// Inflectional features: nominal

		if (xpostag.matches("[na].m.*|v..p.m.*|[pm]..m.*")) res.add(UFeat.GENDER_MASC);
		if (xpostag.matches("[na].f.*|v..p.f.*|[pm]..f.*")) res.add(UFeat.GENDER_FEM);

		if (xpostag.matches("[na]..s.*|v..[^p]....s.*|v..p..s.*|[pm]...s.*")) res.add(UFeat.NUMBER_SING);
		if (xpostag.matches("[na]..p.*|v..[^p]....p.*|v..p..p.*|[pm]...p.*")) res.add(UFeat.NUMBER_SING);
		if (xpostag.matches("n..d.*")) res.add(UFeat.NUMBER_PTAN); // Fuzzy borders.
		if (xpostag.matches("n..v.*")) res.add(UFeat.NUMBER_PTAN); // Fuzzy borders.

		if (xpostag.matches("[na]...n.*|v..p...n.*|[pm]....n.*")) res.add(UFeat.CASE_NOM);
		if (xpostag.matches("[na]...a.*|v..p...a.*|[pm]....a.*")) res.add(UFeat.CASE_ACC);
		if (xpostag.matches("[na]...d.*|v..p...d.*|[pm]....d.*")) res.add(UFeat.CASE_DAT);
		if (xpostag.matches("[na]...g.*|v..p...g.*|[pm]....g.*")) res.add(UFeat.CASE_GEN);
		if (xpostag.matches("[na]...l.*|v..p...l.*|[pm]....l.*")) res.add(UFeat.CASE_LOC);
		if (xpostag.matches("[na]...v.*|v..p...v.*")) res.add(UFeat.CASE_VOC);

		if (xpostag.matches("a.....n.*|v..p......n.*")) res.add(UFeat.DEFINITE_IND);
		if (xpostag.matches("mo.*") && lemma.matches("(treš|ceturt|piekt|sest|septīt|astot|devīt)[sa]")) res.add(UFeat.DEFINITE_IND);
		if (xpostag.matches("a.....y.*|v..p......y.*")) res.add(UFeat.DEFINITE_DEF);
		if (xpostag.matches("mo.*") && !lemma.matches("(treš|ceturt|piekt|sest|septīt|astot|devīt)[sa]")) res.add(UFeat.DEFINITE_DEF);

		//if (xpostag.matches("a.....p.*|rp.*|v.ypd.*")) res.add(UFeat.DEGREE_POS);
		if (xpostag.matches("a.....p.*|rp.*|mo.*")) res.add(UFeat.DEGREE_POS);
		if (xpostag.matches("a.....c.*|rc.*")) res.add(UFeat.DEGREE_CMP);
		if (xpostag.matches("a.....s.*|rs.*")) res.add(UFeat.DEGREE_SUP);
		if (xpostag.matches("v..pd.*"))
		{
			try
			{
				Word analysis = getMorpho().analyze(form);
				Wordform correctOne = analysis.getMatchingWordform(
						xpostag.contains("_") ? xpostag.substring(0, xpostag.indexOf('_')) : xpostag,
						true);
				String degree = correctOne.getValue(AttributeNames.i_Degree);
				if (degree == null || degree.equals(AttributeNames.v_Positive)) res.add(UFeat.DEGREE_POS);
				else if (degree.equals(AttributeNames.v_Comparative)) res.add(UFeat.DEGREE_CMP);
				else if (degree.equals(AttributeNames.v_Superlative)) res.add(UFeat.DEGREE_SUP);
				else System.err.printf("\"%s\" with tag %s has unrecognized degree value %s",
							form, xpostag, degree);
			}
			catch (Exception e)
			{
				System.err.println("Could not initialize Morphology, Degree for participles is not added.");
				e.printStackTrace(System.err);
			}

		}
		// Patalogical cases like "pirmākais un vispirmākais" are not represented.

		// Inflectional features: verbal

		//if (xpostag.matches("v..[^p]....[123].*")) res.add(UFeat.VERBFORM_FIN); // According to local understanding
		if (xpostag.matches("v..[^pn].*")) res.add(UFeat.VERBFORM_FIN); // According to UD rule of thumb.
		if (xpostag.matches("v..n.*")) res.add(UFeat.VERBFORM_INF);
		if (xpostag.matches("v..pd.*")) res.add(UFeat.VERBFORM_PART);
		if (xpostag.matches("a.*") && lemma.matches(".*?oš[sa]")) res.add(UFeat.VERBFORM_PART); // Some deverbal adjectives slip unmarked.
		if (xpostag.matches("v..p[pu].*")) res.add(UFeat.VERBFORM_TRANS);

		if (xpostag.matches("v..i.*")) res.add(UFeat.MOOD_IND);
		if (xpostag.matches("v..m.*")) res.add(UFeat.MOOD_IMP);
		if (xpostag.matches("v..c.*")) res.add(UFeat.MOOD_CND);
		if (xpostag.matches("v..r.*")) res.add(UFeat.MOOD_QOT);
		if (xpostag.matches("v..d.*")) res.add(UFeat.MOOD_NEC);

		if (xpostag.matches("v..[^p]s.*|v..pd....s.*")) res.add(UFeat.TENSE_PAST);
		if (xpostag.matches("v..[^p]p.*|v..pd....p.*")) res.add(UFeat.TENSE_PRES);
		if (xpostag.matches("v..[^p]f.*")) res.add(UFeat.TENSE_FUT);

		if (xpostag.matches("v..pd...ap.*")) res.add(UFeat.ASPECT_IMP);
		if (xpostag.matches("v..pd....s.*")) res.add(UFeat.ASPECT_PERF);

		if (xpostag.matches("v..[^p].....a.*|v..p.....a.*")) res.add(UFeat.VOICE_ACT);
		if (xpostag.matches("a.*") && lemma.matches(".*?oš[sa]")) res.add(UFeat.VOICE_ACT); // Some deverbal adjectives slip unmarked.
		if (xpostag.matches("v..[^p].....p.*|v..p.....p.*")) res.add(UFeat.VOICE_PASS); // Some deverbal adjectives slip unmarked.

		if (xpostag.matches("p.1.*|v..[^p]...1.*")) res.add(UFeat.PERSON_1);
		if (xpostag.matches("a.*") && lemma.matches("(man|mūs)ēj(ais|ā)")) res.add(UFeat.PERSON_1);
		if (xpostag.matches("p.2.*|v..[^p]...2.*")) res.add(UFeat.PERSON_2);
		if (xpostag.matches("a.*") && lemma.matches("(tav|jūs)ēj(ais|ā)")) res.add(UFeat.PERSON_2);
		if (xpostag.matches("p.3.*|v..[^p]...3.*")) res.add(UFeat.PERSON_3);
		if (xpostag.matches("a.*") && lemma.matches("viņēj(ais|ā)")) res.add(UFeat.PERSON_3);

		if (xpostag.matches("v..[^p]......y.*")) res.add(UFeat.NEGATIVE_POS); // Minimal annotations, for nomens manual labor is needed.
		if (xpostag.matches("v..[^p]......n.*")) res.add(UFeat.NEGATIVE_NEG); // Minimal annotations, for nomens manual labor is needed.

		// Lexical features

		if (xpostag.matches("p[ps].*")) res.add(UFeat.PRONTYPE_PRS);
		if (xpostag.matches("a.*") && lemma.matches("(man|mūs|tav|jūs|viņ|sav)ēj(ais|ā)"))
			res.add(UFeat.PRONTYPE_PRS);
		if (xpostag.matches("px.*")) res.add(UFeat.PRONTYPE_RCP);
		if (xpostag.matches("pq.*")) res.add(UFeat.PRONTYPE_INT);
		if (xpostag.matches("r0.*") && lemma.matches("cik|kad|kā|kurp?|kāpēc|kādēļ|kālab(ad)?"))
			res.add(UFeat.PRONTYPE_INT);
		if (xpostag.matches("n.*") && lemma.equals("kuriene") &&
				"xPrep".equals(XPathEngine.get().evaluate("../../xtype", aNode)))
			res.add(UFeat.PRONTYPE_INT);
		if (xpostag.matches("pr.*")) res.add(UFeat.PRONTYPE_REL);
		if (xpostag.matches("pd.*")) res.add(UFeat.PRONTYPE_DEM);
		if (xpostag.matches("r0.*") && lemma.matches("te|tur|šeit|tad|tagad|tik|tā"))
			res.add(UFeat.PRONTYPE_DEM);
		if (xpostag.matches("n.*") && lemma.equals("t(ur|ej)iene") &&
				"xPrep".equals(XPathEngine.get().evaluate("../../xtype", aNode)))
			res.add(UFeat.PRONTYPE_DEM);
		if (xpostag.matches("pg.*")) res.add(UFeat.PRONTYPE_TOT);
		if (xpostag.matches("r0.*") && lemma.matches("vienmēr|visur|visad(iņ)?"))
			res.add(UFeat.PRONTYPE_TOT);
		if (xpostag.matches("n.*") && lemma.equals("vis(ur|ad)iene") &&
				"xPrep".equals(XPathEngine.get().evaluate("../../xtype", aNode)))
			res.add(UFeat.PRONTYPE_TOT);
		if (xpostag.matches("p.....y.*")) res.add(UFeat.PRONTYPE_NEG);
		if (xpostag.matches("r0.*") && lemma.matches("ne.*"))
			res.add(UFeat.PRONTYPE_NEG);
		if (xpostag.matches("n.*") && lemma.equals("nek(ur|ad)iene") &&
				"xPrep".equals(XPathEngine.get().evaluate("../../xtype", aNode)))
			res.add(UFeat.PRONTYPE_NEG);
		if (xpostag.matches("pi.*")) res.add(UFeat.PRONTYPE_IND);
		if (xpostag.matches("r0.*") &&
				"xParticle".equals(XPathEngine.get().evaluate("../../xtype", aNode)))
		{
			NodeList result = (NodeList) XPathEngine.get().evaluate("../node[m.rf/tag = 'qs' and (m.rf/lemma = 'kaut' or m.rf/lemma = 'diez' or m.rf/lemma = 'diezin' or m.rf/lemma = 'nez' or m.rf/lemma = 'nezin')]", aNode, XPathConstants.NODESET);
			if (result != null && result.getLength() > 0) res.add(UFeat.PRONTYPE_IND);
		}

		if (xpostag.matches("mc.*|xn.*")) res.add(UFeat.NUMTYPE_CARD); // Nouns like "simts", "desmits" are not marked.
		if (xpostag.matches("mo.*|xo.*")) res.add(UFeat.NUMTYPE_ORD);
		if (xpostag.matches("r0.*") && lemma.matches("(vien|div|trīs|četr|piec|seš|septiņ|astoņ|deviņ|desmit|pusotr)reiz"))
			res.add(UFeat.NUMTYPE_MULT); // Incomplete list.
		if (xpostag.matches("mf.*")) res.add(UFeat.NUMTYPE_FRAC); // Nouns like "desmitdaļa" are not marked.

		if (xpostag.matches("ps.*")) res.add(UFeat.POSS_YES);
		if (xpostag.matches("a.*") && lemma.matches("(man|mūs|tav|jūs|viņ|sav)ēj(ais|ā)"))
			res.add(UFeat.POSS_YES);

		if (xpostag.matches("px.*|v.y.*")) res.add(UFeat.REFLEX_YES); // Currently it is impossible to split out "reflexive particle" of each verb.

		return res;
	}
}
