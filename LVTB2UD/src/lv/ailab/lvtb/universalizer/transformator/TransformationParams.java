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
	/**
	 * For already processed nodes without tag set the phrase tag based on node
	 * chosen as substructure root.
	 */
	public Boolean INDUCE_PHRASE_TAGS = true;
	/**
	 * What to when a file contains an untransformable tree? For true - whole
	 * file is omitted; for false - only specific tree.
	 */
	public Boolean OMIT_WHOLE_FILES = false;

	/**
	 * If true, inserted words ar converted to ellipsis and then processed
	 * according UD ellipsis guidelines. Otherwise transformator just crash on
	 * them.
	 */
	public Boolean TURN_INSERTED_WORD_ELLIPSIS = true;

	// ===== For fitting UD standard.
	/**
	 * To fit UD standard, this must be true. If this is false, enhanced graph
	 * will contain empty nodes for inserted commas and nonpredicative empty
	 * nodes.
	 */
	public Boolean UD_STANDARD_NULLNODES = true; // NB! Check this before each UD release!

	/**
	 * To fit UD standard, this must be true. When true, each ellipsis node with
	 * morphology is split into empty ellipsis node and dependant node with
	 * morphology.
	 */
	public Boolean SPLIT_NONEMPTY_ELLIPSIS = true; // NB! Check this before each UD release!

	/**
	 * To fit UD standard this must be true. When true, preprocessind is done to
	 * rise punct dependants of other punct.
	 */
	public Boolean NORMALIZE_PUNCT_ATTACHMENT = true; // NB! Check this before each UD release!

	/**
	 * To fit UD standard this must be true. When true, nonprojective
	 * punctuation is relinked.
	 */
	public Boolean NORMALIZE_NONPROJ_PUNCT = true; // NB! Check this before each UD release!

	// ===== Enhanced.
	/**
	 * To fit UD standard, this must be true. If this is false, enhanced graph
	 * may contain multiple links/labels per the same node pair and link
	 * direction.
	 */
	public Boolean NO_EDEP_DUPLICATES = true; // NB! Check this before each UD release!

	/**
	 * Should transformators add controlled/raised subject links in enhanced
	 * graph?
	 */
	public Boolean ADD_CONTROL_SUBJ = true; // NB! Check this before each UD release!

	/**
	 * Should transformators add coordination propagation links in enhanced
	 * graph?
	 */
	public Boolean PROPAGATE_CONJUNCTS = true; // NB! Check this before each UD release!

	/**
	 * Should enhanced links with role "dep" be thrown out?
	 */
	public Boolean CLEANUP_UNLABELED_EDEPS = false;

	/**
	 * Get default parameter set.
	 */
	public TransformationParams(){};
}
