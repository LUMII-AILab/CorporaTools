package lv.ailab.lvtb.universalizer.pml.utils;

import lv.ailab.lvtb.universalizer.pml.PmlANode;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Utility methods for processing PML-A node lists.
 * Created on 2016-04-22. Splited in multiple classes on 2018-01-12.
 *
 * @author Lauma
 */
public class PmlANodeListUtils
{
	/**
	 * Create splice array that contains all nodes from the given array, to whom
	 * begin <= ord < end.
	 * @param nodes	list of nodes to splice
	 * @param begin smallest index (inclusive)
	 * @param end	largest index (excluse)
	 * @return	list with all elements satisfying the criterion, ordered in the
	 * 			same order as in input data
	 */

	public static ArrayList<PmlANode> ordSplice(
			List<? extends PmlANode> nodes, int begin, int end)
	{
		if (nodes == null) return null;
		ArrayList<PmlANode> res = new ArrayList<>();
		for (PmlANode n : nodes)
		{
			int ord = n.getDeepOrd();
			if (ord >= begin && ord < end) res.add(n);
		}
		return res;
	}

	/**
	 * Find node with the smallest ord value in its descendants.
	 * @param nodes list of nodes where to search
	 * @return	node with smallest given ord value in descendants
	 */
	public static <N extends PmlANode> N getFirstByDescOrd(List<N> nodes)
	{
		if (nodes == null) return null;
		if (nodes.size() == 1) return nodes.get(0);
		int smallestOrd = Integer.MAX_VALUE;
		N bestNode = null;
		for (N node : nodes)
		{
			Integer smallestTemp = node.getMinDescOrd();
			if (smallestTemp != null && smallestTemp < smallestOrd)
			{
				smallestOrd = smallestTemp;
				bestNode = node;
			}
		}
		return bestNode;
	}

	/**
	 * Find node with the biggest ord value in its descendants.
	 * @param nodes list of nodes where to search
	 * @return	node with smallest given ord value in descendants
	 */
	public static <N extends PmlANode> N getLastByDescOrd(List<N> nodes)
	{
		if (nodes == null) return null;
		if (nodes.size() == 1) return nodes.get(0);
		int biggestOrd = Integer.MIN_VALUE;
		N bestNode = null;
		for (N node : nodes)
		{
			Integer biggestTemp = node.getMaxDescOrd();
			if (biggestTemp > biggestOrd)
			{
				biggestOrd = biggestTemp;
				bestNode = node;
			}
		}
		return bestNode;
	}

	/**
	 * Find node with the biggest ord value. Nodes with no ord are ignored.
	 * @param nodes list of nodes where to search
	 * @return	node with largest given ord value
	 */
	public static <N extends PmlANode> N getLastByOrd(List<N> nodes)
	{
		if (nodes == null) return null;
		if (nodes.size() == 1) return nodes.get(0);
		int biggestOrd = Integer.MIN_VALUE;
		N bestNode = null;
		for (N node : nodes)
		{
			int ord = node.getOrd();
			if (ord > 0 && ord > biggestOrd)
			{
				biggestOrd = ord;
				bestNode = node;
			}
		}
		return bestNode;
	}

	/**
	 * Find node with the smallest ord nonzero value. Nodes with no ord are
	 * ignored.
	 * @param nodes list of nodes where to search
	 * @return	node with largest given ord value
	 */
	public static <N extends PmlANode> N getFirstByOrd(List<N> nodes)
	{
		if (nodes == null) return null;
		if (nodes.size() == 1) return nodes.get(0);
		int smallestOrd = Integer.MAX_VALUE;
		N bestNode = null;
		for (N node : nodes)
		{
			int ord = node.getOrd();
			if (ord > 0 && ord < smallestOrd)
			{
				smallestOrd = ord;
				bestNode = node;
			}
		}
		return bestNode;
	}

	/**
	 * Sort list PML-A nodes by ord values.
	 * @param nodes	list to sort
	 * @return	sorted list
	 */
	public static ArrayList<PmlANode> asOrderedList(List<? extends PmlANode> nodes)
	{
		// TODO: is there some more effective way?
		TreeMap<Integer, ArrayList<PmlANode>> semiRes = new TreeMap<>();

		for (PmlANode node : nodes)
		{
			int ord = node.getDeepOrd();
			if (!semiRes.containsKey(ord)) semiRes.put(ord, new ArrayList<>());
			semiRes.get(ord).add(node);
		}
		ArrayList<PmlANode> res = new ArrayList<>();
		for (Integer ordKey : semiRes.keySet())
			res.addAll(semiRes.get(ordKey));

		return res;
	}
}
