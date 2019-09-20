package lv.ailab.lvtb.universalizer.pml;

import java.util.List;

/**
 * PML a-level node inteface. The ID-less xinfo, pmcinfo and coordinfo
 * structures also are considered to be a PML nodes in the understanding of this
 * class. Thus, node with ID representing complex predicate has a xinfo as a
 * child.
 * Currently this design choice reflect the organization of the PML XML
 * representation, where xinfo, pmcinfo and coordinfo nodes are included in the
 * childlist of the "normal" node representing phrase's position in the sentence
 * structure. Maybe this should be reconsidered later, as it might be unsensible
 * for other representations.
 *
 * Created on 2018-01-24.
 * @author Lauma
 */
public interface PmlANode
{
	//=== Field querying =======================================================
	/**
	 * Find ID attribute.
	 * @return	attribute value
	 */
	public String getId();
	/**
	 * Determine node type.
	 * @return 	type of the node - X, COORD or PMC for ID-less nodes, ROOT for
	 * 			root of the tree, NODE for others.
	 */
	public Type getNodeType();
	/**
	 * Method for convenience: check, if given node is a phrase node.
	 * @return	true, if node's type is PmlANode.Type.X or PmlANode.Type.COORD
	 * 			or PmlANode.Type.PMC
	 */
	public boolean isPhraseNode();
	/**
	 * Find the role of this node.
	 * @return	role value for NODE, nothing for other node types
	 */
	public String getRole();
	/**
	 * Find pmctype, coordtype or xtype for this node.
	 * @return	phrase type for X, COORD, PMC, nothing for other node types
	 */
	public String getPhraseType();
	/**
	 * Find pmctype, coordtype, xtype or role for this node.
	 * @return	phrase type or dependency role or LVtbHelperRoles.ROOT for root
	 */
	public String getAnyLabel();
	/**
	 * Find the closest ancestor (given node included), whose label is not
	 * crdPart, crdParts or crdClauses, and return its role, pmctype, coordtype,
	 * or xtype.
	 * @return	phrase type or dependency role or LVtbHelperRoles.ROOT for root.
	 */
	public String getEffectiveLabel();
	/**
	 * Find the phrase tag of this node.
	 * @return	tag value for X or COORD having a tag value, nothing for other
	 * 			nodes and node types
	 */
	public String getPhraseTag();
	/**
	 * Find tag attribute. Use either morphotag or x-word tag or coordination
	 * tag. For tokenless reduction nodes return reduction tag. For PMC node
	 * return first basElem's tag. For coordinations with no given tag return
	 * tag obtained from first coordinated part.
	 * @return	tag
	 */
	public String getAnyTag();
	/**
	 * Find lemma from morphological layer.
	 * @return	lemma from morphological layer.
	 */
	public boolean isPureReductionNode();
	/**
	 * Find reduction field value.
	 * @return	reduction value
	 */
	public String getReduction();
	/**
	 * Find reduction field value and cut off the ending part in braces.
	 * @return	reduction tag
	 */
	public String getReductionTagPart();
	/**
	 * Find reduction field value and cut off the begining part before braces
	 * and braces themselves.
	 * @return	reduction wordform
	 */
	public String getReductionFormPart();
	/**
	 * Find reduction field value, split in tag and lemma, and then induce lemma
	 * with the help of morphological analyzer.
	 * @return	reduction lemma
	 */
	public String getReductionLemma();
	/**
	 * Find ord value for this node, if there is one.
	 * @return	ord value, or 0, if no ord found, or null if node is null
	 */
	public Integer getOrd();
	/**
	 * Get ord value for given node, if there is one. Otherwise, if this node
	 * has a phrase node (or is a phrase node), use this function on its
	 * constituents and return the smallest. Other-otherwise, if this node is
	 * an empty reduction node, use this function on its children and return the
	 * smallest.
	 * @return	ord value, or 0, if ord can't be found (no phrase children with
	 * 			ord values etc.), or null if node is null
	 */
	public Integer getDeepOrd();
	/**
	 * Find smallest ord number found in the subtree rooted in this node.
	 * @return	ord number or null if none of the nodes in the subtree has an
	 *			ord number
	 */
	public Integer getMinDescOrd();
	/**
	 * Find biggest ord number found in the subtree rooted in this node.
	 * @return	ord number or null if none of the nodes in the subtree has an
	 *			ord number
	 */
	public Integer getMaxDescOrd();

	//=== Functions to access children, parent, ancestors, etc. ================
	/**
	 * Get underlying PML-M level node.
	 * @return PML-M node or null if such node was not found.
	 */
	public PmlMNode getM();
	/**
	 * Find PML parent for this node or phrase structure. For a node
	 * representing a phrase constituent this will return phrase node (Type.X,
	 * Type.COORD or Type.PMC) containing it. For a phrase node this will return
	 * node (Type.NODE or Type.ROOT) representing phrase in the sentence.
	 * @return	null for root, otherwise any PML node (can be also phrase node)
	 */
	//public <PMLN extends PmlANode> PMLN getParent();
	public PmlANode getParent();
	/**
	 * If this a normal node (with ID) that has a constituent node, find its
	 * pmcinfo, coordinfo or xinfo structure.
	 * @return	phrase, coordination or x-word structure
	 */
	public PmlANode getPhraseNode();
	/**
	 * Find all node children in PML sense - for normal node this is returns all
	 * dependents, for phrase node - all constituents.
	 * @return	children list with no guaranteed order
	 */
	public List<PmlANode> getChildren();
	/**
	 * Find all node children with the given role. For normal node this returns
	 * all dependents with given role, for phrase node - all constituents.
	 * @param role	role restriction
	 * @return	children list with no guaranteed order
	 */
	public List<PmlANode> getChildren(String role);
	/**
	 * Find descendant node by given ID (thus, no phrase nodes will be found)
	 * @param id	an ID to search
	 * @return	first node found
	 */
	public PmlANode getDescendant(String id);
	/**
	 * Find this or descendant node by given ID (thus, no phrase nodes will be
	 * found)
	 * @param id	an ID to search
	 * @return	first node found
	 */
	public PmlANode getThisOrDescendant(String id);
	/**
	 * Get any descendants of any type. Root is not included.
	 * @return	descendant list
	 */
	public List<PmlANode> getDescendants();
	/**
	 * Get any descendants whose role, xtype, coordtype, or pmctype matches
	 * the given label. Root is not included.
	 * @param anyLabel	label to restrict search
	 * @return	list with found descendants
	 */
	public List<PmlANode> getDescendants(String anyLabel);
	/**
	 * Get all descendants having both morphology and ord value. Root is not
	 * included.
	 * @return	descendant list sorted by ord values
	 */
	public List<PmlANode> getDescendantsWithOrdAndM();

	/**
	 * Find pure ellipsis (no corresponding token) in the subtree headed by this
	 * node. Parameter allows to find either all ellipsis or only leaf nodes.
	 * @param leafsOnly	if true, only leaf nodes are returned
	 * @return	list of ellipsis nodes in no particular order
	 */
	public List<PmlANode> getPureEllipsisDescendants(boolean leafsOnly);
	/**
	 * Find ellipsis nodes with corresponding token in the subtree headed by
	 * this node. Parameter allows to find either all ellipsis or only leaf
	 * nodes.
	 * @param leafsOnly	if true, only leaf nodes are returned
	 * @return	list of ellipsis nodes in no particular order
	 */
	public List<PmlANode> getMorphoEllipsisDescendants(boolean leafsOnly);

	/**
	 * Find nodes having m-token, form_change "insert" and no w-token.
	 * @return list of inserted token nodes in no particular order
	 */
	public List<PmlANode> getInsertedMorphoDescendants();

	/**
	 * Find parent or the closest ancestor, that is not coordination phrase or
	 * crdPart node.
	 * @return	PML a-level node or xinfo, pmcinfo, or coordinfo
	 */
	public PmlANode getEffectiveAncestor();
	/**
	 * Return this node, parent or the closest ancestor, that is not
	 * coordination phrase or crdPart node.
	 * @return	PML a-level node or xinfo, pmcinfo, or coordinfo
	 */
	public PmlANode getThisOrEffectiveAncestor();

	//=== Comparison ===========================================================
	/**
	 * Returns whether this node is the same node as the given one. This is used
	 * to determine if no circular dependencies are drawn etc. If two objects
	 * are equal according to Object.equals function, they must return true.
	 * @param other	the node to test against
	 * @return	true if the nodes are the same, false otherwise
	 */
	public boolean isSameNode(PmlANode other);

	/**
	 * Returns the lenght of the shortest path connecting this node and root.
	 * @return	0 for root node, 1 for root's dependents and constituents, 2 for
	 * 			for their dependents and constituents, etc.
	 */
	public Integer getDepthInTree();

	//=== Tree modification ====================================================
	/**
	 * Remove this node from tree.
	 */
	public void delete();
	/**
	 * Remove this nodes m-node.
	 */
	public void deleteM();

	/**
	 * Set a phraseTag for X or COORD node, return false for other node types.
	 * @param tag	tag value to set
	 * @return	true if tag was set
	 */
	public boolean setPhraseTag(String tag);

	/**
	 * Set a reduction tag if there is none, return false otherwise.
	 * @param tag	tag value to set
	 * @return	true if tag was set
	 */
	public boolean setReductionTag(String tag);

	/**
	 * Set a reduction form, if there is none, return false otherwise.
	 * @param form	form value to set
	 * @return	true if form was set
	 */
	public boolean setReductionForm(String form);

	/**
	 * Split nonempty ellipsis node into empty ellipsis node and dependant
	 * child.
	 * @param idPostfix	string to append to the node ID to create ID for new
	 *                  node.
	 * @return	if an actual split was done
	 */
	public boolean splitMorphoEllipsis(String idPostfix);


	/**
	 * Distinguished node types. Logic-wise, ROOT is a subtlype of NODE.
	 */
	public static enum Type
	{
		ROOT, NODE, X, COORD, PMC;
	}
}
