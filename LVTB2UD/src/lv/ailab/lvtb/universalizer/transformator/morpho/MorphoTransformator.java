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

/**
 * This is the part of transformation where tokens for CoNLL-U table is created
 * and information form morphology fields is obtained.
 * TODO: split pre-syntax and post-syntax moprhology.
 */
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

			// Make new token.
			previousToken = transformCurrentNode(current, previousToken, paragraphChange);

			if (currentWs != null && !currentWs.isEmpty())
				prevW = currentWs.get(currentWs.size() - 1);
			prevOrd = currentOrd;
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
				!mForm.replace(" ", "").matches("u\\.t\\.jpr\\.|u\\.c\\.|u\\.tml\\.|v\\.tml\\."))
		{
			throw new IllegalArgumentException(String.format(
					"Node \"%s\" with form \"%s\" and lemma \"%s\" contains spaces",
					lvtbAId, mForm, mLemma));
			// There was an  obsolete piece of code that separated in multiple
			// tokens with "words with spaces" LVTB used to have in previos
			// versions. However, currently all such cases should be removed
			// from data.
		}

		List<PmlWNode> wNodes = mNode.getWs();
		Set<LvtbFormChange> formChanges = mNode.getFormChange();
		if (formChanges == null) formChanges = new HashSet<>();
		String source = mNode.getSourceString();

		// Form matches source - nothing to worry about.
		if (mForm.equals(source))
			return transfOnMatch(aNode, previousToken, paragraphChange);
		// Form does not match source, but there is no form_change available
		else if (formChanges.isEmpty())
			return transfOnEmptyFormCh(aNode, previousToken, paragraphChange);

		// If only correction is spelling, add in misc correct form and process as normal
		else if (formChanges.contains(LvtbFormChange.SPELL) && formChanges.size() == 1)
			return transfOnSpellOnly(aNode, previousToken, paragraphChange);
		// Inserted punctuation
		else if (formChanges.contains(LvtbFormChange.INSERT) && formChanges.contains(LvtbFormChange.PUNCT)
				&& formChanges.size() == 2)
			return transfOnPunctInsert(aNode, previousToken, paragraphChange);
		// Some weard inserted thing, shouldn't be there
		else if (formChanges.contains(LvtbFormChange.INSERT))
			return transfOnOtherInsert(aNode, previousToken, paragraphChange);
		// Renmoved punctuation (good case - no other problems)
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
		if (noSpaceAfter) res.misc.add("SpaceAfter=No");
		if (paragraphChange || wNodes != null && wNodes.size() > 1 &&
				hasParaChange(wNodes.get(0), wNodes.get(wNodes.size() -1)))
			res.misc.add("NewPar=Yes");
		//	Add note to misc field if retokenization has been done.
		if (formChanges.contains(LvtbFormChange.SPACING))
			res.misc.add("CorrectionType=Spacing"); // This happens if word is split between rows.

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
		logger.doInsentenceWarning(String.format(
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
		res.misc.add("CorrectedForm="+mForm);
		if (noSpaceAfter) res.misc.add("SpaceAfter=No");
		if (paragraphChange || wNodes != null && wNodes.size() > 1 &&
				hasParaChange(wNodes.get(0), wNodes.get(wNodes.size() -1)))
			res.misc.add("NewPar=Yes");

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
	protected Token transfOnSpellOnly(PmlANode aNode, Token previousToken, boolean paragraphChange)
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
		res.misc.add("CorrectedForm="+mForm);
		res.misc.add("CorrectionType=Spelling");
		if (noSpaceAfter) res.misc.add("SpaceAfter=No");
		if (paragraphChange || wNodes != null && wNodes.size() > 1 &&
				hasParaChange(wNodes.get(0), wNodes.get(wNodes.size() -1)))
			res.misc.add("NewPar=Yes");

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
		String mLemma = mNode.getLemma();
		String lvtbTag = mNode.getTag();
		String source = mNode.getSourceString();
		List<PmlWNode> wNodes = mNode.getWs();

		if (wNodes != null && !wNodes.isEmpty() )
			logger.doInsentenceWarning(String.format(
					"Node \"%s\" has both w.rf and form change \"insert\"",
					lvtbAId));

		Token res =  makeNewToken(
				previousToken.idBegin, previousToken.idSub + 1,
				lvtbAId, source, mLemma, lvtbTag, true);
		res.misc.add("CorrectionType=InsertedPunctuation");
		if (paragraphChange) res.misc.add("NewPar=Yes"); // Chan this really be there?
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
	protected Token transfOnOtherInsert(
			PmlANode aNode, Token previousToken, boolean paragraphChange)
	{
		String lvtbAId = aNode.getId();
		PmlMNode mNode = aNode.getM();
		String mForm = mNode.getForm();
		String mLemma = mNode.getLemma();
		String lvtbTag = mNode.getTag();
		String source = mNode.getSourceString();
		List<PmlWNode> wNodes = mNode.getWs();

		if (wNodes != null && !wNodes.isEmpty() )
			logger.doInsentenceWarning(String.format(
					"Node \"%s\" has both w.rf and form change \"insert\"",
					lvtbAId));
		logger.doInsentenceWarning(String.format(
				"Node \"%s\" with form \"%s\" has form change \"insert\", but not \"punct\"",
				lvtbAId, mForm));

		Token res = makeNewToken(
				previousToken.idBegin, previousToken.idSub + 1,
				lvtbAId, source, mLemma, lvtbTag, true);
		res.misc.add("CorrectionType=Inserted");
		if (paragraphChange) res.misc.add("NewPar=Yes"); // Chan this really be there?
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

		String lastPart = source.substring(mForm.length());

		previousToken = makeNewToken(
				previousToken == null ? 1 : previousToken.idBegin + 1, 0,
				lvtbAId, mForm, mLemma, lvtbTag, true);
		if (paragraphChange) previousToken.misc.add("NewPar=Yes");
		if (!source.contains(" ")) previousToken.misc.add("SpaceAfter=No");
		else logger.doInsentenceWarning(String.format(
				"Don't know how to add SpaceAfter for \"%s\"",
				lvtbAId));

		Token nextToken =  makeNewToken(
				previousToken.idBegin + 1, 0,
				lvtbAId, lastPart, null, "z_", false);
		nextToken.misc.add("CorrectionType=RemovedPunctuation");
		if (noSpaceAfter || wNodes != null && wNodes.size() > 1 &&
				hasParaChange(wNodes.get(0), wNodes.get(wNodes.size() -1)))
			nextToken.misc.add("SpaceAfter=No");
		nextToken.setParentDeps(
				previousToken, UDv2Relations.PUNCT, true, true);
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

		Matcher m = Pattern.compile("(.*?)([-,.])").matcher(source);
		if (m.matches() && formChanges.contains(LvtbFormChange.SPELL)
				&& formChanges.size() == 3)
		// Together with spelling error
		{
			String firstPart = m.group(1);
			String lastPart = m.group(2);
			previousToken = makeNewToken(
					previousToken == null ? 1 : previousToken.idBegin + 1, 0,
					lvtbAId, firstPart, mLemma, lvtbTag, true);
			previousToken.misc.add("CorrectedForm="+mForm);
			previousToken.misc.add("CorrectionType=Spelling");
			if (paragraphChange) previousToken.misc.add("NewPar=Yes");
			if (!source.contains(" ")) previousToken.misc.add("SpaceAfter=No");
			else logger.doInsentenceWarning(String.format(
					"Don't know how to add SpaceAfter for \"%s\"",
					lvtbAId));

			Token nextToken = makeNewToken(
					previousToken.idBegin + 1, 0,
					lvtbAId, lastPart, null, "z_", false);
			nextToken.misc.add("CorrectionType=RemovedPunctuation");
			if (wNodes != null && wNodes.size() > 1 &&
					hasParaChange(wNodes.get(0), wNodes.get(wNodes.size() -1)))
				nextToken.misc.add("NewPar=Yes");
			if (noSpaceAfter) nextToken.misc.add("SpaceAfter=No");
			nextToken.setParentDeps(previousToken, UDv2Relations.PUNCT, true, true);
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

		previousToken = makeNewToken(
				previousToken == null ? 1 : previousToken.idBegin + 1, 0,
				lvtbAId,
				forNexTok.stream().map(PmlWNode::getToken).reduce((s1, s2) -> s1 + s2).get(),
				mLemma, lvtbTag, true);
		previousToken.misc.add("CorrectedForm="+mForm);
		previousToken.misc.add("CorrectionType=Spacing,Spelling");
		if (paragraphChange ||
				PmlIdUtils.isParaBorderBetween(forNexTok.peek().getId(), forNexTok.poll().getId()))
			previousToken.misc.add("NewPar=Yes");

		while (!unprocessedWs.isEmpty())
		{
			forNexTok = new LinkedList<>();
			while (!unprocessedWs.isEmpty() && unprocessedWs.peek().noSpaceAfter())
				forNexTok.push(unprocessedWs.remove());
			Token nextToken = makeNewToken(
					previousToken.idBegin + 1, 0, lvtbAId,
					forNexTok.stream().map(PmlWNode::getToken).reduce((s1, s2) -> s1 + s2).get(),
					null, "N/a", false);
			nextToken.misc.add("CorrectionType=Spacing,Spelling");
			if (PmlIdUtils.isParaBorderBetween(forNexTok.peek().getId(), forNexTok.poll().getId()))
				nextToken.misc.add("NewPar=Yes");
			nextToken.setParentDeps(previousToken, UDv2Relations.GOESWITH, true, true);
			previousToken = nextToken;
		}

		if (noSpaceAfter) previousToken.misc.add("SpaceAfter=No");
		return previousToken;
	}

	/**
	 * Helper method: make new token and fill in the values.
	 * @param representative	should it be put in the PML-A -> CoNLL mapping
	 *                          of the sentence
	 * @return	newly made token
	 */
	protected Token makeNewToken(
			int tokenIdBegin, int tokenIdDecimal, String pmlId,
			String form, String lemma, String tag, boolean representative)
	{
		Token resTok = new Token(tokenIdBegin, form, lemma,
				tag == null ? null : XPosLogic.getXpostag(tag));
		if (tokenIdDecimal > 0) resTok.idSub = tokenIdDecimal;
		if (params.ADD_NODE_IDS && pmlId != null && !pmlId.isEmpty())
		{
			resTok.misc.add("LvtbNodeId=" + pmlId);
			logger.addIdMapping(s.id, resTok.getFirstColumn(), pmlId);
		}
		if (resTok.xpostag != null)
		{
			resTok.upostag = UPosLogic.getUPosTag(resTok.form, resTok.lemma, resTok.xpostag, logger);
			resTok.feats = FeatsLogic.getUFeats(resTok.form, resTok.lemma, resTok.xpostag, logger);
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
			logger.doInsentenceWarning(String.format(
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
				.map(t -> t.form + ((t.misc != null && t.misc.contains("SpaceAfter=No")) ? "" : " "))
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
			t.upostag = UPosLogic.getPostsyntUPosTag(t, logger);
			t.feats = FeatsLogic.getPostsyntUPosTag(t, s.conll, logger);
		}
	}
}
