package lv.ailab.lvtb.universalizer.pml;

public interface PmlWNode
{
	/**
	 * Find ID attribute.
	 * @return	attribute value
	 */
	String getId();

	/**
	 * Find token.
	 * @return token value
	 */
	String getToken();

	/**
	 * Find flag value for if there is no space after this token.
	 * @return true, if there is no space after this token, false otherwise
	 */
	boolean noSpaceAfter();
}
