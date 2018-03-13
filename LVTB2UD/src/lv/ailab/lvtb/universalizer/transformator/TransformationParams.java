package lv.ailab.lvtb.universalizer.transformator;

public class TransformationParams
{
	/**
	 * Add LvtbNodeId in Misc column.
	 */
	public Boolean ADD_NODE_IDS = true;
	/**
	 * Print debug messages on each node.
	 */
	public Boolean DEBUG = false;
	/**
	 * Print warning when ellipis is encoutered.
	 */
	public Boolean WARN_ELLIPSIS = false;
	/**
	 * Print warning when a sentence is omitted.
	 */
	public Boolean WARN_OMISSIONS = true;
	/*
	 * Make enhanced graph.
	 * BUGGED.
	 */
	//public Boolean DO_ENHANCED = true;
	/**
	 * For already processed nodes without tag set the phrase tag based on node
	 * chosen as substructure root.
	 */
	public Boolean INDUCE_PHRASE_TAGS = true;
	/*
	 * Rename newswire IDs' document part.
	 * Deprecated functionality.
	 */
	//@Deprecated
	//public Boolean CHANGE_IDS = false;
	/**
	 * What to when a file contains an untransformable tree? For true - whole
	 * file is omitted; for false - only specific tree.
	 */
	public Boolean OMIT_WHOLE_FILES = false;

	/**
	 * To fit UD standard, this must be true. If this is false, enhanced graph
	 * will contain empty nodes for inserted commas and nonpredicative empty
	 * nodes.
	 * TODO: implement this.
	 */
	public Boolean UD_STANDARD_NULLNODES = true;

	/**
	 * Get default parameter set.
	 */
	public TransformationParams(){};
}
