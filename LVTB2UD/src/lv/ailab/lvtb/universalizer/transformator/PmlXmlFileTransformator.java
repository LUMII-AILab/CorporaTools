package lv.ailab.lvtb.universalizer.transformator;

import lv.ailab.lvtb.universalizer.PmlXmlLoader;
import lv.ailab.lvtb.universalizer.pml.PmlANode;
import lv.ailab.lvtb.universalizer.pml.xmldom.XPathEngine;
import lv.ailab.lvtb.universalizer.pml.xmldom.XmlDomANode;
import lv.ailab.lvtb.universalizer.utils.Tuple;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transformation wrapper for a single file.
 */
// TODO: make omittWholeOnError class variable and do not process any more
// TODO  sentences after first error, if omittWholeOnError = true
public class PmlXmlFileTransformator
{
	public StringBuilder processed;
	protected TransformationParams params;
	public int omitted;
	private int added;
	public int all;
	public boolean hasAuto;
	public boolean hasFixme;
	public boolean hasCrashSent;
	public int depRoleBaseSum;
	public int depRoleBaseSent;
	public int depRoleEnhSum;
	public int depRoleEnhSent;

	public PmlXmlFileTransformator(TransformationParams params)
	{
		this.params = params;
		processed = new StringBuilder();
		omitted = 0;
		added = 0;
		all = 0;
		hasAuto = false;
		hasFixme = false;
		hasCrashSent = false;
		depRoleBaseSum = 0;
		depRoleBaseSent = 0;
		depRoleEnhSum = 0;
		depRoleEnhSent = 0;
	}

	/**
	 * Transform a single knitted LV TreeBank PML file to UD.
	 * @param inputPath   path to PML file
	 */
	public void readAndTransform(String inputPath)
			throws SAXException, ParserConfigurationException, XPathExpressionException, IOException
	{
		NodeList pmlTrees = PmlXmlLoader.getTrees(inputPath);
		System.out.printf("%s trees. ", pmlTrees.getLength());
		StandardLogger.l.printFoundTreesCount(pmlTrees.getLength());
		String paragraphId = "";
		// Print info in the file beginning.
		if (pmlTrees.getLength() > 0)
		{
			all = pmlTrees.getLength();
			String firstComment = XPathEngine.get().evaluate("./comment", pmlTrees.item(0));
			if (firstComment != null && firstComment.startsWith("AUTO"))
			{
				System.out.println("File starts with \"AUTO\" comment, everything is ommited!");
				StandardLogger.l.finishFileWithAUTO();
				omitted = pmlTrees.getLength();
				hasAuto = true;
				return;
			}
			// Print out information about the start of the new document
			processed.append("# newdoc");
			String firstSentId = XPathEngine.get().evaluate("./@id", pmlTrees.item(0));
			Matcher idMatcher = Pattern.compile("a-(.*-p\\d+)s\\d+").matcher(firstSentId);
			if (idMatcher.matches())
			{
				String dicIdForPrint = firstSentId.substring(firstSentId.indexOf("-") + 1,
						firstSentId.lastIndexOf("-"));
				//if (params.CHANGE_IDS != null && params.CHANGE_IDS)
				//	dicIdForPrint = dicIdForPrint.replace("LETA", "newswire");
				processed.append(" id = ");
				processed.append(dicIdForPrint);
				paragraphId = idMatcher.group(1);

			}
			processed.append("\n");
			// Print out information about the start of the first paragraph.
			processed.append("# newpar");
			if (!paragraphId.isEmpty())
			{
				processed.append(" id = ");
				processed.append(paragraphId);
			}
			processed.append("\n");
		}
		// Process all trees, one by one...
		for (int i = 0; i < pmlTrees.getLength(); i++)
		{
			// However, there is no use to continue processing, if in case of
			// an error the whole file will be ommited and there already has
			// been an error.
			if (params.OMIT_WHOLE_FILES && omitted > 0)
			{
				//omitted = pmlTrees.getLength() - added;
				omitted = pmlTrees.getLength();
				added = 0;
				break;
			}

			// A "FIXME" comment mean unfinished and thus untransformable sentence.
			String comment = XPathEngine.get().evaluate("./comment", pmlTrees.item(i));
			if (comment != null && comment.startsWith("FIXME"))
			{
				System.out.println("A sentence with \"FIXME\" ommited.");
				StandardLogger.l.finishSentenceWithFIXME();
				omitted++;
				hasFixme = true;
				continue;
			}

			// Try transforming tree.
			PmlANode pmlTree = new XmlDomANode(pmlTrees.item(i));
			String conllTree = null;
			try
			{
				Tuple<String, Tuple<Integer, Integer>> result =
						SentenceTransformEngine.treeToConll(pmlTree, params);
				conllTree = result.first;
				Tuple<Integer, Integer> depCounts = result.second;
				if (depCounts.first > 0) depRoleBaseSent++;
				depRoleBaseSum = depRoleBaseSum + depCounts.first;
				if (depCounts.second > 0) depRoleEnhSent++;
				depRoleEnhSum = depRoleEnhSum + depCounts.second;
			} catch (Exception e)
			{
				String treeId = pmlTree.getId();
				hasCrashSent = true;
				System.out.printf(
						"Transforming sentence %s completely failed! Check structure and try again.\n",
						treeId);
				e.printStackTrace();
				StandardLogger.l.finishSentenceWithException(treeId, e, false);
			}

			// Has a new paragraph started?
			if (i > 0)
			{
				Matcher idMatcher = Pattern.compile("a-(.*-p\\d+)s\\d+").matcher(pmlTree.getId());
				if (idMatcher.matches())
				{
					String nextParaID = idMatcher.group(1);
					if (!nextParaID.isEmpty() && !paragraphId.equals(nextParaID))
					{
						processed.append("# newpar id = ");
						processed.append(nextParaID);
						processed.append("\n");
						paragraphId = nextParaID;
					}
				}
			}

			// Store obtained results and update stats.
			if (conllTree != null)
			{
				processed.append(conllTree);
				added++;
			}
			else
			{
				omitted++;
				hasCrashSent = true;
			}
		}
	}

	/**
	 * Make new file and print out the transformation results. Do not make
	 * an empty file or a file containing no sentences
	 * @param conllOut    	path for the new result file
	 * @return	if the file was actually written
	 */
	public boolean writeResult(String conllOut)
	throws IOException
	{
		if (omitted + added != all)
		{
			throw new IllegalStateException(String.format(
					"Algorithmic error! Omitted Trees (%s) + Good Trees (%s) != All Trees(%s)",
					omitted, added, all));
		}
		if (params.OMIT_WHOLE_FILES && omitted > 0 || all - omitted < 1)
		{
			System.out.println("Finished - nothing to write.");
			StandardLogger.l.finishFileNormal(true);
			return false;
		}
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(conllOut), "UTF8"));
		out.write(processed.toString());
		out.flush();
		out.close();
		System.out.println("Finished.");
		StandardLogger.l.finishFileNormal(false);
		return true;
	}
}
