package lv.ailab.lvtb.universalizer.pml;

import lv.ailab.lvtb.universalizer.transformator.XPathEngine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Utility methods for processing PML XMLs.
 * Created on 2016-04-22.
 *
 * @author Lauma
 */
public class Utils
{
	/**
	 * Find ord value for given Node. If not found, return 0.
	 */
	public static int getOrd(Node node) throws XPathExpressionException
	{
		String ordStr = XPathEngine.get().evaluate("./ord", node);
		int ord = 0;
		if (ordStr != null) ord = Integer.parseInt(ordStr);
		return ord;
	}

	/**
	 * Create splice array that contains all nodes from the given array, to whom
	 * begin <= ord < end.
	 * @throws XPathExpressionException
	 */

	public static ArrayList<Node> ordSplice(ArrayList<Node> nodes, int begin, int end)
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

	/**
	 * Find node with the smallest ord value in its descendants.
	 */
	public static Node getFirstByOrd(NodeList nodes)
	throws XPathExpressionException
	{
		if (nodes == null) return null;
		if (nodes.getLength() == 1) return nodes.item(1);
		int smallestOrd = Integer.MAX_VALUE;
		Node bestNode = null;
		for (int i = 0; i < nodes.getLength(); i++)
		{
			NodeList ords = (NodeList)XPathEngine.get().evaluate(
					".//ord", nodes.item(i), XPathConstants.NODESET);
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

	/**
	 * Find node with the biggest ord value in its descendants.
	 */
	public static Node getLastByOrd(NodeList nodes)
	throws XPathExpressionException
	{
		if (nodes == null) return null;
		if (nodes.getLength() == 1) return nodes.item(1);
		int biggestOrd = Integer.MIN_VALUE;
		Node bestNode = null;
		for (int i = 0; i < nodes.getLength(); i++)
		{
			NodeList ords = (NodeList) XPathEngine.get().evaluate(
					".//ord", nodes.item(i), XPathConstants.NODESET);
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

	/**
	 * Transform NodeList to ArrayList of Node.
	 * TODO: is there a more optimal implementation?
	 */
	public static ArrayList<Node> asList (NodeList nodes)
	{
		ArrayList<Node> res = new ArrayList<>();
		for (int i = 0; i < nodes.getLength(); i++)
			res.add(nodes.item(i));
		return res;
	}

	/**
	 * Transform NodeList to ArrayList of Node and sort list by ord values.
	 */
	public static ArrayList<Node> asOrderedList(NodeList nodes)
	throws XPathExpressionException
	{
		// TODO: is there some more effective way?
		TreeMap<Integer, ArrayList<Node>> semiRes = new TreeMap<>();

		for (int i = 0; i < nodes.getLength(); i++)
		{
			int smallestOrd = Integer.MAX_VALUE;
			NodeList ords = (NodeList)XPathEngine.get().evaluate(".//ord", nodes.item(i), XPathConstants.NODESET);
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
}
