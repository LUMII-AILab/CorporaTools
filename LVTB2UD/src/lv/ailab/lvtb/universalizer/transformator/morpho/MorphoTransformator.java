package lv.ailab.lvtb.universalizer.transformator.morpho;

import lv.ailab.lvtb.universalizer.conllu.Token;
import lv.ailab.lvtb.universalizer.conllu.UDv2Relations;
import lv.ailab.lvtb.universalizer.pml.LvtbFormChange;
import lv.ailab.lvtb.universalizer.pml.PmlANode;
import lv.ailab.lvtb.universalizer.pml.PmlMNode;
import lv.ailab.lvtb.universalizer.pml.utils.PmlANodeListUtils;
import lv.ailab.lvtb.universalizer.utils.Logger;
import lv.ailab.lvtb.universalizer.transformator.Sentence;
import lv.ailab.lvtb.universalizer.transformator.TransformationParams;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
		List<PmlANode> tokenNodes = s.pmlTree.getDescendantsWithOrdAndM();
		tokenNodes = PmlANodeListUtils.asOrderedList(tokenNodes);
		/*// Selects ord numbers from the tree.
		NodeList ordNodes = (NodeList) XPathEngine.get().evaluate(".//node[m.rf]/ord",
				s.pmlTree, XPathConstants.NODESET);
		List<Integer> ords = new ArrayList<>();
		for (int i = 0; i < ordNodes.getLength(); i++)
		{
			String ordText = ordNodes.item(i).getTextContent();
			if (ordText != null && ordText.trim().length() > 0)
				ords.add(Integer.parseInt(ordText.trim()));
		}
		ords = ords.stream().sorted().collect(Collectors.toList());//*/
		// Finds all nodes and makes CoNLL-U tokens from them.
		Token previousToken = null;
		String prevMId = null;
		int prevOrd = Integer.MIN_VALUE;
		//for (int currentOrd : ords)
		for (PmlANode current : tokenNodes)
		{
			PmlMNode currentM = current.getM();
			Integer currentOrd = current.getOrd();
			if (currentOrd == null || currentOrd < 1) continue;

			// Find the m node to be processed.
			//NodeList nodes = (NodeList)XPathEngine.get().evaluate(".//node[m.rf and ord=" + currentOrd + "]",
			//		s.pmlTree, XPathConstants.NODESET);
			if (prevOrd == currentOrd)
			{
				//warnOut.printf("\"%s\" has several nodes with ord \"%s\", only first used!\n",	s.id, currentOrd);
				logger.doInsentenceWarning(String.format(
						"\"%s\" has several nodes with ord \"%s\", arbitrary order used!", s.id, currentOrd));
			}

			// Determine, if paragraph has border before this token.
			boolean paragraphChange = false;
			String mId = currentM.getId();
			if (mId.matches("m-.*-p\\d+s\\d+w\\d+"))
				mId = mId.substring(mId.indexOf("-") + 1, mId.lastIndexOf("s"));
			//else warnOut.println("Node id \"" + mId + "\" does not match paragraph searching pattern!");
			else logger.doInsentenceWarning(String.format(
					"Node id \"%s\" does not match paragraph searching pattern!", mId));
			if (prevMId!= null && !prevMId.equals(mId))
				paragraphChange = true;

			// Make new token.
			previousToken = transformCurrentToken(current, previousToken, paragraphChange);

			prevMId = mId;
			prevOrd = currentOrd;
		}
	}

	protected Token makeNewToken(
			int tokenIdBegin, int tokenIdDecimal, String pmlId,
			String form, String lemma, String tag,
			PmlANode placementNode, List<String> miscFlags, boolean representative)
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
		boolean noSpaceAfter = mNode.getNoSpaceAfter();

		// Starting from UD v2 numbers and certain abbrieavations are allowed to
		// be tokens with spaces.
		if (!(mForm.contains(" ") || mLemma.contains(" ")) ||
				lvtbTag.matches("x[no].*") ||
				mForm.replace(" ", "").matches("u\\.t\\.jpr\\.|u\\.c\\.|u\\.tml\\.|v\\.tml\\."))
		{
			Set<LvtbFormChange> formChanges = mNode.getFormChange();
			if (formChanges == null) formChanges = new HashSet<>();
			String source = mNode.getSourceString();
			//Gadījumi:
			//--------- Nav jādala sīkāk
			// 1) viss sakrīt -- neko nedara
			// 2) tokens ir ielikts -- liek decimālo tokenu?
			// 3) tokenā ir tikai druķenes (spell), lieku atstarpju nav -- lieto
			//--------- Ir jādala sīkāk
			// 4) oriģinālā ir vairāk atstarpju kā beigās + druķenes? +
			// 5) tokenam ir pielīmēta pieturzīme
			//-------- Pārklājas ar nākamo tokenu? die

			// TODO: paragraph change in the middle of union morph!!!!!!!

			ArrayList<String> miscFlags = new ArrayList<>();
			if (noSpaceAfter) miscFlags.add("SpaceAfter=No");
			if (paragraphChange) miscFlags.add("NewPar=Yes");
			if (mForm.equals(source))
			// Form matches source - nothing to worry about.
			//	Add note to misc field if retokenization has been done.
			{
				if (formChanges.contains(LvtbFormChange.SPACING))
					miscFlags.add("CorrectionType=Spacing");
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
						formChanges.contains(LvtbFormChange.SPELL)))
			// Words that must be written together.
			{
				String[] parts = source.split("\\s+");
				miscFlags.add("CorrectedForm="+mForm);
				miscFlags.add("CorrectionType=Spacing,Spelling");
				previousToken = makeNewToken(
						previousToken == null ? 1 : previousToken.idBegin + 1,
						0,	lvtbAId, parts[1], mLemma, lvtbTag, aNode,
						miscFlags, true);
				for (int i = 1; i < parts.length; i++)
				{
					Token nextToken = makeNewToken(
							previousToken.idBegin + 1, 0,
							lvtbAId, parts[i], null, null, aNode, null, false);
					miscFlags.add("CorrectionType=Spacing,Spelling");
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
			// This obsolete piece of code dealt with "words with spaces" LVTB
			// used to have in previos versions. However, currently all such
			// cases should be removed from data.
			/*
			int baseOrd = aNode.getOrd();
			if (baseOrd < 1)
				throw new IllegalArgumentException(String.format(
						"Node %s has no ord value", aNode.getId()));

			String[] forms = mForm.split(" ");
			String[] lemmas = mLemma.split(" ");
			if (forms.length != lemmas.length)
				logger.doInsentenceWarning(String.format(
						"\"%s\" form \"%s\" do not match \"%s\" on spaces!", s.id, mForm, mLemma));

			// First one is different.
			Token firstTok = new Token(baseOrd + offset, forms[0],
					lemmas[0], getXpostag(lvtbTag, "_SPLIT_FIRST"));
			if (params.ADD_NODE_IDS && lvtbAId != null && !lvtbAId.isEmpty())
			{
				firstTok.misc.add("LvtbNodeId=" + lvtbAId);
				logger.addIdMapping(s.id, firstTok.getFirstColumn(), lvtbAId);
			}
			if (lvtbTag.matches("xf.*"))
			{
				//warnOut.printf("Processing unsplit xf \"%s\", check in treebank!", mForm);
				logger.doInsentenceWarning(String.format(
						"Processing unsplit xf \"%s\", check in treebank!", mForm));
				firstTok.upostag = PosLogic.getUPosTag(firstTok.form, firstTok.lemma, firstTok.xpostag, aNode, logger);
				firstTok.feats = FeatsLogic.getUFeats(firstTok.form, firstTok.lemma, firstTok.xpostag, aNode, logger);
			}
			else if (lvtbTag.matches("x[ux].*"))
			{
				firstTok.upostag = PosLogic.getUPosTag(firstTok.form, firstTok.lemma, firstTok.xpostag, aNode, logger);
				firstTok.feats = FeatsLogic.getUFeats(firstTok.form, firstTok.lemma, firstTok.xpostag, aNode, logger);
			}
			else
			{
				firstTok.upostag = UDv2PosTag.PART;
				firstTok.feats = FeatsLogic.getUFeats(firstTok.form, firstTok.lemma, "qs", aNode, logger);
			}
			if (paragraphChange) firstTok.misc.add("NewPar=Yes");
			s.conll.add(firstTok);
			s.pmlaToConll.put(aNode.getId(), firstTok);

			// The rest
			for (int i = 1; i < forms.length && i < lemmas.length; i++)
			{
				offset++;
				Token nextTok = new Token(baseOrd + offset, forms[i],
						lemmas[i], getXpostag(lvtbTag, "_SPLIT_PART"));
				if (params.ADD_NODE_IDS && lvtbAId != null && !lvtbAId.isEmpty())
				{
					nextTok.misc.add("LvtbNodeId=" + lvtbAId);
					logger.addIdMapping(s.id, nextTok.getFirstColumn(), lvtbAId);

				}
				if (i == forms.length - 1 || i == lemmas.length - 1 || lvtbTag.matches("x.*"))
				{
					nextTok.upostag = PosLogic.getUPosTag(nextTok.form, nextTok.lemma, nextTok.xpostag, aNode, logger);
					nextTok.feats = FeatsLogic.getUFeats(nextTok.form, nextTok.lemma, nextTok.xpostag, aNode, logger);
				}
				else
				{
					nextTok.upostag = UDv2PosTag.PART;
					nextTok.feats = FeatsLogic.getUFeats(nextTok.form, nextTok.lemma, "qs", aNode, logger);
				}
				nextTok.head = Tuple.of(firstTok.getFirstColumn(), firstTok);
				if ((i == forms.length - 1 || i == lemmas.length - 1) && noSpaceAfter)
					nextTok.misc.add("SpaceAfter=No");
				if (lvtbTag.matches("xf.*")) nextTok.deprel = UDv2Relations.FLAT_FOREIGN;
				else if (lvtbTag.matches("x[ux].*")) nextTok.deprel = UDv2Relations.GOESWITH;
				else nextTok.deprel = UDv2Relations.FIXED;
				s.conll.add(nextTok);
			} //*/
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
