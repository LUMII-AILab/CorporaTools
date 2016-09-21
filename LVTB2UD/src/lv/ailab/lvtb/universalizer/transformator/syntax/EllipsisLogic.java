package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.pml.LvtbRoles;
import lv.ailab.lvtb.universalizer.pml.Utils;
import lv.ailab.lvtb.universalizer.transformator.XPathEngine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.lang.reflect.Array;
import java.util.ArrayList;

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
			{
				// Search for first direct object.
				ArrayList <Node> sorted = Utils.asOrderedList(selChildren);
				for (Node obj : sorted)
				{
					String tag = Utils.getTag(obj);
					if (tag.matches("[na]...a.*|[pm]....a.*|v..p...a.*"))
						return obj;
					String parentTag = Utils.getTag(aNode);
					if (tag.matches("[na]...n.*|[pm]....n.*|v..p...n.*") && parentTag.matches("v..d.*"))
						return obj;
				}
				// Return first (indirect) object.
				return Utils.getFirstByOrd(selChildren);
			}
		}

		NodeList children = Utils.getPMLNodeChildren(aNode);
		if (children == null) return null;

		// Rules for specific sequences.
		if (children.getLength() > 1)
		{
			// List of attributes.
			NodeList selChildren = (NodeList) XPathEngine.get().evaluate(
					"./children/node[role='" + LvtbRoles.ATTR + "']",
					aNode, XPathConstants.NODESET);
			if (selChildren.getLength() == children.getLength())
			{
				return Utils.getLastByOrd(selChildren);
			}
			// List of adverbial modifiers.
			else if (selChildren.getLength() < 1)
			{
				selChildren = (NodeList) XPathEngine.get().evaluate(
						"./children/node[role='" + LvtbRoles.ADV +
								"' or role='" + LvtbRoles.PLACECL +
								"' or role='" + LvtbRoles.TIMECL +
								"' or role='" + LvtbRoles.MANCL +
								"' or role='" + LvtbRoles.DEGCL +
								"' or role='" + LvtbRoles.CAUSCL +
								"' or role='" + LvtbRoles.PURPCL +
								"' or role='" + LvtbRoles.CONDCL + "']",
						aNode, XPathConstants.NODESET);
				if (selChildren.getLength() == children.getLength())
				{
					NodeList advChildren = (NodeList) XPathEngine.get().evaluate(
							"./children/node[role='" + LvtbRoles.ADV + "']",
							aNode, XPathConstants.NODESET);
					if (advChildren.getLength() > 0)
						return Utils.getFirstByOrd(advChildren);
				}
			}
		}


		// Rules for parents with only one child.
		if (children.getLength() == 1)
			return children.item(0);

		return null;
	}
}
