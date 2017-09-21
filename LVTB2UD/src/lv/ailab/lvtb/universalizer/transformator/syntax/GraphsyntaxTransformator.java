package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.EnhencedDep;
import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.Utils;
import lv.ailab.lvtb.universalizer.transformator.Sentence;
import lv.ailab.lvtb.universalizer.transformator.SentenceTransformEngine;
import lv.ailab.lvtb.universalizer.util.XPathEngine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.PrintWriter;

public class GraphsyntaxTransformator
{
	/**
	 * In this sentence all the transformations are carried out.
	 */
	public Sentence s;
	protected PrintWriter warnOut;

	public GraphsyntaxTransformator(Sentence sent, PrintWriter warnOut)
	{
		s = sent;
		this.warnOut = warnOut;
	}

	public void transformEnhancedSyntax() throws XPathExpressionException
	{
		propagateConjuncts();
	}

	protected void propagateConjuncts() throws XPathExpressionException
	{
		NodeList crdPartList = (NodeList) XPathEngine.get().evaluate(
				".//node[role/text()=\"crdPart\"]", s.pmlTree, XPathConstants.NODESET);
		if (crdPartList != null) for (int i = 0; i < crdPartList.getLength(); i++)
		{
			// Let's find effective parent - not coordination.
			Node effParent = Utils.getEffectiveAncestor(crdPartList.item(i));

			// Link between parent of the coordination and coordinated part.
			Token effParentTok = s.getEnhancedOrBaseToken(effParent);
			Token childTok = s.getEnhancedOrBaseToken(crdPartList.item(i));
			if (!effParentTok.depsBackbone.equals(EnhencedDep.root()))
				childTok.deps.add(effParentTok.depsBackbone);

			// Links between dependants of the coordination and coordinated parts.
			NodeList dependants = Utils.getPMLNodeChildren(effParent);
			if (dependants != null) for (int j =0; j < dependants.getLength(); j++)
			{
				UDv2Relations role = DepRelLogic.getSingleton().depToUD(
						dependants.item(j), true, warnOut);
				s.setEnhLink(crdPartList.item(i), dependants.item(j), role,false,false);
			}

			// Links between phrase parts
			Node specialPPart = effParent; // PML A node.
			Node phrase = Utils.getPMLParent(specialPPart); // PML phrase or A node/root in the end of the loop.
			Node phraseParent = Utils.getPMLParent(phrase);
			//Node specialPPart = crdPartList.item(i); // PML A node.
			while (phrase != null && Utils.isPhraseNode(phrase))// && s.pmlaToConll.get(Utils.getId(specialPPart)).equals(s.pmlaToConll.get(Utils.getId(phraseParent))))
			{
				// TODO
				// Pārbaudīt visas frāzes, kam šī koordinācija ir sastāvdaļa.
				// Ja tai frāzes daļai, kam atbilst koordinācija, ir pakārtotas
				// citas frāzes daļas, tad pakārtot tās arī koordinētajam elementam.
				Token phraseRootToken = s.getEnhancedOrBaseToken(phraseParent);
				NodeList phraseParts = Utils.getPMLNodeChildren(phrase);
				if (phraseParts != null) for (int j = 0; j < phraseParts.getLength(); j++)
				{
					if (phraseParts.item(j).isSameNode(specialPPart)) continue;

					Token otherPartToken = s.getEnhancedOrBaseToken(phraseParts.item(j));
					if (otherPartToken.depsBackbone.headID.equals(phraseRootToken.getFirstColumn()))
					{
						s.setEnhLink(crdPartList.item(i), phraseParts.item(j), otherPartToken.depsBackbone.role,false,false);
					}

					// Ja daļa nav tiešais pēctecis, tad jāsalīzina, vai tās
					// tokens ir pakārtots tiešā pēcteča tokenam?
				}

				// Path to root goes like this: phrase->node->phrase->node...
				// Must stop, when ...->node->node happens.
				specialPPart = phraseParent;
				phrase = Utils.getEffectiveAncestor(phraseParent);
				phraseParent = Utils.getPMLParent(phrase);
			}
		}
	}

	protected void addControlledSubjects()
	{
		// For each xPred:
		// For each nonroot nonaux noncop part add subj.

		// Ņemt katru xPred-a daļu un tad piemeklēt viņa subjektu. Ja ir koordinācija, paieties uz leju.
	}

}
