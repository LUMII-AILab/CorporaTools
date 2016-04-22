package lv.ailab.lvtb.universalizer.transformator;

import lv.ailab.lvtb.universalizer.conllu.Token;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created on 2016-04-20.
 *
 * @author Lauma
 */
public class Sentence
{
	public String id;
	public Node pmlTree;
	public ArrayList<Token> conll = new ArrayList<>();
	//public HashMap<Token, Node> conllToPmla = new HashMap<>();
	/**
	 * Mapping from A-level ids to CoNLL tokens.
	 * Here goes phrase representing empty nodes, if it has been resolved, which
	 * child will be the parent of the dependency subtree.
	 */
	public HashMap<String, Token> pmlaToConll = new HashMap<>();

	public Sentence(Node pmlTree) throws XPathExpressionException
	{
		this.pmlTree = pmlTree;
		id = XPathEngine.get().evaluate("./@id", this.pmlTree);
	}

	public String toConllU()
	{
		StringBuilder res = new StringBuilder();
		res.append("# Latvian Treebank sentence ID: ");
		res.append(id);
		res.append("\n");
		for (Token t : conll)
			res.append(t.toConllU());
		res.append("\n");
		return res.toString();
	}
}
