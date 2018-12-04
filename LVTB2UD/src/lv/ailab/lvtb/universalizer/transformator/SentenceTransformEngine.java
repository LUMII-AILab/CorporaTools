package lv.ailab.lvtb.universalizer.transformator;

import lv.ailab.lvtb.universalizer.pml.PmlANode;
import lv.ailab.lvtb.universalizer.transformator.morpho.MorphoTransformator;
import lv.ailab.lvtb.universalizer.transformator.syntax.*;
import lv.ailab.lvtb.universalizer.utils.Logger;

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
	protected EllipsisPreprocessor ellipPreproc;
	protected TreesyntaxTransformator syntTransf;
	protected GraphsyntaxTransformator enhSyntTransf;
	protected Logger logger;
	protected TransformationParams params;

	public SentenceTransformEngine(
			PmlANode pmlTree, TransformationParams params, Logger logger)
	{
		s = new Sentence(pmlTree);
		this.logger = logger;
		this.params = params;
		ellipPreproc = new EllipsisPreprocessor(s, logger);
		morphoTransf = new MorphoTransformator(s, params, logger);
		syntTransf = new TreesyntaxTransformator(s, params, logger);
		enhSyntTransf = new GraphsyntaxTransformator(s, logger);
	}

	/**
	 * Create CoNLL-U token table, try to fill it in as much as possible.
	 * @throws NullPointerException		transformation failure, probably because
	 * 									of invalid tree structure
	 * @throws IllegalArgumentException	transformation failure, probably because
	 *	 								of invalid tree structure
	 * @throws IllegalStateException	transformation failure, probably because
	 * 									of algorithmic error
	 */
	public void transform()
	{
		if (params.DEBUG) System.out.printf("Working on sentence \"%s\".\n", s.id);

		morphoTransf.transformTokens();
		logger.flush();
		morphoTransf.extractSendenceText();
		logger.flush();
		boolean noMoreEllipsis = ellipPreproc.removeAllChildlessEllipsis();
		if (params.WARN_ELLIPSIS && !noMoreEllipsis)
			System.out.printf("Sentence \"%s\" has non-trivial ellipsis.\n", s.id);
		syntTransf.transformBaseSyntax();
		logger.flush();
		enhSyntTransf.transformEnhancedSyntax();
		logger.flush();
		morphoTransf.transformPostsyntMorpho();
		//logger.finishSentenceNormal(s.hasFailed);
		logger.finishSentenceNormal();
		//return !s.hasFailed;
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
	public static String treeToConll(
			PmlANode pmlTree, TransformationParams params, Logger logger)
	{
		String id ="<unknown>";
		try {
			SentenceTransformEngine t = new SentenceTransformEngine(pmlTree, params, logger);
			id = t.s.id;
			t.transform();
			return t.s.toConllU();
		} catch (NullPointerException|IllegalArgumentException e)
		{
			System.err.println("Transforming sentence " + id + " completely failed! Check structure and try again.");
			e.printStackTrace();
			logger.finishSentenceWithException(id, e, false);
		}
		catch (IllegalStateException e)
		{
			System.err.println("Transforming sentence " + id + " completely failed! Might be algorithmic error.");
			e.printStackTrace();
			logger.finishSentenceWithException(id, e, false);
		}
		return null;
	}

}
