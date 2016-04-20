package lv.ailab.lvtb.universalizer.transformator;

import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.URelations;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
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
	public Sentence s;

	public static XPath xPathEngine = XPathFactory.newInstance().newXPath();
	public static DepRelLogic depTransf = new DepRelLogic(xPathEngine);
	public static PhraseTransformator phraseTransf = new PhraseTransformator(xPathEngine);

	public SentenceTransformator(Node pmlTree) throws Exception
	{
		s = new Sentence(pmlTree);
	}

	public void transformTokens() throws XPathExpressionException
	{
		FeatsLogic fl = new FeatsLogic(xPathEngine);
		// Selects ord numbers from the tree.
		NodeList ordNodes = (NodeList) xPathEngine.evaluate(".//node[m.rf]/ord",
				s.pmlTree, XPathConstants.NODESET);
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
					s.pmlTree, XPathConstants.NODESET);
			if (nodes.getLength() > 1)
				System.err.printf("\"%s\" has several nodes with ord \"%s\", only first used.\n",
						s.id, ord);
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
						s.id, mForm, mLemma);
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
				s.conllToPmla.put(lastTok, aNode);
				s.pmlaToConll.put(aNode, lastTok);

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
					s.conll.add(nextTok);
					s.conllToPmla.put(nextTok, aNode);
					// Get ready for next token.
					offset++;
					xpostag = getXpostag(lvtbTag, "_SPLIT_PART");
				}
				s.conll.add(lastTok);

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
				s.conll.add(firstTok);
				s.conllToPmla.put(firstTok, aNode);
				s.pmlaToConll.put(aNode, firstTok);

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
					s.conll.add(nextTok);
					s.conllToPmla.put(nextTok, aNode);
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
			s.conll.add(nextTok);
			s.conllToPmla.put(nextTok, aNode);
			s.pmlaToConll.put(aNode, nextTok);
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

	protected void transformSubtree (Node aNode) throws XPathExpressionException
	{
		// TODO ellipsis
		NodeList pmlChildren = (NodeList)xPathEngine.evaluate("./children/*", aNode, XPathConstants.NODESET);
		Node pmlPmc = (Node)xPathEngine.evaluate("./children/pmcinfo", aNode, XPathConstants.NODE);
		Node pmlX = (Node)xPathEngine.evaluate("./children/xinfo", aNode, XPathConstants.NODE);
		Node pmlCoord = (Node)xPathEngine.evaluate("./childen/coordinfo", aNode, XPathConstants.NODE);
		NodeList pmlDependents = (NodeList)xPathEngine.evaluate("./children/node", aNode, XPathConstants.NODESET);
		if (pmlChildren == null || pmlChildren.getLength() < 1)
			return;
		Node newRoot = null;
		// Valid LVTB PMLs have no more than one type of phrase - pmc, x or coord.
		if (pmlPmc != null)
			newRoot = phraseTransf.pmcToUD(pmlPmc, s);
		if (pmlCoord != null)
			newRoot = phraseTransf.coordToUD(pmlCoord, s);
		if (pmlX != null)
			newRoot = phraseTransf.xToUD(pmlX, s);
		if (newRoot == null) newRoot = aNode;
		else s.conllToPmla.put(s.pmlaToConll.get(newRoot), aNode);

		if (pmlDependents != null && pmlDependents.getLength() > 0)
		{
			for (int i = 0; i < pmlDependents.getLength(); i++)
			{
				transformSubtree(pmlDependents.item(i));
				Token conllTok = s.pmlaToConll.get(pmlDependents.item(i));
				conllTok.deprel = depTransf.getUDepFromDep(pmlDependents.item(i));
				conllTok.head = s.pmlaToConll.get(newRoot).idBegin;
			}
		}
	}

	public static String treeToConll(Node pmlTree)
	throws Exception
	{
		SentenceTransformator t = new SentenceTransformator(pmlTree);
		t.transformSyntax();
		return t.s.toConllU();
	}
}
