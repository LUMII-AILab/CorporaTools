package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.pml.LvtbRoles;
import lv.ailab.lvtb.universalizer.pml.LvtbXTypes;
import lv.ailab.lvtb.universalizer.pml.PmlANode;
import lv.ailab.lvtb.universalizer.transformator.Sentence;

import java.util.List;

public class EllipsisPreprocessor
{
	/**
	 * In this sentence all the transformations are carried out.
	 */
	public Sentence s;

	public EllipsisPreprocessor(Sentence sent)
	{
		s = sent;
	}

	public void splitTokenEllipsis()
	{
		List<PmlANode> ellipsisNodes = s.pmlTree.getMorphoEllipsisDescendants(false);
		if (ellipsisNodes == null || ellipsisNodes.isEmpty()) return;
		for (PmlANode ellipsisNode : ellipsisNodes)
		{
			ellipsisNode.splitMorphoEllipsis(Sentence.ID_POSTFIX);
		}
	}

	/**
	 * Remove the childless ellipsis nodes assuming they can be ignored in
	 * latter processing. Replace empty xPreds with just ellipsis nodes.
	 * Currently used for UD transformation.
	 * @return	 true if all ellipsis was removed
	 */
	public boolean removeAllChildlessEllipsis()
	{
		// Childless, empty reductions are removed.
		List<PmlANode> ellipsisChildren = s.pmlTree.getPureEllipsisDescendants(true);
		while (ellipsisChildren != null && !ellipsisChildren.isEmpty())
		{
			for (PmlANode ellipsisChild : ellipsisChildren)
			{
				PmlANode parent = ellipsisChild.getParent();
				ellipsisChild.delete();
				if (LvtbXTypes.XPRED.equals(parent.getPhraseType()))
				{
					List<PmlANode> children = parent.getChildren();
					if (children != null && !children.isEmpty()) continue;

					PmlANode grandparent = parent.getParent();
					String xTag = parent.getPhraseTag();
					if (xTag.contains("[")) xTag = xTag.substring(0, xTag.indexOf("["));
					grandparent.setReductionTag(xTag);
					parent.delete();
				}
			}
			ellipsisChildren = s.pmlTree.getPureEllipsisDescendants(true);
		}

		// Check if there is other reductions.
		ellipsisChildren = s.pmlTree.getPureEllipsisDescendants(false);
		return ellipsisChildren == null || ellipsisChildren.size() <= 0;
	}

	/**
	 * Remove the other childless ellipsis nodes, but leave ones with role
	 * 'pred'.
	 * Replace empty xPreds with just ellipsis nodes.
	 * TODO future project, must be incorporated in main workflow instead of removeAllChildlessEllipsis().
	 * @return	 true if all ellipsis was removed
	 */
	public boolean removeNonpredChildlessEllipsis()
	{
		// Childless, empty reductions are removed, unless their role is "pred".
		boolean searchForMore = true;
		while (searchForMore)
		{
			List<PmlANode> ellipsisChildren = s.pmlTree.getPureEllipsisDescendants(true);
			searchForMore = false;
			for (PmlANode ellipsisChild : ellipsisChildren)
			{
				// Leave pred nodes so that PMC can be converted.
				if (LvtbRoles.PRED.equals(ellipsisChild.getRole()))
					continue;

				// Throw out other childless nodes.
				PmlANode parent = ellipsisChild.getParent();
				ellipsisChild.delete();
				searchForMore = true;

				// If all xPred parts are ellipted, substitute it with single ellipted pred node.
				if (LvtbXTypes.XPRED.equals(parent.getPhraseType()))
				{
					List<PmlANode> children = parent.getChildren();
					if (children != null && !children.isEmpty()) continue;

					PmlANode grandparent = parent.getParent();
					String xTag = parent.getPhraseTag();
					if (xTag.contains("[")) xTag = xTag.substring(0, xTag.indexOf("["));
					grandparent.setReductionTag(xTag);
					parent.delete();
				}
			}
		}

		// Check if there is other reductions.
		List<PmlANode> ellipsisChildren = s.pmlTree.getPureEllipsisDescendants(false);
		return ellipsisChildren == null || ellipsisChildren.size() <= 0;
	}

}
