package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.pml.LvtbRoles;
import lv.ailab.lvtb.universalizer.pml.Utils;
import lv.ailab.lvtb.universalizer.transformator.XPathEngine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

/**
 * Logic how to choose substitute for ellipted nodes.
 * Created on 2016-09-02.
 *
 * @author Lauma
 */
public class EllipsisLogic
{
	public static Node newParent (Node aNode) throws XPathExpressionException
	{
		// This method should not be used for transforming phrase nodes or nodes
		// with morphology.
		if (Utils.getPhraseNode(aNode) != null || Utils.getMNode(aNode) != null)
			return null;

		String lvtbRole = Utils.getRole(aNode);

		// Rules for specific parents.
		if (LvtbRoles.PRED.equals(lvtbRole))
		{
			NodeList selChildren = (NodeList) XPathEngine.get().evaluate(
					"./children/node[role='" + LvtbRoles.SUBJ + "']",
					aNode, XPathConstants.NODESET);
			if (selChildren != null && selChildren.getLength() > 0)
				return Utils.getFirstByOrd(selChildren);

			selChildren = (NodeList) XPathEngine.get().evaluate(
					"./children/node[role='" + LvtbRoles.OBJ + "']",
					aNode, XPathConstants.NODESET);
			if (selChildren != null && selChildren.getLength() > 0)
				return Utils.getFirstByOrd(selChildren);
		}

		// Rules for parents with only one child.
		NodeList children = Utils.getPMLChildren(aNode);
		if (children != null && children.getLength() == 1)
			return children.item(0);

		return null;
	}
}
