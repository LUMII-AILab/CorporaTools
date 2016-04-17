package lv.ailab.lvtb.universalizer;

import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.UPosTag;
import org.w3c.dom.Element;
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
public class Transformator
{
	protected String sentenceID;
	protected Node pmlTree;
	protected ArrayList<Token> conll;
	protected HashMap<Token, Node> mapping = new HashMap<>();
	XPath xPath = XPathFactory.newInstance().newXPath();


	public Transformator(Node pmlTree) throws XPathExpressionException
	{
		this.pmlTree = pmlTree;
		XPath xPath = XPathFactory.newInstance().newXPath();
		sentenceID = xPath.evaluate("LM/@id",
				pmlTree);
	}

	public void transformTokens() throws XPathExpressionException
	{
		conll = new ArrayList<>();
		// Selects ord numbers from the tree.
		NodeList ordNodes = (NodeList)xPath.evaluate("LM//node[m.rf]/ord",
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
			NodeList nodes = (NodeList)xPath.evaluate("LM//node[m.rf and ord=" + ord + "]",
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
		Node mNode = (Node)xPath.evaluate("/node/m.rf[1]",
				pmlTree, XPathConstants.NODE);
		String mForm = xPath.evaluate("/m.rf/form", mNode);
		String mLemma = xPath.evaluate("/m.rf/lemma", mNode);
		String lvtbRole = xPath.evaluate("/node/role", aNode);
		if (mForm.contains(" ") || mLemma.contains(" "))
		{
			String[] forms = mForm.split(" ");
			String[] lemmas = mLemma.split(" ");
			if (forms.length != lemmas.length)
				System.err.printf("\"%s\" form \"%s\" do not match \"%s\" on spaces.\n",
						sentenceID, mForm, mLemma);

			// First one is different.
			String xpostag = xPath.evaluate("/m.rf/tag", mNode);
			if (xpostag.equals("N/A")) xpostag = "_";
			else xpostag = xpostag + "_SPLIT";
			Token firstTok = new Token(
					Integer.parseInt(xPath.evaluate("/node/@ord", aNode)) + offset,
					forms[0], lemmas[0], xpostag);
			firstTok.upostag = getUPosTag(firstTok.form, firstTok.lemma, firstTok.xpostag, lvtbRole);
			conll.add(firstTok);
			mapping.put(firstTok, aNode);

			// The rest
			for(int i = 1; i < forms.length && i < lemmas.length; i++)
			{
				offset++;
				Token nextTok = new Token(
						Integer.parseInt(xPath.evaluate("/node/@ord", aNode)) + offset,
						forms[i], lemmas[i], "SPLIT_FROM_PREV");
				nextTok.upostag = getUPosTag(nextTok.form, nextTok.lemma, nextTok.xpostag, lvtbRole);
				conll.add(nextTok);
				mapping.put(nextTok, aNode);

			}
			// TODO Is reasonable fallback for unequal space count in lemma and form needed?
		} else
		{
			String xpostag = xPath.evaluate("/m.rf/tag", mNode);
			if (xpostag.equals("N/A")) xpostag = "_";
			Token nextTok = new Token(
					Integer.parseInt(xPath.evaluate("/node/@ord", aNode)) + offset,
					mForm, mLemma, xpostag);
			nextTok.upostag = getUPosTag(nextTok.form, nextTok.lemma, nextTok.xpostag, lvtbRole);
			conll.add(nextTok);
			mapping.put(nextTok, aNode);
		}
		// TODO: morpho.
		return offset;
	}

	public UPosTag getUPosTag(String form, String lemma, String xpostag, String lvtbRole)
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

	public void transformSyntax() throws XPathExpressionException
	{
		transformTokens();
		// TODO
	}
}
