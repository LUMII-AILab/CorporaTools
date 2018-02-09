package lv.ailab.lvtb.universalizer.transformator.morpho;

import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.LvtbFormChange;
import lv.ailab.lvtb.universalizer.pml.PmlANode;
import lv.ailab.lvtb.universalizer.pml.PmlMNode;
import lv.ailab.lvtb.universalizer.pml.PmlWNode;
import lv.ailab.lvtb.universalizer.pml.utils.PmlANodeListUtils;
import lv.ailab.lvtb.universalizer.pml.utils.PmlIdUtils;
import lv.ailab.lvtb.universalizer.utils.Logger;
import lv.ailab.lvtb.universalizer.transformator.Sentence;
import lv.ailab.lvtb.universalizer.transformator.TransformationParams;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MorphoTransformator {
	/**
	 * In this sentence all the transformations are carried out.
	 */
	public Sentence s;
	protected TransformationParams params;
	protected Logger logger;

	public MorphoTransformator(Sentence sent, TransformationParams params, Logger logger)
	{
		s = sent;
		this.logger = logger;
		this.params = params;
	}

	/**
	 * Create CoNLL-U token table, fill in ID, FORM, LEMMA, XPOSTAG, UPOSTAG and
	 * FEATS fields.
	 */
	public void transformTokens()
	{
		// Find all nodes having both morpho and ord fields, and sort them
		// according to ord.
		List<PmlANode> tokenNodes = s.pmlTree.getDescendantsWithOrdAndM();
		tokenNodes = PmlANodeListUtils.asOrderedList(tokenNodes);
		Token previousToken = null;
		//String prevMId = null;
		PmlWNode prevW = null;
		int prevOrd = Integer.MIN_VALUE;
		// Make CoNLL token from each node.
		for (PmlANode current : tokenNodes)
		{
			PmlMNode currentM = current.getM();
			List<PmlWNode> currentWs = currentM.getWs();
			Integer currentOrd = current.getOrd();
			if (currentOrd == null || currentOrd < 1) continue;
			if (prevOrd == currentOrd)
			{
				logger.doInsentenceWarning(String.format(
						"\"%s\" has several nodes with ord \"%s\", arbitrary order used!", s.id, currentOrd));
			}

			// Determine, if paragraph has border before this token.
			boolean paragraphChange = false;
			if (prevW != null && currentWs != null && !currentWs.isEmpty())
			{
				PmlWNode nextW = currentWs.get(0);
				String prevId = prevW.getId();
				String nextId = nextW.getId();
				Boolean tempChange = PmlIdUtils.isParaBorderBetween(prevId, nextId);
				if (tempChange == null)
					logger.doInsentenceWarning(String.format(
							"Node id \"%s\" or \"%s\" does not match paragraph searching pattern!",
							prevId, nextId));
				else paragraphChange = tempChange;
			}
			//String mId = currentM.getId();
			//if (mId.matches("m-.*-p\\d+s\\d+w\\d+"))
			//	mId = mId.substring(mId.indexOf("-") + 1, mId.lastIndexOf("s"));
			//else logger.doInsentenceWarning(String.format(
			//		"Node id \"%s\" does not match paragraph searching pattern!", mId));
			//if (prevMId!= null && !prevMId.equals(mId))
			//	paragraphChange = true;

			// Make new token.
			previousToken = transformCurrentToken(current, previousToken, paragraphChange);

			//prevMId = mId;
			if (currentWs != null && !currentWs.isEmpty())
				prevW = currentWs.get(currentWs.size() - 1);
			prevOrd = currentOrd;
		}
	}

	protected Token makeNewToken(
			int tokenIdBegin, int tokenIdDecimal, String pmlId,
			String form, String lemma, String tag,
			PmlANode placementNode, Set<String> miscFlags, boolean representative)
	{
		Token resTok = new Token(tokenIdBegin, form, lemma, tag == null ? null : getXpostag(tag, null));
		if (tokenIdDecimal > 0) resTok.idSub = tokenIdDecimal;
		if (params.ADD_NODE_IDS && pmlId != null && !pmlId.isEmpty())
		{
			resTok.misc.add("LvtbNodeId=" + pmlId);
			logger.addIdMapping(s.id, resTok.getFirstColumn(), pmlId);
		}
		if (resTok.xpostag != null)
		{
			resTok.upostag = PosLogic.getUPosTag(resTok.form, resTok.lemma, resTok.xpostag, placementNode, logger);
			resTok.feats = FeatsLogic.getUFeats(resTok.form, resTok.lemma, resTok.xpostag, placementNode, logger);
		}
		if (miscFlags != null) resTok.misc.addAll(miscFlags);
		s.conll.add(resTok);
		if (representative) s.pmlaToConll.put(pmlId, resTok);
		return resTok;
	}

	protected boolean hasParaChange(PmlWNode one, PmlWNode other)
	{
		String firstId = one.getId();
		String lastId = other.getId();
		Boolean innerParaChange = PmlIdUtils.isParaBorderBetween(
				firstId, lastId);
		if (innerParaChange == null)
		{
			logger.doInsentenceWarning(String.format(
					"Node id \"%s\" or \"%s\" does not match paragraph searching pattern!",
					firstId, lastId));
			return false;
		}
		return innerParaChange;
	}

	/**
	 * Helper method: Create CoNLL-U table entry for one token, fill in ID,
	 * FORM, LEMMA, XPOSTAG, UPOSTAG and FEATS fields.
	 * @param aNode				PML A-level node for which CoNLL entry must be
	 *                          created
	 * @param previousToken		the token after which should follow all newmade
	 *                          tokens
	 * @param paragraphChange	paragraph border detected right before this
	 *                          token.
	 * @return last token made
	 */
	protected Token transformCurrentToken(PmlANode aNode, Token previousToken, boolean paragraphChange)
	{
		PmlMNode mNode = aNode.getM();
		String mForm = mNode.getForm();
		String mLemma = mNode.getLemma();
		String lvtbTag = mNode.getTag();
		String lvtbAId = aNode.getId();
		List<PmlWNode> wNodes = mNode.getWs();
		boolean noSpaceAfter = wNodes != null && !wNodes.isEmpty() &&
				wNodes.get(wNodes.size() - 1).noSpaceAfter();

		// Starting from UD v2 numbers and certain abbrieavations are allowed to
		// be tokens with spaces.
		if (!(mForm.contains(" ") || mLemma.contains(" ")) ||
				lvtbTag.matches("x[no].*") ||
				mForm.replace(" ", "").matches("u\\.t\\.jpr\\.|u\\.c\\.|u\\.tml\\.|v\\.tml\\."))
		{
			Set<LvtbFormChange> formChanges = mNode.getFormChange();
			if (formChanges == null) formChanges = new HashSet<>();
			String source = mNode.getSourceString();

			HashSet<String> miscFlags = new HashSet<>();
			if (noSpaceAfter) miscFlags.add("SpaceAfter=No");
			if (paragraphChange) miscFlags.add("NewPar=Yes");
			if (mForm.equals(source))
			// Form matches source - nothing to worry about.
			//	Add note to misc field if retokenization has been done.
			{
				if (formChanges.contains(LvtbFormChange.SPACING))
					miscFlags.add("CorrectionType=Spacing"); // This actually happens?
				if (wNodes != null && wNodes.size() > 1 &&
						hasParaChange(wNodes.get(0), wNodes.get(wNodes.size() -1)))
						miscFlags.add("NewPar=Yes");
				return makeNewToken(
						previousToken == null ? 1 : previousToken.idBegin + 1,
						0, lvtbAId, mForm, mLemma, lvtbTag, aNode,
						miscFlags, true);
			}
			else if (formChanges.isEmpty())
			// Form does not match source, but there is no form_change available
			{
				// If source contains spaces, nothing good can be done
				if (source.matches(".*\\s.*"))
					throw new IllegalArgumentException(String.format(
							"Node \"%s\" with form \"%s\" has non-matching w-text \"%s\", but no form change",
							lvtbAId, mForm, source));
				else
				// If source contains no spaces, use source as wordform and
				// add corrected form.
				{
					logger.doInsentenceWarning(String.format(
							"Node \"%s\" with form \"%s\" has non-matching w-text \"%s\", but no form change",
							lvtbAId, mForm, source));
					miscFlags.add("CorrectedForm="+mForm);
					if (wNodes != null && wNodes.size() > 1 &&
							hasParaChange(wNodes.get(0), wNodes.get(wNodes.size() -1)))
						miscFlags.add("NewPar=Yes");
					return makeNewToken(
							previousToken == null ? 1 : previousToken.idBegin + 1,
							0, lvtbAId, source, mLemma, lvtbTag,
							aNode, miscFlags, true);
				}
			}
			else if (formChanges.contains(LvtbFormChange.SPELL) && formChanges.size() == 1)
			// If only correction is spelling, add in misc correct form and process as normal
			{
				miscFlags.add("CorrectedForm="+mForm);
				miscFlags.add("CorrectionType=Spelling");
				if (wNodes != null && wNodes.size() > 1 &&
						hasParaChange(wNodes.get(0), wNodes.get(wNodes.size() -1)))
					miscFlags.add("NewPar=Yes");
				return makeNewToken(
						previousToken == null ? 1 : previousToken.idBegin + 1,
						0, lvtbAId, source, mLemma, lvtbTag, aNode,
						miscFlags, true);
			}

			else if (formChanges.contains(LvtbFormChange.INSERT) && formChanges.contains(LvtbFormChange.PUNCT))
			// Inserted punctuation
			{
				miscFlags.add("CorrectionType=InsertedPunctuation");
				return makeNewToken(
						previousToken.idBegin, previousToken.idSub + 1,
						lvtbAId, source, mLemma, lvtbTag, aNode, miscFlags, true);
			}
			else if (formChanges.contains(LvtbFormChange.INSERT))
			// Some weard inserted thing, shouldn't be there
			{
				logger.doInsentenceWarning(String.format(
						"Node \"%s\" with form \"%s\" has form change \"insert\", but not \"punct\"",
						aNode.getId(), mForm));
				miscFlags.add("CorrectionType=Inserted");
				return makeNewToken(
						previousToken.idBegin, previousToken.idSub + 1,
						lvtbAId, source, mLemma, lvtbTag, aNode, miscFlags, true);
			}
			else if(formChanges.contains(LvtbFormChange.UNION) && formChanges.contains(LvtbFormChange.PUNCT)
					&& formChanges.size() == 2 && source.startsWith(mForm))
			// Renmoved punctuation (good case - no other problems)
			{
				String lastPart = source.substring(mForm.length());
				previousToken = makeNewToken(
						previousToken == null ? 1 : previousToken.idBegin + 1,
						0, lvtbAId, mForm, mLemma, lvtbTag, aNode,
						miscFlags, true);

				Token nextToken =  makeNewToken(
						previousToken.idBegin + 1, 0, lvtbAId,
						lastPart, null, null, aNode, null, false);
				nextToken.misc.add("CorrectionType=RemovedPunctuation");
				if (wNodes != null && wNodes.size() > 1 &&
						hasParaChange(wNodes.get(0), wNodes.get(wNodes.size() -1)))
					nextToken.misc.add("NewPar=Yes");
				nextToken.setParentDeps(
						previousToken, UDv2Relations.PUNCT, true, true);
				//nextToken.head = Tuple.of(previousToken.getFirstColumn(), previousToken);
				//nextToken.deprel = UDv2Relations.PUNCT;
				return nextToken;
			}
			else if(formChanges.contains(LvtbFormChange.UNION)
					&& formChanges.contains(LvtbFormChange.PUNCT))
			// Renmoved punctuation other cases
			{
				Matcher m = Pattern.compile("(.*?)([-,.])").matcher(source);
				if (m.matches() && formChanges.contains(LvtbFormChange.SPELL)
						&& formChanges.size() == 3)
				// Together with spelling error
				{
					String firstPart = m.group(1);
					String lastPart = m.group(2);
					miscFlags.add("CorrectedForm="+mForm);
					miscFlags.add("CorrectionType=Spelling");
					previousToken = makeNewToken(
							previousToken == null ? 1 : previousToken.idBegin + 1,
							0, lvtbAId, firstPart, mLemma, lvtbTag,
							aNode, miscFlags, true);
					Token nextToken = makeNewToken(
							previousToken.idBegin + 1, 0,
							lvtbAId, lastPart, null, null, aNode, null, false);
					nextToken.misc.add("CorrectionType=RemovedPunctuation");
					if (wNodes != null && wNodes.size() > 1 &&
							hasParaChange(wNodes.get(0), wNodes.get(wNodes.size() -1)))
						nextToken.misc.add("NewPar=Yes");
					nextToken.setParentDeps(previousToken, UDv2Relations.PUNCT, true, true);
					//nextToken.head = Tuple.of(previousToken.getFirstColumn(), previousToken);
					//nextToken.deprel = UDv2Relations.PUNCT;
					return nextToken;
				}
				else
				// TODO togerther with spacing error
				{
					throw new IllegalArgumentException(String.format(
							"Don't know what to do with node \"%s\" with form \"%s\", w-text \"%s\", and form_change \"%s\"",
							aNode.getId(), mForm, source,
							formChanges.stream().map(LvtbFormChange::toString)
									.reduce((s1, s2) -> s1 + "\", \"" + s2).orElse("")));
				}
			}
			else if (formChanges.contains(LvtbFormChange.UNION) &&
					formChanges.contains(LvtbFormChange.SPACING) &&
					(formChanges.size() == 2 ||
						formChanges.size() == 3 &&
						formChanges.contains(LvtbFormChange.SPELL)) &&
					wNodes != null && wNodes.size() > 1)
			// Words that must be written together.
			{
				LinkedList<PmlWNode> unprocessedWs = new LinkedList<>();
				unprocessedWs.addAll(wNodes);
				LinkedList<PmlWNode> forNexTok = new LinkedList<>();
				while (!unprocessedWs.isEmpty() && unprocessedWs.peek().noSpaceAfter())
					forNexTok.push(unprocessedWs.remove());
				miscFlags.add("CorrectedForm="+mForm);
				miscFlags.add("CorrectionType=Spacing,Spelling");
				if (PmlIdUtils.isParaBorderBetween(forNexTok.peek().getId(), forNexTok.poll().getId()))
					miscFlags.add("NewPar=Yes");
				previousToken = makeNewToken(
						previousToken == null ? 1 : previousToken.idBegin + 1,
						0,	lvtbAId,
						forNexTok.stream().map(PmlWNode::getToken).reduce((s1, s2) -> s1 + s2).get(),
						mLemma, lvtbTag, aNode, miscFlags, true);
				while (!unprocessedWs.isEmpty())
				{
					forNexTok = new LinkedList<>();
					while (!unprocessedWs.isEmpty() && unprocessedWs.peek().noSpaceAfter())
						forNexTok.push(unprocessedWs.remove());
					Token nextToken = makeNewToken(
							previousToken.idBegin + 1,
							0, lvtbAId,
							forNexTok.stream().map(PmlWNode::getToken).reduce((s1, s2) -> s1 + s2).get(),
							null, null, aNode, null, false);
					nextToken.misc.add("CorrectionType=Spacing,Spelling");
					if (PmlIdUtils.isParaBorderBetween(forNexTok.peek().getId(), forNexTok.poll().getId()))
						nextToken.misc.add("NewPar=Yes");
					nextToken.setParentDeps(previousToken, UDv2Relations.GOESWITH, true, true);
					//nextToken.head = Tuple.of(previousToken.getFirstColumn(), previousToken);
					//nextToken.deprel = UDv2Relations.GOESWITH;
					previousToken = nextToken;
				}
				return previousToken;
			}
			else
			{
				throw new IllegalArgumentException(String.format(
						"Don't know what to do with node \"%s\" with form \"%s\", w-text \"%s\", and form_change \"%s\"",
						aNode.getId(), mForm, source,
						formChanges.stream().map(LvtbFormChange::toString)
								.reduce((s1, s2) -> s1 + "\", \"" + s2).orElse("")));
			}
		}
		else
		{
			throw new IllegalArgumentException(String.format(
					"Node \"%s\" with form \"%s\" and lemma \"%s\" contains spaces",
					aNode.getId(), mForm, mLemma));
			// There was an  obsolete piece of code that separated in multiple
			// tokens with "words with spaces" LVTB used to have in previos
			// versions. However, currently all such cases should be removed
			// from data.
		}
	}

	/**
	 * Logic for obtaining XPOSTAG from tag given in LVTB.
	 * @param lvtbTag	tag given in LVTB
	 * @param ending	postfix to be added to the tag
	 * @return XPOSTAG or _ if tag from LVTB is not meaningfull
	 */
	public static String getXpostag (String lvtbTag, String ending)
	{
		if (lvtbTag == null || lvtbTag.length() < 1 || lvtbTag.matches("N/[Aa]"))
			return "_";
		if (ending == null || ending.length() < 1) return lvtbTag.trim();
		else return (lvtbTag + ending).trim();
	}

	/**
	 * Currently extracted from pre-made CoNLL table.
	 * TODO: use PML tree instead?
	 */
	public void extractSendenceText()
	{
		s.text = "";
		for (Token t : s.conll)
		{
			s.text = s.text + t.form;
			if (t.misc == null || !t.misc.contains("SpaceAfter=No"))
				s.text = s.text + " ";
		}
		s.text = s.text.trim();
	}
}
