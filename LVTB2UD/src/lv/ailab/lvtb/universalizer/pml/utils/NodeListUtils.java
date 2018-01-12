package lv.ailab.lvtb.universalizer.pml.utils;

import lv.ailab.lvtb.universalizer.utils.XPathEngine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Utility methods for processing PML XML node lists.
 * Created on 2016-04-22. Splited in multiple classes on 2018-01-12.
 *
 * @author Lauma
 */
public class NodeListUtils
{
	/**
	 * Create splice array that contains all nodes from the given array, to whom
	 * begin <= ord < end.
	 * @param nodes	list of nodes to splice
	 * @param begin smallest index (inclusive)
	 * @param end	largest index (excluse)
	 * @return	list with all elements satisfying the criterion, ordered in the
	 * 			same order as in input data
	 * @throws XPathExpressionException    unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */

	public static ArrayList<Node> ordSplice(List<Node> nodes, int begin, int end)
	throws XPathExpressionException
	{
		if (nodes == null) return null;
		ArrayList<Node> res = new ArrayList<>();
		for (Node n : nodes)
		{
			int ord = NodeFieldUtils.getDeepOrd(n);
			if (ord >= begin && ord < end) res.add(n);
		}
		return res;
	}

	/**
	 * Find node with the smallest ord value in its descendants.
	 * @param nodes list of nodes where to search
	 * @return	node with smallest given ord value in descendants
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static Node getFirstByDescOrd(NodeList nodes)
	throws XPathExpressionException
	{
		if (nodes == null) return null;
		if (nodes.getLength() == 1) return nodes.item(0);
		int smallestOrd = Integer.MAX_VALUE;
		Node bestNode = null;
		for (int i = 0; i < nodes.getLength(); i++)
		{
			NodeList ords = (NodeList) XPathEngine.get().evaluate(
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
	 * @param nodes list of nodes where to search
	 * @return	node with smallest given ord value in descendants
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static Node getLastByDescOrd(NodeList nodes)
	throws XPathExpressionException
	{
		if (nodes == null) return null;
		if (nodes.getLength() == 1) return nodes.item(0);
		int biggestOrd = Integer.MIN_VALUE;
		Node bestNode = null;
		for (int i = 0; i < nodes.getLength(); i++)
		{
			NodeList ords = (NodeList)XPathEngine.get().evaluate(
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
	 * Find node with the biggest ord value. Nodes with no ord are ignored.
	 * @param nodes list of nodes where to search
	 * @return	node with largest given ord value
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static Node getLastByOrd(NodeList nodes)
	throws XPathExpressionException
	{
		if (nodes == null) return null;
		if (nodes.getLength() == 1) return nodes.item(0);
		int biggestOrd = Integer.MIN_VALUE;
		Node bestNode = null;
		for (int i = 0; i < nodes.getLength(); i++)
		{
			int ord = NodeFieldUtils.getOrd(nodes.item(i));
			if (ord > 0 && ord > biggestOrd)
			{
				biggestOrd = ord;
				bestNode = nodes.item(i);
			}
		}
		return bestNode;
	}

	/**
	 * Find node with the biggest ord value. Nodes with no ord are ignored.
	 * @param nodes list of nodes where to search
	 * @return	node with largest given ord value
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static Node getLastByOrd(List<Node> nodes)
			throws XPathExpressionException
	{
		if (nodes == null) return null;
		if (nodes.size() == 1) return nodes.get(0);
		int biggestOrd = Integer.MIN_VALUE;
		Node bestNode = null;
		for (int i = 0; i < nodes.size(); i++)
		{
			int ord = NodeFieldUtils.getOrd(nodes.get(i));
			if (ord > 0 && ord > biggestOrd)
			{
				biggestOrd = ord;
				bestNode = nodes.get(i);
			}
		}
		return bestNode;
	}

	/**
	 * Find node with the smallest ord nonzero value. Nodes with no ord are
	 * ignored.
	 * @param nodes list of nodes where to search
	 * @return	node with largest given ord value
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static Node getFirstByOrd(List<Node> nodes)
			throws XPathExpressionException
	{
		if (nodes == null) return null;
		if (nodes.size() == 1) return nodes.get(0);
		int smallestOrd = Integer.MAX_VALUE;
		Node bestNode = null;
		for (int i = 0; i < nodes.size(); i++)
		{
			int ord = NodeFieldUtils.getOrd(nodes.get(i));
			if (ord > 0 && ord < smallestOrd)
			{
				smallestOrd = ord;
				bestNode = nodes.get(i);
			}
		}
		return bestNode;
	}

	/**
	 * Transform NodeList to ArrayList of Node.
	 * @param nodes	list to tranform
	 * @return	transformed list (original node ordering is preserved)
	 * TODO: is there a more optimal implementation?
	 */
	public static ArrayList<Node> asList (NodeList nodes)
	{
		if (nodes == null) return null;
		ArrayList<Node> res = new ArrayList<>();
		for (int i = 0; i < nodes.getLength(); i++)
			res.add(nodes.item(i));
		return res;
	}

	/**
	 * Transform NodeList to ArrayList of Node and sort list by ord values.
	 * @param nodes	list to sort
	 * @return	sorted list
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public static ArrayList<Node> asOrderedList(NodeList nodes)
	throws XPathExpressionException
	{
		// TODO: is there some more effective way?
		TreeMap<Integer, ArrayList<Node>> semiRes = new TreeMap<>();

		for (int i = 0; i < nodes.getLength(); i++)
		{
			/*int smallestOrd = Integer.MAX_VALUE;
			NodeList ords = (NodeList)XPathEngine.get().evaluate(".//ord", nodes.item(i), XPathConstants.NODESET);
			for (int j = 0; j < ords.getLength(); j ++)
			{
				int ord = Integer.parseInt(ords.item(j).getTextContent());
				if (ord < smallestOrd)smallestOrd = ord;
			}
			if (smallestOrd == Integer.MAX_VALUE) smallestOrd = 0;
			if (!semiRes.containsKey(smallestOrd)) semiRes.put(smallestOrd, new ArrayList<>());
			semiRes.get(smallestOrd).add(nodes.item(i));*/
			Node n = nodes.item(i);
			int ord = NodeFieldUtils.getDeepOrd(n);
			if (!semiRes.containsKey(ord)) semiRes.put(ord, new ArrayList<>());
			semiRes.get(ord).add(n);
		}
		ArrayList<Node> res = new ArrayList<>();
		for (Integer ordKey : semiRes.keySet())
			res.addAll(semiRes.get(ordKey));

		return res;
	}
}
