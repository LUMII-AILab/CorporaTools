package lv.ailab.lvtb.universalizer.pml;

import java.util.List;
import java.util.Set;

/**
 * Simplified PML-M level interface. As of now, this interface does not require
 * node to know which sentence it belongs or who are its siblings
 */
public interface PmlMNode
{
	/**
	 * Find ID attribute.
	 * @return	attribute value
	 */
	String getId();
	/**
	 * Find lemma.
	 * @return	lemma
	 */
	String getLemma();
	/**
	 * Find tag.
	 * @return	tag
	 */
	String getTag();
	/**
	 * Find wordform.
	 * @return	wordform
	 */
	String getForm();
	/**
	 * Get all form_change values.
	 * @return unordered set of values
	 */
	Set<LvtbFormChange> getFormChange();

	/**
	 * Get list of all underlying w nodes in the order they occur in the text.
	 * @return list of w nodes
	 */
	List<PmlWNode> getWs();

	/**
	 * Reconstruct source string (before error corrections) from underlying w
	 * level tokens.
	 * @return	source string without leading or trailing whitespace
	 */
	String getSourceString();
}
