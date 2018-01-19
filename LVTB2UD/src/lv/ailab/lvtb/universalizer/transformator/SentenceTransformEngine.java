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
	protected Logger logger;
	protected TransformationParams params;

	public SentenceTransformEngine(Node pmlTree, TransformationParams params, Logger logger)
			throws XPathExpressionException
	{
		s = new Sentence(pmlTree);
		this.logger = logger;
		this.params = params;
		morphoTransf = new MorphoTransformator(s, params, logger);
		syntTransf = new TreesyntaxTransformator(s, params, logger);
		enhSyntTransf = new GraphsyntaxTransformator(s, logger);
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
		if (params.DEBUG) System.out.printf("Working on sentence \"%s\".\n", s.id);

		morphoTransf.transformTokens();
		logger.flush();
		morphoTransf.extractSendenceText();
		logger.flush();
		boolean noMoreEllipsis = syntTransf.preprocessEmptyEllipsis();
		if (params.WARN_ELLIPSIS && !noMoreEllipsis)
			System.out.printf("Sentence \"%s\" has non-trivial ellipsis.\n", s.id);
		syntTransf.transformBaseSyntax();
		logger.flush();
		if (params.DO_ENHANCED)
		{
			enhSyntTransf.transformEnhancedSyntax();
			logger.flush();
		}
		return !s.hasFailed;
	}

	/**
	 * Utility method for "doing everything": create transformer object,
	 * transform given PML tree and get the string representation for the
	 * resulting CoNLL-U table.
	 * @param pmlTree	tree to transform
	 * @param params	transformation parameters
	 * @return 	UD tree in CoNLL-U format or null if tree could not be
	 * 			transformed.
	 */
	public static String treeToConll(Node pmlTree, TransformationParams params, Logger logger)
	{
		String id ="<unknown>";
		try {
			SentenceTransformEngine t = new SentenceTransformEngine(pmlTree, params, logger);
			id = t.s.id;
			boolean res = t.transform();
			if (res) return t.s.toConllU();
			if (params.WARN_OMISSIONS)
				logger.warnForOmittedSentence(id);
				//warnOut.printf("Sentence \"%s\" is being omitted.\n", t.s.id);
		} catch (NullPointerException|IllegalArgumentException e)
		{
			//warnOut.println("Transforming sentence " + id + " completely failed! Check structure and try again.");
			System.err.println("Transforming sentence " + id + " completely failed! Check structure and try again.");
			//e.printStackTrace(warnOut);
			e.printStackTrace();
			logger.failSentenceForException(id, e, false);
			//throw e;
		}
		catch (XPathExpressionException|IllegalStateException e)
		{
			//warnOut.println("Transforming sentence " + id + " completely failed! Might be algorithmic error.");
			System.err.println("Transforming sentence " + id + " completely failed! Might be algorithmic error.");
			//e.printStackTrace(warnOut);
			e.printStackTrace();
			logger.failSentenceForException(id, e, false);
		}
		return null;
	}

}
