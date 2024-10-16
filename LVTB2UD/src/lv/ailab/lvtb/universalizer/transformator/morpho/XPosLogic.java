package lv.ailab.lvtb.universalizer.transformator.morpho;

/**
 * Logic on forming UD XPOSTAG from information in tha LVTB.
 * Not much of a transforming here, just normalization of how unknown values
 * are represented.
 * This class allows to add some freeform postfix to LVTB tag to form XPOSTAG,
 * however currently this is not uses.
 *
 * Created on 2016-04-20.
 * @author Lauma
 */
public class XPosLogic
{
	/**
	 * Logic for obtaining XPOSTAG from tag given in LVTB.
	 * @param lvtbTag	tag given in LVTB
	 * @return XPOSTAG or _ if tag from LVTB is not meaningful
	 */
	public static String getXpostag (String lvtbTag)
	{
		return getXpostag(lvtbTag, null);
	}
	/**
	 * Logic for obtaining XPOSTAG from tag given in LVTB.
	 * @param lvtbTag	tag given in LVTB
	 * @param ending	postfix to be added to the tag
	 * @return XPOSTAG or _ if tag from LVTB is not meaningful
	 */
	public static String getXpostag (String lvtbTag, String ending)
	{
		if (lvtbTag == null || lvtbTag.length() < 1 || lvtbTag.matches("N/[Aa]"))
			return "_";
		if (ending == null || ending.length() < 1) return lvtbTag.trim();
		else return (lvtbTag + ending).trim();
	}
}
