package lv.ailab.lvtb.universalizer.pml.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for PML node IDs conforming the LVTB ID building conventions,
 * i.e. in one of the following forms:
 *	* a-sourceid-p\d+s\d+w\d+	for A level node ID
 *	* a-sourceid-p\d+s\d+		for A level sentence ID
 *	* m-sourceid-p\d+s\d+w\d+	for M level node ID
 *	* m-sourceid-p\d+s\d+		for M level sentence ID
 *	* w-sourceid-p\d+w\d+		for W level node ID
 * It is assumed that CoNLL paragraph ID will be
 *	* a-sourceid-p\d+
 */
public class PmlIdUtils
{
	/**
	 * Check if the paragraph numbers are different.
	 * FIXME: Source ID is not checked!
	 * @param firstId	ID of the first paragraph
	 * @param secondId	ID of the second paragraph
	 * @return	true if there should be a paragraph border between tokens with
	 * 			such IDs
	 */
	public static Boolean isParaBorderBetween(String firstId, String secondId)
	{
		if (firstId == null || secondId == null ||
				firstId.isEmpty() || secondId.isEmpty()) return null;
		Pattern p = Pattern.compile(".*?-p(\\d+)(m\\d+)?(w\\d+)$");
		Matcher firstMatcher = p.matcher(firstId);
		Matcher secondMatcher = p.matcher(secondId);
		if (!firstMatcher.matches() || !secondMatcher.matches()) return null;
		int firstParaNo = Integer.parseInt(firstMatcher.group(1));
		int secondParaNo = Integer.parseInt(secondMatcher.group(1));
		return firstParaNo != secondParaNo;
	}
}
