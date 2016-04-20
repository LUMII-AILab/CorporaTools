package lv.ailab.lvtb.universalizer.transformator;

import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.URelations;
import lv.ailab.lvtb.universalizer.pml.LvtbCoordTypes;
import lv.ailab.lvtb.universalizer.pml.LvtbPmcTypes;
import lv.ailab.lvtb.universalizer.pml.LvtbRoles;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Created on 2016-04-20.
 *
 * @author Lauma
 */
public class PhraseTransformator
{
	protected XPath xPathEngine;
	protected DepRelLogic depLogic;

	public PhraseTransformator (XPath xPathEngine)
	{
		this.xPathEngine = xPathEngine;
		depLogic = new DepRelLogic(xPathEngine);
	}

	public Node pmcToUD(Node pmcNode, Sentence sent)
	throws XPathExpressionException
	{
		String pmcType = xPathEngine.evaluate("./pmctype", pmcNode);
		NodeList children = (NodeList) xPathEngine.evaluate("./children/*", pmcNode, XPathConstants.NODESET);

		if (pmcType.equals(LvtbPmcTypes.SENT) ||
				pmcType.equals(LvtbPmcTypes.UTER))
		{
			// Find the structure root.
			NodeList preds = (NodeList) xPathEngine.evaluate("./children/node[role = '" + LvtbRoles.PRED +"']", pmcNode, XPathConstants.NODESET);
			Node newRoot = null;
			if (preds != null && preds.getLength() > 1)
				System.err.printf("Sentence \"%s\" has more than one \"%s\" in \"%s\".",
						sent.id, LvtbRoles.PRED, pmcType);
			if (preds != null && preds.getLength() > 0) newRoot = getFirstByOrd(preds);
			else
			{
				preds = (NodeList) xPathEngine.evaluate("./children/node[role = '" + LvtbRoles.BASELEM +"']", pmcNode, XPathConstants.NODESET);
				newRoot = getFirstByOrd(preds);
			}
			if (newRoot == null)
			{
				System.err.printf("Sentence \"%s\" has no \"%s\", \"%s\" in \"%s\".",
						sent.id, LvtbRoles.PRED, LvtbRoles.BASELEM, pmcType);
				newRoot = getFirstByOrd(children);
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
			NodeList preds = (NodeList) xPathEngine.evaluate("./children/node[role = '" + LvtbRoles.PRED +"']", pmcNode, XPathConstants.NODESET);
			Node newRoot = null;
			if (preds != null && preds.getLength() > 1)
				System.err.printf("\"%s\" in sentence \"%s\" has more thatn one \"%s\".",
						pmcType, sent.id, LvtbRoles.PRED);
			if (preds != null && preds.getLength() > 0) newRoot = getFirstByOrd(preds);
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

	public Node coordToUD(Node coordNode, Sentence sent)
	throws XPathExpressionException
	{
		String coordType = xPathEngine.evaluate("./pmctype", coordNode);
		NodeList children = (NodeList) xPathEngine.evaluate("./children/*", coordNode, XPathConstants.NODESET);

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
			return coordPartsChildListToUD(asOrderedList(children), sent, coordType);
		}
		if (coordType.equals(LvtbCoordTypes.CRDCLAUSES))
		{
			NodeList semicolons = (NodeList)xPathEngine.evaluate("./children/node[m.rf/lemma = ';']", coordNode, XPathConstants.NODESET);
			if (semicolons == null || semicolons.getLength() < 1)
				return coordPartsChildListToUD(asOrderedList(children), sent, coordType);
			ArrayList<Node> sortedSemicolons = asOrderedList(semicolons);
			ArrayList<Node> sortedChildren = asOrderedList(children);
			int semicOrd = getOrd(sortedSemicolons.get(0));
			Node newRoot = coordPartsChildListToUD(
					ordSplice(sortedChildren, 0, semicOrd), sent, coordType);
			Token newRootToken = sent.pmlaToConll.get(newRoot);
			for (int i  = 1; i < sortedSemicolons.size(); i++)
			{
				int nextSemicOrd = getOrd(sortedSemicolons.get(i));
				Node newSubroot = coordPartsChildListToUD(
						ordSplice(sortedChildren, semicOrd, nextSemicOrd), sent, coordType);
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

	protected Node coordPartsChildListToUD(
			ArrayList<Node> sordedNodes, Sentence sent, String coordType)
	throws XPathExpressionException
	{
		// Find the structure root.
		Node newRoot = null;
		for (Node n : sordedNodes)
			if (LvtbRoles.CRDPART.equals(xPathEngine.evaluate("./role", n)))
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

	public Node xToUD(Node pmcNode, Sentence sent)
	throws XPathExpressionException
	{
		String xType = xPathEngine.evaluate("./pmctype", pmcNode);
		NodeList children = (NodeList) xPathEngine.evaluate("./children/*", pmcNode, XPathConstants.NODESET);

		System.err.printf("Sentence \"%s\" has unrecognized \"%s\".",
				sent.id, xType);
		return null;
	}

	protected void allAsDependents(
			Sentence sent, Node newRoot, NodeList children, String phraseType)
	throws XPathExpressionException
	{
		allAsDependents(sent, newRoot, asList(children), phraseType);
	}

	protected void allAsDependents(
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
			childToken.deprel = depLogic.getUDepFromPhrsePart(child, phraseType);
		}
	}

	protected int getOrd(Node node) throws XPathExpressionException
	{
		String ordStr = xPathEngine.evaluate("./ord", node);
		int ord = 0;
		if (ordStr != null) ord = Integer.parseInt(ordStr);
		return ord;
	}

	protected static ArrayList<Node> asList (NodeList nodes)
	{
		ArrayList<Node> res = new ArrayList<>();
		for (int i = 0; i < nodes.getLength(); i++)
			res.add(nodes.item(i));
		return res;
	}

	protected ArrayList<Node> asOrderedList(NodeList nodes)
	throws XPathExpressionException
	{
		// TODO: is there some more effective way?
		TreeMap<Integer, ArrayList<Node>> semiRes = new TreeMap<>();

		for (int i = 0; i < nodes.getLength(); i++)
		{
			int smallestOrd = Integer.MAX_VALUE;
			NodeList ords = (NodeList)xPathEngine.evaluate(".//ord", nodes.item(i), XPathConstants.NODESET);
			for (int j = 0; j < ords.getLength(); j ++)
			{
				int ord = Integer.parseInt(ords.item(j).getTextContent());
				if (ord < smallestOrd)smallestOrd = ord;
			}
			if (smallestOrd == Integer.MAX_VALUE) smallestOrd = 0;
			if (!semiRes.containsKey(smallestOrd)) semiRes.put(smallestOrd, new ArrayList<>());
			semiRes.get(smallestOrd).add(nodes.item(i));
		}
		ArrayList<Node> res = new ArrayList<>();
		for (Integer ordKey : semiRes.keySet())
			res.addAll(semiRes.get(ordKey));

		return res;
	}

	protected ArrayList<Node> ordSplice(ArrayList<Node> nodes, int begin, int end)
	throws XPathExpressionException
	{
		if (nodes == null) return null;
		ArrayList<Node> res = new ArrayList<>();
		for (Node n : nodes)
		{
			int ord = getOrd(n);
			if (ord >= begin && ord < end) res.add(n);
		}
		return res;
	}

	protected Node getFirstByOrd(NodeList nodes) throws XPathExpressionException
	{
		if (nodes == null) return null;
		if (nodes.getLength() == 1) return nodes.item(1);
		int smallestOrd = Integer.MAX_VALUE;
		Node bestNode = null;
		for (int i = 0; i < nodes.getLength(); i++)
		{
			NodeList ords = (NodeList)xPathEngine.evaluate(".//ord", nodes.item(i), XPathConstants.NODESET);
			for (int j = 0; j < ords.getLength(); j ++)
			{
				int ord = Integer.parseInt(ords.item(j).getTextContent());
				if (ord < smallestOrd)
				{
					smallestOrd = ord;
					bestNode = nodes.item(i);
				}
			}
		}
		return bestNode;
	}

	protected Node getLastByOrd(NodeList nodes) throws XPathExpressionException
	{
		if (nodes == null) return null;
		if (nodes.getLength() == 1) return nodes.item(1);
		int biggestOrd = Integer.MIN_VALUE;
		Node bestNode = null;
		for (int i = 0; i < nodes.getLength(); i++)
		{
			NodeList ords = (NodeList)xPathEngine.evaluate(".//ord", nodes.item(i), XPathConstants.NODESET);
			for (int j = 0; j < ords.getLength(); j ++)
			{
				int ord = Integer.parseInt(ords.item(j).getTextContent());
				if (ord > biggestOrd)
				{
					biggestOrd = ord;
					bestNode = nodes.item(i);
				}
			}
		}
		return bestNode;
	}

}
