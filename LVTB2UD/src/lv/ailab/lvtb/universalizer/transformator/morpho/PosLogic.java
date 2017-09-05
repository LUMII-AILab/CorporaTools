package lv.ailab.lvtb.universalizer.transformator.morpho;

import lv.ailab.lvtb.universalizer.conllu.UDv2PosTag;
import lv.ailab.lvtb.universalizer.pml.LvtbXTypes;
import lv.ailab.lvtb.universalizer.pml.Utils;
import lv.ailab.lvtb.universalizer.transformator.XPathEngine;
import org.w3c.dom.Node;
import lv.ailab.lvtb.universalizer.pml.LvtbRoles;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathExpressionException;
import java.io.PrintWriter;

/**
 * Logic on obtaining Universal POS tags from Latvian Treebank tags.
 * Created on 2016-04-20.
 *
 * @author Lauma
 */
public class PosLogic
{
	/* TODO: izcelt no SentenceTransformEngine ārā arī sadalāmo tokenu POS loģiku.
	public static UDv2PosTag getUPostTagForPart(String lemma, String xpostag, Node aNode, boolean isLast)
	throws XPathExpressionException
	{
	}*/

	public static UDv2PosTag getUPosTag(
			String lemma, String xpostag, Node aNode, PrintWriter warnOut)
	throws XPathExpressionException
	{
		String lvtbRole = Utils.getRole(aNode);
		if (xpostag.matches("N/[Aa]")) return UDv2PosTag.X; // Not given.
		else if (xpostag.matches("nc.*")) return UDv2PosTag.NOUN; // Or sometimes SCONJ
		else if (xpostag.matches("np.*")) return UDv2PosTag.PROPN;
		else if (xpostag.matches("v..[^p].*")) return UDv2PosTag.VERB;
		else if (xpostag.matches("v..p[dpu].*")) return UDv2PosTag.VERB;
		else if (xpostag.matches("a.*"))
		{
			if (lemma.matches("(manējais|tavējais|mūsējais|jūsējais|viņējais|savējais|daudzi|vairāki)") ||
					lemma.matches("(manējā|tavējā|mūsējā|jūsējā|viņējā|savējā|daudzas|vairākas)"))
			{
				if (lvtbRole.equals("attr")) return UDv2PosTag.DET;
				else return UDv2PosTag.PRON;
			}
			else return UDv2PosTag.ADJ;
		}
		else if (xpostag.matches("p[px].*")) return UDv2PosTag.PRON;
		else if (xpostag.matches("pd.*"))
		{
			if (lvtbRole.equals(LvtbRoles.ATTR)) return UDv2PosTag.DET;
			else if (lvtbRole.equals(LvtbRoles.BASELEM) && lemma.matches("tād[sa]"))
			{
				Node parent = Utils.getPMLParent(aNode);
				if (!LvtbXTypes.SUBRANAL.equals(Utils.getRole(parent)))
					return UDv2PosTag.PRON;

				NodeList children = Utils.getAllPMLChildren(parent);
				Node first = Utils.getFirstByDescOrd(children);
				Node last = Utils.getLastByDescOrd(children);
				if (children != null && children.getLength() == 2  &&
						(aNode.isSameNode(first)) &&
						LvtbXTypes.XSIMILE.equals(XPathEngine.get().evaluate("./children/xinfo/xtype", last)))
					return UDv2PosTag.DET;
			}
			return UDv2PosTag.PRON;
		}
		else if (xpostag.matches("p[siqgr].*"))
		{
			if (lvtbRole.equals(LvtbRoles.ATTR)) return UDv2PosTag.DET;
			else return UDv2PosTag.PRON;
		}
		else if (xpostag.matches("r.*")) return UDv2PosTag.ADV; // Or sometimes SCONJ
		else if (xpostag.matches("m[cf].*")) return UDv2PosTag.NUM;
		else if (xpostag.matches("mo.*")) return UDv2PosTag.ADJ;
		else if (xpostag.matches("s.*")) return UDv2PosTag.ADP;
		else if (xpostag.matches("cc.*")) return UDv2PosTag.CCONJ;
		else if (xpostag.matches("cs.*")) return UDv2PosTag.SCONJ;
		else if (xpostag.matches("i.*")) return UDv2PosTag.INTJ;
		else if (xpostag.matches("q.*")) return UDv2PosTag.PART;
		else if (xpostag.matches("z.*")) return UDv2PosTag.PUNCT;
		else if (xpostag.matches("z.*")) return UDv2PosTag.PUNCT;
		else if (xpostag.matches("y.*"))
		{
			if (lemma.matches("\\p{Lu}+")) return UDv2PosTag.PROPN;
			else if (lemma.matches("(utt\\.|u\\.t\\.jpr\\.|u\\.c\\.|u\\.tml\\.|v\\.tml\\.)")) return UDv2PosTag.SYM;
			else if (lemma.matches("\\p{Ll}+-\\p{Ll}")) return UDv2PosTag.NOUN; // Or rarely PROPN
			else return UDv2PosTag.SYM; // Or sometimes PROPN/NOUN
		}
		else if (xpostag.matches("xf.*")) return UDv2PosTag.X; // Or sometimes PROPN/NOUN
		else if (xpostag.matches("xn.*")) return UDv2PosTag.NUM;
		else if (xpostag.matches("xo.*")) return UDv2PosTag.ADJ;
		else if (xpostag.matches("xu.*")) return UDv2PosTag.SYM;
		else if (xpostag.matches("xx.*")) return UDv2PosTag.SYM; // Or sometimes PROPN/NOUN
		else warnOut.printf("Could not obtain UPOSTAG for \"%s\" with XPOSTAG \"%s\".\n",
					lemma, xpostag);

		return UDv2PosTag.X;
	}

}
