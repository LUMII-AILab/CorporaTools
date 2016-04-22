package lv.ailab.lvtb.universalizer.transformator;

import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.URelations;
import lv.ailab.lvtb.universalizer.pml.LvtbCoordTypes;
import lv.ailab.lvtb.universalizer.pml.LvtbPmcTypes;
import lv.ailab.lvtb.universalizer.pml.LvtbRoles;
import lv.ailab.lvtb.universalizer.pml.Utils;
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
public class PhraseTransformator
{

	public static Node pmcToUD(Node pmcNode, Sentence sent)
	throws XPathExpressionException
	{
		String pmcType = XPathEngine.get().evaluate("./pmctype", pmcNode);
		NodeList children = (NodeList) XPathEngine.get().evaluate("./children/*", pmcNode, XPathConstants.NODESET);

		if (pmcType.equals(LvtbPmcTypes.SENT) ||
				pmcType.equals(LvtbPmcTypes.UTER))
		{
			// Find the structure root.
			NodeList preds = (NodeList) XPathEngine.get().evaluate("./children/node[role = '" + LvtbRoles.PRED +"']", pmcNode, XPathConstants.NODESET);
			Node newRoot = null;
			if (preds != null && preds.getLength() > 1)
				System.err.printf("Sentence \"%s\" has more than one \"%s\" in \"%s\".",
						sent.id, LvtbRoles.PRED, pmcType);
			if (preds != null && preds.getLength() > 0) newRoot = Utils.getFirstByOrd(preds);
			else
			{
				preds = (NodeList) XPathEngine.get().evaluate("./children/node[role = '" + LvtbRoles.BASELEM +"']", pmcNode, XPathConstants.NODESET);
				newRoot = Utils.getFirstByOrd(preds);
			}
			if (newRoot == null)
			{
				System.err.printf("Sentence \"%s\" has no \"%s\", \"%s\" in \"%s\".",
						sent.id, LvtbRoles.PRED, LvtbRoles.BASELEM, pmcType);
				newRoot = Utils.getFirstByOrd(children);
			}
			if (newRoot == null)
				throw new IllegalArgumentException("Sentence \"" + sent.id + "\" seems to be empty.");

			// Create dependency structure in conll table.
			allAsDependents(sent, newRoot, children, pmcType);

			return newRoot;
		}
		if (pmcType.equals(LvtbPmcTypes.SUBRCL) || pmcType.equals(LvtbPmcTypes.MAINCL)
				|| pmcType.equals(LvtbPmcTypes.INSPMC) || pmcType.equals(LvtbPmcTypes.SPCPMC)
				|| pmcType.equals(LvtbPmcTypes.PARTICLE) || pmcType.equals(LvtbPmcTypes.DIRSPPMC)
				|| pmcType.equals(LvtbPmcTypes.QUOT) || pmcType.equals(LvtbPmcTypes.ADRESS)
				|| pmcType.equals(LvtbPmcTypes.INTERJ))
		{
			// Find the structure root.
			NodeList preds = (NodeList) XPathEngine.get().evaluate("./children/node[role = '" + LvtbRoles.PRED +"']", pmcNode, XPathConstants.NODESET);
			Node newRoot = null;
			if (preds != null && preds.getLength() > 1)
				System.err.printf("\"%s\" in sentence \"%s\" has more thatn one \"%s\".",
						pmcType, sent.id, LvtbRoles.PRED);
			if (preds != null && preds.getLength() > 0) newRoot = Utils.getFirstByOrd(preds);
			if (newRoot == null)
				throw new IllegalArgumentException(
						"\"" + pmcType +"\" in entence \"" + sent.id + "\" seems to be empty.");

			// Create dependency structure in conll table.
			allAsDependents(sent, newRoot, children, pmcType);
			return newRoot;
		}
		System.err.printf("Sentence \"%s\" has unrecognized \"%s\".",
				sent.id, pmcType);
		return null;
	}

	public static Node coordToUD(Node coordNode, Sentence sent)
	throws XPathExpressionException
	{
		String coordType = XPathEngine.get().evaluate("./pmctype", coordNode);
		NodeList children = (NodeList) XPathEngine.get().evaluate("./children/*", coordNode, XPathConstants.NODESET);

		if (coordType.equals(LvtbCoordTypes.CRDPARTS))
		{
			// Find the structure root.
			/*NodeList crdParts = (NodeList) xPathEngine.evaluate("./children/node[role = '" + LvtbRoles.CRDPART +"']", coordNode, XPathConstants.NODESET);
			Node newRoot = null;
			if (crdParts != null && crdParts.getLength() > 0)
				newRoot = getFirstByOrd(crdParts);

			if (newRoot == null)
			{
				System.err.printf("Sentence \"%s\" has no \"%s\" in \"%s\".",
						sent.id, LvtbRoles.CRDPART, coordType);
				newRoot = getFirstByOrd(children);
			}
			if (newRoot == null)
				throw new IllegalArgumentException(
						"\"" + coordType +"\" in entence \"" + sent.id + "\" seems to be empty.");

			// Create dependency structure in conll table.
			allAsDependents(sent, newRoot, children, coordType);//*/
			return coordPartsChildListToUD(Utils.asOrderedList(children), sent, coordType);
		}
		if (coordType.equals(LvtbCoordTypes.CRDCLAUSES))
		{
			NodeList semicolons = (NodeList)XPathEngine.get().evaluate("./children/node[m.rf/lemma = ';']", coordNode, XPathConstants.NODESET);
			if (semicolons == null || semicolons.getLength() < 1)
				return coordPartsChildListToUD(Utils.asOrderedList(children), sent, coordType);
			ArrayList<Node> sortedSemicolons = Utils.asOrderedList(semicolons);
			ArrayList<Node> sortedChildren = Utils.asOrderedList(children);
			int semicOrd = Utils.getOrd(sortedSemicolons.get(0));
			Node newRoot = coordPartsChildListToUD(
					Utils.ordSplice(sortedChildren, 0, semicOrd), sent, coordType);
			Token newRootToken = sent.pmlaToConll.get(newRoot);
			for (int i  = 1; i < sortedSemicolons.size(); i++)
			{
				int nextSemicOrd = Utils.getOrd(sortedSemicolons.get(i));
				Node newSubroot = coordPartsChildListToUD(
						Utils.ordSplice(sortedChildren, semicOrd, nextSemicOrd), sent, coordType);
				Token subrootToken = sent.pmlaToConll.get(newSubroot);
				subrootToken.deprel = URelations.PARATAXIS;
				subrootToken.head = newRootToken.idBegin;
			}

			return newRoot;
		}
		System.err.printf("Sentence \"%s\" has unrecognized \"%s\".",
				sent.id, coordType);
		return null;
	}

	protected static Node coordPartsChildListToUD(
			ArrayList<Node> sordedNodes, Sentence sent, String coordType)
	throws XPathExpressionException
	{
		// Find the structure root.
		Node newRoot = null;
		for (Node n : sordedNodes)
			if (LvtbRoles.CRDPART.equals(XPathEngine.get().evaluate("./role", n)))
		{
			newRoot = n;
			break;
		}
		if (newRoot == null)
		{
			System.err.printf("Sentence \"%s\" has no \"%s\" in \"%s\".",
					sent.id, LvtbRoles.CRDPART, coordType);
			newRoot = sordedNodes.get(0);
		}
		if (newRoot == null)
			throw new IllegalArgumentException(
					"\"" + coordType +"\" in entence \"" + sent.id + "\" seems to be empty.");

		// Create dependency structure in conll table.
		allAsDependents(sent, newRoot, sordedNodes, coordType);
		return newRoot;
	}

	public static Node xToUD(Node pmcNode, Sentence sent)
	throws XPathExpressionException
	{
		String xType = XPathEngine.get().evaluate("./pmctype", pmcNode);
		NodeList children = (NodeList) XPathEngine.get().evaluate("./children/*", pmcNode, XPathConstants.NODESET);

		System.err.printf("Sentence \"%s\" has unrecognized \"%s\".",
				sent.id, xType);
		return null;
	}

	protected static void allAsDependents(
			Sentence sent, Node newRoot, NodeList children, String phraseType)
	throws XPathExpressionException
	{
		allAsDependents(sent, newRoot, Utils.asList(children), phraseType);
	}

	protected static void allAsDependents(
			Sentence sent, Node newRoot, ArrayList<Node> children, String phraseType)
	throws XPathExpressionException
	{
		// Process root.
		Token rootToken = sent.pmlaToConll.get(newRoot);
		sent.conllToPmla.put(rootToken, newRoot);
		//if (phraseNode != null) sent.pmlaToConll.put(phraseNode, rootToken);
		// Process children.
		for (Node child : children)
		{
			if (child.equals(newRoot)) continue;
			Token childToken = sent.pmlaToConll.get(child);
			childToken.head = rootToken.idBegin;
			childToken.deprel = DepRelLogic.getUDepFromPhrsePart(child, phraseType);
		}
	}




}
