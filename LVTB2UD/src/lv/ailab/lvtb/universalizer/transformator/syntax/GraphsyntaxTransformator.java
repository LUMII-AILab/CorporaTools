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
	 *  * controled/rised subjects.
	 * TODO
	 *  * case information;
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
			Token parentTok = s.getEnhancedOrBaseToken(xPredList.item(xPredI));

			// Collect all subject nodes.
			ArrayList<Node> subjs = new ArrayList<>();
			NodeList tmp = (NodeList) XPathEngine.get().evaluate(
					"./children/node[role/text()='subj']", xPredList.item(xPredI), XPathConstants.NODESET);
			if (tmp != null) subjs.addAll(Utils.asList(tmp));
			Node ancestor = Utils.getPMLParent(xPredList.item(xPredI));
			while (ancestor.getNodeName().equals("coordinfo"))
			{
				ancestor = Utils.getPMLParent(ancestor); // PML node
				tmp = (NodeList) XPathEngine.get().evaluate(
						"./children/node[role/text()='subj']", ancestor , XPathConstants.NODESET);
				if (tmp != null) subjs.addAll(Utils.asList(tmp));
				ancestor = Utils.getPMLParent(ancestor); // PML node or phrase
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
						for (String xPartId : xPartIds)
						{
							Node xPartNode = s.findPmlNode(xPartId);
							Tuple<UDv2Relations, String> role = DepRelLogic.getSingleton().depToUDEnhanced(
									subjNode, xPartNode, subjLvtbRole, warnOut);
							// Only UD subjects will have aditional link.
							if (role.first == UDv2Relations.NSUBJ ||
									role.first == UDv2Relations.NSUBJ_PASS ||
									role.first == UDv2Relations.CSUBJ ||
									role.first == UDv2Relations.CSUBJ_PASS)
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
			// This is the "empty" PML node that represents a coordination as a
			// whole - it has ID, role and dependants for this coordination.
			Node parentNode = s.findPmlNode(coordId);
			Token parentNodeTok = s.getEnhancedOrBaseToken(parentNode);

			Node grandParentNode = Utils.getPMLParent(parentNode);
			Node greatGrandParentNode = Utils.getPMLParent(grandParentNode);

			// Here we want coordination's dependency head.
			Node coordDepParent = grandParentNode;
			if (Utils.isPhraseNode(coordDepParent)) coordDepParent = greatGrandParentNode;

			// Those are the conjunts of the above-found coordination.
			for (String coordPartId : s.coordPartsUnder.get(coordId))
			{
				Node partNode = s.findPmlNode(coordPartId);
				Token partNodeTok = s.getEnhancedOrBaseToken(partNode);
				if (!partNodeTok.equals(parentNodeTok))
				{
					// Link between parent of the coordination and coordinated part.
					//if (!parentNodeTok.depsBackbone.isRootDep())
					if (!Utils.isRoot(coordDepParent) && !parentNodeTok.depsBackbone.isRootDep())
					{
						Tuple<UDv2Relations, String> role = DepRelLogic.getSingleton().depToUDEnhanced(
								partNode, coordDepParent,
								Utils.getRole(parentNode), warnOut);
						//partNodeTok.deps.add(parentNodeTok.depsBackbone);
						s.setEnhLink(coordDepParent, partNode, role,
								false, false);
					}

					// Links between dependants of the coordination and coordinated parts.
					NodeList dependents = Utils.getPMLNodeChildren(parentNode);
					if (dependents != null)
						for (int dependentI = 0; dependentI < dependents.getLength(); dependentI++)
					{
						//UDv2Relations role = DepRelLogic.getSingleton().depToUD(
						//		dependents.item(dependentI), true, warnOut);
						Tuple<UDv2Relations, String> role = DepRelLogic.getSingleton().depToUDEnhanced(
								dependents.item(dependentI), partNode,
								Utils.getRole(dependents.item(dependentI)),
								warnOut);
						s.setEnhLink(partNode, dependents.item(dependentI),
								role,false,false);
					}

					// Links between phrase parts
					if (grandParentNode.getNodeName().equals("xinfo")
							|| grandParentNode.getNodeName().equals("pmcinfo"))
					{
						// Renaming for convenience
						Node phrase = grandParentNode;
						Node phraseParent = greatGrandParentNode;
						Token phraseRootToken = s.getEnhancedOrBaseToken(phraseParent);
						NodeList phraseParts = Utils.getPMLNodeChildren(phrase);
						if (phraseParts != null)
							for (int phrasePartI = 0; phrasePartI < phraseParts.getLength(); phrasePartI++)
						{
							if (Utils.getAnyLabel(phraseParts.item(phrasePartI)).equals(LvtbRoles.PUNCT)
									|| phraseParts.item(phrasePartI).isSameNode(partNode))
								continue;

							Token otherPartToken = s.getEnhancedOrBaseToken(phraseParts.item(phrasePartI));
							if (otherPartToken.depsBackbone.headID.equals(phraseRootToken.getFirstColumn()))
								s.setEnhLink(partNode, phraseParts.item(phrasePartI),
										otherPartToken.depsBackbone.getRoleTuple(), false, false);
							// Todo: use/make analogue to DepRelLogic.getSingleton().depToUD(node, node, ...) ?
						}
					}
				}
			}
		}
	}
}
