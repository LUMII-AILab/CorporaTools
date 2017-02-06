package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.LvtbRoles;
import lv.ailab.lvtb.universalizer.pml.Utils;
import lv.ailab.lvtb.universalizer.transformator.Sentence;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;

/**
 * Logic how to choose substitute for ellipted nodes.
 * Created on 2016-09-02.
 *
 * @author Lauma
 */
public class EllipsisLogic
{
	public static Node newParent (Node aNode, Sentence s) throws XPathExpressionException
	{
		// This method should not be used for transforming phrase nodes or nodes
		// with morphology.
		if (Utils.getPhraseNode(aNode) != null || Utils.getMNode(aNode) != null)
			return null;

		NodeList children = Utils.getPMLNodeChildren(aNode);
		if (children == null) return null;

		ArrayList<Node> sortedChildren = Utils.asOrderedList(children);
		String lvtbRole = Utils.getRole(aNode);

		// Rules for specific parents.
		if (LvtbRoles.PRED.equals(lvtbRole))
		{
			// Search if there is an aux or cop.
			for (Node n : sortedChildren)
			{
				UDv2Relations noRedUDrole = DepRelLogic.depToUDNoRed(n);
				if (noRedUDrole == null)
					throw new IllegalStateException(
							"Unfinished token is accessed during ellipsis processing for " + Utils
									.getId(n));
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
			for (UDv2Relations role : priorities) for (Node n : sortedChildren)
			{
				UDv2Relations noRedUDrole = DepRelLogic.depToUDNoRed(n);
				if (noRedUDrole == null)
					throw new IllegalStateException(
							"Unfinished token is accessed during ellipsis processing for " + Utils.getId(n));
				if (noRedUDrole.equals(role)) return n;
			}

/*			NodeList selChildren = (NodeList) XPathEngine.get().evaluate(
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
			}//*/
		}



		// Rules for specific sequences.
		if (children.getLength() > 1)
		{
			// Taken from UDv2 guidelines.
			UDv2Relations[] priorities = new UDv2Relations[] {
					UDv2Relations.AMOD, UDv2Relations.NUMMOD, UDv2Relations.DET,
					UDv2Relations.NMOD, UDv2Relations.CASE};
			for (UDv2Relations role : priorities) for (Node n : sortedChildren)
			{
				UDv2Relations noRedUDrole = DepRelLogic.depToUDNoRed(n);
				if (noRedUDrole == null)
					throw new IllegalStateException(
							"Unfinished token is accessed during ellipsis processing for " + Utils.getId(n));
				if (noRedUDrole.equals(role)) return n;
			}
/*			// List of attributes.
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
//*/	}

		// Rules for parents with only one child.
		if (children.getLength() == 1)
			return children.item(0);

		return null;
	}
}
