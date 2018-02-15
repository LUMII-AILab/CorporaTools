package lv.ailab.lvtb.universalizer.conllu;

public enum MiscKeys
{
	SPACE_AFTER ("SpaceAfter"),
	NEW_PAR ("NewPar"),
	LVTB_NODE_ID("LvtbNodeId"),
	CORRECTION_TYPE("CorrectionType"),
	CORRECTED_FORM("CorrectedForm"),
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
