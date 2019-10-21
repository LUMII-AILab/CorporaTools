package lv.ailab.lvtb.universalizer.transformator.morpho;

import lv.ailab.lvtb.universalizer.conllu.*;
import lv.ailab.lvtb.universalizer.pml.*;
import lv.ailab.lvtb.universalizer.pml.utils.PmlANodeListUtils;
import lv.ailab.lvtb.universalizer.pml.utils.PmlIdUtils;
import lv.ailab.lvtb.universalizer.transformator.Sentence;
import lv.ailab.lvtb.universalizer.transformator.StandardLogger;
import lv.ailab.lvtb.universalizer.transformator.TransformationParams;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is the part of transformation where tokens for CoNLL-U table is created
 * and information form morphology fields is obtained.
 * Transformator relies on w level tokens being smaller than m level units.
 */
public class MorphoTransformator {
	/**
	 * In this sentence all the transformations are carried out.
	 */
	public Sentence s;
	protected TransformationParams params;

	public MorphoTransformator(Sentence sent, TransformationParams params)
	{
		s = sent;
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
				StandardLogger.l.doInsentenceWarning(String.format(
						"\"%s\" has several nodes with ord \"%s\", arbitrary order used!", s.id, currentOrd));

			// Determine, if paragraph has border before this token.
			boolean paragraphChange = false;
			if (prevW != null && currentWs != null && !currentWs.isEmpty())
			{
				PmlWNode nextW = currentWs.get(0);
				String prevId = prevW.getId();
				String nextId = nextW.getId();
				Boolean tempChange = PmlIdUtils.isParaBorderBetween(prevId, nextId);
				if (tempChange == null)
					StandardLogger.l.doInsentenceWarning(String.format(
							"Node id \"%s\" or \"%s\" does not match paragraph searching pattern!",
							prevId, nextId));
				else paragraphChange = tempChange;
			}

			// Make new token.
			previousToken = transformCurrentNode(current, previousToken, paragraphChange);

			if (currentWs != null && !currentWs.isEmpty())
				prevW = currentWs.get(currentWs.size() - 1);
			prevOrd = currentOrd;
		}
		if (params.ADD_NODE_IDS && params.SPLIT_NONEMPTY_ELLIPSIS) cleanupIds();
	}

	/**
	 * Check all references to original nodes and remove artificially added node
	 * ID prefixes.
	 */
	protected void cleanupIds()
	{
		for (Token t : s.conll)
		{
			HashSet<String> misc = t.misc.get(MiscKeys.LVTB_NODE_ID);
			for (String id : misc)
			{
				if (id.endsWith(Sentence.ID_POSTFIX))
				{
					String newId = id.substring(0, id.length() - Sentence.ID_POSTFIX.length());
					misc.remove(id);
					misc.add(newId);
				}
			}
		}
	}

	/**
	 * Helper method: Create necessary CoNLL table entries describing one M
	 * node, make links between and fill in necessary fields.
	 * @param aNode				PML A-level node for which CoNLL entry must be
	 *                          created
	 * @param previousToken		the token after which should follow all newmade
	 *                          tokens
	 * @param paragraphChange	paragraph border detected right before this
	 *                          token.
	 * @return last token made
	 */
	protected Token transformCurrentNode(
			PmlANode aNode, Token previousToken, boolean paragraphChange)
	{
		PmlMNode mNode = aNode.getM();
		String mForm = mNode.getForm();
		String mLemma = mNode.getLemma();
		String lvtbTag = mNode.getTag();
		String lvtbAId = aNode.getId();

		// Starting from UD v2 numbers and certain abbrieavations are allowed to
		// be tokens with spaces.
		if ((mForm.contains(" ") || mLemma.contains(" ")) &&
				!lvtbTag.matches("x[no].*") &&
				!mForm.replace(" ", "").matches("u\\.t\\.jpr\\.|u\\.c\\.|u\\.tml\\.|v\\.tml\\.|u\\.t\\.t\\.|(P\\.)+S\\.|N\\.B\\."))
			throw new IllegalArgumentException(String.format(
					"Node \"%s\" with form \"%s\" and lemma \"%s\" contains spaces",
					lvtbAId, mForm, mLemma)); // UD allowed words with spaces are supposed to be the same as LVTB

		List<PmlWNode> wNodes = mNode.getWs();
		Set<LvtbFormChange> formChanges = mNode.getFormChange();
		if (formChanges == null) formChanges = new HashSet<>();
		String source = mNode.getSourceString();

		// Form matches source - nothing to worry about.
		if (mForm.equals(source))
			return transfOnMatch(aNode, previousToken, paragraphChange);
		// Form does not match source, but there is no form_change available
		//else if (formChanges.isEmpty())
		//	return transfOnEmptyFormCh(aNode, previousToken, paragraphChange);

		// If only correction is spelling, add in misc correct form and process as normal
		// If also has missing space after, add in misc CorrectSpaceAfter.
		else if (formChanges.contains(LvtbFormChange.SPELL) && formChanges.size() == 1)
			return transfOnSpellOnly(aNode, previousToken, paragraphChange, false);
			// If only correction is spelling, add in misc correct form and process as normal
			// If also has missing space after, add in misc CorrectSpaceAfter.
		else if (formChanges.contains(LvtbFormChange.SPELL) && formChanges.contains(LvtbFormChange.SPACING)
				&& !source.contains(" ") && formChanges.size() == 2)
			return transfOnSpellOnly(aNode, previousToken, paragraphChange, true);
		// Inserted punctuation
		else if (formChanges.contains(LvtbFormChange.INSERT) && formChanges.contains(LvtbFormChange.PUNCT)
				&& formChanges.size() == 2)
		{
			Token prevRealTok = previousToken;
			while (prevRealTok != null && prevRealTok.idSub > 0)
			{
				int index = s.conll.indexOf(prevRealTok);
				prevRealTok = index > 0 ? s.conll.get(index - 1) : null;
			}
			if (prevRealTok != null)
				prevRealTok.addMisc(MiscKeys.CORRECTION_TYPE, MiscValues.INS_PUNCT_AFTER);
			return params.UD_STANDARD_NULLNODES ? previousToken : transfOnPunctInsert(aNode, previousToken, paragraphChange);
		}
		// Removed punctuation (good case - no other problems)
		else if(formChanges.contains(LvtbFormChange.UNION) && formChanges.contains(LvtbFormChange.PUNCT)
				&& formChanges.size() == 2 && source.startsWith(mForm))
			return transfOnCleanPunctDel(aNode, previousToken, paragraphChange);
		// Renmoved punctuation other cases
		else if(formChanges.contains(LvtbFormChange.UNION)
				&& formChanges.contains(LvtbFormChange.PUNCT))
			return transfOnUglyPunctDel(aNode, previousToken, paragraphChange);
		// Words that must be written together.
		else if (formChanges.contains(LvtbFormChange.UNION) &&
				formChanges.contains(LvtbFormChange.SPACING) &&
				(formChanges.size() == 2 ||
					formChanges.size() == 3 &&
					formChanges.contains(LvtbFormChange.SPELL)) &&
				wNodes != null && wNodes.size() > 1)
			return transfOnRemovedSpaces(aNode, previousToken, paragraphChange);
		// Words that must be split?
	/*	else if (formChanges.contains(LvtbFormChange.SPACING) &&
				!formChanges.contains(LvtbFormChange.UNION) &&
				!formChanges.contains(LvtbFormChange.PUNCT))
			return transfOnAddedSpaces(aNode, previousToken, paragraphChange);//*/
		// Don't know what to do.
		else
			throw new IllegalArgumentException(String.format(
					"Don't know what to do with node \"%s\" with form \"%s\", w-text \"%s\", and form_change \"%s\"",
					lvtbAId, mForm, source,
					formChanges.stream().map(LvtbFormChange::toString)
							.reduce((s1, s2) -> s1 + "\", \"" + s2).orElse("")));
	}

	/**
	 * Helper method: Create necessary CoNLL table entries for mNode where
	 * source text from w level matches the wordworm.
	 * @param aNode				PML A-level node for which CoNLL entry must be
	 *                          created
	 * @param previousToken		the token after which should follow all newmade
	 *                          tokens
	 * @param paragraphChange	paragraph border detected right before this
	 *                          token.
	 * @return last token made
	 */
	protected Token transfOnMatch(
			PmlANode aNode, Token previousToken, boolean paragraphChange)
	{
		String lvtbAId = aNode.getId();
		PmlMNode mNode = aNode.getM();
		String mForm = mNode.getForm();
		String mLemma = mNode.getLemma();
		String lvtbTag = mNode.getTag();
		Set<LvtbFormChange> formChanges = mNode.getFormChange();
		if (formChanges == null) formChanges = new HashSet<>();
		List<PmlWNode> wNodes = mNode.getWs();
		boolean noSpaceAfter = wNodes != null && !wNodes.isEmpty() &&
				wNodes.get(wNodes.size() - 1).noSpaceAfter();

		Token res = makeNewToken(
				previousToken == null ? 1 : previousToken.idBegin + 1, 0,
				lvtbAId, mForm, mLemma, lvtbTag, true);
		if (noSpaceAfter) res.addMisc(MiscKeys.SPACE_AFTER, MiscValues.NO);
		if (paragraphChange || wNodes != null && wNodes.size() > 1 &&
				hasParaChange(wNodes.get(0), wNodes.get(wNodes.size() -1)))
			res.addMisc(MiscKeys.NEW_PAR, MiscValues.YES);;
		//	Add note to misc field if retokenization has been done.
		// This happens if word is split between rows.
		if (formChanges.contains(LvtbFormChange.SPACING))
		{
			res.addMisc(MiscKeys.CORRECTION_TYPE, MiscValues.SPACING);
			if (noSpaceAfter) res.addMisc(MiscKeys.CORRECT_SPACE_AFTER, MiscValues.YES);
		}

		return res;
	}

	/**
	 * Helper method: Create necessary CoNLL table entries for mNode where
	 * source text from w level does not match the wordworm, but the form_change
	 * is empty.
	 * @param aNode				PML A-level node for which CoNLL entry must be
	 *                          created
	 * @param previousToken		the token after which should follow all newmade
	 *                          tokens
	 * @param paragraphChange	paragraph border detected right before this
	 *                          token.
	 * @return last token made
	 */
	@Deprecated
	protected Token transfOnEmptyFormCh(
			PmlANode aNode, Token previousToken, boolean paragraphChange)
	{
		String lvtbAId = aNode.getId();
		PmlMNode mNode = aNode.getM();
		String mForm = mNode.getForm();
		String source = mNode.getSourceString();

		// If source contains spaces, nothing good can be done
		if (source.matches(".*\\s.*"))
			throw new IllegalArgumentException(String.format(
					"Node \"%s\" with form \"%s\" has non-matching w-text \"%s\", but no form change",
					lvtbAId, mForm, source));

		// If source contains no spaces, use source as wordform and
		// add corrected form.
		StandardLogger.l.doInsentenceWarning(String.format(
				"Node \"%s\" with form \"%s\" has non-matching w-text \"%s\", but no form change",
				lvtbAId, mForm, source));

		String mLemma = mNode.getLemma();
		String lvtbTag = mNode.getTag();
		List<PmlWNode> wNodes = mNode.getWs();
		boolean noSpaceAfter = wNodes != null && !wNodes.isEmpty() &&
				wNodes.get(wNodes.size() - 1).noSpaceAfter();

		Token res =  makeNewToken(
				previousToken == null ? 1 : previousToken.idBegin + 1, 0,
				lvtbAId, source, mLemma, lvtbTag, true);
		res.addMisc(MiscKeys.CORRECT_FORM, mForm);
		res.feats.add(UDv2Feat.TYPO_YES);
		if (noSpaceAfter) res.addMisc(MiscKeys.SPACE_AFTER, MiscValues.NO);
		if (paragraphChange || wNodes != null && wNodes.size() > 1 &&
				hasParaChange(wNodes.get(0), wNodes.get(wNodes.size() -1)))
			res.addMisc(MiscKeys.NEW_PAR, MiscValues.YES);

		return res;
	}

	/**
	 * Helper method: Create necessary CoNLL table entries for mNode where
	 * source text from w level does not match the wordworm and the only
	 * form_change is "spell".
	 * @param aNode				PML A-level node for which CoNLL entry must be
	 *                          created
	 * @param previousToken		the token after which should follow all newmade
	 *                          tokens
	 * @param paragraphChange	paragraph border detected right before this
	 *                          token.
	 * @return last token made
	 */
	protected Token transfOnSpellOnly(PmlANode aNode, Token previousToken,
									  boolean paragraphChange, boolean missingSpacAfter)
	{
		String lvtbAId = aNode.getId();
		PmlMNode mNode = aNode.getM();
		String mForm = mNode.getForm();
		String mLemma = mNode.getLemma();
		String lvtbTag = mNode.getTag();
		String source = mNode.getSourceString();
		List<PmlWNode> wNodes = mNode.getWs();
		boolean noSpaceAfter = wNodes != null && !wNodes.isEmpty() &&
				wNodes.get(wNodes.size() - 1).noSpaceAfter();

		Token res = makeNewToken(
				previousToken == null ? 1 : previousToken.idBegin + 1, 0,
				lvtbAId, source, mLemma, lvtbTag, true);
		res.addMisc(MiscKeys.CORRECT_FORM, mForm);
		res.addMisc(MiscKeys.CORRECTION_TYPE, MiscValues.SPELLING);
		res.feats.add(UDv2Feat.TYPO_YES);
		if (noSpaceAfter)
		{
			res.addMisc(MiscKeys.SPACE_AFTER, MiscValues.NO);
			if (missingSpacAfter) res.addMisc(MiscKeys.CORRECT_SPACE_AFTER, MiscValues.YES);
		}
		else if (missingSpacAfter)
			StandardLogger.l.doInsentenceWarning(String.format(
					"Node \"%s\" with form \"%s\" and source \"%s\" has ignoded form_change=spacing",
					lvtbAId, mForm, source));
		if (paragraphChange || wNodes != null && wNodes.size() > 1 &&
				hasParaChange(wNodes.get(0), wNodes.get(wNodes.size() -1)))
			res.addMisc(MiscKeys.NEW_PAR, MiscValues.YES);

		return res;
	}
	/**
	 * Helper method: Create necessary CoNLL table entries for inserted
	 * punctuation mNode. Warn, if there are links to w level nodes.
	 * @param aNode				PML A-level node for which CoNLL entry must be
	 *                          created
	 * @param previousToken		the token after which should follow all newmade
	 *                          tokens
	 * @param paragraphChange	paragraph border detected right before this
	 *                          token.
	 * @return last token made
	 */
	protected Token transfOnPunctInsert(PmlANode aNode, Token previousToken, boolean paragraphChange)
	{
		String lvtbAId = aNode.getId();
		PmlMNode mNode = aNode.getM();
		String mform = mNode.getForm();
		String mLemma = mNode.getLemma();
		String lvtbTag = mNode.getTag();
		List<PmlWNode> wNodes = mNode.getWs();

		if (wNodes != null && !wNodes.isEmpty() )
			StandardLogger.l.doInsentenceWarning(String.format(
					"Node \"%s\" has both w.rf and form change \"insert\"",
					lvtbAId));

		Token res =  makeNewToken(
				previousToken.idBegin, previousToken.idSub + 1,
				lvtbAId, mform, mLemma, lvtbTag, true);
		res.addMisc(MiscKeys.CORRECTION_TYPE, MiscValues.INS_PUNCT);
		// Chan this really be there?
		if (paragraphChange) res.addMisc(MiscKeys.NEW_PAR, MiscValues.YES);
		return res;

	}

	/**
	 * Helper method: Create necessary CoNLL table entries for inserted mNode
	 * hat is not punctuation. Warn, as this is not supposed to happen in data.
	 * @param aNode				PML A-level node for which CoNLL entry must be
	 *                          created
	 * @param previousToken		the token after which should follow all newmade
	 *                          tokens
	 * @param paragraphChange	paragraph border detected right before this
	 *                          token.
	 * @return last token made
	 */
	@Deprecated
	protected Token transfOnOtherInsert(
			PmlANode aNode, Token previousToken, boolean paragraphChange)
	{
		String lvtbAId = aNode.getId();
		PmlMNode mNode = aNode.getM();
		String mForm = mNode.getForm();
		String mLemma = mNode.getLemma();
		String lvtbTag = mNode.getTag();
		//String source = mNode.getSourceString();
		List<PmlWNode> wNodes = mNode.getWs();

		if (wNodes != null && !wNodes.isEmpty() )
			StandardLogger.l.doInsentenceWarning(String.format(
					"Node \"%s\" has both w.rf and form change \"insert\"",
					lvtbAId));
		StandardLogger.l.doInsentenceWarning(String.format(
				"Node \"%s\" with form \"%s\" has form change \"insert\", but not \"punct\"",
				lvtbAId, mForm));

		Token res = makeNewToken(
				previousToken.idBegin, previousToken.idSub + 1,
				lvtbAId, mForm, mLemma, lvtbTag, true);
		res.addMisc(MiscKeys.CORRECTION_TYPE, MiscValues.INSERTED);
		// Chan this really be there?
		if (paragraphChange) res.addMisc(MiscKeys.NEW_PAR, MiscValues.YES);
		return res;
	}

	/**
	 * Helper method: Create necessary CoNLL table entries for node after which
	 * unnecessary punctuation is removed. The good case - without spelling
	 * errors.
	 * @param aNode				PML A-level node for which CoNLL entry must be
	 *                          created
	 * @param previousToken		the token after which should follow all newmade
	 *                          tokens
	 * @param paragraphChange	paragraph border detected right before this
	 *                          token.
	 * @return last token made
	 */
	// TODO rewrite using w-level tokenization
	protected Token transfOnCleanPunctDel(
			PmlANode aNode, Token previousToken, boolean paragraphChange)
	{
		// TODO handle better spaces in the middle!
		String lvtbAId = aNode.getId();
		PmlMNode mNode = aNode.getM();
		String mForm = mNode.getForm();
		String mLemma = mNode.getLemma();
		String lvtbTag = mNode.getTag();
		String source = mNode.getSourceString();
		List<PmlWNode> wNodes = mNode.getWs();
		boolean noSpaceAfter = wNodes != null && !wNodes.isEmpty() &&
				wNodes.get(wNodes.size() - 1).noSpaceAfter();

		String lastPart = source.substring(mForm.length()).trim();

		previousToken = makeNewToken(
				previousToken == null ? 1 : previousToken.idBegin + 1, 0,
				lvtbAId, mForm, mLemma, lvtbTag, true);
		if (paragraphChange) previousToken.addMisc(MiscKeys.NEW_PAR, MiscValues.YES); //previousToken.misc.add("NewPar=Yes");
		source = source.substring(0, mForm.length());
		if (!source.contains(" ")) previousToken.addMisc(MiscKeys.SPACE_AFTER, MiscValues.NO); //previousToken.misc.add("SpaceAfter=No");
		else if (source.trim().contains(" ")) StandardLogger.l.doInsentenceWarning(String.format(
				"Don't know how to add SpaceAfter for \"%s\"",
				lvtbAId));

		Token nextToken =  makeNewToken(
				previousToken.idBegin + 1, 0,
				lvtbAId, lastPart, null, "z_", false);
		nextToken.addMisc(MiscKeys.CORRECTION_TYPE, MiscValues.REM_PUNCT);//nextToken.misc.add("CorrectionType=RemovedPunctuation");
		if (noSpaceAfter || wNodes != null && wNodes.size() > 1 &&
				hasParaChange(wNodes.get(0), wNodes.get(wNodes.size() -1)))
			nextToken.addMisc(MiscKeys.SPACE_AFTER, MiscValues.NO); //nextToken.misc.add("SpaceAfter=No");
		nextToken.setHead(
				previousToken, UDv2Relations.PUNCT, true, true,
				params.NO_EDEP_DUPLICATES);
		return nextToken;
	}

	/**
	 * Helper method: Create necessary CoNLL table entries for node after which
	 * unnecessary punctuation is removed. The bad case - the undeleted wordform
	 * is not the prefix of the original string.
	 * @param aNode				PML A-level node for which CoNLL entry must be
	 *                          created
	 * @param previousToken		the token after which should follow all newmade
	 *                          tokens
	 * @param paragraphChange	paragraph border detected right before this
	 *                          token.
	 * @return last token made
	 */
	// TODO rewrite using w-level tokenization
	protected Token transfOnUglyPunctDel(
			PmlANode aNode, Token previousToken, boolean paragraphChange)
	{
		// TODO handle better spaces in the middle!
		String lvtbAId = aNode.getId();
		PmlMNode mNode = aNode.getM();
		String mForm = mNode.getForm();
		String mLemma = mNode.getLemma();
		String lvtbTag = mNode.getTag();
		String source = mNode.getSourceString();
		Set<LvtbFormChange> formChanges = mNode.getFormChange();
		if (formChanges == null) formChanges = new HashSet<>();
		List<PmlWNode> wNodes = mNode.getWs();
		boolean noSpaceAfter = wNodes != null && !wNodes.isEmpty() &&
				wNodes.get(wNodes.size() - 1).noSpaceAfter();

		Matcher m = Pattern.compile("(.*?)([-,.]+)").matcher(source);
		if (m.matches() && formChanges.contains(LvtbFormChange.SPELL)
				&& formChanges.size() == 3)
		// Together with spelling error
		{
			String firstPart = m.group(1).trim();
			String lastPart = m.group(2);
			previousToken = makeNewToken(
					previousToken == null ? 1 : previousToken.idBegin + 1, 0,
					lvtbAId, firstPart, mLemma, lvtbTag, true);
			previousToken.addMisc(MiscKeys.CORRECT_FORM, mForm); //previousToken.misc.add("CorrectedForm="+mForm);
			previousToken.addMisc(MiscKeys.CORRECTION_TYPE, MiscValues.SPELLING); //previousToken.misc.add("CorrectionType=Spelling");
			previousToken.feats.add(UDv2Feat.TYPO_YES);
			if (paragraphChange) previousToken.addMisc(MiscKeys.NEW_PAR, MiscValues.YES); //previousToken.misc.add("NewPar=Yes");
			if (!source.contains(" ")) previousToken.addMisc(MiscKeys.SPACE_AFTER, MiscValues.NO); //previousToken.misc.add("SpaceAfter=No");
			//else logger.doInsentenceWarning(String.format(
			//		"Don't know how to add SpaceAfter for \"%s\"", lvtbAId));

			Token nextToken = makeNewToken(
					previousToken.idBegin + 1, 0,
					lvtbAId, lastPart, null, "z_", false);
			nextToken.addMisc(MiscKeys.CORRECTION_TYPE, MiscValues.REM_PUNCT);//nextToken.misc.add("CorrectionType=RemovedPunctuation");
			if (wNodes != null && wNodes.size() > 1 &&
					hasParaChange(wNodes.get(0), wNodes.get(wNodes.size() -1)))
				nextToken.addMisc(MiscKeys.NEW_PAR, MiscValues.YES);//nextToken.misc.add("NewPar=Yes");
			if (noSpaceAfter) nextToken.addMisc(MiscKeys.SPACE_AFTER, MiscValues.NO);//nextToken.misc.add("SpaceAfter=No");
			nextToken.setHead(previousToken, UDv2Relations.PUNCT, true,
					true, params.NO_EDEP_DUPLICATES);
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
	/**
	 * Helper method: Create necessary CoNLL table entries for node which
	 * consists of multiple wrongly separated (with spaces!!!) tokens.
	 * @param aNode				PML A-level node for which CoNLL entry must be
	 *                          created
	 * @param previousToken		the token after which should follow all newmade
	 *                          tokens
	 * @param paragraphChange	paragraph border detected right before this
	 *                          token.
	 * @return last token made
	 */
	protected Token transfOnRemovedSpaces(
			PmlANode aNode, Token previousToken, boolean paragraphChange)
	{
		String lvtbAId = aNode.getId();
		PmlMNode mNode = aNode.getM();
		String mForm = mNode.getForm();
		String mLemma = mNode.getLemma();
		String lvtbTag = mNode.getTag();
		List<PmlWNode> wNodes = mNode.getWs();
		boolean noSpaceAfter = wNodes != null && !wNodes.isEmpty() &&
				wNodes.get(wNodes.size() - 1).noSpaceAfter();

		LinkedList<PmlWNode> unprocessedWs = new LinkedList<>();
		unprocessedWs.addAll(wNodes);
		LinkedList<PmlWNode> forNexTok = new LinkedList<>();
		while (!unprocessedWs.isEmpty() && unprocessedWs.peek().noSpaceAfter())
			forNexTok.push(unprocessedWs.remove());
		if (!unprocessedWs.isEmpty()) forNexTok.push(unprocessedWs.remove());

		previousToken = makeNewToken(
				previousToken == null ? 1 : previousToken.idBegin + 1, 0,
				lvtbAId,
				forNexTok.stream().map(PmlWNode::getToken).reduce((s1, s2) -> s1 + s2).get(),
				mLemma, lvtbTag, true);
		previousToken.addMisc(MiscKeys.CORRECT_FORM, mForm);//previousToken.misc.add("CorrectedForm="+mForm);
		previousToken.addMisc(MiscKeys.CORRECTION_TYPE, MiscValues.SPACING);//previousToken.misc.add("CorrectionType=Spacing,Spelling");
		previousToken.addMisc(MiscKeys.CORRECTION_TYPE, MiscValues.SPELLING);//previousToken.misc.add("CorrectionType=Spacing,Spelling");
		previousToken.feats.add(UDv2Feat.TYPO_YES);
		if (paragraphChange ||
				PmlIdUtils.isParaBorderBetween(forNexTok.peek().getId(), forNexTok.poll().getId()))
			previousToken.addMisc(MiscKeys.NEW_PAR, MiscValues.YES);//previousToken.misc.add("NewPar=Yes");

		while (!unprocessedWs.isEmpty())
		{
			forNexTok = new LinkedList<>();
			while (!unprocessedWs.isEmpty() && unprocessedWs.peek().noSpaceAfter())
				forNexTok.push(unprocessedWs.remove());
			if (!unprocessedWs.isEmpty()) forNexTok.push(unprocessedWs.remove());

			Token nextToken = makeNewToken(
					previousToken.idBegin + 1, 0, lvtbAId,
					forNexTok.stream().map(PmlWNode::getToken).reduce((s1, s2) -> s1 + s2).get(),
					null, "N/a", false);
			nextToken.addMisc(MiscKeys.CORRECTION_TYPE, MiscValues.SPACING);//nextToken.misc.add("CorrectionType=Spacing,Spelling");
			nextToken.addMisc(MiscKeys.CORRECTION_TYPE, MiscValues.SPELLING);//nextToken.misc.add("CorrectionType=Spacing,Spelling");
			nextToken.feats.add(UDv2Feat.TYPO_YES);
			if (PmlIdUtils.isParaBorderBetween(forNexTok.peek().getId(), forNexTok.poll().getId()))
				nextToken.addMisc(MiscKeys.NEW_PAR, MiscValues.YES);//nextToken.misc.add("NewPar=Yes");
			nextToken.setHead(previousToken, UDv2Relations.GOESWITH, true,
					true, params.NO_EDEP_DUPLICATES);
			previousToken.addMisc(MiscKeys.CORRECT_SPACE_AFTER, MiscValues.NO);
			previousToken = nextToken;
		}

		if (noSpaceAfter) previousToken.addMisc(MiscKeys.SPACE_AFTER, MiscValues.NO);//previousToken.misc.add("SpaceAfter=No");
		return previousToken;
	}

	/**
	 * Helper method: Create necessary CoNLL table entries for mNode where
	 * source text from w level is split with missing spaces?
	 * Is this used?
	 * @param aNode				PML A-level node for which CoNLL entry must be
	 *                          created
	 * @param previousToken		the token after which should follow all newmade
	 *                          tokens
	 * @param paragraphChange	paragraph border detected right before this
	 *                          token.
	 * @return last token made
	 */
	@Deprecated
	protected Token transfOnAddedSpaces(PmlANode aNode, Token previousToken, boolean paragraphChange)
	{
		String lvtbAId = aNode.getId();
		PmlMNode mNode = aNode.getM();
		String mForm = mNode.getForm();
		String mLemma = mNode.getLemma();
		String lvtbTag = mNode.getTag();
		String source = mNode.getSourceString();
		List<PmlWNode> wNodes = mNode.getWs();
		boolean noSpaceAfter = wNodes != null && !wNodes.isEmpty() &&
				wNodes.get(wNodes.size() - 1).noSpaceAfter();

		Token res = makeNewToken(
				previousToken == null ? 1 : previousToken.idBegin + 1, 0,
				lvtbAId, source, mLemma, lvtbTag, true);
		if (!mForm.equals(source))
		{
			res.addMisc(MiscKeys.CORRECT_FORM, mForm);//res.misc.add("CorrectedForm="+mForm);
			res.addMisc(MiscKeys.CORRECTION_TYPE, MiscValues.SPELLING);//res.misc.add("CorrectionType=Spelling");
			res.feats.add(UDv2Feat.TYPO_YES);
		}
		res.addMisc(MiscKeys.CORRECTION_TYPE, MiscValues.SPACING);
		if (noSpaceAfter)
		{
			res.addMisc(MiscKeys.SPACE_AFTER, MiscValues.NO);//res.misc.add("SpaceAfter=No");
			res.addMisc(MiscKeys.CORRECT_SPACE_AFTER, MiscValues.YES);
		}
		if (paragraphChange || wNodes != null && wNodes.size() > 1 &&
				hasParaChange(wNodes.get(0), wNodes.get(wNodes.size() -1)))
			res.addMisc(MiscKeys.NEW_PAR, MiscValues.YES);//res.misc.add("NewPar=Yes");

		return res;
	}

	/**
	 * Helper method: make new token and fill in the values.
	 * @param representative	should it be put in the PML-A -> CoNLL mapping
	 *                          of the sentence
	 * @return	newly made token
	 */
	protected Token makeNewToken(
			int tokenIdBegin, int tokenIdDecimal, String pmlId,
			String form, String lvtbLemma, String lvtbTag, boolean representative)
	{
		String uLemma = LemmaLogic.getULemma(lvtbLemma, lvtbTag);
		Token resTok = new Token(tokenIdBegin, form, uLemma,
				lvtbTag == null ? null : XPosLogic.getXpostag(lvtbTag));
		if (tokenIdDecimal > 0) resTok.idSub = tokenIdDecimal;
		if (params.ADD_NODE_IDS && pmlId != null && !pmlId.isEmpty())
		{
			resTok.addMisc(MiscKeys.LVTB_NODE_ID, pmlId);//resTok.misc.add("LvtbNodeId=" + pmlId);
			StandardLogger.l.addIdMapping(s.id, resTok.getFirstColumn(), pmlId);
		}
		if (resTok.xpostag != null)
		{
			resTok.upostag = UPosLogic.getUPosTag(resTok.form, lvtbLemma, resTok.xpostag);
			resTok.feats = FeatsLogic.getUFeats(resTok.form, lvtbLemma, resTok.xpostag);
		}
		s.conll.add(resTok);
		if (representative) s.pmlaToConll.put(pmlId, resTok);
		return resTok;
	}

	/**
	 * Helper method : checks if there are paragraph change between two w nodes
	 * and warns on all problems.
	 * @return 	either result of PmlIdUtils.isParaBorderBetween() or false in
	 * 			the case of any problem
	 */
	protected boolean hasParaChange(PmlWNode one, PmlWNode other)
	{
		String firstId = one.getId();
		String lastId = other.getId();
		Boolean innerParaChange = PmlIdUtils.isParaBorderBetween(
				firstId, lastId);
		if (innerParaChange == null)
		{
			StandardLogger.l.doInsentenceWarning(String.format(
					"Node id \"%s\" or \"%s\" does not match paragraph searching pattern!",
					firstId, lastId));
			return false;
		}
		return innerParaChange;
	}

	/**
	 * Currently extracted from pre-made CoNLL table.
	 * TODO: use PML tree instead?
	 */
	public void extractSendenceText()
	{
		s.text = s.conll.stream()
				.filter(t -> t.idSub <= 0)
				.map(t -> t.form + (t.checkMisc(MiscKeys.SPACE_AFTER, MiscValues.NO) ? "" : " "))
				.reduce((s1, s2) -> s1 + s2)
				.orElse("")
				.trim();
	}

	/**
	 * Update morphology after syntax transformation has been done.
	 */
	public void transformPostsyntMorpho()
	{
		for (Token t : s.conll)
		{
			t.upostag = UPosLogic.getPostsyntUPosTag(t);
			t.feats = FeatsLogic.getPostsyntUPosTag(t, s.conll);
		}
	}

	/**
	 * Return true if lemma matches one of for "standard" auxiliary verbs:
	 * būt, tikt, tapt, kļūt. For legacy reasons also mach negated lemmas.
	 */
	public static boolean isTrueAux (String lemma)
	{
		if (lemma == null) return false;
		return lemma.matches("(ne)?(būt|tikt|tapt|kļūt)");
	}
}
