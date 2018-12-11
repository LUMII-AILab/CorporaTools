package lv.ailab.lvtb.universalizer.transformator.morpho;

import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.UDv2PosTag;
import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.transformator.StandardLogger;
import lv.ailab.lvtb.universalizer.utils.Logger;

/**
 * Logic on obtaining Universal POS tags from Latvian Treebank tags.
 * Created on 2016-04-20.
 * TODO: atšķirīga uzvedība, ja logger ir null - izdrukā konsolē. Kāpēc?
 * @author Lauma
 */
public class UPosLogic
{
	/* TODO: izcelt no SentenceTransformEngine ārā arī sadalāmo tokenu POS loģiku.*/

	/**
	 * Use this to obtain UPOSTAG, if no syntactic information is available.
	 */
	public static UDv2PosTag getUPosTag(
			String form, String lemma, String xpostag)
	{
		if (lemma == null) lemma = ""; // To avoid null pointer exceptions.
		if (xpostag.matches("N/[Aa]")) return UDv2PosTag.X; // Not given.
		else if (xpostag.matches("nc.*")) return UDv2PosTag.NOUN; // Or sometimes SCONJ
		else if (xpostag.matches("np.*")) return UDv2PosTag.PROPN;
		else if (xpostag.matches("v[c].*") && lemma.matches("(ne)?būt")) return UDv2PosTag.AUX;
		else if (xpostag.matches("v[t].*") && lemma.matches("(ne)?(kļūt|tikt|tapt)")) return UDv2PosTag.AUX;
		else if (xpostag.matches("v.*")) return UDv2PosTag.VERB;
			//else if (xpostag.matches("v..[^p].*")) return UDv2PosTag.VERB;
			//else if (xpostag.matches("v..p[dpu].*")) return UDv2PosTag.VERB;
		else if (xpostag.matches("a.*"))
		{
			if (lemma.matches("(manējais|tavējais|mūsējais|jūsējais|viņējais|savējais|daudzi|vairāki)") ||
					lemma.matches("(manējā|tavējā|mūsējā|jūsējā|viņējā|savējā|daudzas|vairākas)"))
				return UDv2PosTag.PRON;
			else return UDv2PosTag.ADJ;
		}
		else if (xpostag.matches("p[pxd].*")) return UDv2PosTag.PRON;
		else if (xpostag.matches("p[siqgr].*")) return UDv2PosTag.PRON;
		else if (xpostag.matches("r.*")) return UDv2PosTag.ADV; // Or sometimes SCONJ
		else if (xpostag.matches("m[cf].*")) return UDv2PosTag.NUM;
		else if (xpostag.matches("mo.*")) return UDv2PosTag.ADJ;
		else if (xpostag.matches("s.*")) return UDv2PosTag.ADP;
		else if (xpostag.matches("cc.*")) return UDv2PosTag.CCONJ;
		else if (xpostag.matches("cs.*")) return UDv2PosTag.SCONJ;
		else if (xpostag.matches("i.*")) return UDv2PosTag.INTJ;
		else if (xpostag.matches("q.*")) return UDv2PosTag.PART;
		else if (xpostag.matches("z.*")) return UDv2PosTag.PUNCT;
		else if (xpostag.matches("yn.*")) return UDv2PosTag.NOUN;
		else if (xpostag.matches("yp.*")) return UDv2PosTag.PROPN;
		else if (xpostag.matches("ya.*")) return UDv2PosTag.ADJ;
		else if (xpostag.matches("yv.*")) return UDv2PosTag.VERB;
		else if (xpostag.matches("yr.*")) return UDv2PosTag.ADV;
		else if (xpostag.matches("yd.*")) return UDv2PosTag.SYM;
		else if (xpostag.matches("xf.*")) return UDv2PosTag.X; // Or sometimes PROPN/NOUN
		else if (xpostag.matches("xn.*")) return UDv2PosTag.NUM;
		else if (xpostag.matches("xo.*")) return UDv2PosTag.ADJ;
		else if (xpostag.matches("xu.*")) return UDv2PosTag.SYM;
		else if (xpostag.matches("xx.*")) return UDv2PosTag.SYM; // Or sometimes PROPN/NOUN
			//else warnOut.printf("Could not obtain UPOSTAG for \"%s\" with XPOSTAG \"%s\".\n", lemma, xpostag);
		else
		{
			String errorMsg = String.format(
					"Could not obtain UPOSTAG for \"%s\" with XPOSTAG \"%s\".", lemma, xpostag);
			if (StandardLogger.l != null)
				StandardLogger.l.doInsentenceWarning(errorMsg);
			else System.out.println(errorMsg);
		}
		return UDv2PosTag.X;
	}

	/**
	 * Use this to obtain UPOSTAG, if syntactic information (upostag for
	 * parameter token) is available.
	 */	public static UDv2PosTag getPostsyntUPosTag (Token token)
	{
		String xpostag = token.xpostag == null ? "" : token.xpostag; // To avoid null pointer exeption. But should we?
		String lemma = token.lemma == null ? "" : token.lemma; // To avoid null pointer exeption. But should we?
		UDv2Relations deprel = token.deprel;
		if (xpostag.matches("a.*"))
		{
			if (lemma.matches("(manējais|tavējais|mūsējais|jūsējais|viņējais|savējais|daudzi|vairāki)") ||
					lemma.matches("(manējā|tavējā|mūsējā|jūsējā|viņējā|savējā|daudzas|vairākas)"))
			{
				if (deprel == UDv2Relations.DET) return UDv2PosTag.DET;
				else return UDv2PosTag.PRON;
			}
			else return UDv2PosTag.ADJ;
		}
		else if (xpostag.matches("p[dsiqgr].*"))
		{
			if (deprel == UDv2Relations.DET) return UDv2PosTag.DET;
			else return UDv2PosTag.PRON;
		}
		if (token.xpostag == null)
			System.out.println(token.toConllU());
		if (token.upostag == null)
			return getUPosTag(token.form, token.lemma, token.xpostag);
		return token.upostag;
	}
}
