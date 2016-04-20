package lv.ailab.lvtb.universalizer;

import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.UFeat;
import lv.ailab.lvtb.universalizer.conllu.UPosTag;
import lv.ailab.lvtb.universalizer.conllu.URelations;
import lv.semti.morphology.analyzer.Analyzer;
import lv.semti.morphology.analyzer.Word;
import lv.semti.morphology.analyzer.Wordform;
import lv.semti.morphology.attributes.AttributeNames;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created on 2016-04-17.
 * Asumes normalized ord values.
 *
 * @author Lauma
 */
public class SentenceTransformator
{
	protected String sentenceID;
	protected Node pmlTree;
	protected ArrayList<Token> conll;
	protected HashMap<Token, Node> conllToPmla = new HashMap<>();
	protected HashMap<Node, Token> pmlaToConll = new HashMap<>();

	public static Analyzer helperMorpho = null;
	public static XPath xPathEngine = XPathFactory.newInstance().newXPath();

	public static void initMorpho() throws Exception
	{
		if (helperMorpho == null) helperMorpho = new Analyzer();
	}

	public SentenceTransformator(Node pmlTree) throws Exception
	{
		this.pmlTree = pmlTree;
		XPath xPath = XPathFactory.newInstance().newXPath();
		sentenceID = xPath.evaluate("./@id", this.pmlTree);
	}

	public void transformTokens() throws XPathExpressionException
	{
		conll = new ArrayList<>();
		// Selects ord numbers from the tree.
		NodeList ordNodes = (NodeList) xPathEngine.evaluate(".//node[m.rf]/ord",
				pmlTree, XPathConstants.NODESET);
		List<Integer> ords = new ArrayList<>();
		for (int i = 0; i < ordNodes.getLength(); i++)
		{
			String ordText = ordNodes.item(i).getTextContent();
			if (ordNodes != null && ordText.trim().length() > 0)
				ords.add(Integer.parseInt(ordText.trim()));
		}
		ords = ords.stream().sorted().collect(Collectors.toList());
		// Finds all nodes and makes CoNLL-U tokens from them.
		int offset = 0;
		for (int ord : ords)
		{
			if (ord < 1) continue;
			NodeList nodes = (NodeList) xPathEngine.evaluate(".//node[m.rf and ord=" + ord + "]",
					pmlTree, XPathConstants.NODESET);
			if (nodes.getLength() > 1)
				System.err.printf("\"%s\" has several nodes with ord \"%s\", only first used.\n",
						sentenceID, ord);
			offset = transformCurrentToken(nodes.item(0), offset);

		}
	}

	protected int transformCurrentToken(Node aNode, int offset)
	throws XPathExpressionException
	{
		Node mNode = (Node) xPathEngine.evaluate("./m.rf[1]",
				aNode, XPathConstants.NODE);
		String mForm = xPathEngine.evaluate("./form", mNode);
		String mLemma = xPathEngine.evaluate("./lemma", mNode);
		String lvtbRole = xPathEngine.evaluate("./role", aNode);
		String lvtbTag = xPathEngine.evaluate("./tag", mNode);
		if (mForm.contains(" ") || mLemma.contains(" "))
		{
			String[] forms = mForm.split(" ");
			String[] lemmas = mLemma.split(" ");
			if (forms.length != lemmas.length)
				System.err.printf("\"%s\" form \"%s\" do not match \"%s\" on spaces.\n",
						sentenceID, mForm, mLemma);
			int length = Math.min(forms.length, lemmas.length);

			// If the root is last token.
			if (lvtbTag.matches("xn.*"))
			{
				// The last one is different.
				Token lastTok = new Token(
						Integer.parseInt(xPathEngine.evaluate("./ord", aNode)) + length-1,
						forms[length-1], lemmas[length-1],
						getXpostag(lvtbTag, "_SPLIT_PART"));
				lastTok.upostag = getUPosTag(lastTok.lemma, lastTok.xpostag, lvtbRole);
				lastTok.feats = getUFeats(lastTok.form, lastTok.lemma, lastTok.xpostag, aNode);
				conllToPmla.put(lastTok, aNode);
				pmlaToConll.put(aNode, lastTok);

				// Process the rest.
				// First one has different xpostag.
				String xpostag = getXpostag(lvtbTag, "_SPLIT_FIRST");
				for (int i = 0; i < length - 1; i++)
				{
					Token nextTok = new Token(
							Integer.parseInt(xPathEngine.evaluate("./ord", aNode)) + offset,
							forms[i], lemmas[i], xpostag);
					nextTok.upostag = getUPosTag(nextTok.lemma, nextTok.xpostag, lvtbRole);
					nextTok.feats = getUFeats(nextTok.form, nextTok.lemma, nextTok.xpostag, aNode);
					nextTok.head = nextTok.idBegin;
					nextTok.deprel = URelations.COMPOUND;
					conll.add(nextTok);
					conllToPmla.put(nextTok, aNode);
					// Get ready for next token.
					offset++;
					xpostag = getXpostag(lvtbTag, "_SPLIT_PART");
				}
				conll.add(lastTok);

			}
			// If the root is first token.
			else
			{
				// First one is different.
				Token firstTok = new Token(
						Integer.parseInt(xPathEngine.evaluate("./ord", aNode)) + offset,
						forms[0], lemmas[0], getXpostag(lvtbTag, "_SPLIT_FIRST"));
				firstTok.upostag = getUPosTag(firstTok.lemma, firstTok.xpostag, lvtbRole);
				firstTok.feats = getUFeats(firstTok.form, firstTok.lemma, firstTok.xpostag, aNode);
				conll.add(firstTok);
				conllToPmla.put(firstTok, aNode);
				pmlaToConll.put(aNode, firstTok);

				// The rest
				for (int i = 1; i < forms.length && i < lemmas.length; i++)
				{
					offset++;
					Token nextTok = new Token(
							Integer.parseInt(xPathEngine.evaluate("./ord", aNode)) + offset,
							forms[i], lemmas[i], getXpostag(lvtbTag, "_SPLIT_PART"));
					nextTok.upostag = getUPosTag(nextTok.lemma, nextTok.xpostag, lvtbRole);
					nextTok.feats = getUFeats(nextTok.form, nextTok.lemma, nextTok.xpostag, aNode);
					nextTok.head = firstTok.idBegin;
					if (lvtbTag.matches("xf.*")) nextTok.deprel = URelations.FOREIGN;
					else nextTok.deprel = URelations.MWE;
					conll.add(nextTok);
					conllToPmla.put(nextTok, aNode);

				}
			}
			// TODO Is reasonable fallback for unequal space count in lemma and form needed?
		} else
		{
			Token nextTok = new Token(
					Integer.parseInt(xPathEngine.evaluate("./ord", aNode)) + offset,
					mForm, mLemma,
					getXpostag(xPathEngine.evaluate("./tag", mNode), null));
			nextTok.upostag = getUPosTag(nextTok.lemma, nextTok.xpostag, lvtbRole);
			nextTok.feats = getUFeats(nextTok.form, nextTok.lemma, nextTok.xpostag, aNode);
			conll.add(nextTok);
			conllToPmla.put(nextTok, aNode);
			pmlaToConll.put(aNode, nextTok);
		}
		// TODO: morpho.
		return offset;
	}

	/**
	 * Logic for obtaining XPOSTAG from tag given in LVTB.
	 */
	public static String getXpostag (String lvtbTag, String ending)
	{
		if (lvtbTag == null || lvtbTag.length() < 1 || lvtbTag.matches("N/[Aa]"))
			return "_";
		if (ending == null || ending.length() < 1) return lvtbTag;
		else return lvtbTag + ending;
	}

	public static UPosTag getUPosTag(String lemma, String xpostag, String lvtbRole)
	{
		if (xpostag.matches("N/[Aa]")) return UPosTag.X; // Not given.
		else if (xpostag.matches("nc.*")) return UPosTag.NOUN; // Or sometimes SCONJ
		else if (xpostag.matches("np.*")) return UPosTag.PROPN;
		else if (xpostag.matches("v..[^p].*")) return UPosTag.VERB;
		else if (xpostag.matches("v..p[dpu].*")) return UPosTag.VERB;
		else if (xpostag.matches("a.*"))
		{
			if (lemma.matches("(manējais|tavējais|mūsējais|jūsējais|viņējais|savējais|daudzi|vairāki)") ||
					lemma.matches("(manējā|tavējā|mūsējā|jūsējā|viņējā|savējā|daudzas|vairākas)"))
			{
				if (lvtbRole.equals("attr")) return UPosTag.DET;
				else return UPosTag.PRON;
			}
			else return UPosTag.ADJ;
		}
		else if (xpostag.matches("p[px].*")) return UPosTag.PRON;
		else if (xpostag.matches("p[sdiqg].*"))
		{
			if (lvtbRole.equals("attr")) return UPosTag.DET;
			else return UPosTag.PRON;
		}
		else if (xpostag.matches("pr.*")) return UPosTag.SCONJ;
		else if (xpostag.matches("r.*")) return UPosTag.ADJ; // Or sometimes SCONJ
		else if (xpostag.matches("m[cf].*")) return UPosTag.NUM;
		else if (xpostag.matches("mo.*")) return UPosTag.ADJ;
		else if (xpostag.matches("s.*")) return UPosTag.ADP;
		else if (xpostag.matches("cc.*")) return UPosTag.CONJ;
		else if (xpostag.matches("cs.*")) return UPosTag.SCONJ;
		else if (xpostag.matches("i.*")) return UPosTag.INTJ;
		else if (xpostag.matches("q.*")) return UPosTag.PART;
		else if (xpostag.matches("z.*")) return UPosTag.PUNCT;
		else if (xpostag.matches("z.*")) return UPosTag.PUNCT;
		else if (xpostag.matches("y.*"))
		{
			if (lemma.matches("\\p{Lu}+")) return UPosTag.PROPN;
			else if (lemma.matches("(utt\\.|u\\.t\\.jpr\\.|u\\.c\\.|u\\.tml\\.|v\\.tml\\.)")) return UPosTag.SYM;
			else if (lemma.matches("\\p{Ll}+-\\p{Ll}")) return UPosTag.NOUN; // Or rarely PROPN
			else return UPosTag.SYM; // Or sometimes PROPN/NOUN
		}
		else if (xpostag.matches("xf.*")) return UPosTag.X; // Or sometimes PROPN/NOUN
		else if (xpostag.matches("xn.*")) return UPosTag.NUM;
		else if (xpostag.matches("xo.*")) return UPosTag.ADJ;
		else if (xpostag.matches("xu.*")) return UPosTag.SYM;
		else if (xpostag.matches("xx.*")) return UPosTag.SYM; // Or sometimes PROPN/NOUN
		else System.err.printf("Could not obtain UPOSTAG for \"%s\" with XPOSTAG \"%s\".\n",
					lemma, xpostag);

		return UPosTag.X;
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
				initMorpho();
				Word analysis = helperMorpho.analyze(form);
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
				"xPrep".equals(xPathEngine.evaluate("../../xtype", aNode)))
			res.add(UFeat.PRONTYPE_INT);
		if (xpostag.matches("pr.*")) res.add(UFeat.PRONTYPE_REL);
		if (xpostag.matches("pd.*")) res.add(UFeat.PRONTYPE_DEM);
		if (xpostag.matches("r0.*") && lemma.matches("te|tur|šeit|tad|tagad|tik|tā"))
			res.add(UFeat.PRONTYPE_DEM);
		if (xpostag.matches("n.*") && lemma.equals("t(ur|ej)iene") &&
				"xPrep".equals(xPathEngine.evaluate("../../xtype", aNode)))
			res.add(UFeat.PRONTYPE_DEM);
		if (xpostag.matches("pg.*")) res.add(UFeat.PRONTYPE_TOT);
		if (xpostag.matches("r0.*") && lemma.matches("vienmēr|visur|visad(iņ)?"))
			res.add(UFeat.PRONTYPE_TOT);
		if (xpostag.matches("n.*") && lemma.equals("vis(ur|ad)iene") &&
				"xPrep".equals(xPathEngine.evaluate("../../xtype", aNode)))
			res.add(UFeat.PRONTYPE_TOT);
		if (xpostag.matches("p.....y.*")) res.add(UFeat.PRONTYPE_NEG);
		if (xpostag.matches("r0.*") && lemma.matches("ne.*"))
			res.add(UFeat.PRONTYPE_NEG);
		if (xpostag.matches("n.*") && lemma.equals("nek(ur|ad)iene") &&
				"xPrep".equals(xPathEngine.evaluate("../../xtype", aNode)))
			res.add(UFeat.PRONTYPE_NEG);
		if (xpostag.matches("pi.*")) res.add(UFeat.PRONTYPE_IND);
		if (xpostag.matches("r0.*") &&
				"xParticle".equals(xPathEngine.evaluate("../../xtype", aNode)))
		{
			NodeList result = (NodeList) xPathEngine.evaluate("../node[m.rf/tag = 'qs' and (m.rf/lemma = 'kaut' or m.rf/lemma = 'diez' or m.rf/lemma = 'diezin' or m.rf/lemma = 'nez' or m.rf/lemma = 'nezin')]", aNode, XPathConstants.NODESET);
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

	public void transformSyntax() throws XPathExpressionException
	{
		transformTokens();
		// TODO
	}

	public String toConllU()
	{
		StringBuilder res = new StringBuilder();
		res.append("# Latvian Treebank sentence ID: ");
		res.append(sentenceID);
		res.append("\n");
		for (Token t : conll)
			res.append(t.toConllU());
		res.append("\n");
		return res.toString();
	}

	public static String treeToConll(Node pmlTree)
	throws Exception
	{
		SentenceTransformator t = new SentenceTransformator(pmlTree);
		t.transformSyntax();
		return t.toConllU();
	}


}
