package lv.ailab.lvtb.universalizer.transformator;

import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.UFeat;
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

	public static XPath xPathEngine = XPathFactory.newInstance().newXPath();

	public SentenceTransformator(Node pmlTree) throws Exception
	{
		this.pmlTree = pmlTree;
		XPath xPath = XPathFactory.newInstance().newXPath();
		sentenceID = xPath.evaluate("./@id", this.pmlTree);
	}

	public void transformTokens() throws XPathExpressionException
	{
		conll = new ArrayList<>();
		FeatsLogic fl = new FeatsLogic(xPathEngine);
		// Selects ord numbers from the tree.
		NodeList ordNodes = (NodeList) xPathEngine.evaluate(".//node[m.rf]/ord",
				pmlTree, XPathConstants.NODESET);
		List<Integer> ords = new ArrayList<>();
		for (int i = 0; i < ordNodes.getLength(); i++)
		{
			String ordText = ordNodes.item(i).getTextContent();
			if (ordText != null && ordText.trim().length() > 0)
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
			offset = transformCurrentToken(nodes.item(0), offset, fl);
		}
	}

	protected int transformCurrentToken(Node aNode, int offset, FeatsLogic featsTransformer)
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
				lastTok.upostag = PosLogic.getUPosTag(lastTok.lemma, lastTok.xpostag, lvtbRole);
				lastTok.feats = featsTransformer.getUFeats(lastTok.form, lastTok.lemma, lastTok.xpostag, aNode);
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
					nextTok.upostag = PosLogic.getUPosTag(nextTok.lemma, nextTok.xpostag, lvtbRole);
					nextTok.feats = featsTransformer.getUFeats(nextTok.form, nextTok.lemma, nextTok.xpostag, aNode);
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
				firstTok.upostag = PosLogic.getUPosTag(firstTok.lemma, firstTok.xpostag, lvtbRole);
				firstTok.feats = featsTransformer.getUFeats(firstTok.form, firstTok.lemma, firstTok.xpostag, aNode);
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
					nextTok.upostag = PosLogic.getUPosTag(nextTok.lemma, nextTok.xpostag, lvtbRole);
					nextTok.feats = featsTransformer.getUFeats(nextTok.form, nextTok.lemma, nextTok.xpostag, aNode);
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
			nextTok.upostag = PosLogic.getUPosTag(nextTok.lemma, nextTok.xpostag, lvtbRole);
			nextTok.feats = featsTransformer.getUFeats(nextTok.form, nextTok.lemma, nextTok.xpostag, aNode);
			conll.add(nextTok);
			conllToPmla.put(nextTok, aNode);
			pmlaToConll.put(aNode, nextTok);
		}
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

	public void transformSyntax() throws XPathExpressionException
	{
		transformTokens();
		// TODO
	}

	protected void transformSubtree (Node aNode)
	{

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
