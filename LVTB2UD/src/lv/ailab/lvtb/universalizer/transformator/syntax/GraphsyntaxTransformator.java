package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.EnhencedDep;
import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.LvtbRoles;
import lv.ailab.lvtb.universalizer.pml.Utils;
import lv.ailab.lvtb.universalizer.transformator.Sentence;
import lv.ailab.lvtb.universalizer.util.Tuple;
import lv.ailab.lvtb.universalizer.util.XPathEngine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * This is the part where enhanced dependencies graph features are made. To use
 * this on a given Sentence, TreesyntaxTransformator must be used on that
 * sentence beforehand.
 */
public class GraphsyntaxTransformator
{
	/**
	 * In this sentence all the transformations are carried out.
	 */
	public Sentence s;
	/**
	 * Stream for warnings.
	 */
	protected PrintWriter warnOut;

	public GraphsyntaxTransformator(Sentence sent, PrintWriter warnOut)
	{
		s = sent;
		this.warnOut = warnOut;
	}

	/**
	 * Add enhanced dependencies graph features. Assumed that
	 * TreesyntaxTransformator is already used on the sendence and that ellipsis
	 * is already handled there.
	 * Currently supported features:
	 *  * conjuct propagation;
	 *  * controled/rised subjects;
	 *  * case information.
	 * TODO
	 *  * relative clauses.
	 * @throws XPathExpressionException unsuccessfull XPath evaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public void transformEnhancedSyntax() throws XPathExpressionException
	{
		s.populateCoordPartsUnder();
		propagateConjuncts();
		addControlledSubjects();
	}

	/**
	 * Add enhanced dependencies subject links between various parts of xPred
	 * and respective subjects. No links are added for xPred parts transformed
	 * to aux, auxpass or cop. No links are added for subjects transformed as
	 * something else than nsubj, nsubjpass, csubj, csubjpass.
	 * To use this, Sentence.populateCoordPartsUnder() must be called
	 * beforehand.
	 * @throws XPathExpressionException unsuccessfull XPath evaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	protected void addControlledSubjects() throws XPathExpressionException
	{
		// Find all nodes consisting of xPred with dependant subj.
		NodeList xPredList = (NodeList) XPathEngine.get().evaluate(
				".//node[children/xinfo/xtype/text()='xPred']",
				s.pmlTree, XPathConstants.NODESET);
		if (xPredList != null)
			for (int xPredI = 0; xPredI < xPredList.getLength(); xPredI++)
		{
			// Get base token.
			//Token parentTok = s.getEnhancedOrBaseToken(xPredList.item(xPredI));

			// Collect all subject nodes.
			ArrayList<Node> subjs = new ArrayList<>();
			NodeList tmp = (NodeList) XPathEngine.get().evaluate(
					"./children/node[role/text()='subj']", xPredList.item(xPredI), XPathConstants.NODESET);
			if (tmp != null) subjs.addAll(Utils.asList(tmp));
			boolean predIsCoordinated = false;
			Node ancestor = Utils.getPMLParent(xPredList.item(xPredI));
			while (ancestor.getNodeName().equals("coordinfo"))
			{
				ancestor = Utils.getPMLParent(ancestor); // PML node
				tmp = (NodeList) XPathEngine.get().evaluate(
						"./children/node[role/text()='subj']", ancestor , XPathConstants.NODESET);
				if (tmp != null) subjs.addAll(Utils.asList(tmp));
				ancestor = Utils.getPMLParent(ancestor); // PML node or phrase
				predIsCoordinated = true;
			}
			// If no subjects found, nothing to do.
			if (subjs.isEmpty()) continue;

			// Work on each xPred part
			NodeList xPredParts = Utils.getPMLNodeChildren(Utils.getPhraseNode(xPredList.item(xPredI)));
			if (xPredParts != null)
				for (int xPredPartI = 0; xPredPartI < xPredParts.getLength(); xPredPartI++)
			{
				// Do nothing with auxiliaries
				Token xPredPartTok = s.getEnhancedOrBaseToken(xPredParts.item(xPredPartI));
				if (xPredPartTok.depsBackbone.role == UDv2Relations.AUX
						|| xPredPartTok.depsBackbone.role == UDv2Relations.AUX_PASS
						|| xPredPartTok.depsBackbone.role == UDv2Relations.COP)
					continue;
				// Do nothing with nomens
				if (xPredPartTok.xpostag != null && xPredPartTok.xpostag.matches("[napxm].*|v..pd...[ap]p.*]"))
					continue;
				// TODO what to do with past participles?

				// For each other part a ling between each subject and this part
				// must be made.
				for (Node subj : subjs)
				{
					String subjLvtbRole = Utils.getRole(subj); // It should be "subj" always.
					// Find each coordinated subject part.
					HashSet<String> subjIds = s.getCoordPartsUnderOrNode(subj);
					// Find each coordinated x-part part.
					HashSet<String> xPartIds = s.getCoordPartsUnderOrNode(xPredParts.item(xPredPartI));
					// Make a link.
					for (String subjId : subjIds)
					{
						Node subjNode = s.findPmlNode(subjId);
						Token subjTok = s.getEnhancedOrBaseToken(subjNode);
						//Tuple<UDv2Relations, String> role = subjTok.depsBackbone.getRoleTuple();
						for (String xPartId : xPartIds)
						{
							Node xPartNode = s.findPmlNode(xPartId);
							// TODO tweak this, when nested xPreds will be made.
							Tuple<UDv2Relations, String> role = DepRelLogic.getSingleton().depToUDEnhanced(
									subjNode, xPredList.item(xPredI), subjLvtbRole, warnOut);
									//subjNode, xPartNode, subjLvtbRole, warnOut);
							// Only UD subjects will have aditional link.
							//if (role.first == UDv2Relations.NSUBJ ||
							//		role.first == UDv2Relations.NSUBJ_PASS ||
							//		role.first == UDv2Relations.CSUBJ ||
							//		role.first == UDv2Relations.CSUBJ_PASS)
								s.setEnhLink(xPartNode, subjNode, role, false, false);
						}
					}
				}
			}
		}
	}

	/**
	 * Add enhanced dependencies links related to second and further coordinated
	 * parts.
	 * To use this, Sentence.populateCoordPartsUnder() must be called
	 * beforehand.
	 * @throws XPathExpressionException unsuccessfull XPath evaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	protected void propagateConjuncts() throws XPathExpressionException
	{
		for (String coordId : s.coordPartsUnder.keySet())
		{
			Node coordANode = s.findPmlNode(coordId);
			for (String coordPartId : s.coordPartsUnder.get(coordId))
			{
				Node partNode = s.findPmlNode(coordPartId);
				processSingleConjunct(partNode, coordANode);
			}
		}
	}

	protected void processSingleConjunct(Node coordPartNode, Node wholeCoordANode)
	throws XPathExpressionException
	{
		// This is the "empty" PML node that represents a coordination as a
		// whole - it has ID, role and dependants for this coordination.
		Token wholeCoordNodeTok = s.getEnhancedOrBaseToken(wholeCoordANode);

		// This is coordinated part node.
		Token partNodeTok = s.getEnhancedOrBaseToken(coordPartNode);
		if (partNodeTok.equals(wholeCoordNodeTok)) return;

		// This is coordination's dependency head or phrase containing it.
		Node coordParentNode = Utils.getPMLParent(wholeCoordANode);
		Node coordGrandParentNode = Utils.getPMLParent(coordParentNode);

		if (Utils.isPhraseNode(coordParentNode))
		{
			// Renaming for convenience
			Node phrase = coordParentNode;
			Node phraseParent = coordGrandParentNode;

			// TODO saite ar vecāku gudrākā veidā?
			// Link between parent of the coordination and coordinated part.
			if (!wholeCoordNodeTok.depsBackbone.isRootDep())
				partNodeTok.deps.add(wholeCoordNodeTok.depsBackbone);

			// Links between phrase parts
			if (coordParentNode.getNodeName().equals("xinfo")
					|| coordParentNode.getNodeName().equals("pmcinfo"))
			{
				Token phraseRootToken = s.getEnhancedOrBaseToken(phraseParent);
				NodeList phraseParts = Utils.getPMLNodeChildren(phrase);
				if (phraseParts != null)
					for (int phrasePartI = 0; phrasePartI < phraseParts.getLength(); phrasePartI++)
					{
						if (Utils.getAnyLabel(phraseParts.item(phrasePartI)).equals(LvtbRoles.PUNCT)
								|| phraseParts.item(phrasePartI).isSameNode(coordPartNode))
							continue;

						Token otherPartToken = s.getEnhancedOrBaseToken(phraseParts.item(phrasePartI));
						if (otherPartToken.depsBackbone.headID.equals(phraseRootToken.getFirstColumn()))
							s.setEnhLink(coordPartNode, phraseParts.item(phrasePartI),
									otherPartToken.depsBackbone.getRoleTuple(), false, false);
						// Todo: use/make analogue to DepRelLogic.getSingleton().depToUD(node, node, ...) ?
					}
			}
		} else
		{
			// Link between parent of the coordination and coordinated part.
			if (!Utils.isRoot(coordParentNode) && !wholeCoordNodeTok.depsBackbone.isRootDep())
			{
				Tuple<UDv2Relations, String> role = DepRelLogic.getSingleton().depToUDEnhanced(
						coordPartNode, coordParentNode,
						Utils.getRole(wholeCoordANode), warnOut);
				//partNodeTok.deps.add(parentNodeTok.depsBackbone);
				s.setEnhLink(coordParentNode, coordPartNode, role,
						false, false);
			}
		}

		// Links between dependants of the coordination and coordinated parts.
		NodeList dependents = Utils.getPMLNodeChildren(wholeCoordANode);
		if (dependents != null)
			for (int dependentI = 0; dependentI < dependents.getLength(); dependentI++)
			{
				Tuple<UDv2Relations, String> role = DepRelLogic.getSingleton().depToUDEnhanced(
						dependents.item(dependentI), coordPartNode,
						Utils.getRole(dependents.item(dependentI)),
						warnOut);
				s.setEnhLink(coordPartNode, dependents.item(dependentI),
						role,false,false);
			}
	}
}
