package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.EnhencedDep;
import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.Utils;
import lv.ailab.lvtb.universalizer.transformator.Sentence;
import lv.ailab.lvtb.universalizer.util.XPathEngine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.PrintWriter;

public class EnhancedSyntaxTransformator {
	/**
	 * In this sentence all the transformations are carried out.
	 */
	public Sentence s;
	protected PrintWriter warnOut;

	public EnhancedSyntaxTransformator(Sentence sent, PrintWriter warnOut)
	{
		s = sent;
		this.warnOut = warnOut;
	}

	public void transformEnhancedSyntax()
	{

	}

	protected void transformSubtree(Node aNode) throws XPathExpressionException
	{
		NodeList children = Utils.getAllPMLChildren(aNode);
		// First process the cildren.
		if (children == null || children.getLength() < 1) return;
		String aId = Utils.getId(aNode);
		for (int i = 0; i < children.getLength(); i++)
			transformSubtree(children.item(i));
		// Then assign all necessery links between this node and it's children?
		if (Utils.isReductionNode(aNode))
		{
			Token elevChild = s.pmlaToConll.get(aId);
			int position = s.conll.indexOf(elevChild);
			while (elevChild.idBegin == s.conll.get(position+1).idBegin)
				position++;
			Token newTok = new Token();
			//TODO

		}
	}
/*	public void transformEnhancedSyntax()
	{
		for (Token t : s.conll)
		{
			if (t.head == null) continue;
			if (t.head.second != null) t.deps.add(new EnhencedDep(t.head.second, t.deprel));
			else if (t.head.first.equals("0") && t.deprel.equals(UDv2Relations.ROOT))
				t.deps.add(EnhencedDep.root());
			else
				warnOut.printf("Failed to copy HEAD/DEPREL to DEPS for token with ID %s in sentence %s.\n",
						t.head.first, s.id);
		}
	}*/
}
