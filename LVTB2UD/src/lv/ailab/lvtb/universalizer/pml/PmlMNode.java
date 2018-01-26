package lv.ailab.lvtb.universalizer.pml;

/**
 * Simplified PML-M level interface. As of now, this interface do not require
 * node to know which sentence it belongs or who are its siblings
 */
public interface PmlMNode
{
	/**
	 * Find ID attribute.
	 * @return	attribute value
	 */
	public String getId();
	/**
	 * Find lemma.
	 * @return	lemma
	 */
	public String getLemma();
	/**
	 * Find tag.
	 * @return	tag
	 */
	public String getTag();
	/**
	 * Find wordform.
	 * @return	wordform
	 */
	public String getForm();
	/**
	 * Determine, if final token in this morphological unit has no_space_after
	 * set.
	 * @return	true, if there is no space after this unit
	 */
	public Boolean getNoSpaceAfter();

}
