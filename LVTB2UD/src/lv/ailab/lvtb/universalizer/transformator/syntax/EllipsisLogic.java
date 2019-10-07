package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.LvtbPmcTypes;
import lv.ailab.lvtb.universalizer.pml.LvtbRoles;
import lv.ailab.lvtb.universalizer.pml.PmlANode;
import lv.ailab.lvtb.universalizer.pml.utils.PmlANodeListUtils;
import lv.ailab.lvtb.universalizer.utils.Tuple;

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
	/**
	 * Logic how to choose substitute for ellipted nodes.
	 * @param aNode	node for which child to elevate must be chosen
	 * @return	tuple of node to elevate and boolean to determin, if a complex
	 * 			ellipsis where children ar eligible for orphan relation (true)
	 * 			or simple ellipsis where orphan relation can't be used (false)
	 */
	public static Tuple<PmlANode,Boolean> newParent (PmlANode aNode)
	{
		// This method should not be used for transforming phrase nodes or nodes
		// with morphology.
		if (aNode.getPhraseNode() != null || aNode.getM() != null)
			return null;

		List<PmlANode> children = aNode.getChildren();
		if (children == null) return null;

		String lvtbEffRole = aNode.getEffectiveLabel();
		String lvtbTag = aNode.getAnyTag();
		String lvtbEffPrevRole = null;
		PmlANode tmpAnc = aNode.getEffectiveAncestor();
		if (tmpAnc != null) tmpAnc = tmpAnc.getParent();
		if (tmpAnc != null)lvtbEffPrevRole = tmpAnc.getEffectiveLabel();

		// Rules for specific parents.
		if (LvtbRoles.PRED.equals(lvtbEffRole) || lvtbTag.matches("v..[^pn].*")
				|| lvtbTag.matches("v..(n|p[up]).*") && (LvtbRoles.SPC.equals(lvtbEffRole)
						|| LvtbRoles.BASELEM.equals(lvtbEffRole) && LvtbPmcTypes.SPCPMC.equals(lvtbEffPrevRole)))
		{
			Tuple<PmlANode,Boolean> res = newParentForVerbal(aNode);
			if (res != null) return res;
		}

		// Rules for specific sequences.
		if (children.size() > 1)
		{
			Tuple<PmlANode,Boolean> res = newParentForNominals(aNode);
			if (res != null) return res;
		}

		// Rules for parents with only one child.
		if (children.size() == 1)
			return Tuple.of(children.get(0), false);

		return null;
	}

	protected static Tuple<PmlANode,Boolean> newParentForVerbal(PmlANode aNode)
	{
		List<PmlANode> children = aNode.getChildren();
		if (children == null) return null;

		ArrayList<PmlANode> sortedChildren = PmlANodeListUtils.asOrderedList(children);
		String lvtbEffRole = aNode.getEffectiveLabel();
		String lvtbTag = aNode.getAnyTag();

		// In case of reduced predicte, search if there is an aux or cop.
		if (LvtbRoles.PRED.equals(lvtbEffRole) || lvtbTag.matches("v..[^pn].*"))
			for (PmlANode n : sortedChildren)
			{
				UDv2Relations noRedUDrole = DepRelLogic.depToUDLogic(
						n, n.getParent(), n.getRole()).first;
				if (noRedUDrole == null)
					throw new IllegalStateException(String.format(
							"Could not determine potential UD role during ellipsis processing for %s",
							n.getId()));
				if (noRedUDrole.equals(UDv2Relations.AUX) || noRedUDrole.equals(UDv2Relations.COP))
					return Tuple.of(n, false);
			}

		// Taken from UDv2 guidelines.
		UDv2Relations[] priorities = new UDv2Relations[] {
				UDv2Relations.NSUBJ, UDv2Relations.NSUBJ_PASS,
				UDv2Relations.OBJ, UDv2Relations.IOBJ, UDv2Relations.OBL,
				UDv2Relations.ADVMOD, UDv2Relations.CSUBJ,
				UDv2Relations.CSUBJ_PASS, UDv2Relations.XCOMP,
				UDv2Relations.CCOMP, UDv2Relations.ADVCL,
				UDv2Relations.DISLOCATED, UDv2Relations.VOCATIVE};
		for (UDv2Relations role : priorities) for (PmlANode n : sortedChildren)
		{
			UDv2Relations noRedUDrole = DepRelLogic.depToUDLogic(
					n, n.getParent(), n.getRole()).first;
			if (noRedUDrole == null)
				throw new IllegalStateException(String.format(
						"Could not determine potential UD role during ellipsis processing for %s",
						n.getId()));
			if (noRedUDrole.equals(role)) return Tuple.of(n, true);
		}
		return null;
	}

	protected static Tuple<PmlANode,Boolean> newParentForNominals (PmlANode aNode)
	{
		List<PmlANode> children = aNode.getChildren();
		if (children == null) return null;

		ArrayList<PmlANode> sortedChildren = PmlANodeListUtils.asOrderedList(children);

		// Taken from UDv2 guidelines (except ACL).
		UDv2Relations[] priorities = new UDv2Relations[] {
				UDv2Relations.AMOD, UDv2Relations.NUMMOD, UDv2Relations.DET,
				UDv2Relations.NMOD, UDv2Relations.CASE, UDv2Relations.ACL};
		for (UDv2Relations role : priorities) for (PmlANode n : sortedChildren)
		{
			UDv2Relations noRedUDrole = DepRelLogic.depToUDLogic(
					n, n.getParent(), n.getRole()).first;
			if (noRedUDrole == null)
				throw new IllegalStateException(String.format(
						"Could not determine potential UD role during ellipsis processing for %s",
						n.getId()));
			if (noRedUDrole.equals(role)) return Tuple.of(n, false);
		}
		return null;
	}
}
