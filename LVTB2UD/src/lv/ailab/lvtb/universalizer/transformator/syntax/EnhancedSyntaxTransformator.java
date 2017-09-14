package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.transformator.Sentence;
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
		// TODO
	}

}
