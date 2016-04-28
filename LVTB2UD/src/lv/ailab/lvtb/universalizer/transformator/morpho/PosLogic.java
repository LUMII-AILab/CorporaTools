package lv.ailab.lvtb.universalizer.transformator.morpho;

import lv.ailab.lvtb.universalizer.conllu.UPosTag;

/**
 * Logic on obtaining Universal POS tags from Latvian Treebank tags.
 * Created on 2016-04-20.
 *
 * @author Lauma
 */
public class PosLogic
{
	public static UPosTag getUPosTag(String lemma, String xpostag, String lvtbRole)
	{
		if (xpostag.matches("N/[Aa]")) return UPosTag.X; // Not given.
		else if (xpostag.matches("nc.*")) return UPosTag.NOUN; // Or sometimes SCONJ
		else if (xpostag.matches("np.*")) return UPosTag.PROPN;
		else if (xpostag.matches("v..[^p].*")) return UPosTag.VERB;
		else if (xpostag.matches("v..p[dpu].*")) return UPosTag.VERB;
		else if (xpostag.matches("a.*"))
		{
			if (lemma.matches("(manējais|tavējais|mūsējais|jūsējais|viņējais|savējais|daudzi|vairāki)") ||
					lemma.matches("(manējā|tavējā|mūsējā|jūsējā|viņējā|savējā|daudzas|vairākas)"))
			{
				if (lvtbRole.equals("attr")) return UPosTag.DET;
				else return UPosTag.PRON;
			}
			else return UPosTag.ADJ;
		}
		else if (xpostag.matches("p[px].*")) return UPosTag.PRON;
		else if (xpostag.matches("p[sdiqg].*"))
		{
			if (lvtbRole.equals("attr")) return UPosTag.DET;
			else return UPosTag.PRON;
		}
		else if (xpostag.matches("pr.*")) return UPosTag.SCONJ;
		else if (xpostag.matches("r.*")) return UPosTag.ADV; // Or sometimes SCONJ
		else if (xpostag.matches("m[cf].*")) return UPosTag.NUM;
		else if (xpostag.matches("mo.*")) return UPosTag.ADJ;
		else if (xpostag.matches("s.*")) return UPosTag.ADP;
		else if (xpostag.matches("cc.*")) return UPosTag.CONJ;
		else if (xpostag.matches("cs.*")) return UPosTag.SCONJ;
		else if (xpostag.matches("i.*")) return UPosTag.INTJ;
		else if (xpostag.matches("q.*")) return UPosTag.PART;
		else if (xpostag.matches("z.*")) return UPosTag.PUNCT;
		else if (xpostag.matches("z.*")) return UPosTag.PUNCT;
		else if (xpostag.matches("y.*"))
		{
			if (lemma.matches("\\p{Lu}+")) return UPosTag.PROPN;
			else if (lemma.matches("(utt\\.|u\\.t\\.jpr\\.|u\\.c\\.|u\\.tml\\.|v\\.tml\\.)")) return UPosTag.SYM;
			else if (lemma.matches("\\p{Ll}+-\\p{Ll}")) return UPosTag.NOUN; // Or rarely PROPN
			else return UPosTag.SYM; // Or sometimes PROPN/NOUN
		}
		else if (xpostag.matches("xf.*")) return UPosTag.X; // Or sometimes PROPN/NOUN
		else if (xpostag.matches("xn.*")) return UPosTag.NUM;
		else if (xpostag.matches("xo.*")) return UPosTag.ADJ;
		else if (xpostag.matches("xu.*")) return UPosTag.SYM;
		else if (xpostag.matches("xx.*")) return UPosTag.SYM; // Or sometimes PROPN/NOUN
		else System.err.printf("Could not obtain UPOSTAG for \"%s\" with XPOSTAG \"%s\".\n",
					lemma, xpostag);

		return UPosTag.X;
	}

}
