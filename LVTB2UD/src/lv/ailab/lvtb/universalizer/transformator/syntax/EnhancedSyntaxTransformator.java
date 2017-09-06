package lv.ailab.lvtb.universalizer.transformator.syntax;

import lv.ailab.lvtb.universalizer.conllu.EnhencedDep;
import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
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
	}
}
