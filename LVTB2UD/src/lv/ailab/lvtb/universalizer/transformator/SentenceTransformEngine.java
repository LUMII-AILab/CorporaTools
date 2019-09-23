package lv.ailab.lvtb.universalizer.transformator;

import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.PmlANode;
import lv.ailab.lvtb.universalizer.transformator.morpho.MorphoTransformator;
import lv.ailab.lvtb.universalizer.transformator.syntax.EllipsisPreprocessor;
import lv.ailab.lvtb.universalizer.transformator.syntax.NewSyntaxTransformator;
import lv.ailab.lvtb.universalizer.utils.Tuple;

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
	protected NewSyntaxTransformator syntTransf;
	protected TransformationParams params;

	public SentenceTransformEngine(
			PmlANode pmlTree, TransformationParams params)
	{
		s = new Sentence(pmlTree);
		this.params = params;
		ellipPreproc = new EllipsisPreprocessor(s);
		morphoTransf = new MorphoTransformator(s, params);
		syntTransf = new NewSyntaxTransformator(s, params);
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

		if (params.TURN_INSERTED_WORD_ELLIPSIS) ellipPreproc.replaceInsertedWords();
		if (params.SPLIT_NONEMPTY_ELLIPSIS) ellipPreproc.splitTokenEllipsis();
		morphoTransf.transformTokens();
		StandardLogger.l.flush();
		morphoTransf.extractSendenceText();
		StandardLogger.l.flush();
		boolean noMoreEllipsis = ellipPreproc.removeAllChildlessEllipsis();
		if (params.WARN_ELLIPSIS && !noMoreEllipsis)
			System.out.printf("Sentence \"%s\" has non-trivial ellipsis.\n", s.id);
		syntTransf.prepare();
		syntTransf.transform();
		syntTransf.aftercare();
		StandardLogger.l.flush();
		morphoTransf.transformPostsyntMorpho();
		StandardLogger.l.finishSentenceNormal();
		/*try
		{
			Transformer transformer = TransformerFactory.newInstance().newTransformer();

			// Uncomment if you do not require XML declaration
			// transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

			//A character stream that collects its output in a string buffer,
			//which can then be used to construct a string.
			StringWriter writer = new StringWriter();

			//transform document to string
			transformer.transform(new DOMSource(((XmlDomANode)s.pmlTree).domNode), new StreamResult(writer));

			String xmlString = writer.getBuffer().toString();
			System.out.println(xmlString);						//Print to console or logs
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}*/
	}

	/**
	 * Utility method for "doing everything": create transformer object,
	 * transform given PML tree and get the string representation for the
	 * resulting CoNLL-U table.
	 * @param pmlTree	tree to transform
	 * @param params	transformation parameters
	 * @return 	UD tree in CoNLL-U format or null if tree could not be
	 * 			transformed paired with stats about dep roles (basic dependency
	 * 			'dep' first, then ehnhanced dependency 'dep').
	 */
	public static Tuple<String, Tuple<Integer, Integer>> treeToConll(
			PmlANode pmlTree, TransformationParams params)
	{
		String id ="<unknown>";
		try {
			SentenceTransformEngine t = new SentenceTransformEngine(pmlTree, params);
			id = t.s.id;
			t.transform();
			return Tuple.of(t.s.toConllU(),
					Tuple.of(t.s.countUdBaseRole(UDv2Relations.DEP), t.s.countUdEnhRole(UDv2Relations.DEP)));
		} catch (NullPointerException|IllegalArgumentException e)
		{
			System.err.println("Transforming sentence " + id + " completely failed! Check structure and try again.");
			e.printStackTrace();
			StandardLogger.l.finishSentenceWithException(id, e, false);
		}
		catch (IllegalStateException e)
		{
			System.err.println("Transforming sentence " + id + " completely failed! Might be algorithmic error.");
			e.printStackTrace();
			StandardLogger.l.finishSentenceWithException(id, e, false);
		}
		return null;
	}

}
