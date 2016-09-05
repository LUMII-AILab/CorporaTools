package lv.ailab.lvtb.universalizer.transformator;

import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.URelations;
import lv.ailab.lvtb.universalizer.transformator.morpho.FeatsLogic;
import lv.ailab.lvtb.universalizer.transformator.morpho.PosLogic;
import lv.ailab.lvtb.universalizer.pml.Utils;
import lv.ailab.lvtb.universalizer.transformator.syntax.DepRelLogic;
import lv.ailab.lvtb.universalizer.transformator.syntax.EllipsisLogic;
import lv.ailab.lvtb.universalizer.transformator.syntax.PhraseTransformator;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Logic for transforming LVTB sentence annotations to UD.
 * No change is done in PML tree, all results are stored in CoNLL-U table only.
 * Assumes normalized ord values (only morpho tokens are normalized).
 * XPathExpressionException everywhere, because all the navigation in the XML is
 * done with XPaths.
 * Created on 2016-04-17.
 *
 * @author Lauma
 */
public class SentenceTransformator
{
	public Sentence s;
	/**
	 * Indication that transformation has failed and the obtained conll data is
	 * garbage.
	 */
	public boolean hasFailed;
	protected PhraseTransformator pTransf;
	public static boolean DEBUG = false;
	public static boolean WARN_ELLIPSIS = true;
	public static boolean WARN_OMISSIONS = true;
	/**
	 * For already processed nodes without tag set the phrase tag based on node
	 * chosen as substructure root.
	 */
	public static boolean INDUCE_PHRASE_TAGS = true;

	public SentenceTransformator(Node pmlTree) throws XPathExpressionException
	{
		s = new Sentence(pmlTree);
		hasFailed = false;
		pTransf = new PhraseTransformator(s);
	}

	/**
	 * Create CoNLL-U token table, try to fill it in as much as possible.
	 * @return	true, if tree has no untranformable ellipsis; false if tree
	 * 			contains untransformable ellipsis and, thus, result data
	 * 		    has garbage syntax.
	 * @throws XPathExpressionException
	 */
	public boolean transform() throws XPathExpressionException
	{
		if (DEBUG) System.out.printf("Working on sentence \"%s\".\n", s.id);

		transformTokens();
		boolean noMoreEllipsis = preprocessEllipsis();
		if (WARN_ELLIPSIS && !noMoreEllipsis)
			System.out.printf("Sentence \"%s\" has non-trivial ellipsis.\n", s.id);
		transformSyntax();
		return !hasFailed;
	}

	/**
	 * Utility method for "doing everything": create transformer object,
	 * transform given PML tree and get the string representation for the
	 * resulting CoNLL-U table.
	 * @param pmlTree	tree to transform
	 * @return 	UD tree in CoNLL-U format or null if tree could not be
	 * 			transformed.
	 * @throws XPathExpressionException
	 */
	public static String treeToConll(Node pmlTree)
	throws XPathExpressionException
	{
		SentenceTransformator t = new SentenceTransformator(pmlTree);
		boolean res = t.transform();
		if (res) return t.s.toConllU();
		if (WARN_OMISSIONS)
			System.out.printf("Sentence \"%s\" is being omitted.\n", t.s.id);
		return null;
	}

	/**
	 * Create CoNLL-U token table, fill in ID, FORM, LEMMA, XPOSTAG, UPOSTAG and
	 * FEATS fields.
	 * @throws XPathExpressionException
	 */
	public void transformTokens() throws XPathExpressionException
	{
		// Selects ord numbers from the tree.
		NodeList ordNodes = (NodeList)XPathEngine.get().evaluate(".//node[m.rf]/ord",
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
			NodeList nodes = (NodeList)XPathEngine.get().evaluate(".//node[m.rf and ord=" + ord + "]",
					s.pmlTree, XPathConstants.NODESET);
			if (nodes.getLength() > 1)
				System.err.printf("\"%s\" has several nodes with ord \"%s\", only first used.\n",
						s.id, ord);
			offset = transformCurrentToken(nodes.item(0), offset);
		}
	}

	/**
	 * Helper method: Create CoNLL-U table entry for one token, fill in ID,
	 * FORM, LEMMA, XPOSTAG, UPOSTAG and FEATS fields.
	 * @param aNode		PML A-level node for which CoNLL entry must be created.
	 * @param offset	Difference between PML node's ord value and ID value for
	 *                  CoNLL token to be created.
	 * @return Offset for next token.
	 * @throws XPathExpressionException
	 */
	protected int transformCurrentToken(Node aNode, int offset)
	throws XPathExpressionException
	{
		Node mNode = (Node)XPathEngine.get().evaluate("./m.rf[1]",
				aNode, XPathConstants.NODE);
		String mForm = XPathEngine.get().evaluate("./form", mNode);
		String mLemma = XPathEngine.get().evaluate("./lemma", mNode);
		String lvtbRole = Utils.getRole(aNode);
		String lvtbTag = XPathEngine.get().evaluate("./tag", mNode);
		boolean noSpaceAfter = false;
		if ("1".equals(XPathEngine.get().evaluate(
				"./w.rf/no_space_after|./w.rf/LM[last()]/no_space_after", mNode)))
			noSpaceAfter = true;

		if (mForm.contains(" ") || mLemma.contains(" "))
		{
			int baseOrd = Utils.getOrd(aNode);
			if (baseOrd < 1)
				throw new IllegalArgumentException("Node " + Utils.getId(aNode) + "has no ord value");

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
				Token lastTok = new Token(baseOrd + length-1 + offset, forms[length-1],
						lemmas[length-1], getXpostag(lvtbTag, "_SPLIT_PART"));
				lastTok.upostag = PosLogic.getUPosTag(lastTok.lemma, lastTok.xpostag, aNode);
				lastTok.feats = FeatsLogic.getUFeats(lastTok.form, lastTok.lemma, lastTok.xpostag, aNode);
				if (noSpaceAfter) lastTok.misc = "SpaceAfter=No";
				s.pmlaToConll.put(Utils.getId(aNode), lastTok);

				// Process the rest.
				// First one has different xpostag.
				String xpostag = getXpostag(lvtbTag, "_SPLIT_FIRST");
				for (int i = 0; i < length - 1; i++)
				{
					Token nextTok = new Token(baseOrd + offset, forms[i], lemmas[i], xpostag);
					nextTok.upostag = PosLogic.getUPosTag(nextTok.lemma, nextTok.xpostag, aNode);
					nextTok.feats = FeatsLogic.getUFeats(nextTok.form, nextTok.lemma, nextTok.xpostag, aNode);
					nextTok.head = lastTok.idBegin;
					nextTok.deprel = URelations.COMPOUND;
					s.conll.add(nextTok);
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
				Token firstTok = new Token(baseOrd + offset, forms[0],
						lemmas[0], getXpostag(lvtbTag, "_SPLIT_FIRST"));
				firstTok.upostag = PosLogic.getUPosTag(firstTok.lemma, firstTok.xpostag, aNode);
				firstTok.feats = FeatsLogic.getUFeats(firstTok.form, firstTok.lemma, firstTok.xpostag, aNode);
				s.conll.add(firstTok);
				s.pmlaToConll.put(Utils.getId(aNode), firstTok);

				// The rest
				for (int i = 1; i < forms.length && i < lemmas.length; i++)
				{
					offset++;
					Token nextTok = new Token(baseOrd + offset, forms[i],
							lemmas[i], getXpostag(lvtbTag, "_SPLIT_PART"));
					nextTok.upostag = PosLogic.getUPosTag(nextTok.lemma, nextTok.xpostag, aNode);
					nextTok.feats = FeatsLogic.getUFeats(nextTok.form, nextTok.lemma, nextTok.xpostag, aNode);
					nextTok.head = firstTok.idBegin;
					if ((i == forms.length - 1 || i == lemmas.length - 1) && noSpaceAfter)
						nextTok.misc = "SpaceAfter=No";
					if (lvtbTag.matches("xf.*")) nextTok.deprel = URelations.FOREIGN;
					else nextTok.deprel = URelations.MWE;
					s.conll.add(nextTok);
				}
			}
			// TODO Is reasonable fallback for unequal space count in lemma and form needed?
		} else
		{
			Token nextTok = new Token(
					Utils.getOrd(aNode) + offset, mForm, mLemma,
					getXpostag(XPathEngine.get().evaluate("./tag", mNode), null));
			nextTok.upostag = PosLogic.getUPosTag(nextTok.lemma, nextTok.xpostag, aNode);
			nextTok.feats = FeatsLogic.getUFeats(nextTok.form, nextTok.lemma, nextTok.xpostag, aNode);
			if (noSpaceAfter)
				 nextTok.misc = "SpaceAfter=No";
			s.conll.add(nextTok);
			s.pmlaToConll.put(Utils.getId(aNode), nextTok);
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
		if (ending == null || ending.length() < 1) return lvtbTag;
		else return lvtbTag + ending;
	}

	/**
	 * Remove the ellipsis nodes that can be ignored in latter processing.
	 * @return	 true if all ellipsis was removed
	 * @throws XPathExpressionException
	 */
	public boolean preprocessEllipsis() throws XPathExpressionException
	{
		// Childless, empty reductions are removed.
		NodeList ellipsisChildren = (NodeList) XPathEngine.get().evaluate(
				".//node[reduction and not(m.rf) and not(children)]", s.pmlTree, XPathConstants.NODESET);
		if (ellipsisChildren != null) for (int i = 0; i < ellipsisChildren.getLength(); i++)
		{
			Node current = ellipsisChildren.item(i);
			//Node morpho = Utils.getMNode(current);
			//Node phraseChild = Utils.getPhraseNode(current);
			//NodeList children = Utils.getPMLChildren(current);
			//if (morpho == null && phraseChild == null &&
			//		(children == null || children.getLength() < 1))
				current.getParentNode().removeChild(current);
		}

		// Check if there is other reductions.
		ellipsisChildren = (NodeList) XPathEngine.get().evaluate(
				".//node[reduction and not(m.rf)]", s.pmlTree, XPathConstants.NODESET);
		if (ellipsisChildren != null && ellipsisChildren.getLength() > 0) return false;
		/*for (int i = 0; i < ellipsisChildren.getLength(); i++)
		{
					NodeList morpho = (NodeList) XPathEngine.get().evaluate(
							"../../m.rf", ellipsisChildren.item(i), XPathConstants.NODESET);
			if (morpho == null || morpho.getLength() < 1) return false;
		}*/

		return true;
	}

	/**
	 * Fill in DEPREL and HEAD fields in CoNLL-U table.
	 * @throws XPathExpressionException
	 */
	public void transformSyntax() throws XPathExpressionException
	{
		Node pmlPmc = (Node)XPathEngine.get().evaluate(
				"./children/pmcinfo", s.pmlTree, XPathConstants.NODE);
		transformPhraseParts(pmlPmc);
		if (hasFailed) return;

		Node newRoot = pTransf.anyPhraseToUD(pmlPmc);
		if (newRoot == null)
			throw new IllegalArgumentException("Sentence " + s.id +" has no root PMC.");
		Token conllRoot = s.pmlaToConll.get(Utils.getId(newRoot));
		s.pmlaToConll.put(Utils.getId(s.pmlTree), conllRoot);
		conllRoot.head = 0;
		conllRoot.deprel = URelations.ROOT;
		transformDependents(s.pmlTree, newRoot);
	}

	/**
	 * Helper method: fill in DEPREL and HEAD fields in CoNLL-U table for given
	 * subtree.
	 * @param aNode	root of the subtree to process
	 * @throws XPathExpressionException
	 */
	protected void transformSubtree (Node aNode) throws XPathExpressionException
	{
		if (hasFailed) return;
		if (DEBUG) System.out.printf("Working on node \"%s\".\n", Utils.getId(aNode));

		NodeList children = Utils.getPMLChildren(aNode);
		if (children == null || children.getLength() < 1) return;

		Node newRoot = aNode;

		// Valid LVTB PMLs have no more than one type of phrase - pmc, x or coord.
		Node phraseNode = Utils.getPhraseNode(aNode);
		String reduction = Utils.getReduction(aNode);

		//// Process phrase overlords.
		if (phraseNode != null)
		{
			transformPhraseParts(phraseNode);
			if (hasFailed) return;
			newRoot = pTransf.anyPhraseToUD(phraseNode);
			if (newRoot == null)
				throw new IllegalStateException(
						"Algorithmic error: phrase transformation returned \"null\" root in sentence " + s.id);

			if (INDUCE_PHRASE_TAGS)
			{
				String phraseTag = Utils.getTag(aNode);
				String newRootTag = Utils.getTag(newRoot);
				if ((phraseTag == null || phraseTag.length() < 1 || phraseTag.matches("N/[Aa]")) &&
						newRootTag != null && newRootTag.length() > 0)
				{
					String type = phraseNode.getNodeName();
					if (type.equals("xinfo") || type.equals("coordinfo"))
					{
						Node tag = (Node)XPathEngine.get().evaluate("./tag", phraseNode, XPathConstants.NODE);
						if (tag == null) tag = phraseNode.getOwnerDocument().createElement("tag");
						while (tag.getFirstChild() != null)
							tag.removeChild(tag.getFirstChild());
						tag.appendChild(phraseNode.getOwnerDocument().createTextNode(newRootTag + "[INDUCED]"));
						phraseNode.appendChild(tag);
					}
				}
			}
		}
		//// Process reduction nodes.
		else if (Utils.getMNode(aNode) == null && reduction != null && reduction.length() > 0)
		{
			//System.out.println ("reduction " + reduction);
			Node redRoot = EllipsisLogic.newParent(aNode);
			if (redRoot == null)
			{
				hasFailed = true;
				return;
			}
			newRoot = redRoot;
			transformSubtree(newRoot);
			if (hasFailed) return;
		}

		//// Add information about new subroot in the result structure.
		s.pmlaToConll.put(Utils.getId(aNode), s.pmlaToConll.get(Utils.getId(newRoot)));

		//// Process dependants (except the newRoot).
		transformDependents(aNode, newRoot);
	}

	/**
	 * Helper method: fill in DEPREL and HEAD fields in CoNLL-U table for PML
	 * dependency children of the given node. If the newRoot is one of the
	 * dependents, then it must be processed before invoking this method.
	 * @param parentANode	node whose dependency children will be processed
	 * @param newRoot		node that will be the root of the coresponding UD
	 *                  	structure
	 * @throws XPathExpressionException
	 */
	protected void transformDependents(Node parentANode, Node newRoot)
	throws XPathExpressionException
	{
		if (hasFailed) return;
		NodeList pmlDependents = (NodeList)XPathEngine.get().evaluate(
				"./children/node", parentANode, XPathConstants.NODESET);
		Token newRootTok = s.pmlaToConll.get(Utils.getId(newRoot));
		if (pmlDependents != null && pmlDependents.getLength() > 0)
			for (int i = 0; i < pmlDependents.getLength(); i++)
		{
			// This happens in case of ellipsis.
			if (pmlDependents.item(i).isSameNode(newRoot)) continue;

			transformSubtree(pmlDependents.item(i));
			if (hasFailed) return;
			Token conllTok = s.pmlaToConll.get(Utils.getId(pmlDependents.item(i)));
			conllTok.deprel = DepRelLogic.depToUD(pmlDependents.item(i));
			conllTok.head = newRootTok.idBegin;
		}
	}

	/**
	 * Helper method: process subtrees under each part of PML phrase.
	 * @param phraseInfoNode	node whose dependency children will be processed
	 * @throws XPathExpressionException
	 */
	protected void transformPhraseParts(Node phraseInfoNode)
	throws XPathExpressionException
	{
		if (hasFailed) return;
		NodeList parts = (NodeList)XPathEngine.get().evaluate(
				"./children/node", phraseInfoNode, XPathConstants.NODESET);
		if (parts != null && parts.getLength() > 0)
			for (int i = 0; i < parts.getLength(); i++)
			{
				transformSubtree(parts.item(i));
				if (hasFailed) return;
			}
	}

}
