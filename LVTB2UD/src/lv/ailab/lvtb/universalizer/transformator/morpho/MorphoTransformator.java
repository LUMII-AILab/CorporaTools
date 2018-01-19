package lv.ailab.lvtb.universalizer.transformator.morpho;

import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.UDv2PosTag;
import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.utils.NodeFieldUtils;
import lv.ailab.lvtb.universalizer.utils.Logger;
import lv.ailab.lvtb.universalizer.transformator.Sentence;
import lv.ailab.lvtb.universalizer.transformator.TransformationParams;
import lv.ailab.lvtb.universalizer.utils.XPathEngine;
import lv.ailab.lvtb.universalizer.utils.Tuple;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MorphoTransformator {
	/**
	 * In this sentence all the transformations are carried out.
	 */
	public Sentence s;
	protected TransformationParams params;
	protected Logger logger;

	public MorphoTransformator(Sentence sent, TransformationParams params, Logger logger)
	{
		s = sent;
		this.logger = logger;
		this.params = params;
	}

	/**
	 * Create CoNLL-U token table, fill in ID, FORM, LEMMA, XPOSTAG, UPOSTAG and
	 * FEATS fields.
	 * @throws XPathExpressionException unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public void transformTokens() throws XPathExpressionException
	{
		// Selects ord numbers from the tree.
		NodeList ordNodes = (NodeList) XPathEngine.get().evaluate(".//node[m.rf]/ord",
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
		String prevMId = null;
		for (int currentOrd : ords)
		{
			if (currentOrd < 1) continue;

			// Find the m node to be processed.
			NodeList nodes = (NodeList)XPathEngine.get().evaluate(".//node[m.rf and ord=" + currentOrd + "]",
					s.pmlTree, XPathConstants.NODESET);
			if (nodes.getLength() > 1)
				//warnOut.printf("\"%s\" has several nodes with ord \"%s\", only first used!\n",	s.id, currentOrd);
				logger.doInsentenceWarning(String.format(
						"\"%s\" has several nodes with ord \"%s\", only first used!", s.id, currentOrd));

			// Determine, if paragraph has border before this token.
			boolean paragraphChange = false;
			String mId = NodeFieldUtils.getMId(nodes.item(0));
			if (mId.matches("m-.*-p\\d+s\\d+w\\d+"))
				mId = mId.substring(mId.indexOf("-") + 1, mId.lastIndexOf("s"));
			//else warnOut.println("Node id \"" + mId + "\" does not match paragraph searching pattern!");
			else logger.doInsentenceWarning(String.format(
					"Node id \"%s\" does not match paragraph searching pattern!", mId));
			if (prevMId!= null && !prevMId.equals(mId))
				paragraphChange = true;

			// Make new token.
			offset = transformCurrentToken(nodes.item(0), offset, paragraphChange);

			prevMId = mId;
		}
	}

	/**
	 * Helper method: Create CoNLL-U table entry for one token, fill in ID,
	 * FORM, LEMMA, XPOSTAG, UPOSTAG and FEATS fields.
	 * @param aNode		PML A-level node for which CoNLL entry must be created.
	 * @param offset	Difference between PML node's ord value and ID value for
	 *                  CoNLL token to be created.
	 * @param paragraphChange	paragraph border detected right before this
	 *                          token.
	 * @return Offset for next token.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	protected int transformCurrentToken(Node aNode, int offset, boolean paragraphChange)
			throws XPathExpressionException
	{
		Node mNode = (Node)XPathEngine.get().evaluate("./m.rf[1]",
				aNode, XPathConstants.NODE);
		String mForm = XPathEngine.get().evaluate("./form", mNode);
		String mLemma = XPathEngine.get().evaluate("./lemma", mNode);
		String lvtbTag = XPathEngine.get().evaluate("./tag", mNode);
		String lvtbAId = NodeFieldUtils.getId(aNode);
		boolean noSpaceAfter = false;
		if ("1".equals(XPathEngine.get().evaluate(
				"./w.rf/no_space_after|./w.rf/LM[last()]/no_space_after", mNode)))
			noSpaceAfter = true;

		// Starting from UD v2 numbers and certain abbrieavations are allowed to
		// be tokens with spaces.
		if ((mForm.contains(" ") || mLemma.contains(" ")) &&
				!lvtbTag.matches("x[no].*") &&
				!mForm.replace(" ", "").matches("u\\.t\\.jpr\\.|u\\.c\\.|u\\.tml\\.|v\\.tml\\."))
		{
			int baseOrd = NodeFieldUtils.getOrd(aNode);
			if (baseOrd < 1)
				throw new IllegalArgumentException("Node " + NodeFieldUtils.getId(aNode) + "has no ord value");

			String[] forms = mForm.split(" ");
			String[] lemmas = mLemma.split(" ");
			if (forms.length != lemmas.length)
				//warnOut.printf("\"%s\" form \"%s\" do not match \"%s\" on spaces!\n", s.id, mForm, mLemma);
				logger.doInsentenceWarning(String.format(
						"\"%s\" form \"%s\" do not match \"%s\" on spaces!", s.id, mForm, mLemma));

			// First one is different.
			Token firstTok = new Token(baseOrd + offset, forms[0],
					lemmas[0], getXpostag(lvtbTag, "_SPLIT_FIRST"));
			if (params.ADD_NODE_IDS && lvtbAId != null && !lvtbAId.isEmpty())
			{
				firstTok.misc.add("LvtbNodeId=" + lvtbAId);
				logger.addIdMapping(s.id, firstTok.getFirstColumn(), lvtbAId);
			}
			if (lvtbTag.matches("xf.*"))
			{
				//warnOut.printf("Processing unsplit xf \"%s\", check in treebank!", mForm);
				logger.doInsentenceWarning(String.format(
						"Processing unsplit xf \"%s\", check in treebank!", mForm));
				firstTok.upostag = PosLogic.getUPosTag(firstTok.lemma, firstTok.xpostag, aNode, logger);
				firstTok.feats = FeatsLogic.getUFeats(firstTok.form, firstTok.lemma, firstTok.xpostag, aNode, logger);
			}
			else if (lvtbTag.matches("x[ux].*"))
			{
				firstTok.upostag = PosLogic.getUPosTag(firstTok.lemma, firstTok.xpostag, aNode, logger);
				firstTok.feats = FeatsLogic.getUFeats(firstTok.form, firstTok.lemma, firstTok.xpostag, aNode, logger);
			}
			else
			{
				firstTok.upostag = UDv2PosTag.PART;
				firstTok.feats = FeatsLogic.getUFeats(firstTok.form, firstTok.lemma, "qs", aNode, logger);
			}
			if (paragraphChange) firstTok.misc.add("NewPar=Yes");
			s.conll.add(firstTok);
			s.pmlaToConll.put(NodeFieldUtils.getId(aNode), firstTok);

			// The rest
			for (int i = 1; i < forms.length && i < lemmas.length; i++)
			{
				offset++;
				Token nextTok = new Token(baseOrd + offset, forms[i],
						lemmas[i], getXpostag(lvtbTag, "_SPLIT_PART"));
				if (params.ADD_NODE_IDS && lvtbAId != null && !lvtbAId.isEmpty())
				{
					nextTok.misc.add("LvtbNodeId=" + lvtbAId);
					logger.addIdMapping(s.id, nextTok.getFirstColumn(), lvtbAId);

				}
				if (i == forms.length - 1 || i == lemmas.length - 1 || lvtbTag.matches("x.*"))
				{
					nextTok.upostag = PosLogic.getUPosTag(nextTok.lemma, nextTok.xpostag, aNode, logger);
					nextTok.feats = FeatsLogic.getUFeats(nextTok.form, nextTok.lemma, nextTok.xpostag, aNode, logger);
				}
				else
				{
					nextTok.upostag = UDv2PosTag.PART;
					nextTok.feats = FeatsLogic.getUFeats(nextTok.form, nextTok.lemma, "qs", aNode, logger);
				}
				nextTok.head = Tuple.of(firstTok.getFirstColumn(), firstTok);
				if ((i == forms.length - 1 || i == lemmas.length - 1) && noSpaceAfter)
					nextTok.misc.add("SpaceAfter=No");
				if (lvtbTag.matches("xf.*")) nextTok.deprel = UDv2Relations.FLAT_FOREIGN;
				else if (lvtbTag.matches("x[ux].*")) nextTok.deprel = UDv2Relations.GOESWITH;
				else nextTok.deprel = UDv2Relations.FIXED;
				s.conll.add(nextTok);
			}
			// TODO Is reasonable fallback for unequal space count in lemma and form needed?
		} else
		{
			Token nextTok = new Token(
					NodeFieldUtils.getOrd(aNode) + offset, mForm, mLemma,
					getXpostag(XPathEngine.get().evaluate("./tag", mNode), null));
			if (params.ADD_NODE_IDS && lvtbAId != null && !lvtbAId.isEmpty())
			{
				nextTok.misc.add("LvtbNodeId=" + lvtbAId);
				logger.addIdMapping(s.id, nextTok.getFirstColumn(), lvtbAId);
			}
			nextTok.upostag = PosLogic.getUPosTag(nextTok.lemma, nextTok.xpostag, aNode, logger);
			nextTok.feats = FeatsLogic.getUFeats(nextTok.form, nextTok.lemma, nextTok.xpostag, aNode, logger);
			if (noSpaceAfter)
				nextTok.misc.add("SpaceAfter=No");
			if (paragraphChange)
				nextTok.misc.add("NewPar=Yes");
			s.conll.add(nextTok);
			s.pmlaToConll.put(NodeFieldUtils.getId(aNode), nextTok);
		}
		return offset;
	}

	/**
	 * Logic for obtaining XPOSTAG from tag given in LVTB.
	 * @param lvtbTag	tag given in LVTB
	 * @param ending	postfix to be added to the tag
	 * @return XPOSTAG or _ if tag from LVTB is not meaningfull
	 */
	public static String getXpostag (String lvtbTag, String ending)
	{
		if (lvtbTag == null || lvtbTag.length() < 1 || lvtbTag.matches("N/[Aa]"))
			return "_";
		if (ending == null || ending.length() < 1) return lvtbTag.trim();
		else return (lvtbTag + ending).trim();
	}

	/**
	 * Currently extracted from pre-made CoNLL table.
	 * TODO: use PML tree instead?
	 */
	public void extractSendenceText()
	{
		s.text = "";
		for (Token t : s.conll)
		{
			s.text = s.text + t.form;
			if (t.misc == null || !t.misc.contains("SpaceAfter=No"))
				s.text = s.text + " ";
		}
		s.text = s.text.trim();
	}
}
