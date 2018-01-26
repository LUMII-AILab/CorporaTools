package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.LvtbRoles;
import lv.ailab.lvtb.universalizer.pml.PmlANode;
import lv.ailab.lvtb.universalizer.pml.utils.PmlANodeListUtils;
import lv.ailab.lvtb.universalizer.utils.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Logic how to choose substitute for ellipted nodes.
 * Created on 2016-09-02.
 *
 * @author Lauma
 */
public class EllipsisLogic
{
	public static PmlANode newParent (PmlANode aNode, DepRelLogic drLogic, Logger logger)
	{
		// This method should not be used for transforming phrase nodes or nodes
		// with morphology.
		if (aNode.getPhraseNode() != null || aNode.getM() != null)
			return null;

		List<PmlANode> children = aNode.getChildren();
		if (children == null) return null;

		ArrayList<PmlANode> sortedChildren = PmlANodeListUtils.asOrderedList(children);
		String lvtbEffRole = aNode.getEffectiveLabel();
		String lvtbTag = aNode.getAnyTag();
		
		// Rules for specific parents.
		if (LvtbRoles.PRED.equals(lvtbEffRole) || lvtbTag.matches("v..[^pn].*")
				|| LvtbRoles.SPC.equals(lvtbEffRole) && lvtbTag.matches("v..(n|p[up]).*"))
		{
			// In case of reduced predicte, search if there is an aux or cop.
			if (LvtbRoles.PRED.equals(lvtbEffRole) || lvtbTag.matches("v..[^pn].*"))
				for (PmlANode n : sortedChildren)
			{
				UDv2Relations noRedUDrole = drLogic.depToUDLogic(
						n, n.getParent(), n.getRole()).first;
				if (noRedUDrole == null)
					throw new IllegalStateException(String.format(
							"Could not determine potential UD role during ellipsis processing for %s",
							n.getId()));
				if (noRedUDrole.equals(UDv2Relations.AUX) || noRedUDrole.equals(UDv2Relations.COP))
					return n;
			}

			// Taken from UDv2 guidelines.
			UDv2Relations[] priorities = new UDv2Relations[] {
					UDv2Relations.NSUBJ, UDv2Relations.NSUBJ_PASS,
					UDv2Relations.OBJ, UDv2Relations.IOBJ, UDv2Relations.OBL,
					UDv2Relations.ADVMOD, UDv2Relations.CSUBJ,
					UDv2Relations.CSUBJ_PASS, UDv2Relations.XCOMP,
					UDv2Relations.CCOMP, UDv2Relations.ADVCL};
			for (UDv2Relations role : priorities) for (PmlANode n : sortedChildren)
			{
				UDv2Relations noRedUDrole = drLogic.depToUDLogic(
						n, n.getParent(), n.getRole()).first;
				if (noRedUDrole == null)
					throw new IllegalStateException(String.format(
							"Could not determine potential UD role during ellipsis processing for %s",
							n.getId()));
				if (noRedUDrole.equals(role)) return n;
			}
		}

		// Rules for specific sequences.
		if (children.size() > 1)
		{
			// Taken from UDv2 guidelines.
			UDv2Relations[] priorities = new UDv2Relations[] {
					UDv2Relations.AMOD, UDv2Relations.NUMMOD, UDv2Relations.DET,
					UDv2Relations.NMOD, UDv2Relations.CASE};
			for (UDv2Relations role : priorities) for (PmlANode n : sortedChildren)
			{
				UDv2Relations noRedUDrole = drLogic.depToUDLogic(
						n, n.getParent(), n.getRole()).first;
				if (noRedUDrole == null)
					throw new IllegalStateException(String.format(
							"Could not determine potential UD role during ellipsis processing for %s",
							n.getId()));
				if (noRedUDrole.equals(role)) return n;
			}
		}

		// Rules for parents with only one child.
		if (children.size() == 1)
			return children.get(0);

		return null;
	}
}
