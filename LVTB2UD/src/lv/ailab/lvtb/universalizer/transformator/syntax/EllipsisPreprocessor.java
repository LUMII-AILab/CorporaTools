package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.pml.LvtbRoles;
import lv.ailab.lvtb.universalizer.pml.LvtbXTypes;
import lv.ailab.lvtb.universalizer.pml.PmlANode;
import lv.ailab.lvtb.universalizer.transformator.Sentence;
import lv.ailab.lvtb.universalizer.utils.Logger;

import java.util.List;

public class EllipsisPreprocessor
{
	/**
	 * In this sentence all the transformations are carried out.
	 */
	public Sentence s;
	/**
	 * Stream for warnings and other logs.
	 */
	protected Logger logger;

	public EllipsisPreprocessor(Sentence sent, Logger logger)
	{
		s = sent;
		this.logger = logger;
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
		List<PmlANode> ellipsisChildren = s.pmlTree.getEllipsisDescendants(true);
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
			ellipsisChildren = s.pmlTree.getEllipsisDescendants(true);
		}

		// Check if there is other reductions.
		ellipsisChildren = s.pmlTree.getEllipsisDescendants(false);
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
			List<PmlANode> ellipsisChildren = s.pmlTree.getEllipsisDescendants(true);
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
		List<PmlANode> ellipsisChildren = s.pmlTree.getEllipsisDescendants(false);
		return ellipsisChildren == null || ellipsisChildren.size() <= 0;
	}

}
