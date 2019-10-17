package lv.ailab.lvtb.universalizer.conllu;

public enum MiscKeys
{
	SPACE_AFTER ("SpaceAfter"),
	NEW_PAR ("NewPar"),
	LVTB_NODE_ID("LvtbNodeId"),
	CORRECTION_TYPE("CorrectionType"),
	CORRECT_FORM("CorrectForm"),
	CORRECT_SPACE_AFTER("CorrectSpaceAfter"),
	;

	final String strRep;

	MiscKeys(String strRep)
	{
		this.strRep = strRep;
	}
	public String toString()
	{
		return strRep;
	}
}
