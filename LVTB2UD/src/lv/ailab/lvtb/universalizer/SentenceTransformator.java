package lv.ailab.lvtb.universalizer;

import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.UFeat;
import lv.ailab.lvtb.universalizer.conllu.UPosTag;
import lv.ailab.lvtb.universalizer.conllu.URelations;
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
	XPath xPath = XPathFactory.newInstance().newXPath();


	public SentenceTransformator(Node pmlTree) throws XPathExpressionException
	{
		this.pmlTree = pmlTree;
		XPath xPath = XPathFactory.newInstance().newXPath();
		sentenceID = xPath.evaluate("./@id", this.pmlTree);
	}

	public void transformTokens() throws XPathExpressionException
	{
		conll = new ArrayList<>();
		// Selects ord numbers from the tree.
		NodeList ordNodes = (NodeList)xPath.evaluate(".//node[m.rf]/ord",
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
			NodeList nodes = (NodeList)xPath.evaluate(".//node[m.rf and ord=" + ord + "]",
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
		Node mNode = (Node)xPath.evaluate("./m.rf[1]",
				aNode, XPathConstants.NODE);
		String mForm = xPath.evaluate("./form", mNode);
		String mLemma = xPath.evaluate("./lemma", mNode);
		String lvtbRole = xPath.evaluate("./role", aNode);
		String lvtbTag = xPath.evaluate("./tag", mNode);
		if (mForm.contains(" ") || mLemma.contains(" "))
		{
			String[] forms = mForm.split(" ");
			String[] lemmas = mLemma.split(" ");
			if (forms.length != lemmas.length)
				System.err.printf("\"%s\" form \"%s\" do not match \"%s\" on spaces.\n",
						sentenceID, mForm, mLemma);
			//int length = Math.min(forms.length, lemmas.length);

			// First one is different.
			String xpostag = lvtbTag;
			if (xpostag.equals("N/A")) xpostag = "_";
			else xpostag = xpostag + "_SPLIT_FIRST";
			Token firstTok = new Token(
					Integer.parseInt(xPath.evaluate("./ord", aNode)) + offset,
					forms[0], lemmas[0], xpostag);
			firstTok.upostag = getUPosTag(firstTok.lemma, firstTok.xpostag, lvtbRole);
			firstTok.feats = getUFeats(firstTok.form, firstTok.lemma, firstTok.xpostag, lvtbRole);
			conll.add(firstTok);
			conllToPmla.put(firstTok, aNode);
			pmlaToConll.put(aNode, firstTok);

			// The rest
			for(int i = 1; i < forms.length && i < lemmas.length; i++)
			{
				offset++;
				xpostag = lvtbTag;
				if (xpostag.equals("N/A")) xpostag = "_";
				else xpostag = xpostag + "_SPLIT_PART";
				Token nextTok = new Token(
						Integer.parseInt(xPath.evaluate("./ord", aNode)) + offset,
						forms[i], lemmas[i], xpostag);
				nextTok.upostag = getUPosTag(nextTok.lemma, nextTok.xpostag, lvtbRole);
				nextTok.feats = getUFeats(nextTok.form, nextTok.lemma, nextTok.xpostag, lvtbRole);
				nextTok.head = firstTok.idBegin;
				nextTok.deprel = URelations.MWE;
				conll.add(nextTok);
				conllToPmla.put(nextTok, aNode);

			}
			// TODO Is reasonable fallback for unequal space count in lemma and form needed?
		} else
		{
			String xpostag = xPath.evaluate("./tag", mNode);
			if (xpostag.equals("N/A")) xpostag = "_";
			Token nextTok = new Token(
					Integer.parseInt(xPath.evaluate("./ord", aNode)) + offset,
					mForm, mLemma, xpostag);
			nextTok.upostag = getUPosTag(nextTok.lemma, nextTok.xpostag, lvtbRole);
			nextTok.feats = getUFeats(nextTok.form, nextTok.lemma, nextTok.xpostag, lvtbRole);
			conll.add(nextTok);
			conllToPmla.put(nextTok, aNode);
			pmlaToConll.put(aNode, nextTok);
		}
		// TODO: morpho.
		return offset;
	}

	public static UPosTag getUPosTag(String lemma, String xpostag, String lvtbRole)
	{
		if (xpostag.matches("nc.*")) return UPosTag.NOUN; // Or sometimes SCONJ
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

	public static ArrayList<UFeat> getUFeats(String form, String lemma, String xpostag, String lvtbRole)
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
		if (xpostag.matches("a.....y.*|v..p......y.*")) res.add(UFeat.DEFINITE_DEF);
		if (xpostag.matches("mo.*") && !lemma.matches("(treš|ceturt|piekt|sest|septīt|astot|devīt)[sa]")) res.add(UFeat.DEFINITE_DEF);

		// TODO DEGREE

		// Inflectional features: verbal

		if (xpostag.matches("v..[^p]....[123].*")) res.add(UFeat.VERBFORM_FIN); // According to local understanding
		//if (xpostag.matches("v..[^pn].*")) res.add(UFeat.VERBFORM_FIN); // According to UD rule of thumb.
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
		if (xpostag.matches("a.*") && lemma.matches("(manēj|mūsēj)(ais|ā)")) res.add(UFeat.PERSON_1);
		if (xpostag.matches("p.2.*|v..[^p]...2.*")) res.add(UFeat.PERSON_2);
		if (xpostag.matches("a.*") && lemma.matches("(tavēj|jūsēj)(ais|ā)")) res.add(UFeat.PERSON_2);
		if (xpostag.matches("p.3.*|v..[^p]...3.*")) res.add(UFeat.PERSON_3);
		if (xpostag.matches("a.*") && lemma.matches("viņēj(ais|ā)")) res.add(UFeat.PERSON_3);

		if (xpostag.matches("v..[^p]......y.*")) res.add(UFeat.NEGATIVE_POS); // Minimal annotations, for nomens manual labor is needed.
		if (xpostag.matches("v..[^p]......n.*")) res.add(UFeat.NEGATIVE_NEG); // Minimal annotations, for nomens manual labor is needed.

		// Lexical features

		// TODO

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
	throws XPathExpressionException
	{
		SentenceTransformator t = new SentenceTransformator(pmlTree);
		t.transformSyntax();
		return t.toConllU();
	}


}
