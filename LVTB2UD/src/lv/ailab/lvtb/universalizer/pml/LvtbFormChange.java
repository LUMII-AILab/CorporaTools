package lv.ailab.lvtb.universalizer.pml;

public enum LvtbFormChange
{
	INSERT("insert"),
	//NUM_NORMALIZATION("num_normalization"),
	PUNCT("punct"),
	SPACING("spacing"),
	SPELL("spell"),
	UNION("union");

	final String strRep;

	LvtbFormChange(String strRep)
	{
		this.strRep = strRep;
	}
	public String toString()
	{
		return strRep;
	}
	public static LvtbFormChange fromString(String value)
	{
		if (value == null) return null;
		switch (value)
		{
			case "insert": return INSERT;
			case "punct": return PUNCT;
			case "spacing": return SPACING;
			case "spell": return SPELL;
			case "union": return UNION;
			default: return null;
		}
	}
}
