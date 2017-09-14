package lv.ailab.lvtb.universalizer.transformator;

import lv.ailab.lvtb.universalizer.transformator.morpho.MorphoTransformator;
import lv.ailab.lvtb.universalizer.transformator.syntax.*;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpressionException;
import java.io.PrintWriter;

/**
 * Logic for transforming LVTB sentence annotations to UD.
 * No change is done in PML tree, all results are stored in CoNLL-U table only.
 * Assumes normalized ord values (only morpho tokens are numbered).
 * TODO: switch to full ord values?
 * XPathExpressionException everywhere, because all the navigation in the XML is
 * done with XPaths.
 * Created on 2016-04-17.
 *
 * @author Lauma
 */
public class SentenceTransformEngine
{
	public Sentence s;
	protected MorphoTransformator morphoTransf;
	protected TreesyntaxTransformator syntTransf;
	protected GraphsyntaxTransformator enhSyntTransf;
	protected PrintWriter warnOut;
	public static boolean DEBUG = false;
	public static boolean WARN_ELLIPSIS = false;
	public static boolean WARN_OMISSIONS = true;
	public static boolean DO_ENHANCED = true;
	/**
	 * For already processed nodes without tag set the phrase tag based on node
	 * chosen as substructure root.
	 */
	public static boolean INDUCE_PHRASE_TAGS = true;

	public SentenceTransformEngine(Node pmlTree, PrintWriter warnOut)
			throws XPathExpressionException
	{
		s = new Sentence(pmlTree);
		this.warnOut = warnOut;
		morphoTransf = new MorphoTransformator(s, warnOut);
		syntTransf = new TreesyntaxTransformator(s, warnOut, INDUCE_PHRASE_TAGS, DEBUG);
		enhSyntTransf = new GraphsyntaxTransformator(s, warnOut);
	}

	/**
	 * Create CoNLL-U token table, try to fill it in as much as possible.
	 * @return	true, if tree has no untranformable ellipsis; false if tree
	 * 			contains untransformable ellipsis and, thus, result data
	 * 		    has garbage syntax.
	 * @throws XPathExpressionException	unsuccessfull XPathevaluation (anywhere
	 * 									in the PML tree) most probably due to
	 * 									algorithmical error.
	 */
	public boolean transform() throws XPathExpressionException
	{
		if (DEBUG) System.out.printf("Working on sentence \"%s\".\n", s.id);

		morphoTransf.transformTokens();
		warnOut.flush();
		morphoTransf.extractSendenceText();
		warnOut.flush();
		boolean noMoreEllipsis = syntTransf.preprocessEmptyEllipsis();
		if (WARN_ELLIPSIS && !noMoreEllipsis)
			System.out.printf("Sentence \"%s\" has non-trivial ellipsis.\n", s.id);
		syntTransf.transformBaseSyntax();
		warnOut.flush();
		if (DO_ENHANCED)
		{
			enhSyntTransf.transformEnhancedSyntax();
			warnOut.flush();
		}
		return !syntTransf.hasFailed;
	}

	/**
	 * Utility method for "doing everything": create transformer object,
	 * transform given PML tree and get the string representation for the
	 * resulting CoNLL-U table.
	 * @param pmlTree	tree to transform
	 * @return 	UD tree in CoNLL-U format or null if tree could not be
	 * 			transformed.
	 */
	public static String treeToConll(Node pmlTree, PrintWriter warnOut)
	{
		String id ="<unknown>";
		try {
			SentenceTransformEngine t = new SentenceTransformEngine(pmlTree, warnOut);
			id = t.s.id;
			boolean res = t.transform();
			if (res) return t.s.toConllU();
			if (WARN_OMISSIONS)
				warnOut.printf("Sentence \"%s\" is being omitted.\n", t.s.id);
		} catch (NullPointerException|IllegalArgumentException e)
		{
			warnOut.println("Transforming sentence " + id + " completely failed! Check structure and try again.");
			System.err.println("Transforming sentence " + id + " completely failed! Check structure and try again.");
			e.printStackTrace(warnOut);
			e.printStackTrace();
			//throw e;
		}
		catch (XPathExpressionException|IllegalStateException e)
		{
			warnOut.println("Transforming sentence " + id + " completely failed! Might be algorithmic error.");
			System.err.println("Transforming sentence " + id + " completely failed! Might be algorithmic error.");
			e.printStackTrace(warnOut);
			e.printStackTrace();
			//throw new RuntimeException(e);
		}
		return null;
	}

}
